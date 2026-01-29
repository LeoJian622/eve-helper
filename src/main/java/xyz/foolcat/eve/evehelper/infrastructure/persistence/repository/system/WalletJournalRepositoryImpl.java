package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.application.assembler.system.WalletJournalAssembler;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.WalletJournal;
import xyz.foolcat.eve.evehelper.domain.repository.system.WalletJournalRepository;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.WalletJournalPO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.WalletJournalMapper;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author Leojan
 */
@Repository
@RequiredArgsConstructor
public class WalletJournalRepositoryImpl implements WalletJournalRepository {

    private final WalletJournalMapper walletJournalMapper;
    private final WalletJournalAssembler walletJournalAssembler;

    @Override
    public int updateBatch(List<WalletJournal> list) {
        return walletJournalMapper.updateBatch(walletJournalAssembler.domain2Po(list));
    }

    @Override
    public int updateBatchSelective(List<WalletJournal> list) {
        return walletJournalMapper.updateBatchSelective(walletJournalAssembler.domain2Po(list));
    }

    @Override
    public int batchInsert(List<WalletJournal> list) {
        return walletJournalMapper.batchInsert(walletJournalAssembler.domain2Po(list));
    }

    @Override
    public int insertOrUpdate(WalletJournal record) {
        return walletJournalMapper.insertOrUpdate(walletJournalAssembler.domain2Po(record));
    }

    @Override
    public int insertOrUpdateSelective(WalletJournal record) {
        return walletJournalMapper.insertOrUpdateSelective(walletJournalAssembler.domain2Po(record));
    }

    @Override
    public void saveOrUpdateBatch(List<WalletJournal> walletJournals) {

    }

    /**
     * 根据时间跨度计算角色某些类型的总税金额
     *
     * 税类型可以又 bounty_prizes，ess_escrow_transfer，corporate_reward_payout 等
     *
     * @param start 起始时间
     * @param end   结束时间
     * @param refType 记录类型集合
     * @return 角色->总额的key-value
     */
    @Override
    public List<Map<String, Object>> selectMapByDatetime(Date start, Date end, List<String> refType) {
        return walletJournalMapper.selectMaps(new QueryWrapper<WalletJournalPO>()
                .select("`character` as name,sum(amount) as amount")
                .lambda()
                .and(item -> item.in(WalletJournalPO::getRefType, refType)
                .between(WalletJournalPO::getDate, start, end)
                .groupBy(WalletJournalPO::getCharacter)));
    }
} 