package xyz.foolcat.eve.evehelper.domain.repository.system;

import xyz.foolcat.eve.evehelper.domain.model.entity.system.WalletJournal;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author Leojan
 */
public interface WalletJournalRepository {
    int updateBatch(List<WalletJournal> list);

    int updateBatchSelective(List<WalletJournal> list);

    int batchInsert(List<WalletJournal> list);

    int insertOrUpdate(WalletJournal record);

    int insertOrUpdateSelective(WalletJournal record);

    void saveOrUpdateBatch(List<WalletJournal> walletJournals);

    List<Map<String, Object>> selectMapByDatetime(Date start, Date end, List<String> refType);
}