package xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system;

/**
 * 钱包总览按 division 汇总持久化载体。
 *
 * <p>作为数据库固定列聚合查询结果集(Mapper)的临时载体,规避 Mapper 直接返回领域类型。
 * 由仓储层将本 PO 映射为领域读模型
 * {@link xyz.foolcat.eve.evehelper.domain.model.vo.WalletOverviewVO.DivisionSummary}。
 * 不对应具体表。</p>
 *
 * @param division 钱包 division 编号
 * @param balance  该 division 当前余额
 * @param income   该 division 收入
 * @param expense  该 division 支出
 */
public record WalletOverviewDivisionPO(Integer division, Double balance, Double income, Double expense) {}