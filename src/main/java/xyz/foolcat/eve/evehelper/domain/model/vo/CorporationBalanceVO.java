package xyz.foolcat.eve.evehelper.domain.model.vo;

import java.util.List;

/**
 * 军团钱包余额读模型（018:按用户聚合余额查询）。
 *
 * <p>包含军团基本信息、总余额和各分账（division 1-7）余额明细。
 * 分账余额来自 ESI 直查（/corporations/{id}/wallets/），多角色权限合并后的结果。</p>
 *
 * <p>018 IMPORTANT-2：{@code partial} 标记本次合并是否不完整。军团分账靠用户在该军团的
 * 多个人物逐个尝试并取并集，任一人物失败都会让结果缺少它本可贡献的 division，
 * 而 {@code totalBalance} 只是已取到分账之和 —— 与「军团账上真的只有这么多」同形。
 * 例如 7 个分账中 4 个查询失败时，totalBalance 会系统性低估且无任何提示。
 * 故 {@code partial} 为 true 时，调用方不应把 totalBalance 当作军团总资产。</p>
 *
 * @param corpId                  军团ID
 * @param corpName                军团名称
 * @param totalBalance            已取到的各分账余额之和（partial 为 true 时不代表军团总资产）
 * @param divisions               已取到的分账余额明细（按 division 升序）
 * @param partial                 是否存在人物查询失败导致分账集合不完整
 * @param unavailableCharacterIds 查询失败的人物ID（partial 为 true 时非空），便于定位需重新授权的角色
 */
public record CorporationBalanceVO(
        Integer corpId,
        String corpName,
        Double totalBalance,
        List<WalletDivisionBalance> divisions,
        boolean partial,
        List<Integer> unavailableCharacterIds) {
}
