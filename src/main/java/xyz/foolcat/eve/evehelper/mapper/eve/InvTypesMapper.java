package xyz.foolcat.eve.evehelper.mapper.eve;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import xyz.foolcat.eve.evehelper.domain.eve.InvTypes;

@Mapper
public interface InvTypesMapper {
    int deleteByPrimaryKey(Integer typeid);

    int insert(InvTypes record);

    int insertSelective(InvTypes record);

    InvTypes selectByPrimaryKey(Integer typeid);

    int updateByPrimaryKeySelective(InvTypes record);

    int updateByPrimaryKey(InvTypes record);

    int updateBatch(List<InvTypes> list);

    Integer selectIdByTypeName(String name);
}