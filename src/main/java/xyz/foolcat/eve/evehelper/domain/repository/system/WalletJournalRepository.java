package xyz.foolcat.eve.evehelper.domain.repository.system;

import xyz.foolcat.eve.evehelper.domain.model.entity.system.WalletJournal;

import java.util.List;

public interface WalletJournalRepository {
    int updateBatch(List<WalletJournal> list);

    int updateBatchSelective(List<WalletJournal> list);

    int batchInsert(List<WalletJournal> list);

    int insertOrUpdate(WalletJournal record);

    int insertOrUpdateSelective(WalletJournal record);
} 