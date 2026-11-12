package xyz.foolcat.eve.evehelper.application.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import xyz.foolcat.eve.evehelper.domain.model.vo.SkillQueueInfo;
import xyz.foolcat.eve.evehelper.domain.model.vo.SkillQueueItem;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * 技能队列响应 DTO(022 US1 contracts §1 skill-queue)。
 *
 * <p>queue 为技能队列条目列表(含 skillName,由控制器层批量解析后注入);
 * queueEndsAt 为队列全部完成时间(空队列时为 null,不序列化)。</p>
 *
 * @param queue       技能队列条目列表
 * @param queueEndsAt 队列全部完成时间(末条 endDate;空队列时为 null)
 * @author Leojan
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SkillQueueResponse(
        List<QueueItem> queue,
        OffsetDateTime queueEndsAt
) {
    /**
     * 技能队列单条条目(skillId/skillName/finishedLevel/trainingStartSp/skillpointsInSkill/endDate/position)。
     *
     * @param skillId            技能类型 ID
     * @param skillName          技能名称(由 InvTypesService 批量解析;解析不到为 null)
     * @param finishedLevel      目标完成等级
     * @param trainingStartSp    训练开始时已有技能点
     * @param skillpointsInSkill 训练完成后总技能点
     * @param endDate            训练完成时间(可能为 null)
     * @param position           队列位置
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record QueueItem(
            Integer skillId,
            String skillName,
            Integer finishedLevel,
            Long trainingStartSp,
            Long skillpointsInSkill,
            OffsetDateTime endDate,
            Integer position
    ) {
    }

    /**
     * 从领域 VO 构建响应 DTO。
     *
     * @param info     领域技能队列 VO
     * @param nameMap  skillId → skillName 映射(控制器层批量解析结果;解析不到的 key 对应 null)
     * @return 响应 DTO
     */
    public static SkillQueueResponse from(SkillQueueInfo info, Map<Integer, String> nameMap) {
        List<QueueItem> items = info.queue().stream()
                .map(item -> new QueueItem(
                        item.skillId(),
                        nameMap.get(item.skillId()),
                        item.finishedLevel(),
                        item.trainingStartSp(),
                        item.skillpointsInSkill(),
                        item.endDate(),
                        item.position()
                ))
                .toList();
        return new SkillQueueResponse(items, info.queueEndsAt());
    }
}
