package xyz.foolcat.eve.evehelper.domain.service.eve;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.domain.model.entity.eve.IndustryActivityMaterials;
import xyz.foolcat.eve.evehelper.domain.repository.eve.IndustryActivityMaterialsRepository;

/**
 * @author yongj
 */
@Service
@Slf4j
@Transactional(rollbackFor = RuntimeException.class)
@RequiredArgsConstructor
public class IndustryActivityMaterialsService {

    private final IndustryActivityMaterialsRepository industryActivityMaterialsRepository;


    public int insert(IndustryActivityMaterials record) {
        return industryActivityMaterialsRepository.insert(record);
    }


    public int insertSelective(IndustryActivityMaterials record) {
        return industryActivityMaterialsRepository.insertSelective(record);
    }

}
