package xyz.foolcat.eve.evehelper.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xyz.foolcat.eve.evehelper.domain.system.InvTypes;

@Mapper
public interface InvTypesMapper extends BaseMapper<InvTypes> {

    int updateBatch(List<InvTypes> list);

    int updateBatchSelective(List<InvTypes> list);

    int batchInsert(@Param("list") List<InvTypes> list);
}