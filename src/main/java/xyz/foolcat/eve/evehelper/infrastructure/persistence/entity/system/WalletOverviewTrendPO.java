package xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system;

/**
 * 钱包总览按时间桶趋势持久化载体。
 *
 * <p>作为数据库固定列聚合查询结果集(Mapper)的临时载体,规避 Mapper 直接返回领域类型。
 * 由仓储层将本 PO 映射为领域读模型
 * {@link xyz.foolcat.eve.evehelper.domain.model.vo.WalletOverviewVO.TrendPoint}。
 * 不对应具体表。</p>
 *
 * @param bucket  时间桶标识(如年月或天)
 * @param income  该桶收入
 * @param expense 该桶支出
 * @param net     该桶净额
 */
public record WalletOverviewTrendPO(String bucket, Double income, Double expense, Double net) {}