package xyz.foolcat.eve.evehelper.domain.model.vo;

import java.util.List;

/**
 * 角色技能列表(ESI /characters/{id}/skills/ 响应映射)。
 * <p>
 * 领域层读模型,供角色面板展示使用。需要授权 accessToken 方可查询。
 *
 * @param totalSp        总技能点
 * @param unallocatedSp  未分配技能点
 * @param skills         技能列表
 * @author Leojan
 */
public record CharacterSkillList(
        Long totalSp,
        Integer unallocatedSp,
        List<SkillItem> skills
) {
    /**
     * 单条技能信息。
     *
     * @param skillId            技能类型 ID
     * @param trainedSkillLevel  已训练等级
     * @param activeSkillLevel   当前生效等级
     * @param skillpointsInSkill 当前技能点
     */
    public record SkillItem(
            Integer skillId,
            Integer trainedSkillLevel,
            Integer activeSkillLevel,
            Long skillpointsInSkill
    ) {
    }
}
