package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.eve;

import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.application.assembler.eve.InvuniquenamesAssembler;
import xyz.foolcat.eve.evehelper.domain.model.entity.eve.Invuniquenames;
import xyz.foolcat.eve.evehelper.domain.repository.eve.InvuniquenamesRepository;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.eve.InvUniqueNamesPO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.eve.InvuniquenamesMapper;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author yongj
 */
@Repository
public class InvuniquenamesRepositoryImpl implements InvuniquenamesRepository {

    @Resource
    private InvuniquenamesMapper invuniquenamesMapper;

    @Resource
    private InvuniquenamesAssembler invuniquenamesAssembler;

    @Override
    public int updateBatch(List<Invuniquenames> list) {
        List<InvUniqueNamesPO> collect = list.stream().map(invuniquenamesAssembler::entity2Po).collect(Collectors.toList());
        return invuniquenamesMapper.updateBatch(collect);
    }

    @Override
    public int batchInsert(List<Invuniquenames> list) {
        List<InvUniqueNamesPO> collect = list.stream().map(invuniquenamesAssembler::entity2Po).collect(Collectors.toList());
        return invuniquenamesMapper.batchInsert(collect);
    }

    @Override
    public int insertOrUpdate(Invuniquenames record) {
        return invuniquenamesMapper.insertOrUpdate(invuniquenamesAssembler.entity2Po(record));
    }

    @Override
    public int insertOrUpdateSelective(Invuniquenames record) {
        return invuniquenamesMapper.insertOrUpdateSelective(invuniquenamesAssembler.entity2Po(record));
    }
} 