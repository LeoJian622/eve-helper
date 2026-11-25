package xyz.foolcat.eve.evehelper.domain.model.vo;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 团员追踪聚合读模型(T038)。
 * <p>
 * 由 {@code MemberTrackingApplicationService} 在应用层合成,
 * 将 ESI 原始 {@link MemberTrackingItem} 列表附加 online 派生字段,
 * 并打上追踪时间戳。
 *
 * @param members   成员追踪列表(含 online 派生)
 * @param trackedAt 本次追踪时间戳
 * @author Leojian
 */
public record MemberTrackingInfo(
        List<MemberTrackingMember> members,
        OffsetDateTime trackedAt
) {

    /**
     * 单个成员追踪信息(含 online 派生)。
     *
     * @param characterId 角色 ID
     * @param online      是否在线(服务端由 logon/logoff 派生)
     * @param locationId  最近位置 ID
     * @param shipTypeId  最近舰船类型 ID
     * @param logonDate   最近登录时间
     * @param logoffDate  最近登出时间
     */
    public record MemberTrackingMember(
            Integer characterId,
            boolean online,
            Long locationId,
            Integer shipTypeId,
            OffsetDateTime logonDate,
            OffsetDateTime logoffDate
    ) {
    }
}
