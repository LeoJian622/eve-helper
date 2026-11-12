package xyz.foolcat.eve.evehelper.domain.model.vo;

import java.time.OffsetDateTime;

/**
 * 角色面板综合信息(US1 角色面板视图 VO)。
 * <p>
 * 组合公开信息、在线状态、位置、舰船类型与舰船名称,
 * 由 {@code CharacterPanelApplicationService#panel} 组装。
 * 所有字段均取自网关查询结果,应用层不做缓存。
 *
 * @param characterId    角色 ID
 * @param name           角色名
 * @param corporationId  军团 ID
 * @param allianceId     联盟 ID(无联盟时为 null)
 * @param birthday       角色创建时间
 * @param securityStatus 安全等级
 * @param online         当前是否在线
 * @param lastLogin      最近登录时间(可能为 null)
 * @param lastLogout     最近登出时间(可能为 null)
 * @param solarSystemId  星系 ID
 * @param stationId      空间站 ID(在建筑内时为 null)
 * @param structureId    建筑 ID(在空间站内时为 null)
 * @param shipTypeId     舰船类型 ID
 * @param shipName       舰船名称(解析不到时为 null)
 * @author Leojan
 */
public record CharacterPanelInfo(
        Integer characterId,
        String name,
        Integer corporationId,
        Integer allianceId,
        OffsetDateTime birthday,
        Double securityStatus,
        boolean online,
        OffsetDateTime lastLogin,
        OffsetDateTime lastLogout,
        Integer solarSystemId,
        Integer stationId,
        Long structureId,
        Integer shipTypeId,
        String shipName
) {
}
