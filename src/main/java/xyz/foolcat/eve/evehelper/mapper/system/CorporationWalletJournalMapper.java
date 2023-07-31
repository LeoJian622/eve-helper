package xyz.foolcat.eve.evehelper.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xyz.foolcat.eve.evehelper.domain.system.CorporationWalletJournal;

@Mapper
public interface CorporationWalletJournalMapper extends BaseMapper<CorporationWalletJournal> {
    int updateBatch(List<CorporationWalletJournal> list);

    int updateBatchSelective(List<CorporationWalletJournal> list);

    int batchInsert(@Param("list") List<CorporationWalletJournal> list);

    int insertOrUpdate(CorporationWalletJournal record);

    int insertOrUpdateSelective(CorporationWalletJournal record);
}