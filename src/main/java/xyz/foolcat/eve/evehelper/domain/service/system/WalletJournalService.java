package xyz.foolcat.eve.evehelper.domain.service.system;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.WalletJournal;
import xyz.foolcat.eve.evehelper.domain.model.vo.TaxReturnResult;
import xyz.foolcat.eve.evehelper.domain.repository.system.WalletJournalRepository;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.domain.util.AuthorizeUtil;
import xyz.foolcat.eve.evehelper.domain.util.UserUtil;
import xyz.foolcat.eve.evehelper.shared.kernel.constants.GlobalConstants;

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
@Transactional(rollbackFor = RuntimeException.class)
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
     */
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
         */
        List<WalletJournal> walletJournals = Stream.iterate(1, i -> i + 1).limit(maxPage)
                .map(i -> esiApiService.queryCharacterWalletJournal(cId, i, accessToken)
                        .collectList().block())
                .sequential().filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        walletJournalRepository.saveOrUpdateBatch(walletJournals);
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


