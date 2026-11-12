package xyz.foolcat.eve.evehelper.domain.model.vo;

import java.time.OffsetDateTime;

/**
 * 军团成员追踪信息(领域读模型)。
 * <p>
 * 映射 ESI {@code /corporations/{id}/membertracking/} 返回的成员追踪数据。
 * 仅保留伴侣 APP 所需的五个字段(baseId/startDate 按 YAGNI 丢弃)。
 *
 * @param characterId 角色 ID
 * @param locationId  最近位置 ID(空间站/星系)
 * @param shipTypeId  最近舰船类型 ID
 * @param logonDate   最近登录时间
 * @param logoffDate  最近登出时间
 * @author Leojan
 * date 2026-11-13
 */
public record MemberTrackingItem(
        Integer characterId,
        Long locationId,
        Integer shipTypeId,
        OffsetDateTime logonDate,
        OffsetDateTime logoffDate
) {
}
