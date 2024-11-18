package xyz.foolcat.eve.evehelper.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xyz.foolcat.eve.evehelper.domain.system.WalletJournal;

@Mapper
public interface WalletJournalMapper extends BaseMapper<WalletJournal> {
    int updateBatch(List<WalletJournal> list);

    int updateBatchSelective(List<WalletJournal> list);

    int batchInsert(@Param("list") List<WalletJournal> list);

    int insertOrUpdate(WalletJournal record);

    int insertOrUpdateSelective(WalletJournal record);
}