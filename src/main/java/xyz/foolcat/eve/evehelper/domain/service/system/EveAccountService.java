package xyz.foolcat.eve.evehelper.domain.service.system;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.repository.system.EveAccountRepository;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;
import xyz.foolcat.eve.evehelper.shared.result.ResultCode;

import java.util.List;

@Service
@Slf4j
@Transactional(rollbackFor = RuntimeException.class)
@RequiredArgsConstructor
public class EveAccountService  {

    private final EveAccountRepository eveAccountRepository;

    public int updateBatch(List<EveAccount> list) {
        return eveAccountRepository.updateBatch(list);
    }

    public int batchInsert(List<EveAccount> list) {
        return eveAccountRepository.batchInsert(list);
    }

    public int insertOrUpdate(EveAccount record) {
        return eveAccountRepository.insertOrUpdate(record);
    }

    public int insertOrUpdateSelective(EveAccount record) {
        return eveAccountRepository.insertOrUpdateSelective(record);
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
        EveAccount account = eveAccountRepository.getAccount(userId, cId);
        if (account == null) {
            throw new EveHelperException(ResultCode.USER_ACCOUNT_NOT_EXIST);
        }
        return account;
    }

    public int updateBatchSelective(List<EveAccount> list) {
        return eveAccountRepository.updateBatchSelective(list);
    }
}

