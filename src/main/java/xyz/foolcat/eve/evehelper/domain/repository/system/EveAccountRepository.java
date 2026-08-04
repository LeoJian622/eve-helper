package xyz.foolcat.eve.evehelper.domain.repository.system;

import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;

import java.util.List;

public interface EveAccountRepository  {

    int updateBatch(List<EveAccount> list);

    int updateBatchSelective(List<EveAccount> list);

    int batchInsert(List<EveAccount> list);

    boolean insertOrUpdate(EveAccount record);

    int insertOrUpdateSelective(EveAccount record);

    EveAccount getAccount(Integer userId, Integer cId);

    List<EveAccount> getAccountList(Integer userId);
}