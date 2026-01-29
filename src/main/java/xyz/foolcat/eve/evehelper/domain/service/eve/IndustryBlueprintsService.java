package xyz.foolcat.eve.evehelper.domain.service.eve;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.domain.model.entity.eve.IndustryBlueprints;
import xyz.foolcat.eve.evehelper.domain.repository.eve.IndustryBlueprintsRepository;

import java.util.List;

/**
 * @author yongj
 */
@Service
@Slf4j
@Transactional(rollbackFor = RuntimeException.class)
@RequiredArgsConstructor
public class IndustryBlueprintsService{

    private final IndustryBlueprintsRepository industryBlueprintsRepository;

    
    public int deleteByPrimaryKey(Integer blueprinttypeid) {
        return industryBlueprintsRepository.deleteByPrimaryKey(blueprinttypeid);
    }

    
    public int insert(IndustryBlueprints record) {
        return industryBlueprintsRepository.insert(record);
    }

    
    public int insertSelective(IndustryBlueprints record) {
        return industryBlueprintsRepository.insertSelective(record);
    }

    
    public IndustryBlueprints selectByPrimaryKey(Integer blueprinttypeid) {
        return industryBlueprintsRepository.selectByPrimaryKey(blueprinttypeid);
    }

    
    public int updateByPrimaryKeySelective(IndustryBlueprints record) {
        return industryBlueprintsRepository.updateByPrimaryKeySelective(record);
    }

    
    public int updateByPrimaryKey(IndustryBlueprints record) {
        return industryBlueprintsRepository.updateByPrimaryKey(record);
    }

    
    public int updateBatch(List<IndustryBlueprints> list) {
        return industryBlueprintsRepository.updateBatch(list);
    }

}
