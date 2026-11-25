package xyz.foolcat.eve.evehelper.application.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterPanelInfo;

import java.time.OffsetDateTime;

/**
 * 角色面板响应 DTO(022 US1 contracts §1 panel)。
 *
 * <p>扁平 VO 转嵌套结构:location(solarSystemId/stationId/structureId)、
 * ship(typeId/name)。null 字段不序列化,与 Result 信封 NON_NULL 一致。</p>
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
 * @param location       位置信息(嵌套)
 * @param ship           舰船信息(嵌套)
 * @author Leojan
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CharacterPanelResponse(
        Integer characterId,
        String name,
        Integer corporationId,
        Integer allianceId,
        OffsetDateTime birthday,
        Double securityStatus,
        boolean online,
        OffsetDateTime lastLogin,
        OffsetDateTime lastLogout,
        Location location,
        Ship ship
) {
    /**
     * 位置信息(solarSystemId/stationId/structureId 三者组合,stationId 与 structureId 互斥)。
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Location(
            Integer solarSystemId,
            Integer stationId,
            Long structureId
    ) {
    }

    /**
     * 舰船信息(typeId 为舰船类型 ID,name 为解析后的舰船名称)。
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Ship(
            Integer typeId,
            String name
    ) {
    }

    /**
     * 从领域 VO 构建响应 DTO。
     *
     * @param info 领域面板 VO
     * @return 响应 DTO
     */
    public static CharacterPanelResponse from(CharacterPanelInfo info) {
        return new CharacterPanelResponse(
                info.characterId(),
                info.name(),
                info.corporationId(),
                info.allianceId(),
                info.birthday(),
                info.securityStatus(),
                info.online(),
                info.lastLogin(),
                info.lastLogout(),
                new Location(info.solarSystemId(), info.stationId(), info.structureId()),
                new Ship(info.shipTypeId(), info.shipName())
        );
    }
}
