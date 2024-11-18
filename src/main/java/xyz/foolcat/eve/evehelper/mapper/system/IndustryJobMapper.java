package xyz.foolcat.eve.evehelper.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xyz.foolcat.eve.evehelper.domain.system.IndustryJob;

@Mapper
public interface IndustryJobMapper extends BaseMapper<IndustryJob> {
    int updateBatch(List<IndustryJob> list);

    int updateBatchSelective(List<IndustryJob> list);

    int batchInsert(@Param("list") List<IndustryJob> list);

    int insertOrUpdate(IndustryJob record);

    int insertOrUpdateSelective(IndustryJob record);

    int batchInsertOrUpdate(@Param("list") List<IndustryJob> list);
}