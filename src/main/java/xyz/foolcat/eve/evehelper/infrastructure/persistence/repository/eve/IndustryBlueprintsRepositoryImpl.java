package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.eve;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.domain.model.entity.eve.IndustryBlueprints;
import xyz.foolcat.eve.evehelper.domain.repository.eve.IndustryBlueprintsRepository;
import xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence.IndustryBlueprintsPoConverter;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.eve.IndustryBlueprintsPO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.eve.IndustryBlueprintsMapper;

import java.util.List;
import java.util.stream.Collectors;
/**
 * @author Leojan
 */
@Repository
@RequiredArgsConstructor
public class IndustryBlueprintsRepositoryImpl implements IndustryBlueprintsRepository {

    private final IndustryBlueprintsMapper industryBlueprintsMapper;
    private final IndustryBlueprintsPoConverter industryBlueprintsPoConverter;

    @Override
    public int deleteByPrimaryKey(Integer blueprinttypeid) {
        return industryBlueprintsMapper.deleteByPrimaryKey(blueprinttypeid);
    }

    @Override
    public int insert(IndustryBlueprints record) {
        return industryBlueprintsMapper.insert(industryBlueprintsPoConverter.domain2Po(record));
    }

    @Override
    public int insertSelective(IndustryBlueprints record) {
        return industryBlueprintsMapper.insertSelective(industryBlueprintsPoConverter.domain2Po(record));
    }

    @Override
    public IndustryBlueprints selectByPrimaryKey(Integer blueprinttypeid) {
        return industryBlueprintsPoConverter.po2Domain(industryBlueprintsMapper.selectByPrimaryKey(blueprinttypeid));
    }

    @Override
    public int updateByPrimaryKeySelective(IndustryBlueprints record) {
        return industryBlueprintsMapper.updateByPrimaryKeySelective(industryBlueprintsPoConverter.domain2Po(record));
    }

    @Override
    public int updateByPrimaryKey(IndustryBlueprints record) {
        return industryBlueprintsMapper.updateByPrimaryKey(industryBlueprintsPoConverter.domain2Po(record));
    }

    @Override
    public int updateBatch(List<IndustryBlueprints> list) {
        List<IndustryBlueprintsPO> industryBlueprintsPOS = list.stream().map(industryBlueprintsPoConverter::domain2Po).collect(Collectors.toList());
        return industryBlueprintsMapper.updateBatch(industryBlueprintsPOS);
    }
}
