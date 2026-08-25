package xyz.foolcat.eve.evehelper.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.application.security.AccessGuard;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.model.vo.WalletDivisionBalance;
import xyz.foolcat.eve.evehelper.domain.model.vo.WalletOverviewVO;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.domain.repository.system.EveAccountRepository;
import xyz.foolcat.eve.evehelper.domain.repository.system.WalletJournalRepository;
import xyz.foolcat.eve.evehelper.domain.util.AuthorizeUtil;
import xyz.foolcat.eve.evehelper.shared.kernel.constants.GlobalConstants;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;
import xyz.foolcat.eve.evehelper.shared.result.ResultCode;

import java.text.ParseException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final EsiGateway esiGateway;
    private final AuthorizeUtil authorizeUtil;
    private final EveAccountRepository eveAccountRepository;

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
        // 016:currentBalance 改由 ESI 直查(/characters/{id}/wallet/),不再从 journal 聚合取
        Double cb = fetchCharacterBalance(cid);
        // 人物总览:division 传 null 不过滤(硬性裁定),userId 也传 null(FR-004 人物维共享,不加 user_id 谓词)
        var aggregate = walletJournalRepository.selectOverviewAggregate(ownerId, null, null, rStart, rEnd);
        List<WalletOverviewVO.CategorySummary> categories =
                walletJournalRepository.selectOverviewCategories(ownerId, null, null, rStart, rEnd);
        String granularity = granularity(rStart, rEnd);
        List<WalletOverviewVO.TrendPoint> trend =
                walletJournalRepository.selectOverviewTrend(ownerId, null, null, rStart, rEnd, granularity);

        // 空聚合防御:无流水时仓储返回零值聚合;极端下聚合为 null 时回退为零值,list 为空
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

    /**
     * 军团钱包总览。
     *
     * <p>按人物维度入口:通过人物ID归属校验后反查军团ID,再执行军团总览业务。
     * 人物ID同时完成归属校验(authorize)与军团定位(EveAccount.corpId),
     * 消除直接暴露军团ID的 IDOR 风险。</p>
     *
     * @param cid      人物ID(归属校验 + 反查军团ID)
     * @param division 军团分账(1..7);null 为全量(分账分布 1..7,currentBalance=各分账最新余额和)
     * @param range    预设时间范围(today/last7d/last30d/last90d/year),与 start/end 二选一;都空为全量
     * @param start    起始时间(可空)
     * @param end      结束时间(可空)
     * @return 军团钱包总览读模型(全量 divisions 为 1..7 分账分布,单分账 divisions=null)
     */
    public WalletOverviewVO getCorporationOverview(Integer cid, Integer division, String range, Date start, Date end) {
        // 入参边界校验先于归属校验:非法入参直接拒绝,不触达越权探测路径
        if (cid == null || !isValidCorpDivision(division) || !isValidRange(range)
                || (range != null && (start != null || end != null))) {
            log.warn("军团钱包总览参数不合法: cid={}, division={}, range={}, hasStart={}, hasEnd={}",
                    cid, division, range, start != null, end != null);
            throw new EveHelperException(ResultCode.PARAM_ERROR);
        }
        Date[] resolved = resolveRange(range, start, end);
        Date rStart = resolved[0];
        Date rEnd = resolved[1];
        // 人物维度归属:authorize 校验该人物属于当前用户,同时反查军团ID
        EveAccount account = authorizeUtil.authorize(cid);
        Integer corpId = account.getCorpId();
        // 防御:人物未绑定军团(数据异常)时 corpId 为 null,后续 longValue()/仓储查询会 NPE 或静默零值;
        // fail-closed 显式拒绝,提示账号状态不完整(区别于参数错误的 400 与越权的 403)
        if (corpId == null) {
            log.warn("军团钱包总览:人物 {} 未绑定军团(account.corpId=null),拒绝访问", cid);
            throw new EveHelperException(ResultCode.USER_ACCOUNT_NOT_EXIST);
        }
        // 军团维度读过滤(US2b):corporationScope 返回当前同步者 userId(ROOT=null 看全量),
        // 透传 selectOverview* 仓储按 user_id 过滤,军团总览只有同步者数据可见
        Long scope = accessGuard.corporationScope("军团钱包总览");

        // 016:军团余额改由 ESI 直查(/corporations/{id}/wallets/),取各分账实时余额
        List<WalletDivisionBalance> esiBalances = fetchCorporationBalances(corpId, scope);

        Long ownerId = corpId.longValue();
        // 单分账带 division 过滤,全量无过滤;收支/类目/趋势沿用人物三条查询
        var aggregate = walletJournalRepository.selectOverviewAggregate(ownerId, division, scope, rStart, rEnd);
        List<WalletOverviewVO.CategorySummary> categories =
                walletJournalRepository.selectOverviewCategories(ownerId, division, scope, rStart, rEnd);
        String granularity = granularity(rStart, rEnd);
        List<WalletOverviewVO.TrendPoint> trend =
                walletJournalRepository.selectOverviewTrend(ownerId, division, scope, rStart, rEnd, granularity);

        Double ti = aggregate == null ? 0.0 : aggregate.totalIncome();
        Double te = aggregate == null ? 0.0 : aggregate.totalExpense();
        Double nf = aggregate == null ? 0.0 : aggregate.netFlow();
        Long jc = aggregate == null ? 0L : aggregate.journalCount();
        OffsetDateTime asOf = aggregate == null ? null : aggregate.asOfTime();

        // ESI 余额按 division 建索引,供全量/单分账共用
        Map<Integer, Double> balanceByDivision = new HashMap<>();
        if (esiBalances != null) {
            for (WalletDivisionBalance b : esiBalances) {
                balanceByDivision.put(b.division(), b.balance() == null ? 0.0 : b.balance());
            }
        }

        Double cb;
        List<WalletOverviewVO.DivisionSummary> divs = null;
        if (division == null) {
            // 军团全量:分账分布合并为 1..7 补零;currentBalance = 各分账 ESI 余额之和
            divs = mergeCorpDivisions(balanceByDivision,
                    walletJournalRepository.selectOverviewDivisionFlow(ownerId, scope, rStart, rEnd));
            cb = 0.0;
            for (WalletOverviewVO.DivisionSummary d : divs) {
                cb += d.balance();
            }
        } else {
            // 单分账:直接从 ESI 余额取该 division 值,无数据回退 0
            cb = balanceByDivision.getOrDefault(division, 0.0);
        }

        return new WalletOverviewVO(
                cb, ti, te, nf, jc, asOf,
                categories == null ? Collections.emptyList() : categories,
                trend == null ? Collections.emptyList() : trend,
                divs);
    }

    private boolean isValidCorpDivision(Integer division) {
        return division == null || (division >= 1 && division <= 7);
    }

    /**
     * 军团全量分账分布:余额按 division(Map 入参,016 由 ESI 提供)与收支按 division 归并,按 1..7 补零成有序列表。
     */
    private List<WalletOverviewVO.DivisionSummary> mergeCorpDivisions(
            Map<Integer, Double> balanceByDiv, List<WalletOverviewVO.DivisionSummary> flow) {
        Map<Integer, WalletOverviewVO.DivisionSummary> flowByDiv = new HashMap<>();
        if (flow != null) {
            for (WalletOverviewVO.DivisionSummary d : flow) {
                flowByDiv.put(d.division(), d);
            }
        }
        List<WalletOverviewVO.DivisionSummary> result = new ArrayList<>(7);
        for (int div = 1; div <= 7; div++) {
            double bal = balanceByDiv == null ? 0.0 : balanceByDiv.getOrDefault(div, 0.0);
            WalletOverviewVO.DivisionSummary f = flowByDiv.get(div);
            double inc = f == null ? 0.0 : (f.income() == null ? 0.0 : f.income());
            double exp = f == null ? 0.0 : (f.expense() == null ? 0.0 : f.expense());
            result.add(new WalletOverviewVO.DivisionSummary(div, bal, inc, exp));
        }
        return result;
    }

    /**
     * 016:人物钱包余额直查 ESI。
     *
     * <p>通过 authorize 拿 EveAccount → getAccessToken → queryCharacterWalletBalance。
     * 任何环节失败(balance 取不到/授权失败/网络异常)回退 0,不影响总览其余字段。</p>
     */
    private Double fetchCharacterBalance(Integer cid) {
        try {
            EveAccount account = authorizeUtil.authorize(cid);
            String token = esiGateway.getAccessToken(cid, account.getUserId());
            Double balance = esiGateway.queryCharacterWalletBalance(cid, token);
            return balance == null ? 0.0 : balance;
        } catch (ParseException e) {
            log.warn("人物钱包余额 ESI 查询失败(JWT): cid={}", cid, e);
            return 0.0;
        } catch (RuntimeException e) {
            log.warn("人物钱包余额 ESI 查询失败: cid={}", cid, e);
            return 0.0;
        }
    }

    /**
     * 016:军团钱包各分账余额直查 ESI。
     *
     * <p>从 scope(userId)反查该用户在该军团的角色 → getAccessToken → queryCorporationWalletBalances。
     * scope 为 null(管理员/ROOT)时以 SYSTEM_USER_ID 的角色列表为候选(全局查找)。</p>
     */
    private List<WalletDivisionBalance> fetchCorporationBalances(Integer corpId, Long scope) {
        int lookupUserId = scope == null ? GlobalConstants.SYSTEM_USER_ID : scope.intValue();
        Integer charId = resolveCharIdInCorp(corpId, lookupUserId);
        if (charId == null) {
            log.warn("军团钱包余额 ESI 查询:找不到军团 {} 的关联角色(userId={}),回退空余额", corpId, lookupUserId);
            return Collections.emptyList();
        }
        try {
            EveAccount account = authorizeUtil.authorize(charId);
            String token = esiGateway.getAccessToken(charId, account.getUserId());
            List<WalletDivisionBalance> balances = esiGateway.queryCorporationWalletBalances(corpId, token);
            return balances == null ? Collections.emptyList() : balances;
        } catch (ParseException e) {
            log.warn("军团钱包余额 ESI 查询失败(JWT): corpId={}, charId={}", corpId, charId, e);
            return Collections.emptyList();
        } catch (RuntimeException e) {
            log.warn("军团钱包余额 ESI 查询失败: corpId={}, charId={}", corpId, charId, e);
            return Collections.emptyList();
        }
    }

    /**
     * 从指定用户的角色列表中查找属于目标军团的角色ID。
     */
    private Integer resolveCharIdInCorp(Integer corpId, int userId) {
        List<EveAccount> accounts = eveAccountRepository.getAccountList(userId);
        if (accounts == null) {
            return null;
        }
        return accounts.stream()
                .filter(a -> corpId.equals(a.getCorpId()))
                .map(EveAccount::getCharacterId)
                .findFirst()
                .orElse(null);
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