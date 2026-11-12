package xyz.foolcat.eve.evehelper.domain.model.vo;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 技能队列综合信息(US1 技能队列视图 VO)。
 * <p>
 * 由 {@code CharacterPanelApplicationService#skillQueue} 组装。
 * <p>
 * 空队列语义:{@code queue} 为空列表、{@code queueEndsAt} 为 null,
 * 与查询失败(异常)可区分。
 *
 * @param queue       技能队列(空队列返回空列表)
 * @param queueEndsAt 队列全部完成时间(末条 endDate;空队列时为 null)
 * @author Leojan
 */
public record SkillQueueInfo(
        List<SkillQueueItem> queue,
        OffsetDateTime queueEndsAt
) {
}
