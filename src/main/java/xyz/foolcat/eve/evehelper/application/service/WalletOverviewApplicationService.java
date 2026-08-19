package xyz.foolcat.eve.evehelper.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.application.security.AccessGuard;
import xyz.foolcat.eve.evehelper.domain.model.vo.WalletOverviewVO;
import xyz.foolcat.eve.evehelper.domain.repository.system.WalletJournalRepository;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;
import xyz.foolcat.eve.evehelper.shared.result.ResultCode;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * 钱包总览应用服务(人物侧, US1)。
 *
 * <p>提供人物钱包整体概览用例:当前余额、收支合计、净流水、条数、统计截点,
 * 以及按交易类型(类目)与按时间桶(趋势)两个维度的汇总。</p>
 *
 * <p>关键裁决(US1):人物总览 <b>division 传 null 不过滤</b>(存量人物 journal division 为 NULL/1,
 * 绝不可写 division=0 否则对存量数据全零);时间过滤只作用于收支/类目/趋势,<b>不</b>作用于
 * currentBalance 与 asOfTime(二者取全量最新);角色 total overview 不返回 divisions 维度(divisions=null)。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletOverviewApplicationService {

    private static final long DAY_MS = 86400000L;
    private static final long MAX_DAILY_SPAN_MS = 92L * DAY_MS;

    private static final Set<String> RANGES = Set.of("today", "last7d", "last30d", "last90d", "year");

    private final WalletJournalRepository walletJournalRepository;
    private final AccessGuard accessGuard;

    /**
     * 人物钱包总览。
     *
     * @param cid   人物ID
     * @param range 预设时间范围(today/last7d/last30d/last90d/year),与 start/end 二选一;都空为全量
     * @param start 起始时间(可空)
     * @param end   结束时间(可空)
     * @return 钱包总览读模型(divisions 恒为 null)
     */
    public WalletOverviewVO getCharacterOverview(Integer cid, String range, Date start, Date end) {
        // 入参边界校验先于归属校验:非法入参直接拒绝,不触达越权探测路径
        if (cid == null || !isValidRange(range) || (range != null && (start != null || end != null))) {
            log.warn("钱包总览参数不合法: cid={}, range={}, hasStart={}, hasEnd={}",
                    cid, range, start != null, end != null);
            throw new EveHelperException(ResultCode.PARAM_ERROR);
        }
        Date[] resolved = resolveRange(range, start, end);
        Date rStart = resolved[0];
        Date rEnd = resolved[1];
        // 归属校验先于业务逻辑:cid 为用户可控入参,须先确认该人物属于当前用户(防御 IDOR)
        accessGuard.requireOwnership(String.valueOf(cid), "钱包总览");

        Long ownerId = cid.longValue();
        // 人物总览:division 传 null 不过滤(硬性裁定)
        var aggregate = walletJournalRepository.selectOverviewAggregate(ownerId, null, rStart, rEnd);
        List<WalletOverviewVO.CategorySummary> categories =
                walletJournalRepository.selectOverviewCategories(ownerId, null, rStart, rEnd);
        String granularity = granularity(rStart, rEnd);
        List<WalletOverviewVO.TrendPoint> trend =
                walletJournalRepository.selectOverviewTrend(ownerId, null, rStart, rEnd, granularity);

        // 空聚合防御:无流水时仓储返回零值聚合;极端下聚合为 null 时回退为零值,list 为空
        Double cb = aggregate == null ? 0.0 : aggregate.currentBalance();
        Double ti = aggregate == null ? 0.0 : aggregate.totalIncome();
        Double te = aggregate == null ? 0.0 : aggregate.totalExpense();
        Double nf = aggregate == null ? 0.0 : aggregate.netFlow();
        Long jc = aggregate == null ? 0L : aggregate.journalCount();
        java.time.OffsetDateTime asOf = aggregate == null ? null : aggregate.asOfTime();

        return new WalletOverviewVO(
                cb, ti, te, nf, jc, asOf,
                categories == null ? Collections.emptyList() : categories,
                trend == null ? Collections.emptyList() : trend,
                null);
    }

    private boolean isValidRange(String range) {
        return range == null || RANGES.contains(range);
    }

    /**
     * 解析时间范围(硬性裁定)。range 预设与 start/end 二选一;都空 → 全量(null,null)。
     *
     * @return 长度 2 数组[start, end]。
     */
    private Date[] resolveRange(String range, Date start, Date end) {
        if (range == null) {
            if (start != null && end != null && end.before(start)) {
                throw new EveHelperException(ResultCode.PARAM_ERROR);
            }
            return new Date[]{start, end};
        }
        Date now = new Date();
        long nowMs = now.getTime();
        long startMs;
        switch (range) {
            case "today" -> {
                OffsetDateTime today = OffsetDateTime.now(ZoneOffset.UTC).withHour(0).withMinute(0).withSecond(0).withNano(0);
                return new Date[]{Date.from(today.toInstant()), now};
            }
            case "last7d" -> startMs = nowMs - 7L * DAY_MS;
            case "last30d" -> startMs = nowMs - 30L * DAY_MS;
            case "last90d" -> startMs = nowMs - 90L * DAY_MS;
            case "year" -> startMs = nowMs - 365L * DAY_MS;
            default -> throw new EveHelperException(ResultCode.PARAM_ERROR);
        }
        return new Date[]{new Date(startMs), now};
    }

    /**
     * 趋势桶粒度(硬性裁定):s,e 均 null(全量)→ 月;有界区间 e-s ≤ 92 天 → 日;否则月。
     */
    private String granularity(Date s, Date e) {
        if (s == null || e == null) {
            return "%Y-%m";
        }
        return (e.getTime() - s.getTime()) <= MAX_DAILY_SPAN_MS ? "%Y-%m-%d" : "%Y-%m";
    }
}