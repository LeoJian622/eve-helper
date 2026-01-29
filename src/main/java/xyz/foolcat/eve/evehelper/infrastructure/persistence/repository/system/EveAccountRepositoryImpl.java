package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.application.assembler.system.EveAccountAssembler;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.repository.system.EveAccountRepository;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.EveAccountMapper;

import java.util.List;

/**
 * @author yongj
 */
@Repository
@RequiredArgsConstructor
public class EveAccountRepositoryImpl implements EveAccountRepository {

    private final EveAccountMapper eveAccountMapper;

    private final EveAccountAssembler eveAccountAssembler;

    @Override
    public int updateBatch(List<EveAccount> list) {
        return eveAccountMapper.updateBatch(eveAccountAssembler.domain2Po(list));
    }

    @Override
    public int updateBatchSelective(List<EveAccount> list) {
        return eveAccountMapper.updateBatchSelective(eveAccountAssembler.domain2Po(list));
    }

    @Override
    public int batchInsert(List<EveAccount> list) {
        return eveAccountMapper.batchInsert(eveAccountAssembler.domain2Po(list));
    }

    @Override
    public int insertOrUpdate(EveAccount record) {
        return eveAccountMapper.insertOrUpdate(eveAccountAssembler.domain2Po(record));
    }

    @Override
    public int insertOrUpdateSelective(EveAccount record) {
        return eveAccountMapper.insertOrUpdateSelective(eveAccountAssembler.domain2Po(record));
    }
    // TODO: 实现 BaseRepository<EveAccount, Long> 的方法

    @Override
    public EveAccount getAccount(Integer userId, Integer cId) {
        return eveAccountAssembler.po2Domain(
        eveAccountMapper.queryOneUserIdAndCharacterId(userId, cId));
    }


}