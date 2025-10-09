package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.eve;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.application.assembler.eve.InvuniquenamesAssembler;
import xyz.foolcat.eve.evehelper.domain.model.entity.eve.InvUniqueNames;
import xyz.foolcat.eve.evehelper.domain.repository.eve.InvuniquenamesRepository;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.eve.InvUniqueNamesPO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.eve.InvuniquenamesMapper;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author yongj
 */
@Repository
@RequiredArgsConstructor
public class InvuniquenamesRepositoryImpl implements InvuniquenamesRepository {

    private final InvuniquenamesMapper invuniquenamesMapper;
    private final InvuniquenamesAssembler invuniquenamesAssembler;

    @Override
    public int updateBatch(List<InvUniqueNames> list) {
        List<InvUniqueNamesPO> collect = list.stream().map(invuniquenamesAssembler::domain2Po).collect(Collectors.toList());
        return invuniquenamesMapper.updateBatch(collect);
    }

    @Override
    public int batchInsert(List<InvUniqueNames> list) {
        List<InvUniqueNamesPO> collect = list.stream().map(invuniquenamesAssembler::domain2Po).collect(Collectors.toList());
        return invuniquenamesMapper.batchInsert(collect);
    }

    @Override
    public int insertOrUpdate(InvUniqueNames record) {
        return invuniquenamesMapper.insertOrUpdate(invuniquenamesAssembler.domain2Po(record));
    }

    @Override
    public int insertOrUpdateSelective(InvUniqueNames record) {
        return invuniquenamesMapper.insertOrUpdateSelective(invuniquenamesAssembler.domain2Po(record));
    }

    @Override
    public InvUniqueNames selectById(Integer id) {
        return invuniquenamesAssembler.po2Domain(invuniquenamesMapper.selectById(id));
    }
} 