package xyz.foolcat.eve.evehelper.service.eve;

import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import xyz.foolcat.eve.evehelper.domain.eve.IndustryBlueprints;
import java.util.List;
import xyz.foolcat.eve.evehelper.mapper.eve.IndustryBlueprintsMapper;
@Service
public class IndustryBlueprintsService{

    @Resource
    private IndustryBlueprintsMapper industryBlueprintsMapper;

    
    public int deleteByPrimaryKey(Integer blueprinttypeid) {
        return industryBlueprintsMapper.deleteByPrimaryKey(blueprinttypeid);
    }

    
    public int insert(IndustryBlueprints record) {
        return industryBlueprintsMapper.insert(record);
    }

    
    public int insertSelective(IndustryBlueprints record) {
        return industryBlueprintsMapper.insertSelective(record);
    }

    
    public IndustryBlueprints selectByPrimaryKey(Integer blueprinttypeid) {
        return industryBlueprintsMapper.selectByPrimaryKey(blueprinttypeid);
    }

    
    public int updateByPrimaryKeySelective(IndustryBlueprints record) {
        return industryBlueprintsMapper.updateByPrimaryKeySelective(record);
    }

    
    public int updateByPrimaryKey(IndustryBlueprints record) {
        return industryBlueprintsMapper.updateByPrimaryKey(record);
    }

    
    public int updateBatch(List<IndustryBlueprints> list) {
        return industryBlueprintsMapper.updateBatch(list);
    }

}
