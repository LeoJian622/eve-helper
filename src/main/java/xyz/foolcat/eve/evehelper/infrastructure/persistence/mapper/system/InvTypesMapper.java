package xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.InvTypesPO;

import java.util.List;

/**
 * @author Leojan
 */
@Mapper
public interface InvTypesMapper extends BaseMapper<InvTypesPO> {
    int updateBatch(List<InvTypesPO> list);

    int updateBatchSelective(List<InvTypesPO> list);

    int batchInsert(List<InvTypesPO> list);

    int insertOrUpdateSelective(InvTypesPO record);

    List<InvTypesPO> selectTypeNameByIds(List<Integer> typeIds);
}
