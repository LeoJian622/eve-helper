package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.eve;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.application.assembler.eve.IndustryActivityMaterialsAssembler;
import xyz.foolcat.eve.evehelper.domain.model.entity.eve.IndustryActivityMaterials;
import xyz.foolcat.eve.evehelper.domain.repository.eve.IndustryActivityMaterialsRepository;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.eve.IndustryActivityMaterialsMapper;

/**
 * @author yongj
 */
@Repository
@RequiredArgsConstructor
public class IndustryActivityMaterialsRepositoryImpl implements IndustryActivityMaterialsRepository {

    private final IndustryActivityMaterialsMapper industryActivityMaterialsMapper;

    private final IndustryActivityMaterialsAssembler industryActivityMaterialsAssembler;

    @Override
    public int insert(IndustryActivityMaterials record) {
        return industryActivityMaterialsMapper.insert(industryActivityMaterialsAssembler.entity2Po(record));
    }

    @Override
    public int insertSelective(IndustryActivityMaterials record) {
        return industryActivityMaterialsMapper.insertSelective(industryActivityMaterialsAssembler.entity2Po(record));
    }
}