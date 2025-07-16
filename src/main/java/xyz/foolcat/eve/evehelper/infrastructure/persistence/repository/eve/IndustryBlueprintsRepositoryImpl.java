package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.eve;

import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.application.assembler.eve.IndustryBlueprintsAssembler;
import xyz.foolcat.eve.evehelper.domain.model.entity.eve.IndustryBlueprints;
import xyz.foolcat.eve.evehelper.domain.repository.eve.IndustryBlueprintsRepository;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.eve.IndustryBlueprintsPO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.eve.IndustryBlueprintsMapper;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class IndustryBlueprintsRepositoryImpl implements IndustryBlueprintsRepository {

    @Resource
    private IndustryBlueprintsMapper industryBlueprintsMapper;

    @Resource
    private IndustryBlueprintsAssembler industryBlueprintsAssembler;

    @Override
    public int deleteByPrimaryKey(Integer blueprinttypeid) {
        return industryBlueprintsMapper.deleteByPrimaryKey(blueprinttypeid);
    }

    @Override
    public int insert(IndustryBlueprints record) {
        return industryBlueprintsMapper.insert(industryBlueprintsAssembler.entity2Po(record));
    }

    @Override
    public int insertSelective(IndustryBlueprints record) {
        return industryBlueprintsMapper.insertSelective(industryBlueprintsAssembler.entity2Po(record));
    }

    @Override
    public IndustryBlueprints selectByPrimaryKey(Integer blueprinttypeid) {
        return industryBlueprintsAssembler.po2Entity(industryBlueprintsMapper.selectByPrimaryKey(blueprinttypeid));
    }

    @Override
    public int updateByPrimaryKeySelective(IndustryBlueprints record) {
        return industryBlueprintsMapper.updateByPrimaryKeySelective(industryBlueprintsAssembler.entity2Po(record));
    }

    @Override
    public int updateByPrimaryKey(IndustryBlueprints record) {
        return industryBlueprintsMapper.updateByPrimaryKey(industryBlueprintsAssembler.entity2Po(record));
    }

    @Override
    public int updateBatch(List<IndustryBlueprints> list) {
        List<IndustryBlueprintsPO> industryBlueprintsPOS = list.stream().map(industryBlueprintsAssembler::entity2Po).collect(Collectors.toList());
        return industryBlueprintsMapper.updateBatch(industryBlueprintsPOS);
    }
} 