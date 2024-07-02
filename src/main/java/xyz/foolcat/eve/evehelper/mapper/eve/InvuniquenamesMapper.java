package xyz.foolcat.eve.evehelper.mapper.eve;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xyz.foolcat.eve.evehelper.domain.eve.Invuniquenames;

@Mapper
public interface InvuniquenamesMapper extends BaseMapper<Invuniquenames> {
    int updateBatch(List<Invuniquenames> list);

    int batchInsert(@Param("list") List<Invuniquenames> list);

    int insertOrUpdate(Invuniquenames record);

    int insertOrUpdateSelective(Invuniquenames record);
}