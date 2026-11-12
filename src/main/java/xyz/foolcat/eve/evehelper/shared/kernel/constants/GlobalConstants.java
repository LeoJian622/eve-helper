package xyz.foolcat.eve.evehelper.shared.kernel.constants;

/**
 * 系统常量
 *
 * @author Leojan
 * date 2021-12-28 16:39
 */

public interface GlobalConstants {

    String URL_PERM_ROLES_KEY = "system:perm_roles_rule:url:";
    String BTN_PERM_ROLES_KEY = "system:perm_roles_rule:btn:";
    String ROOT_ROLE_CODE = "ADMIN";
    String ESI_ACCESS_TOKEN_KEY = "esi_access_token:";
    String TOKEN_PERN = "Bearer ";

    /**
     * 系统内部调用所用的用户ID（定时任务、机器人等无安全上下文的场景）。
     * 仅可通过 AuthorizeUtil.authorizeInternal 显式传入，不得作为未认证请求的兜底身份。
     * <p>
     * <b>可配置</b>：生产部署通过环境变量 {@code SYSTEM_USER_ID}（对应配置项
     * {@code eve.helper.system-user-id}，见 {@code EveHelperSecurityConfig}）
     * 指定非默认值，避免「用户 1 即特权系统账户」固化进代码（IDOR 审计 LOW-9）。
     * 本常量值 1 为默认回退，供测试与未配置环境使用。
     */
    Integer SYSTEM_USER_ID = 1;

    // ── 022 P2 Redis 键常量（跨层消费方收敛，data-model.md §4） ──

    /**
     * 改密纪元标记键前缀（022 T006）。
     * <p>
     * 消费方:TokenService、JwtAuthorizationTokenFilter、UserApplicationService。
     * 格式:{@code auth:user:revoke_epoch:<userId>}
     */
    String REVOKE_EPOCH_KEY_PREFIX = "auth:user:revoke_epoch:";

    /**
     * 改密失败限流计数键前缀（022 T041）。
     * <p>
     * 消费方:UserApplicationService。
     * 格式:{@code pwd:attempt:<userId>}
     */
    String PWD_ATTEMPT_KEY_PREFIX = "pwd:attempt:";

    /**
     * 用户→会话反向索引键前缀（022 T005）。
     * <p>
     * 消费方:UserApplicationService、TokenService。
     * 格式:{@code refresh_user:<userId>}
     */
    String REFRESH_USER_KEY_PREFIX = "refresh_user:";

    /**
     * 系统同步角色 ID（QQ 机器人同步用的固定角色）。
     * <p>
     * 镜像 infrastructure/util/TaskConstant.CHARACTER_ID；
     * application 层禁 import infrastructure，故将守卫常量置于 shared。
     * 解绑角色时若 characterId 等于此值则拒绝（系统同步角色不可解绑）。
     */
    Integer SYSTEM_SYNC_CHARACTER_ID = 2112818290;

}
