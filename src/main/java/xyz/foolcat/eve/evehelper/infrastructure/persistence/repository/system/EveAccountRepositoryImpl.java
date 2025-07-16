package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.repository.system.EveAccountRepository;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.EveAccountMapper;

import java.util.List;

@Repository
public class EveAccountRepositoryImpl implements EveAccountRepository {
    @Autowired
    private EveAccountMapper eveAccountMapper;

    public int updateBatch(List<EveAccount> list) {
        return eveAccountMapper.updateBatch(list);
    }
    public int updateBatchSelective(List<EveAccount> list) {
        return eveAccountMapper.updateBatchSelective(list);
    }
    public int batchInsert(List<EveAccount> list) {
        return eveAccountMapper.batchInsert(list);
    }
    public int insertOrUpdate(EveAccount record) {
        return eveAccountMapper.insertOrUpdate(record);
    }
    public int insertOrUpdateSelective(EveAccount record) {
        return eveAccountMapper.insertOrUpdateSelective(record);
    }
    // TODO: 实现 BaseRepository<EveAccount, Long> 的方法
} 