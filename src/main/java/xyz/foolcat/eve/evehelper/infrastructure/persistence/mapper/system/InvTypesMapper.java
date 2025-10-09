package xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.InvTypesPO;
import xyz.foolcat.eve.evehelper.interfaces.web.vo.InvTypesVO;

import java.util.List;

@Mapper
public interface InvTypesMapper extends BaseMapper<InvTypesPO> {
    int updateBatch(List<InvTypesPO> list);

    int updateBatchSelective(List<InvTypesPO> list);

    int batchInsert(List<InvTypesPO> list);

    int insertOrUpdate(InvTypesPO record);

    int insertOrUpdateSelective(InvTypesPO record);

    InvTypesVO selcetByMarketGroupId(Long marketGroupID);

    List<InvTypesPO> selectTypeNameByIds(List<Integer> typeIds);
    // 只保留基础 CRUD
}
