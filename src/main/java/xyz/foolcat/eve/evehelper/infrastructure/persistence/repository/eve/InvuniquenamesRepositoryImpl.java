package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.eve;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.domain.model.entity.eve.InvUniqueNames;
import xyz.foolcat.eve.evehelper.domain.repository.eve.InvuniquenamesRepository;
import xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence.InvuniquenamesPoConverter;
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
    private final InvuniquenamesPoConverter invuniquenamesPoConverter;

    @Override
    public int updateBatch(List<InvUniqueNames> list) {
        List<InvUniqueNamesPO> collect = list.stream().map(invuniquenamesPoConverter::domain2Po).collect(Collectors.toList());
        return invuniquenamesMapper.updateBatch(collect);
    }

    @Override
    public int batchInsert(List<InvUniqueNames> list) {
        List<InvUniqueNamesPO> collect = list.stream().map(invuniquenamesPoConverter::domain2Po).collect(Collectors.toList());
        return invuniquenamesMapper.batchInsert(collect);
    }

    @Override
    public boolean insertOrUpdate(InvUniqueNames record) {
        return invuniquenamesMapper.insertOrUpdate(invuniquenamesPoConverter.domain2Po(record));
    }

    @Override
    public int insertOrUpdateSelective(InvUniqueNames record) {
        return invuniquenamesMapper.insertOrUpdateSelective(invuniquenamesPoConverter.domain2Po(record));
    }

    @Override
    public InvUniqueNames selectById(Integer id) {
        return invuniquenamesPoConverter.po2Domain(invuniquenamesMapper.selectById(id));
    }
}
