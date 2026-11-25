package xyz.foolcat.eve.evehelper.domain.model.vo;

import java.util.List;

/**
 * 角色技能综合信息(US1 技能视图 VO)。
 * <p>
 * 合并技能列表与属性信息,由 {@code CharacterPanelApplicationService#skills} 组装。
 *
 * @param totalSp        总技能点
 * @param unallocatedSp  未分配技能点
 * @param attributes     角色属性(整体嵌入)
 * @param skills         技能列表
 * @author Leojan
 */
public record CharacterSkillsInfo(
        Long totalSp,
        Integer unallocatedSp,
        CharacterAttributesInfo attributes,
        List<CharacterSkillList.SkillItem> skills
) {
}
