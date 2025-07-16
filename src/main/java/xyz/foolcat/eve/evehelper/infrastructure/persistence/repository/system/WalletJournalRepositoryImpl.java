package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.domain.repository.system.WalletJournalRepository;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.WalletJournalPO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.WalletJournalMapper;
import java.util.List;

@Repository
public class WalletJournalRepositoryImpl implements WalletJournalRepository {
    @Autowired
    private WalletJournalMapper walletJournalMapper;

    @Override
    public int updateBatch(List<WalletJournalPO> list) {
        return walletJournalMapper.updateBatch(list);
    }

    @Override
    public int updateBatchSelective(List<WalletJournalPO> list) {
        return walletJournalMapper.updateBatchSelective(list);
    }

    @Override
    public int batchInsert(List<WalletJournalPO> list) {
        return walletJournalMapper.batchInsert(list);
    }

    @Override
    public int insertOrUpdate(WalletJournalPO record) {
        return walletJournalMapper.insertOrUpdate(record);
    }

    @Override
    public int insertOrUpdateSelective(WalletJournalPO record) {
        return walletJournalMapper.insertOrUpdateSelective(record);
    }
} 