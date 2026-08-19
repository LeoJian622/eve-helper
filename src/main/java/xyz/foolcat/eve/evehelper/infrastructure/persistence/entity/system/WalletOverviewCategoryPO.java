package xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system;

/**
 * 钱包总览按交易类型汇总持久化载体。
 *
 * <p>作为数据库固定列聚合查询结果集(Mapper)的临时载体,规避 Mapper 直接返回领域类型。
 * 由仓储层将本 PO 映射为领域读模型
 * {@link xyz.foolcat.eve.evehelper.domain.model.vo.WalletOverviewVO.CategorySummary}。
 * 不对应具体表。</p>
 *
 * @param refType 交易类型(ESI ref_type)
 * @param income  该类型收入合计
 * @param expense 该类型支出合计
 * @param count   该类型流水条数
 */
public record WalletOverviewCategoryPO(String refType, Double income, Double expense, Long count) {}