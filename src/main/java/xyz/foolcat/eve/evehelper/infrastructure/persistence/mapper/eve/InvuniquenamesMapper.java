package xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.eve;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.eve.InvUniqueNamesPO;

import java.util.List;

@Mapper
public interface InvuniquenamesMapper extends BaseMapper<InvUniqueNamesPO> {
    int updateBatch(List<InvUniqueNamesPO> collect);

    int batchInsert(List<InvUniqueNamesPO> collect);

    int insertOrUpdate(InvUniqueNamesPO record);

    int insertOrUpdateSelective(InvUniqueNamesPO invUniqueNamesPO);
    // 只保留基础 CRUD
}
