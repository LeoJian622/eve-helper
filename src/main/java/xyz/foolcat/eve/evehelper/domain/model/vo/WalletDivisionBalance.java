package xyz.foolcat.eve.evehelper.domain.model.vo;

/**
 * 军团钱包分账余额(ESI /corporations/{id}/wallets/ 直接返回的分账余额)。
 *
 * @param division 分账编号(1..7)
 * @param balance  该分账当前余额
 */
public record WalletDivisionBalance(Integer division, Double balance) {}
