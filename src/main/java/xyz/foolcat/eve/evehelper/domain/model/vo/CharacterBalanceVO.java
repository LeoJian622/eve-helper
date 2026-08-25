package xyz.foolcat.eve.evehelper.domain.model.vo;

import xyz.foolcat.eve.evehelper.shared.kernel.enums.BalanceQueryStatus;

/**
 * 人物钱包余额读模型（018:按用户聚合余额查询）。
 *
 * <p>包含人物基本信息、当前余额与查询状态。余额来自 ESI 直查（/characters/{id}/wallet/）。</p>
 *
 * <p>018 IMPORTANT-2：{@code status} 用于区分「余额确实为 0」与「余额没查到」。
 * 此前查询失败被降级为 {@code 0.0}，与真实的零余额同形，调用方无从分辨。
 * 现约定：{@code status != OK} 时 {@code balance} 为 {@code null}，
 * 调用方（尤其展示 ISK 资产的前端）必须据 status 判断数值可信度，
 * 不可把 {@code null} 或 0 直接当作「账上没钱」。</p>
 *
 * @param characterId   人物ID
 * @param characterName 人物名称
 * @param balance       当前余额（ISK）；status 非 OK 时为 null
 * @param status        查询状态（OK / UNAUTHORIZED / UPSTREAM_ERROR）
 */
public record CharacterBalanceVO(
        Integer characterId,
        String characterName,
        Double balance,
        BalanceQueryStatus status) {
}
