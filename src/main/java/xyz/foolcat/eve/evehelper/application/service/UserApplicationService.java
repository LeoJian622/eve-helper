package xyz.foolcat.eve.evehelper.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.service.system.EveAccountService;

import java.util.List;

/**
 * 用户应用服务
 *
 * @author Leojan
 * date 2026-07-31 16:08
 */

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserApplicationService {

    private final EveAccountService eveAccountService;
    /**
     * 获取用户绑定的所有角色
     */
    public List<EveAccount>  queryAccountList(Integer userId) {
        return eveAccountService.getAccountList(userId);
    }
}
