package xyz.foolcat.eve.evehelper.shared.kernel.enums;

/**
 * ESI 授权状态
 * <p>
 * 用于标识用户绑定角色相对 ESI 数据服务的授权有效性。
 * <ul>
 *     <li>AUTHORIZED:授权正常,持有 refreshToken 且能成功换取 accessToken</li>
 *     <li>EXPIRED:授权过期,持有 refreshToken 但换取 accessToken 失败(4xx invalid_grant)</li>
 *     <li>NOT_AUTHORIZED:未授权,无 refreshToken</li>
 *     <li>UNKNOWN:无法判定,判定过程失败(ESI 5xx / 网络 / 超时)</li>
 * </ul>
 *
 * @author Leojan
 * date 2026-08-04
 */
public enum EsiAuthStatus {

    AUTHORIZED("授权正常"),
    EXPIRED("授权过期"),
    NOT_AUTHORIZED("未授权"),
    UNKNOWN("无法判定");

    private final String description;

    EsiAuthStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
