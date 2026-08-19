package xyz.foolcat.eve.evehelper.domain.model.vo;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 钱包总览读模型。
 *
 * <p>角色/军团钱包整体概览的跨层查询结果载体。包含当前余额、收支合计、净流水、
 * 流水条数、统计截点时间,以及按类目(bucket)、按收入/支出类型、按 division 三个维度的汇总。</p>
 *
 * <p>金额统一使用 {@link Double}(与 ESI 金额精度约定一致),条数/计数使用 {@link Long},
 * 时间使用 {@link java.time.OffsetDateTime}。</p>
 *
 * @param currentBalance 当前余额
 * @param totalIncome    总收入
 * @param totalExpense   总支出
 * @param netFlow        净流水(收入 - 支出)
 * @param journalCount   流水条数
 * @param asOfTime       统计截点时间
 * @param categories     按交易类型汇总(见 {@link CategorySummary})
 * @param trend          按时间桶(bucket)汇总之趋势(见 {@link TrendPoint})
 * @param divisions      按 division 汇总(见 {@link DivisionSummary})
 */
public record WalletOverviewVO(
        Double currentBalance,
        Double totalIncome,
        Double totalExpense,
        Double netFlow,
        Long journalCount,
        OffsetDateTime asOfTime,
        List<CategorySummary> categories,
        List<TrendPoint> trend,
        List<DivisionSummary> divisions) {

    /**
     * 按交易类型(refType)的收入/支出/条数汇总。
     *
     * @param refType 交易类型(ESI ref_type)
     * @param income 该类型收入合计
     * @param expense 该类型支出合计
     * @param count 该类型流水条数
     */
    public record CategorySummary(String refType, Double income, Double expense, Long count) {}

    /**
     * 按时间桶(bucket)的收入/支出/净额趋势点。
     *
     * @param bucket 时间桶标识(如年月或天)
     * @param income 该桶收入
     * @param expense 该桶支出
     * @param net 该桶净额
     */
    public record TrendPoint(String bucket, Double income, Double expense, Double net) {}

    /**
     * 按 division 的余额/收支汇总。
     *
     * @param division 钱包 division 编号
     * @param balance 该 division 当前余额
     * @param income 该 division 收入
     * @param expense 该 division 支出
     */
    public record DivisionSummary(Integer division, Double balance, Double income, Double expense) {}
}