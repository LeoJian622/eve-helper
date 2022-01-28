package xyz.foolcat.eve.evehelper.service.eve;

import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import xyz.foolcat.eve.evehelper.mapper.eve.IndustryActivityMaterialsMapper;
import xyz.foolcat.eve.evehelper.domain.eve.IndustryActivityMaterials;
@Service
public class IndustryActivityMaterialsService{

    @Resource
    private IndustryActivityMaterialsMapper industryActivityMaterialsMapper;

    
    public int insert(IndustryActivityMaterials record) {
        return industryActivityMaterialsMapper.insert(record);
    }

    
    public int insertSelective(IndustryActivityMaterials record) {
        return industryActivityMaterialsMapper.insertSelective(record);
    }

}
