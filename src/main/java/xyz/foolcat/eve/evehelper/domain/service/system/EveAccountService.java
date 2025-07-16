package xyz.foolcat.eve.evehelper.domain.service.system;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.EveAccountMapper;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;
import xyz.foolcat.eve.evehelper.shared.result.ResultCode;

import java.util.List;

@Service
public class EveAccountService extends ServiceImpl<EveAccountMapper, EveAccount> {


    public int updateBatch(List<EveAccount> list) {
        return baseMapper.updateBatch(list);
    }

    public int batchInsert(List<EveAccount> list) {
        return baseMapper.batchInsert(list);
    }

    public int insertOrUpdate(EveAccount record) {
        return baseMapper.insertOrUpdate(record);
    }

    public int insertOrUpdateSelective(EveAccount record) {
        return baseMapper.insertOrUpdateSelective(record);
    }

    /**
     * 根据人物或者公司ID以及用户ID或者QQ
     * 获取游戏账户信息
     *
     * @param cId    人物或者公司ID
     * @param userId 用户ID或者QQ
     * @return 游戏账户信息
     * @throws EveHelperException 账户不存在则抛出异常
     */
    public EveAccount getAccountOne(Integer userId, Integer cId) {
        EveAccount account = lambdaQuery()
                .and(q -> q.eq(EveAccount::getUserId, userId).or().eq(EveAccount::getQq, userId))
                .and(q -> q.eq(EveAccount::getCharacterId, cId).or().eq(EveAccount::getCorpId, cId))
                .one();
        if (account == null) {
            throw new EveHelperException(ResultCode.USER_ACCOUNT_NOT_EXIST);
        }
        return account;
    }

    public int updateBatchSelective(List<EveAccount> list) {
        return baseMapper.updateBatchSelective(list);
    }
}

