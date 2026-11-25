package xyz.foolcat.eve.evehelper.domain.model.vo;

/**
 * 角色当前位置信息(ESI /characters/{id}/location/ 响应映射)。
 * <p>
 * 领域层读模型,供角色面板展示使用。需要授权 accessToken 方可查询。
 * <p>
 * 建筑内位置语义:structureId 有值时 stationId 为 null;空间站内反之。
 *
 * @param solarSystemId 星系 ID
 * @param stationId     空间站 ID(在建筑内时为 null)
 * @param structureId   建筑 ID(在空间站内时为 null)
 * @author Leojan
 */
public record CharacterLocationInfo(
        Integer solarSystemId,
        Integer stationId,
        Long structureId
) {
}
