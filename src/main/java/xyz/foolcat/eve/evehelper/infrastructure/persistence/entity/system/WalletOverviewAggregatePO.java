package xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system;

import java.time.OffsetDateTime;

/**
 * 钱包总览摘要持久化载体。
 *
 * <p>仅作为数据库查询结果集(Mapper 聚合 SQL)的临时载体,规避 Mapper 直接返回领域 VO 类型。
 * 由仓储层将本 PO 映射为领域读模型 {@link xyz.foolcat.eve.evehelper.domain.model.vo.WalletOverviewAggregate}。
 * 不对应具体表,不参与增删改。</p>
 *
 * @param totalIncome    总收入
 * @param totalExpense   总支出
 * @param netFlow        净流水(收入 - 支出)
 * @param journalCount   流水条数
 * @param currentBalance 当前余额
 * @param asOfTime       统计截点时间
 */
public record WalletOverviewAggregatePO(
        Double totalIncome, Double totalExpense, Double netFlow,
        Long journalCount, Double currentBalance, OffsetDateTime asOfTime) {}