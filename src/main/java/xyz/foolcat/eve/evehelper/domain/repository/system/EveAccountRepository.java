package xyz.foolcat.eve.evehelper.domain.repository.system;

import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;

import java.util.List;

public interface EveAccountRepository  {

    int updateBatch(List<EveAccount> list);

    int updateBatchSelective(List<EveAccount> list);

    int batchInsert(List<EveAccount> list);

    boolean insertOrUpdate(EveAccount record);

    int insertOrUpdateSelective(EveAccount record);

    EveAccount getAccount(Integer userId, Integer characterId);

    List<EveAccount> getAccountList(Integer userId);

    /**
     * 查询全部绑定角色（检测遍历用）。
     *
     * @return 所有 eve_account 行对应的领域实体列表
     */
    List<EveAccount> listAll();

    /**
     * 按军团 ID 查询绑定角色（燃料扇出 / 同步选角用）。
     *
     * @param corpId 军团 ID
     * @return 该军团下所有角色列表；未知军团返回空列表
     */
    List<EveAccount> listByCorpId(Integer corpId);
}