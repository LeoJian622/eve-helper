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
         */
        EveAccount eveAccount = authorizeUtil.authorize(cId);
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
            BigDecimal multiply = amount.divide(new BigDecimal(nowTax).multiply(new BigDecimal("100000000")),0, RoundingMode.HALF_DOWN).multiply(BigDecimal.ONE.subtract(new BigDecimal(normalTax)));
            return new TaxReturnResult(item.get("name").toString(), multiply.doubleValue());
        }).sorted(Comparator.comparing(TaxReturnResult::amount).reversed()).collect(Collectors.toList());
    }

}


