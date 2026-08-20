package xyz.foolcat.eve.evehelper.domain.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.service.system.EveAccountService;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;
import xyz.foolcat.eve.evehelper.shared.result.ResultCode;

/**
 * 权限及用户工具
 *
 * @author Leojan
 * date 2024-07-03 15:02
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthorizeUtil {

    private final EveAccountService eveAccountService;

    /**
     * 按当前登录用户校验并获取游戏账户（请求上下文路径）。
     * <p>
     * 未认证、匿名或主体类型无法识别时一律拒绝（fail-closed）。
     * 此前的实现在 UserUtil.getUserId() 返回 -1 时回落为硬编码用户 1，
     * 会使任何主体不可识别的请求静默以该用户身份执行（含用其 refreshToken 换取 ESI token）。
     * 系统内部调用请改用 {@link #authorizeInternal}。
     *
     * @param characterId 人物或军团ID
     * @return 游戏账户信息
     * @throws EveHelperException 未认证或该账户不属于当前用户时抛出
     */
    public EveAccount authorize(Integer characterId) {
        Integer userId = UserUtil.getUserId();
        if (userId == null || userId <= 0) {
            log.warn("游戏账户访问越权：未认证或主体无法识别 characterId={}", characterId);
            throw new EveHelperException(ResultCode.ACCESS_UNAUTHORIZED);
        }
        return eveAccountService.getAccountOne(userId, characterId);
    }

    /**
     * 以显式指定的用户身份获取游戏账户（系统内部路径）。
     * <p>
     * 供定时任务、机器人等无安全上下文的场景使用，调用方必须显式声明操作身份
     * （通常为 {@code GlobalConstants.SYSTEM_USER_ID}），使内部通道在调用点可见、可审计。
     * <p>
     * 内部通道不得被请求路径借用：若检测到当前线程已有可识别的登录主体，
     * 说明这是处理外部请求的线程，一律拒绝（运行时不变量，不仅靠注释约定）。
     *
     * @param userId 操作身份的用户ID
     * @param characterId    人物或军团ID
     * @return 游戏账户信息
     * @throws EveHelperException userId 缺失、处于请求上下文、或该账户不属于该用户时抛出
     */
    public EveAccount authorizeInternal(Integer userId, Integer characterId) {
        if (userId == null || userId <= 0) {
            log.warn("系统内部调用未声明操作身份：characterId={}", characterId);
            throw new EveHelperException(ResultCode.PARAM_ERROR);
        }
        Integer currentUserId = UserUtil.getUserId();
        if (currentUserId != null && currentUserId > 0) {
            log.warn("内部通道被请求路径误用：currentUserId={}, 声明身份={}, characterId={}", currentUserId, userId, characterId);
            throw new EveHelperException(ResultCode.ACCESS_UNAUTHORIZED);
        }
        return eveAccountService.getAccountOne(userId, characterId);
    }
}