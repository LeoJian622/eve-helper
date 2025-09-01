package xyz.foolcat.eve.evehelper.shared.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import xyz.foolcat.eve.evehelper.domain.service.system.EveAccountService;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;

/**
 * 权限及用户工具
 *
 * @author Leojan
 * date 2024-07-03 15:02
 */

@Component
@RequiredArgsConstructor
public class AuthorizeUtil {

    private final EveAccountService eveAccountService;

    public EveAccount authorize(Integer cId) {
        Integer userId = UserUtil.getUserId();
        if (userId == -1) {
            /*
            -1 代表系统内部调用，不考虑用户，用于定时任务等使用
             */
            return eveAccountService.getAccountOne(1, 2112818290);
        }
        return eveAccountService.getAccountOne(userId, cId);
    }
}
