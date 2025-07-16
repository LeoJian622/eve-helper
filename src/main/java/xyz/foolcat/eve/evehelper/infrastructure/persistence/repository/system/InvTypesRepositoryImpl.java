package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.domain.repository.system.InvTypesRepository;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.InvTypesPO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.InvTypesMapper;
import xyz.foolcat.eve.evehelper.interfaces.web.vo.InvTypesVO;
import java.util.List;

@Repository
public class InvTypesRepositoryImpl implements InvTypesRepository {
    @Autowired
    private InvTypesMapper invTypesMapper;

    @Override
    public int updateBatch(List<InvTypesPO> list) {
        return invTypesMapper.updateBatch(list);
    }

    @Override
    public int updateBatchSelective(List<InvTypesPO> list) {
        return invTypesMapper.updateBatchSelective(list);
    }

    @Override
    public int batchInsert(List<InvTypesPO> list) {
        return invTypesMapper.batchInsert(list);
    }

    @Override
    public int insertOrUpdate(InvTypesPO record) {
        return invTypesMapper.insertOrUpdate(record);
    }

    @Override
    public int insertOrUpdateSelective(InvTypesPO record) {
        return invTypesMapper.insertOrUpdateSelective(record);
    }

    @Override
    public InvTypesVO selcetByMarketGroupId(Long marketGroupID) {
        return invTypesMapper.selcetByMarketGroupId(marketGroupID);
    }
} 