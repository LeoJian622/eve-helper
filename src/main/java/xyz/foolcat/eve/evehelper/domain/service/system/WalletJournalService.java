package xyz.foolcat.eve.evehelper.domain.service.system;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.application.assembler.esi.WalletJournalConverter;
import xyz.foolcat.eve.evehelper.application.dto.response.TaxReturnDTO;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.WalletJournal;
import xyz.foolcat.eve.evehelper.domain.service.esi.EsiApiService;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.EsiClient;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.api.WalletApi;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.WalletJournalMapper;
import xyz.foolcat.eve.evehelper.shared.util.AuthorizeUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
@Transactional(rollbackFor = RuntimeException.class)
@RequiredArgsConstructor
public class WalletJournalService extends ServiceImpl<WalletJournalMapper, WalletJournal> {

    private final EsiApiService esiApiService;

    private final WalletApi walletApi;

    private final WalletJournalConverter walletJournalConverter;

    private final AuthorizeUtil authorizeUtil;

    public int batchInsert(List<WalletJournal> list) {
        return baseMapper.batchInsert(list);
    }

    public int insertOrUpdate(WalletJournal record) {
        return baseMapper.insertOrUpdate(record);
    }

    public int insertOrUpdateSelective(WalletJournal record) {
        return baseMapper.insertOrUpdateSelective(record);
    }

    public int updateBatch(List<WalletJournal> list) {
        return baseMapper.updateBatch(list);
    }

    public int updateBatchSelective(List<WalletJournal> list) {
        return baseMapper.updateBatchSelective(list);
    }

    /**
     * 正则表达式
     */
    private final Pattern essType = Pattern.compile("赏金池转移给(.*)");

    private final Pattern bountyType = Pattern.compile("(.*)因在");

    private final Pattern dedType = Pattern.compile(".*对(.*)的服务给予报酬。");

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
        Integer maxPage = walletApi.queryCorporationWalletJournalMaxPage(eveAccount.getCorpId(), 1, EsiClient.SERENITY, accessToken);

        /*
         * 获取钱包记录
         */
        List<WalletJournal> walletJournals = Stream.iterate(1, i -> i + 1).limit(maxPage)
                .map(i -> walletApi.queryCorporationWalletJournal(eveAccount.getCorpId(), 1, EsiClient.SERENITY, i, accessToken)
                        .collectList().block())
                .sequential().filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .map(wallet -> {
                    String character = "";
                    if ("bounty_prizes".equals(wallet.getRefType())) {
                        Matcher matcher = bountyType.matcher(wallet.getDescription());
                        if (matcher.find()) {
                            character = matcher.group(1);
                        }
                    }
                    if ("ess_escrow_transfer".equals(wallet.getRefType())) {
                        Matcher matcher = essType.matcher(wallet.getDescription());
                        if (matcher.find()) {
                            character = matcher.group(1);
                        }
                    }
                    if ("corporate_reward_payout".equals(wallet.getRefType())) {
                        Matcher matcher = dedType.matcher(wallet.getDescription());
                        if (matcher.find()) {
                            character = matcher.group(1);
                        }
                    }
                    return walletJournalConverter.conver(wallet, eveAccount.getCorpId(), character);
                })
                .collect(Collectors.toList());
        saveOrUpdateBatch(walletJournals);
    }

    /**
     * 计算退税
     *
     * @param normalTax 正常军团税
     * @param nowTax    当前军团税
     * @param dateTime  月份 yyyy-MM
     */
    public List<TaxReturnDTO> countBoundsReturn(String normalTax, String nowTax, String dateTime) throws ParseException {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
        Date start = simpleDateFormat.parse(dateTime + "01");
        calendar.setTime(start);
        calendar.add(Calendar.MONTH,1);
        Date end = calendar.getTime();
        List<Map<String, Object>> sumList = listMaps(new QueryWrapper<WalletJournal>()
                .select("`character` as name,sum(amount) as amount")
                .lambda()
                .and(item -> item.eq(WalletJournal::getRefType, "bounty_prizes").or().eq(WalletJournal::getRefType, "ess_escrow_transfer").or().eq(WalletJournal::getRefType, "corporate_reward_payout"))
                .between(WalletJournal::getDate, start, end)
                .groupBy(WalletJournal::getCharacter)
        );
        return sumList.stream().map(item -> {
            TaxReturnDTO taxReturnDTO = new TaxReturnDTO();
            taxReturnDTO.setName(item.get("name").toString());
            BigDecimal amount = new BigDecimal(item.get("amount").toString());
            BigDecimal multiply = amount.divide(new BigDecimal(nowTax).multiply(new BigDecimal("100000000")),0, RoundingMode.HALF_DOWN).multiply(BigDecimal.ONE.subtract(new BigDecimal(normalTax)));
            taxReturnDTO.setAmount(multiply.doubleValue());
            return taxReturnDTO;
        }).sorted(Comparator.comparing(TaxReturnDTO::getAmount).reversed()).collect(Collectors.toList());
    }

}


