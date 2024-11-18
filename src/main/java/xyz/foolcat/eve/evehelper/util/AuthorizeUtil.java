package xyz.foolcat.eve.evehelper.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import xyz.foolcat.eve.evehelper.domain.system.EveAccount;
import xyz.foolcat.eve.evehelper.service.system.EveAccountService;

/**
 * 权限及用户工具
 *
 * @author Leojan
 * date 2024-07-03 15:02
 */

@Component
@RequiredArgsConstructor
public class AuthorizeUtil {

    private final static EveAccountService eveAccountService = new EveAccountService();

    public static EveAccount authorize( Integer cId) {
        Integer userId = UserUtil.getUserId();
        if (userId == -1) {
            /*
            -1 代表系统内部调用，不考虑用户，用于定时任务等使用
             */
            return eveAccountService.lambdaQuery().eq(EveAccount::getCharacterId, cId).or().eq(EveAccount::getCorpId, cId).one();
        }
        return eveAccountService.getAccountOne(userId, cId);
    }
}
