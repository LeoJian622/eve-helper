package xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.eve;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.eve.IndustryActivityMaterialsPO;

@Mapper
public interface IndustryActivityMaterialsMapper extends BaseMapper<IndustryActivityMaterialsPO> {
    // 只保留基础 CRUD

    int insertSelective(IndustryActivityMaterialsPO industryActivityMaterialsPO);

    int insertOrUpdate(IndustryActivityMaterialsPO industryActivityMaterialsPO);
}
