package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.eve;

import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.application.assembler.eve.IndustryActivityMaterialsAssembler;
import xyz.foolcat.eve.evehelper.domain.model.entity.eve.IndustryActivityMaterials;
import xyz.foolcat.eve.evehelper.domain.repository.eve.IndustryActivityMaterialsRepository;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.eve.IndustryActivityMaterialsMapper;

import javax.annotation.Resource;

/**
 * @author yongj
 */
@Repository
public class IndustryActivityMaterialsRepositoryImpl implements IndustryActivityMaterialsRepository {

    @Resource
    private IndustryActivityMaterialsMapper industryActivityMaterialsMapper;

    @Resource
    private IndustryActivityMaterialsAssembler industryActivityMaterialsAssembler;

    @Override
    public int insert(IndustryActivityMaterials record) {
        return industryActivityMaterialsMapper.insert(industryActivityMaterialsAssembler.entity2Po(record));
    }

    @Override
    public int insertSelective(IndustryActivityMaterials record) {
        return industryActivityMaterialsMapper.insertSelective(industryActivityMaterialsAssembler.entity2Po(record));
    }
}