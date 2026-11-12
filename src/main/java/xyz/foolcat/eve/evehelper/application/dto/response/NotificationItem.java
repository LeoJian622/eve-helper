package xyz.foolcat.eve.evehelper.application.dto.response;

import xyz.foolcat.eve.evehelper.domain.model.entity.system.AppNotification;

import java.util.Date;

/**
 * 通知条目 DTO(022 US4)。
 *
 * <p>8 字段手工映射,零 MapStruct(KISS)。</p>
 *
 * @param id          通知主键
 * @param eventType   事件类型
 * @param title       标题
 * @param content     正文
 * @param characterId 关联角色ID(可空)
 * @param corpId      关联军团ID(可空)
 * @param isRead      是否已读
 * @param gmtCreate   创建时间
 * @author Leojian
 */
public record NotificationItem(
        Long id,
        String eventType,
        String title,
        String content,
        Integer characterId,
        Integer corpId,
        Boolean isRead,
        Date gmtCreate
) {

    /**
     * 从领域实体构造 DTO。
     *
     * @param entity 领域实体
     * @return DTO
     */
    public static NotificationItem from(AppNotification entity) {
        return new NotificationItem(
                entity.getId(),
                entity.getEventType(),
                entity.getTitle(),
                entity.getContent(),
                entity.getCharacterId(),
                entity.getCorpId(),
                entity.getIsRead(),
                entity.getGmtCreate()
        );
    }
}
