package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.repository.system.EveAccountRepository;
import xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence.EveAccountPoConverter;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.EveAccountMapper;

import java.util.List;

/**
 * @author yongj
 */
@Repository
@RequiredArgsConstructor
public class EveAccountRepositoryImpl implements EveAccountRepository {

    private final EveAccountMapper eveAccountMapper;

    private final EveAccountPoConverter eveAccountPoConverter;

    @Override
    public int updateBatch(List<EveAccount> list) {
        return eveAccountMapper.updateBatch(eveAccountPoConverter.domain2Po(list));
    }

    @Override
    public int updateBatchSelective(List<EveAccount> list) {
        return eveAccountMapper.updateBatchSelective(eveAccountPoConverter.domain2Po(list));
    }

    @Override
    public int batchInsert(List<EveAccount> list) {
        return eveAccountMapper.batchInsert(eveAccountPoConverter.domain2Po(list));
    }

    @Override
    public boolean insertOrUpdate(EveAccount record) {
        return eveAccountMapper.insertOrUpdate(eveAccountPoConverter.domain2Po(record));
    }

    @Override
    public int insertOrUpdateSelective(EveAccount record) {
        return eveAccountMapper.insertOrUpdateSelective(eveAccountPoConverter.domain2Po(record));
    }

    @Override
    public EveAccount getAccount(Integer userId, Integer characterId) {
        return eveAccountPoConverter.po2Domain(
        eveAccountMapper.queryOneUserIdAndCharacterId(userId, characterId));
    }

    @Override
    public List<EveAccount> getAccountList(Integer userId) {
        return eveAccountPoConverter.po2Domain(eveAccountMapper.queryAccountList(userId));
    }


}
