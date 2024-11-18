package xyz.foolcat.eve.evehelper.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xyz.foolcat.eve.evehelper.domain.system.InvTypes;
import xyz.foolcat.eve.evehelper.vo.InvTypesVO;

import java.util.List;

@Mapper
public interface InvTypesMapper extends BaseMapper<InvTypes> {
    int updateBatch(List<InvTypes> list);

    int updateBatchSelective(List<InvTypes> list);

    int batchInsert(@Param("list") List<InvTypes> list);

    int insertOrUpdate(InvTypes record);

    int insertOrUpdateSelective(InvTypes record);

    InvTypesVO selcetByMarketGroupId(@Param("marketGroupID") Long marketGroupID);
}