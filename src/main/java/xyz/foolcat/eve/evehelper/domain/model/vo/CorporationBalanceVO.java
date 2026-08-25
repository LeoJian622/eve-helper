package xyz.foolcat.eve.evehelper.domain.model.vo;

import java.util.List;

/**
 * 军团钱包余额读模型（018:按用户聚合余额查询）。
 *
 * <p>包含军团基本信息、总余额和各分账（division 1-7）余额明细。
 * 分账余额来自 ESI 直查（/corporations/{id}/wallets/），多角色权限合并后的结果。</p>
 *
 * @param corpId       军团ID
 * @param corpName     军团名称
 * @param totalBalance 总余额（各分账之和）
 * @param divisions    各分账余额明细
 */
public record CorporationBalanceVO(
        Integer corpId,
        String corpName,
        Double totalBalance,
        List<WalletDivisionBalance> divisions) {}
