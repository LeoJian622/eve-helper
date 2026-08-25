package xyz.foolcat.eve.evehelper.domain.model.vo;

/**
 * 人物钱包余额读模型（018:按用户聚合余额查询）。
 *
 * <p>包含人物基本信息和当前余额。余额来自 ESI 直查（/characters/{id}/wallet/）。</p>
 *
 * @param characterId   人物ID
 * @param characterName 人物名称
 * @param balance       当前余额（ISK）
 */
public record CharacterBalanceVO(
        Integer characterId,
        String characterName,
        Double balance) {}
