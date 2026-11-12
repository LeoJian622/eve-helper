package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.repository.system.EveAccountRepository;
import xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence.EveAccountPoConverter;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.EveAccountPO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.EveAccountMapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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

    /**
     * 查询全部绑定角色（检测遍历用）。
     * 基于 MyBatis-Plus BaseMapper.selectList(null) 遍历全表。
     */
    @Override
    public List<EveAccount> listAll() {
        return eveAccountPoConverter.po2Domain(eveAccountMapper.selectList(null));
    }

    /**
     * 按军团 ID 查询绑定角色（燃料扇出 / 同步选角用）。
     * 基于 MyBatis-Plus QueryWrapper 参数化查询，无 SQL 拼接。
     */
    @Override
    public List<EveAccount> listByCorpId(Integer corpId) {
        return eveAccountPoConverter.po2Domain(
                eveAccountMapper.selectList(new QueryWrapper<EveAccountPO>().eq("corp_id", corpId))
        );
    }

}
