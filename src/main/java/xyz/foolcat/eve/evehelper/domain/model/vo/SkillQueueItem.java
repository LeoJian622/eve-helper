package xyz.foolcat.eve.evehelper.domain.model.vo;

import java.time.OffsetDateTime;

/**
 * 技能队列条目(ESI /characters/{id}/skillqueue/ 响应映射)。
 * <p>
 * 领域层读模型,供角色面板展示使用。需要授权 accessToken 方可查询。
 *
 * @param skillId            技能类型 ID
 * @param finishedLevel      目标完成等级
 * @param trainingStartSp    训练开始时已有技能点
 * @param skillpointsInSkill 训练完成后总技能点
 * @param startDate          训练开始时间(可能为 null)
 * @param endDate            训练完成时间(可能为 null)
 * @param position           队列位置
 * @author Leojan
 */
public record SkillQueueItem(
        Integer skillId,
        Integer finishedLevel,
        Long trainingStartSp,
        Long skillpointsInSkill,
        OffsetDateTime startDate,
        OffsetDateTime endDate,
        Integer position
) {
}
