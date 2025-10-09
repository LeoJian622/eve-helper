package xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.ObserverPO;

import java.util.List;

@Mapper
public interface ObserverMapper extends BaseMapper<ObserverPO> {
    int updateBatch(List<ObserverPO> list);

    int updateBatchSelective(List<ObserverPO> list);

    int batchInsert(List<ObserverPO> list);

    int insertOrUpdate(ObserverPO record);

    int insertOrUpdateSelective(ObserverPO record);
    // 只保留基础 CRUD
}
