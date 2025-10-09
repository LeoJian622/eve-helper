package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.application.assembler.system.InvTypesAssembler;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.InvTypes;
import xyz.foolcat.eve.evehelper.domain.repository.system.InvTypesRepository;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.InvTypesPO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.InvTypesMapper;
import xyz.foolcat.eve.evehelper.interfaces.web.vo.InvTypesVO;

import java.util.Collections;
import java.util.List;

/**
 * @author Leojan
 */
@Repository
@RequiredArgsConstructor
public class InvTypesRepositoryImpl implements InvTypesRepository {

    private final InvTypesMapper invTypesMapper;
    private final InvTypesAssembler invTypesAssembler;

    @Override
    public int updateBatch(List<InvTypes> list) {
        return invTypesMapper.updateBatch(invTypesAssembler.domain2Po(list));
    }

    @Override
    public int updateBatchSelective(List<InvTypes> list) {
        return invTypesMapper.updateBatchSelective(invTypesAssembler.domain2Po(list));
    }

    @Override
    public int batchInsert(List<InvTypes> list) {
        return invTypesMapper.batchInsert(invTypesAssembler.domain2Po(list));
    }

    @Override
    public int insertOrUpdate(InvTypes record) {
        return invTypesMapper.insertOrUpdate(invTypesAssembler.domain2Po(record));
    }

    @Override
    public int insertOrUpdateSelective(InvTypes record) {
        return invTypesMapper.insertOrUpdateSelective(invTypesAssembler.domain2Po(record));
    }

    @Override
    public InvTypesVO selectByMarketGroupId(Long marketGroupId) {
        return invTypesMapper.selcetByMarketGroupId(marketGroupId);
    }

    @Override
    public List<InvTypes> selectTypeNameByIds(List<Integer> typeIds) {
        // 参数校验，避免空指针异常
        if (typeIds == null || typeIds.isEmpty()) {
            return invTypesAssembler.po2Domain(Collections.emptyList());
        }
        return invTypesAssembler.po2Domain(invTypesMapper.selectTypeNameByIds(typeIds));
    }

    @Override
    public InvTypes selectOneByName(String name) {
        return invTypesAssembler.po2Domain(invTypesMapper.selectOne(new QueryWrapper<InvTypesPO>().lambda().eq(InvTypesPO::getName,name)));
    }

    @Override
    public InvTypes selectOneById(int id) {
        return invTypesAssembler.po2Domain(invTypesMapper.selectById(id));
    }
}