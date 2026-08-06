package xyz.foolcat.eve.evehelper.domain.model.vo;

/**
 * 退税计算结果领域读模型。
 *
 * <p>领域服务 {@code WalletJournalService.countBoundsReturn} 生成此读模型;
 * 由 application/interfaces 层负责转换为响应 DTO(如 {@code TaxReturnDTO})。</p>
 *
 * @param name   人物名称
 * @param amount 退税金额
 * @author Leojan
 */
public record TaxReturnResult(String name, Double amount) {
}
