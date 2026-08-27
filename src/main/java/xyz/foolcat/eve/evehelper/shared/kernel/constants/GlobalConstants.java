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

}
