package xyz.foolcat.eve.evehelper.domain.model.vo;

import java.time.OffsetDateTime;

/**
 * 钱包总览摘要载体。
 *
 * <p>钱包整体概览的轻量聚合结果,仅含标量汇总字段,不含按类目/时间/division 的明细维度数组。
 * 供需要"一屏看头行"的场景使用,避免携带完整明细。</p>
 *
 * <p>金额统一使用 {@link Double},条数使用 {@link Long},统计时间使用
 * {@link java.time.OffsetDateTime}。</p>
 *
 * @param currentBalance 当前余额
 * @param totalIncome    总收入
 * @param totalExpense   总支出
 * @param netFlow        净流水(收入 - 支出)
 * @param journalCount   流水条数
 * @param asOfTime       统计截点时间
 */
public record WalletOverviewAggregate(
        Double currentBalance, Double totalIncome, Double totalExpense,
        Double netFlow, Long journalCount, OffsetDateTime asOfTime) {}