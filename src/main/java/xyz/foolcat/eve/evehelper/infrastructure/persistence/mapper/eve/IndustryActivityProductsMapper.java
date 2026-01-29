package xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.eve;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.eve.IndustryActivityProductsPO;

@Mapper
public interface IndustryActivityProductsMapper extends BaseMapper<IndustryActivityProductsPO> {

    int insertSelective(IndustryActivityProductsPO industryActivityProductsPO);

}
