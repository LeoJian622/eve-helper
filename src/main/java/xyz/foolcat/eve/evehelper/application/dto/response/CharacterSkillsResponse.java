package xyz.foolcat.eve.evehelper.application.dto.response;

import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterAttributesInfo;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterSkillList;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterSkillsInfo;

import java.util.List;

/**
 * 角色技能响应 DTO(022 US1 contracts §1 skills)。
 *
 * <p>包含总技能点、未分配技能点、属性(嵌套)和技能列表(数组)。</p>
 *
 * @param totalSp       总技能点
 * @param unallocatedSp 未分配技能点
 * @param attributes    角色属性(嵌套)
 * @param skills        技能列表
 * @author Leojan
 */
public record CharacterSkillsResponse(
        Long totalSp,
        Integer unallocatedSp,
        Attributes attributes,
        List<SkillItem> skills
) {
    /**
     * 角色五维属性 + 额外洗点次数。
     */
    public record Attributes(
            Integer intelligence,
            Integer memory,
            Integer charisma,
            Integer perception,
            Integer willpower,
            Integer bonusRemaps
    ) {
        /**
         * 从领域属性 VO 构建。
         */
        public static Attributes from(CharacterAttributesInfo info) {
            return new Attributes(
                    info.intelligence(),
                    info.memory(),
                    info.charisma(),
                    info.perception(),
                    info.willpower(),
                    info.bonusRemaps()
            );
        }
    }

    /**
     * 单条技能信息(skillId/trainedSkillLevel/activeSkillLevel/skillpointsInSkill)。
     */
    public record SkillItem(
            Integer skillId,
            Integer trainedSkillLevel,
            Integer activeSkillLevel,
            Long skillpointsInSkill
    ) {
        /**
         * 从领域技能条目构建。
         */
        public static SkillItem from(CharacterSkillList.SkillItem item) {
            return new SkillItem(
                    item.skillId(),
                    item.trainedSkillLevel(),
                    item.activeSkillLevel(),
                    item.skillpointsInSkill()
            );
        }
    }

    /**
     * 从领域 VO 构建响应 DTO。
     *
     * @param info 领域技能 VO
     * @return 响应 DTO
     */
    public static CharacterSkillsResponse from(CharacterSkillsInfo info) {
        return new CharacterSkillsResponse(
                info.totalSp(),
                info.unallocatedSp(),
                Attributes.from(info.attributes()),
                info.skills().stream().map(SkillItem::from).toList()
        );
    }
}
