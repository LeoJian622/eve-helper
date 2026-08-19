package xyz.foolcat.eve.evehelper.domain.service.system;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.WalletJournal;
import xyz.foolcat.eve.evehelper.domain.model.vo.TaxReturnResult;
import xyz.foolcat.eve.evehelper.domain.repository.system.WalletJournalRepository;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.domain.util.AuthorizeUtil;
import xyz.foolcat.eve.evehelper.domain.util.UserUtil;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.EsiException;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.ResultCode;
import xyz.foolcat.eve.evehelper.shared.kernel.constants.GlobalConstants;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Leojan
 */
@Service
@Slf4j
/**
 * 事务说明:类级 @Transactional 覆盖单命令方法;syncCorporationJournal 以
 * {@code @Transactional(propagation = Propagation.NOT_SUPPORTED)} 覆盖,实现分账级
 * 提交隔离(每 division 独立 auto-commit,失败不回滚已成功分账)。
 */
@Transactional
@RequiredArgsConstructor
public class WalletJournalService {

    private final EsiGateway esiApiService;

    private final AuthorizeUtil authorizeUtil;

    private final WalletJournalRepository walletJournalRepository;

    public int batchInsert(List<WalletJournal> list) {
        return walletJournalRepository.batchInsert(list);
    }

    public boolean insertOrUpdate(WalletJournal record) {
        return walletJournalRepository.insertOrUpdate(record);
    }

    public int insertOrUpdateSelective(WalletJournal record) {
        return walletJournalRepository.insertOrUpdateSelective(record);
    }

    public int updateBatch(List<WalletJournal> list) {
        return walletJournalRepository.updateBatch(list);
    }

    public int updateBatchSelective(List<WalletJournal> list) {
        return walletJournalRepository.updateBatchSelective(list);
    }

    /**
     * ESI获取的建筑列表批量获取数据
     *
     * @param cId 角色ID
     * @deprecated 已被 {@link #syncCorporationJournal(Integer)}(division 1-7) 和
     *             {@link #syncCharacterJournal(Integer)}(division=0) 取代。
     */
    @Deprecated
    public void batchInsertOrUpdateFromEsi(Integer cId) throws ParseException {
        /*
          获取游戏人物信息及授权
          请求路径有 SecurityContext 用 authorize(校验归属);
          定时任务等无上下文路径用 authorizeInternal(显式系统身份),二者均 fail-closed。
         */
        EveAccount eveAccount;
        Integer currentUserId = UserUtil.getUserId();
        if (currentUserId != null && currentUserId > 0) {
            eveAccount = authorizeUtil.authorize(cId);
        } else {
            eveAccount = authorizeUtil.authorizeInternal(GlobalConstants.SYSTEM_USER_ID, cId);
        }
        String accessToken = esiApiService.getAccessToken(cId, eveAccount.getUserId());

        /*
          获取总页数
         */
        Integer maxPage = esiApiService.queryCorporationWalletJournalMaxPage(eveAccount.getCorpId(), 1, accessToken);

        /*
         * 获取钱包记录
         */
        List<WalletJournal> walletJournals = Stream.iterate(1, i -> i + 1).limit(maxPage)
                .map(i -> esiApiService.queryCorporationWalletJournal(eveAccount.getCorpId(), 1, i, accessToken)
                        .collectList().block())
                .sequential().filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        walletJournalRepository.saveOrUpdateBatch(walletJournals);
    }

    /**
     * ESI获取的人物钱包流水批量同步(人物端点)。
     *
     * <p>严格仿照 {@link #batchInsertOrUpdateFromEsi}:相同 authorize/authorizeInternal 归属逻辑,
     * 但改走人物钱包 journal 端点(单分账,无 division 参数,ownerId=characterId),幂等 upsert 落库。</p>
     * <p>按 TDD 契约,原实现已删除重建。</p>
     *
     * @param cId 人物ID
     */
    public void syncCharacterJournal(Integer cId) throws ParseException {
        /*
          获取游戏人物信息及授权
          请求路径有 SecurityContext 用 authorize(校验归属);
          定时任务等无上下文路径用 authorizeInternal(显式系统身份),二者均 fail-closed。
         */
        EveAccount eveAccount;
        Integer currentUserId = UserUtil.getUserId();
        if (currentUserId != null && currentUserId > 0) {
            eveAccount = authorizeUtil.authorize(cId);
        } else {
            eveAccount = authorizeUtil.authorizeInternal(GlobalConstants.SYSTEM_USER_ID, cId);
        }
        String accessToken = esiApiService.getAccessToken(cId, eveAccount.getUserId());

        /*
          获取总页数
         */
        Integer maxPage = esiApiService.queryCharacterWalletJournalMaxPage(cId, accessToken);

        /*
         * 获取钱包记录
         * maxPage 为 ESI 返回值,可能为 null(无数据/错误路径),此时按 0 页处理返回空,避免 limit(null) 抛 NPE
         */
        int pages = maxPage == null ? 0 : maxPage;
        List<WalletJournal> walletJournals = Stream.iterate(1, i -> i + 1).limit(pages)
                .map(i -> esiApiService.queryCharacterWalletJournal(cId, i, accessToken)
                        .collectList().block())
                .sequential().filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        // 人物钱包为单分账,归一分账号固定为 0,并回填 ownerId(= 人物ID)
        walletJournals.forEach(j -> {
            j.setOwnerId(cId.longValue());
            j.setDivision(0);
        });
        if (!walletJournals.isEmpty()) {
            walletJournalRepository.saveOrUpdateBatch(walletJournals);
        }
        log.info("钱包流水人物同步完成 cId={} 条数={}", cId, walletJournals.size());
    }

    /**
     * 军团钱包分账区间 1..7。
     */
    private static final int MIN_CORP_DIVISION = 1;
    private static final int MAX_CORP_DIVISION = 7;

    /**
     * 同步军团钱包流水(1..7 个分账)。
     *
     * <p>逐个 division 独立「maxPage→翻页→回填→保存」为一独立提交单元,任一 division 失败仅记录失败、
     * 不影响其它已成功 division 落库。存在失败分账时汇总抛
     * {@link EveHelperException}(含失败 division 明细),但已成功数据已在 DB,不回滚。</p>
     *
     * <p><b>事务边界</b>:本方法<b>不</b>标注 @Transactional —— 军团 7 个 division 各自
     * 「拉取+回填+saveOrUpdateBatch」为一个独立提交单元,失败隔离不整体回滚。</p>
     *
     * @param corpId 军团ID
     * @throws ParseException JWT 解析失败
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void syncCorporationJournal(Integer characterId) throws ParseException {
        EveAccount eveAccount;
        Integer currentUserId = UserUtil.getUserId();
        if (currentUserId != null && currentUserId > 0) {
            eveAccount = authorizeUtil.authorize(characterId);
        } else {
            eveAccount = authorizeUtil.authorizeInternal(GlobalConstants.SYSTEM_USER_ID, characterId);
        }
        // 目标军团ID恒从已授权角色行派生,入参恒为角色ID,不与军团ID复用(消除双重语义)
        Integer corpId = eveAccount.getCorpId();
        if (corpId == null) {
            log.warn("钱包流水军团同步:角色 {} 无关联军团,映射 ESI 403 权限不足", characterId);
            throw new EsiException(ResultCode.ESI_AUTH_PERMISSION_LOW);
        }
        String accessToken = esiApiService.getAccessToken(characterId, eveAccount.getUserId());

        List<Integer> failedDivisions = new ArrayList<>();
        for (int division = MIN_CORP_DIVISION; division <= MAX_CORP_DIVISION; division++) {
            final int currentDivision = division;
            try {
                Integer maxPage = esiApiService.queryCorporationWalletJournalMaxPage(corpId, currentDivision, accessToken);
                int pages = maxPage == null ? 0 : maxPage;
                List<WalletJournal> walletJournals = Stream.iterate(1, i -> i + 1).limit(pages)
                        .map(i -> esiApiService.queryCorporationWalletJournal(corpId, currentDivision, i, accessToken)
                                .collectList().block())
                        .sequential().filter(Objects::nonNull)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList());
                // 回填 ownerId + division
                walletJournals.forEach(j -> {
                    j.setOwnerId(corpId.longValue());
                    j.setDivision(currentDivision);
                });
                if (!walletJournals.isEmpty()) {
                    walletJournalRepository.saveOrUpdateBatch(walletJournals);
                }
                log.info("钱包流水军团同步完成 characterId={} corpId={} division={} 条数={}", characterId, corpId, currentDivision, walletJournals.size());
            } catch (RuntimeException e) {
                // 失败隔离:记录失败分账与原因,继续下一 division
                failedDivisions.add(currentDivision);
                log.warn("钱包流水军团分账同步失败 characterId={} corpId={} division={}: {}", characterId, corpId, currentDivision, e.getMessage());
            }
        }

        if (!failedDivisions.isEmpty()) {
            throw new EveHelperException("军团钱包流水部分分账同步失败,失败 division=" + failedDivisions);
        }
    }

    /**
     * 计算退税
     *
     * @param normalTax 正常军团税
     * @param nowTax    当前军团税
     * @param dateTime  月份 yyyy-MM
     */
    public List<TaxReturnResult> countBoundsReturn(String normalTax, String nowTax, String dateTime) throws ParseException {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
        Date start = simpleDateFormat.parse(dateTime + "01");
        calendar.setTime(start);
        calendar.add(Calendar.MONTH,1);
        Date end = calendar.getTime();
        List<Map<String, Object>> sumList = walletJournalRepository.selectMapByDatetime(start,end,List.of("bounty_prizes","ess_escrow_transfer","corporate_reward_payout"));
        return sumList.stream().map(item -> {
            BigDecimal amount = new BigDecimal(item.get("amount").toString());
            // 退税公式说明：
            // 军团根据当前税率 nowTax 已从玩家收入中扣除的 EV 税（amount 为按当前税率扣除后的实发金额，
            // 由于 EV 税按现税率计，amount / (nowTax * 1e8) 反推出未扣税前的税前基础金额）；
            // 退税额 = 税前基础金额 × (1 - normalTax)，即把玩家应缴的正常军团税 normalTax 之外多扣的部分退还。
            BigDecimal multiply = amount.divide(new BigDecimal(nowTax).multiply(new BigDecimal("100000000")),0, RoundingMode.HALF_DOWN).multiply(BigDecimal.ONE.subtract(new BigDecimal(normalTax)));
            return new TaxReturnResult(item.get("name").toString(), multiply.doubleValue());
        }).sorted(Comparator.comparing(TaxReturnResult::amount).reversed()).collect(Collectors.toList());
    }

}


