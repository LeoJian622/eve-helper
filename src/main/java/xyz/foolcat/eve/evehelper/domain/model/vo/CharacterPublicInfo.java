package xyz.foolcat.eve.evehelper.domain.model.vo;

import java.time.OffsetDateTime;

/**
 * 角色公开信息(ESI /characters/{id}/ 响应映射)。
 * <p>
 * 领域层读模型,供角色面板展示使用。所有字段均来自 ESI 公开端点,无需授权。
 *
 * @param characterId    角色 ID
 * @param name           角色名
 * @param corporationId  军团 ID
 * @param allianceId     联盟 ID(无联盟时为 null)
 * @param birthday       角色创建时间
 * @param securityStatus 安全等级
 * @author Leojan
 */
public record CharacterPublicInfo(
        Integer characterId,
        String name,
        Integer corporationId,
        Integer allianceId,
        OffsetDateTime birthday,
        Double securityStatus
) {
}
