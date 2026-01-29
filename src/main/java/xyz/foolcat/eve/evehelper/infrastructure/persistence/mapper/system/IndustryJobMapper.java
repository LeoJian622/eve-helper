package xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.IndustryJobPO;

import java.util.List;

/**
 * @author Leojan
 */
@Mapper
public interface IndustryJobMapper extends BaseMapper<IndustryJobPO> {
    int updateBatch(List<IndustryJobPO> industryJobPOS);

    int updateBatchSelective(List<IndustryJobPO> industryJobPOS);

    int batchInsert(List<IndustryJobPO> industryJobPOS);

    int insertOrUpdate(IndustryJobPO industryJobPO);

    int insertOrUpdateSelective(IndustryJobPO industryJobPO);

    int batchInsertOrUpdate(List<IndustryJobPO> industryJobPOS);
    // 只保留基础 CRUD
}
