package xyz.foolcat.eve.evehelper.domain.model.vo;

import java.util.List;

/**
 * 用户钱包余额聚合读模型（018:按用户聚合余额查询）。
 *
 * <p>顶层返回结构，包含当前用户绑定的所有人物余额和关联军团余额。</p>
 *
 * @param characters   人物余额列表
 * @param corporations 军团余额列表（按 corpId 去重，同一军团只出现一次）
 */
public record UserWalletBalanceVO(
        List<CharacterBalanceVO> characters,
        List<CorporationBalanceVO> corporations) {}
