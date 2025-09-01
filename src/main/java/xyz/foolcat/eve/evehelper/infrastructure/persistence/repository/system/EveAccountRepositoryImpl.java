package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.repository.system.EveAccountRepository;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.EveAccountMapper;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author yongj
 */
@Repository
public class EveAccountRepositoryImpl implements EveAccountRepository {

    @Resource
    private EveAccountMapper eveAccountMapper;

    @Override
    public int updateBatch(List<EveAccount> list) {
        return eveAccountMapper.updateBatch(list);
    }

    @Override
    public int updateBatchSelective(List<EveAccount> list) {
        return eveAccountMapper.updateBatchSelective(list);
    }

    @Override
    public int batchInsert(List<EveAccount> list) {
        return eveAccountMapper.batchInsert(list);
    }

    @Override
    public int insertOrUpdate(EveAccount record) {
        return eveAccountMapper.insertOrUpdate(record);
    }

    @Override
    public int insertOrUpdateSelective(EveAccount record) {
        return eveAccountMapper.insertOrUpdateSelective(record);
    }
    // TODO: 实现 BaseRepository<EveAccount, Long> 的方法

    @Override
    public EveAccount getAccount(Integer userId, Integer cId) {
        return eveAccountMapper.queryOneUserIdAndCharacterId(userId, cId);
    }


}