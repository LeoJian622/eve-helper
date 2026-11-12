package xyz.foolcat.eve.evehelper.domain.model.vo;

import java.time.OffsetDateTime;

/**
 * 角色在线状态(ESI /characters/{id}/online/ 响应映射)。
 * <p>
 * 领域层读模型,供角色面板展示使用。需要授权 accessToken 方可查询。
 *
 * @param online     当前是否在线
 * @param lastLogin  最近登录时间(可能为 null)
 * @param lastLogout 最近登出时间(可能为 null)
 * @author Leojan
 */
public record CharacterOnlineInfo(
        boolean online,
        OffsetDateTime lastLogin,
        OffsetDateTime lastLogout
) {
}
