package xyz.foolcat.eve.evehelper.domain.model.vo;

/**
 * 角色属性信息(ESI /characters/{id}/attributes/ 响应映射)。
 * <p>
 * 领域层读模型,供角色面板展示使用。需要授权 accessToken 方可查询。
 *
 * @param intelligence 智力
 * @param memory       记忆
 * @param charisma     魅力
 * @param perception   感知
 * @param willpower    毅力
 * @param bonusRemaps  剩余额外洗点次数
 * @author Leojan
 */
public record CharacterAttributesInfo(
        Integer intelligence,
        Integer memory,
        Integer charisma,
        Integer perception,
        Integer willpower,
        Integer bonusRemaps
) {
}
