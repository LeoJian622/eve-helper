package xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.WalletJournalPO;

import java.util.List;

@Mapper
public interface WalletJournalMapper extends BaseMapper<WalletJournalPO> {
    int updateBatch(List<WalletJournalPO> walletJournalPOS);

    int updateBatchSelective(List<WalletJournalPO> walletJournalPOS);

    int batchInsert(List<WalletJournalPO> walletJournalPOS);

    int insertOrUpdate(WalletJournalPO walletJournalPO);

    int insertOrUpdateSelective(WalletJournalPO walletJournalPO);
    // 只保留基础 CRUD
}
