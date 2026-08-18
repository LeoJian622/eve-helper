package xyz.foolcat.eve.evehelper.domain.service.system;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.WalletTransaction;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.domain.repository.system.WalletTransactionRepository;
import xyz.foolcat.eve.evehelper.domain.util.AuthorizeUtil;
import xyz.foolcat.eve.evehelper.domain.util.UserUtil;
import xyz.foolcat.eve.evehelper.shared.kernel.constants.GlobalConstants;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 钱包交易(wallet_transaction)领域服务。
 *
 * <p>负责从 ESI 同步钱包交易(人物单分账 / 军团 1..7 分账),核心为 from_id 游标翻页
 * (plan D2)、归属字段回填、以及军团分账级失败隔离(plan D3)。</p>
 *
 * <p><b>事务边界</b>:本服务<b>不</b>标注 @Transactional —— 军团 7 个 division 各自
 * 「拉取+回填+saveOrUpdateBatch」为一个独立提交单元(MyBatis-Plus 单操作默认自动提交),
 * 任一 division 失败不影响其它已成功 division 的落库(失败隔离,不整体回滚)。</p>
 *
 * @author Leojan
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WalletTransactionService {

    /**
     * 游标翻页上限(防死循环):单次同步最多拉取页数。
     */
    private static final int MAX_CURSOR_PAGES = 500;

    private static final String OWNER_TYPE_CHARACTER = "character";
    private static final String OWNER_TYPE_CORPORATION = "corporation";

    /**
     * 人物钱包为单分账,归一分账号固定为 0。
     */
    private static final int CHARACTER_DIVISION = 0;

    /**
     * 军团钱包分账区间 1..7。
     */
    private static final int MIN_CORP_DIVISION = 1;
    private static final int MAX_CORP_DIVISION = 7;

    private final EsiGateway esiApiService;

    private final AuthorizeUtil authorizeUtil;

    private final WalletTransactionRepository walletTransactionRepository;

    /* ────────────────────────── 人物同步 ────────────────────────── */

    /**
     * 同步人物钱包交易(单分账)。
     *
     * <p>获授权后从最新拉取全部交易,回填 ownerType=character / ownerId=cId / division=0,
     * 幂等 upsert 落库。人物同步任一页失败即上抛(无分账隔离需求)。</p>
     *
     * @param cId 人物ID
     * @throws java.text.ParseException JWT 解析失败
     */
    public void syncCharacterTransactions(Integer cId) throws java.text.ParseException {
        EveAccount eveAccount = authorizeAccount(cId);
        String accessToken = esiApiService.getAccessToken(cId, eveAccount.getUserId());

        List<WalletTransaction> transactions = pullTransactions(
                fromId -> esiApiService.queryCharacterWalletTransactions(cId, fromId, accessToken));
        backfillOwner(transactions, OWNER_TYPE_CHARACTER, cId.longValue(), CHARACTER_DIVISION);

        // 空页终止:无可写数据时不触发保存
        if (!transactions.isEmpty()) {
            walletTransactionRepository.saveOrUpdateBatch(transactions);
        }
        log.info("钱包交易人物同步完成 cId={} 条数={}", cId, transactions.size());
    }

    /* ────────────────────────── 军团同步 ────────────────────────── */

    /**
     * 同步军团钱包交易(1..7 个分账)。
     *
     * <p>逐个 division 独立「拉取+回填+保存」为一独立提交单元,任一 division 失败仅记录失败、
     * 不影响其它已成功 division 落库。返回每 division 成败记录;存在失败分账时汇总抛
     * {@link EveHelperException}(含失败 division 明细),但已成功数据已在 DB,不回滚。</p>
     *
     * @param corpId 军团ID
     * @return 每 division 同步成败记录(顺序 1..7),仅全部成功时正常返回
     * @throws java.text.ParseException JWT 解析失败
     */
    public Map<Integer, Boolean> syncCorporationTransactions(Integer corpId) throws java.text.ParseException {
        EveAccount eveAccount = authorizeAccount(corpId);
        String accessToken = esiApiService.getAccessToken(corpId, eveAccount.getUserId());

        Map<Integer, Boolean> results = new LinkedHashMap<>();
        List<Integer> failedDivisions = new ArrayList<>();
        for (int division = MIN_CORP_DIVISION; division <= MAX_CORP_DIVISION; division++) {
            final int currentDivision = division;
            try {
                List<WalletTransaction> transactions = pullTransactions(
                        fromId -> esiApiService.queryCorporationWalletTransactions(corpId, currentDivision, fromId, accessToken));
                backfillOwner(transactions, OWNER_TYPE_CORPORATION, corpId.longValue(), currentDivision);
                // 空页(无交易)也视为本分账成功;非空才触发保存
                if (!transactions.isEmpty()) {
                    walletTransactionRepository.saveOrUpdateBatch(transactions);
                }
                results.put(currentDivision, Boolean.TRUE);
                log.info("钱包交易军团同步完成 corpId={} division={} 条数={}", corpId, division, transactions.size());
            } catch (RuntimeException e) {
                // 失败隔离:记录失败分账与原因,继续下一 division
                failedDivisions.add(division);
                results.put(division, Boolean.FALSE);
                log.warn("钱包交易军团分账同步失败 corpId={} division={}: {}", corpId, division, e.getMessage());
            }
        }

        if (!failedDivisions.isEmpty()) {
            throw new EveHelperException("军团钱包交易部分分账同步失败,失败 division=" + failedDivisions);
        }
        return results;
    }

    /* ────────────────────────── 内部方法 ────────────────────────── */

    /**
     * 归属解析:请求上下文(有登录主体)→ {@link AuthorizeUtil#authorize},否则
     * 显式系统身份 → {@link AuthorizeUtil#authorizeInternal}。严格仿 WalletJournalService。
     */
    private EveAccount authorizeAccount(Integer cId) {
        Integer currentUserId = UserUtil.getUserId();
        if (currentUserId != null && currentUserId > 0) {
            return authorizeUtil.authorize(cId);
        }
        return authorizeUtil.authorizeInternal(GlobalConstants.SYSTEM_USER_ID, cId);
    }

    /**
     * from_id 游标翻页(plan D2),人物/军团共用。
     *
     * <p>ESI 按 transaction_id 倒序返回(最新在前):首页传 null 拉最新,随后以
     * <b>每页末条(最小)transactionId</b> 作为下一 fromId 继续拉更旧记录,直至空页终止。
     * 以 {@link #MAX_CURSOR_PAGES} 限制页数上限防死循环。</p>
     *
     * @param pageFetcher 单页拉取函数(fromId → 一页交易)
     * @return 已拉全部交易,保留顺序(最新→最旧)
     */
    private List<WalletTransaction> pullTransactions(Function<Long, Flux<WalletTransaction>> pageFetcher) {
        List<WalletTransaction> all = new ArrayList<>();
        Long fromId = null;
        for (int page = 0; page < MAX_CURSOR_PAGES; page++) {
            List<WalletTransaction> pageList = pageFetcher.apply(fromId).collectList().block();
            if (pageList == null || pageList.isEmpty()) {
                break;
            }
            all.addAll(pageList);
            // 末条即该页最小 transactionId,作为下一页游标
            fromId = pageList.get(pageList.size() - 1).getTransactionId();
        }
        log.debug("from_id 游标翻页结束,共拉取 {} 条", all.size());
        return all;
    }

    /**
     * 归属字段回填(ESI 响应本身不含归属信息,由本服务回填)。
     */
    private void backfillOwner(List<WalletTransaction> transactions, String ownerType, Long ownerId, Integer division) {
        for (WalletTransaction tx : transactions) {
            tx.setOwnerType(ownerType);
            tx.setOwnerId(ownerId);
            tx.setDivision(division);
        }
    }
}