package xyz.foolcat.eve.evehelper.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.foolcat.eve.evehelper.application.security.AccessGuard;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.model.vo.WalletDivisionBalance;
import xyz.foolcat.eve.evehelper.domain.model.vo.WalletOverviewAggregate;
import xyz.foolcat.eve.evehelper.domain.model.vo.WalletOverviewVO;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.domain.repository.system.EveAccountRepository;
import xyz.foolcat.eve.evehelper.domain.repository.system.WalletJournalRepository;
import xyz.foolcat.eve.evehelper.domain.util.AuthorizeUtil;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;

import java.text.ParseException;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * WalletOverviewApplicationService 单元测试(TDD, US1)。
 *
 * <p>纯 Mockito 单测(不起 Spring 上下文),聚焦人物钱包总览用例契约:</p>
 * <ul>
 *     <li>入参边界:非法 range / start>end / range 与 start/end 同给 → PARAM_ERROR,且先于归属校验</li>
 *     <li>归属校验:合法入参后调用 accessGuard.requireOwnership(cid 字符串)</li>
 *     <li>range 解析:预设 range → 仓储收到解析后非 null 时间;全量 → null,null</li>
 *     <li>趋势粒度:null,null→月;有界≤92天→日;>92天→月</li>
 *     <li>组装:仓储聚合 → VO 字段透传、divisions=null;空聚合 → 零</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WalletOverviewApplicationService 人物钱包总览用例")
class WalletOverviewApplicationServiceTest {

    private static final int CID = 9001;
    private static final int CORP = 98454654;

    private static final long DAY_MS = 86400000L;

    @Mock
    private AccessGuard accessGuard;

    @Mock
    private WalletJournalRepository walletJournalRepository;

    @Mock
    private EsiGateway esiGateway;

    @Mock
    private AuthorizeUtil authorizeUtil;

    @Mock
    private EveAccountRepository eveAccountRepository;

    @InjectMocks
    private WalletOverviewApplicationService applicationService;

    /**
     * 016:为 fetchCharacterBalance / fetchCorporationBalances 提供通用 lenient stub,
     * 使不聚焦余额路径的测试不受影响(balance 默认 0)。
     */
    @BeforeEach
    void setupEsiBalanceDefaults() throws ParseException {
        EveAccount account = new EveAccount();
        account.setCharacterId(CID);
        account.setUserId(1);
        account.setCorpId(CORP);
        lenient().when(authorizeUtil.authorize(CID)).thenReturn(account);
        lenient().when(esiGateway.getAccessToken(eq(CID), anyInt())).thenReturn("Bearer token");
        lenient().when(esiGateway.queryCharacterWalletBalance(eq(CID), any())).thenReturn(0.0);
        // 军团:返回一个属于目标军团的角色,供 fetchCorporationBalances 找到 charId
        lenient().when(eveAccountRepository.getAccountList(777)).thenReturn(List.of(account));
        lenient().when(esiGateway.queryCorporationWalletBalances(eq(CORP), any())).thenReturn(List.of());
    }

    /* ============================ 入参边界 ============================ */

    @Nested
    @DisplayName("入参边界校验")
    class InputValidation {

        @Test
        @DisplayName("非法 range 抛 PARAM_ERROR,不触达归属校验")
        void invalidRange_rejectedBeforeOwnership() {
            assertThatThrownBy(() -> applicationService.getCharacterOverview(CID, "bogus", null, null))
                    .isInstanceOf(EveHelperException.class)
                    .hasMessageContaining("参数");
            verifyNoInteractions(accessGuard);
            verifyNoInteractions(walletJournalRepository);
        }

        @Test
        @DisplayName("start > end 抛 PARAM_ERROR,不触达归属校验")
        void startAfterEnd_rejectedBeforeOwnership() {
            Date start = new Date();
            Date end = new Date(start.getTime() - DAY_MS);

            assertThatThrownBy(() -> applicationService.getCharacterOverview(CID, null, start, end))
                    .isInstanceOf(EveHelperException.class)
                    .hasMessageContaining("参数");
            verifyNoInteractions(accessGuard);
            verifyNoInteractions(walletJournalRepository);
        }

        @Test
        @DisplayName("range 与 start/end 同时给出抛 PARAM_ERROR,不触达归属校验")
        void rangeWithExplicitDates_rejectedBeforeOwnership() {
            Date end = new Date();
            Date start = new Date(end.getTime() - DAY_MS);

            assertThatThrownBy(() -> applicationService.getCharacterOverview(CID, "last7d", start, end))
                    .isInstanceOf(EveHelperException.class)
                    .hasMessageContaining("参数");
            verifyNoInteractions(accessGuard);
            verifyNoInteractions(walletJournalRepository);
        }

        @Test
        @DisplayName("cid 为空抛 PARAM_ERROR,不触达归属校验")
        void nullCid_rejectedBeforeOwnership() {
            assertThatThrownBy(() -> applicationService.getCharacterOverview(null, "today", null, null))
                    .isInstanceOf(EveHelperException.class)
                    .hasMessageContaining("参数");
            verifyNoInteractions(accessGuard);
            verifyNoInteractions(walletJournalRepository);
        }
    }

    /* ============================ 归属校验 ============================ */

    @Nested
    @DisplayName("归属校验")
    class Ownership {

        @Test
        @DisplayName("合法入参后调用 requireOwnership(cid 字符串, 钱包总览)")
        void validInput_callsRequireOwnership() {
            applicationService.getCharacterOverview(CID, "last7d", null, null);

            verify(accessGuard).requireOwnership(String.valueOf(CID), "钱包总览");
        }
    }

    /* ============================ range 解析 ============================ */

    @Nested
    @DisplayName("range 解析")
    class RangeResolution {

        @Test
        @DisplayName("预设 range → 仓储收到解析后的非 null start/end")
        void presetRange_resolvesNonNullDates() {
            ArgumentCaptor<Date> startCaptor = ArgumentCaptor.forClass(Date.class);
            ArgumentCaptor<Date> endCaptor = ArgumentCaptor.forClass(Date.class);

            applicationService.getCharacterOverview(CID, "last7d", null, null);

            // 人物读:userId 传 null(FR-004 人物维共享,不加 user_id 谓词)
            verify(walletJournalRepository)
                    .selectOverviewAggregate(eq((long) CID), isNull(), isNull(), startCaptor.capture(), endCaptor.capture());
            assertThat(startCaptor.getValue()).isNotNull();
            assertThat(endCaptor.getValue()).isNotNull();
            assertThat(startCaptor.getValue().getTime())
                    .isLessThanOrEqualTo(endCaptor.getValue().getTime());
            // last7d 跨度约 7 天
            assertThat(endCaptor.getValue().getTime() - startCaptor.getValue().getTime())
                    .isBetween(6L * DAY_MS, 8L * DAY_MS);
        }

        @Test
        @DisplayName("全量(无 range 无 start/end)→ 仓储收到 null,null")
        void fullQuery_passesNullDates() {
            applicationService.getCharacterOverview(CID, null, null, null);

            verify(walletJournalRepository)
                    .selectOverviewAggregate(eq((long) CID), isNull(), isNull(), isNull(), isNull());
        }
    }

    /* ============================ 趋势粒度 ============================ */

    @Nested
    @DisplayName("趋势粒度")
    class Granularity {

        @Test
        @DisplayName("无时间范围(全量)→ 月粒度 %Y-%m")
        void fullQuery_monthGranularity() {
            applicationService.getCharacterOverview(CID, null, null, null);

            verify(walletJournalRepository).selectOverviewTrend(
                    eq((long) CID), isNull(), isNull(), isNull(), isNull(), eq("%Y-%m"));
        }

        @Test
        @DisplayName("有界区间 ≤92 天 → 日粒度 %Y-%m-%d")
        void boundedSmallRange_dayGranularity() {
            Date end = new Date();
            Date start = new Date(end.getTime() - 30L * DAY_MS);

            applicationService.getCharacterOverview(CID, null, start, end);

            verify(walletJournalRepository).selectOverviewTrend(
                    eq((long) CID), isNull(), isNull(), eq(start), eq(end), eq("%Y-%m-%d"));
        }

        @Test
        @DisplayName("有界区间 >92 天 → 月粒度 %Y-%m")
        void boundedWideRange_monthGranularity() {
            Date end = new Date();
            Date start = new Date(end.getTime() - 200L * DAY_MS);

            applicationService.getCharacterOverview(CID, null, start, end);

            verify(walletJournalRepository).selectOverviewTrend(
                    eq((long) CID), isNull(), isNull(), eq(start), eq(end), eq("%Y-%m"));
        }
    }

    /* ============================ 组装 ============================ */

    /* ============================ 军团总览(US2) ============================ */

    @Nested
    @DisplayName("军团钱包总览用例")
    class CorporationOverview {

        @Test
        @DisplayName("division 越界(0/8)抛 PARAM_ERROR,不触达归属校验")
        void invalidDivision_rejectedBeforeOwnership() {
            for (int bad : new int[]{0, 8}) {
                assertThatThrownBy(() -> applicationService.getCorporationOverview(CID, bad, null, null, null))
                        .isInstanceOf(EveHelperException.class)
                        .hasMessageContaining("参数");
            }
            verifyNoInteractions(accessGuard);
            verifyNoInteractions(walletJournalRepository);
        }

        @Test
        @DisplayName("cid 为空抛 PARAM_ERROR,不触达归属校验")
        void nullCid_rejectedBeforeOwnership() {
            assertThatThrownBy(() -> applicationService.getCorporationOverview(null, null, null, null, null))
                    .isInstanceOf(EveHelperException.class)
                    .hasMessageContaining("参数");
            verifyNoInteractions(accessGuard);
            verifyNoInteractions(walletJournalRepository);
        }

        @Test
        @DisplayName("合法人物入参后 authorize 反查 corpId,调用 corporationScope 取 scope,并透传仓储(016:余额改走 ESI)")
        void validCidInput_authorizesAndCallsCorporationScope() {
            when(accessGuard.corporationScope("军团钱包总览")).thenReturn(777L);
            when(walletJournalRepository.selectOverviewAggregate(eq((long) CORP), isNull(), eq(777L), isNull(), isNull()))
                    .thenReturn(new WalletOverviewAggregate(0.0, 0.0, 0.0, 0.0, 0L, null));
            when(walletJournalRepository.selectOverviewCategories(any(), any(), any(), any(), any())).thenReturn(List.of());
            when(walletJournalRepository.selectOverviewTrend(any(), any(), any(), any(), any(), any())).thenReturn(List.of());
            // 016:selectOverviewDivisionBalances 已移除(改走 ESI),仅保留 flow
            when(walletJournalRepository.selectOverviewDivisionFlow(eq((long) CORP), eq(777L), isNull(), isNull())).thenReturn(List.of());

            applicationService.getCorporationOverview(CID, null, null, null, null);

            // 人物维度归属:authorize 校验并反查 corpId(至少 1 次;fetchCorporationBalances 内部还会 authorize 同一 CID 取 ESI token,共 2 次)
            verify(authorizeUtil, atLeastOnce()).authorize(CID);
            // US2b:军团维度读改为 corporationScope 取 scope 透传仓储,不再 requireOwnership
            verify(accessGuard).corporationScope("军团钱包总览");
        }

        @Test
        @DisplayName("人物已授权但 corpId 为 null(数据异常)→ USER_ACCOUNT_NOT_EXIST,不触达 corporationScope(017 IMPORTANT-2)")
        void nullCorpId_afterAuthorize_rejected() {
            EveAccount noCorpAccount = new EveAccount();
            noCorpAccount.setCharacterId(CID);
            noCorpAccount.setUserId(1);
            noCorpAccount.setCorpId(null); // 数据异常:人物未绑定军团
            when(authorizeUtil.authorize(CID)).thenReturn(noCorpAccount);

            assertThatThrownBy(() -> applicationService.getCorporationOverview(CID, null, null, null, null))
                    .isInstanceOf(EveHelperException.class)
                    .hasMessageContaining("账号");
            // corpId 为 null 时 fail-closed,不触达后续军团查询路径
            verifyNoInteractions(accessGuard);
            verifyNoInteractions(walletJournalRepository);
        }

        @Test
        @DisplayName("军团全量:分账 1..7 补零,收支按 division 归并,currentBalance=ESI 各分账余额和(016)")
        void fullOverview_divisionsMergedAndBalanceSummed() throws ParseException {
            when(accessGuard.corporationScope("军团钱包总览")).thenReturn(777L);
            // 016:分账余额改由 ESI 直查,不再从仓储 selectOverviewDivisionBalances 取
            when(esiGateway.queryCorporationWalletBalances(eq(CORP), any()))
                    .thenReturn(List.of(
                            new WalletDivisionBalance(1, 100.0),
                            new WalletDivisionBalance(3, 50.0)));
            WalletOverviewAggregate agg = new WalletOverviewAggregate(999.0, 300.0, 100.0, 200.0, 5L, OffsetDateTime.now());
            when(walletJournalRepository.selectOverviewAggregate(eq((long) CORP), isNull(), eq(777L), isNull(), isNull())).thenReturn(agg);
            when(walletJournalRepository.selectOverviewCategories(any(), any(), any(), any(), any())).thenReturn(List.of());
            when(walletJournalRepository.selectOverviewTrend(any(), any(), any(), any(), any(), any())).thenReturn(List.of());
            when(walletJournalRepository.selectOverviewDivisionFlow(eq((long) CORP), eq(777L), isNull(), isNull()))
                    .thenReturn(List.of(
                            new WalletOverviewVO.DivisionSummary(1, 0.0, 10.0, 5.0),
                            new WalletOverviewVO.DivisionSummary(3, 0.0, 20.0, 8.0)));

            WalletOverviewVO vo = applicationService.getCorporationOverview(CID, null, null, null, null);

            assertThat(vo.divisions()).isNotNull();
            assertThat(vo.divisions()).hasSize(7);
            // 有数据分账:div1/div3 归并 ESI balance + DB income/expense
            assertThat(vo.divisions().get(0)).isEqualTo(new WalletOverviewVO.DivisionSummary(1, 100.0, 10.0, 5.0));
            assertThat(vo.divisions().get(2)).isEqualTo(new WalletOverviewVO.DivisionSummary(3, 50.0, 20.0, 8.0));
            // 无数据分账补零:div2、div4-7
            assertThat(vo.divisions().get(1)).isEqualTo(new WalletOverviewVO.DivisionSummary(2, 0.0, 0.0, 0.0));
            assertThat(vo.divisions().get(3)).isEqualTo(new WalletOverviewVO.DivisionSummary(4, 0.0, 0.0, 0.0));
            assertThat(vo.divisions().get(6)).isEqualTo(new WalletOverviewVO.DivisionSummary(7, 0.0, 0.0, 0.0));
            // currentBalance = ESI 分账余额和 150,覆盖 aggregate 的 999
            assertThat(vo.currentBalance()).isEqualTo(150.0);
            // 其余标量透传 aggregate(无 division 过滤)
            assertThat(vo.totalIncome()).isEqualTo(300.0);
            assertThat(vo.totalExpense()).isEqualTo(100.0);
            assertThat(vo.netFlow()).isEqualTo(200.0);
            assertThat(vo.journalCount()).isEqualTo(5L);
            // 016:不再调用 selectOverviewDivisionBalances(已改走 ESI)
            verify(walletJournalRepository, never()).selectOverviewDivisionBalances(any(), any());
        }

        @Test
        @DisplayName("军团单分账:三聚合带 division 过滤,divisions=null,余额来自 ESI(016)")
        void singleDivision_divisionFilteredNoDivisions() throws ParseException {
            when(accessGuard.corporationScope("军团钱包总览")).thenReturn(777L);
            // 016:单分账余额从 ESI 取 div=3 的 balance
            when(esiGateway.queryCorporationWalletBalances(eq(CORP), any()))
                    .thenReturn(List.of(new WalletDivisionBalance(3, 50.0)));
            WalletOverviewAggregate agg = new WalletOverviewAggregate(50.0, 20.0, 0.0, 20.0, 1L, null);
            when(walletJournalRepository.selectOverviewAggregate(eq((long) CORP), eq(3), eq(777L), isNull(), isNull())).thenReturn(agg);
            when(walletJournalRepository.selectOverviewCategories(any(), any(), any(), any(), any())).thenReturn(List.of());
            when(walletJournalRepository.selectOverviewTrend(any(), any(), any(), any(), any(), any())).thenReturn(List.of());

            WalletOverviewVO vo = applicationService.getCorporationOverview(CID, 3, null, null, null);

            verify(walletJournalRepository).selectOverviewAggregate(eq((long) CORP), eq(3), eq(777L), isNull(), isNull());
            verify(walletJournalRepository).selectOverviewCategories(eq((long) CORP), eq(3), eq(777L), isNull(), isNull());
            verify(walletJournalRepository).selectOverviewTrend(eq((long) CORP), eq(3), eq(777L), isNull(), isNull(), eq("%Y-%m"));
            // division 为 null 时才查分账分布;单分账不查
            verify(walletJournalRepository, never()).selectOverviewDivisionBalances(any(), any());
            verify(walletJournalRepository, never()).selectOverviewDivisionFlow(any(), any(), any(), any());
            assertThat(vo.divisions()).isNull();
            // 016:余额来自 ESI,不再从 aggregate.currentBalance
            assertThat(vo.currentBalance()).isEqualTo(50.0);
        }

        @Test
        @DisplayName("军团单分账边界:division=1 与 7 合法不抛,三聚合带对应 division 过滤,divisions=null")
        void singleDivision_boundaries_div1And7Legal() throws ParseException {
            for (int div : new int[]{1, 7}) {
                when(accessGuard.corporationScope("军团钱包总览")).thenReturn(777L);
                when(esiGateway.queryCorporationWalletBalances(eq(CORP), any()))
                        .thenReturn(List.of(new WalletDivisionBalance(div, 10.0)));
                WalletOverviewAggregate agg = new WalletOverviewAggregate(10.0, 5.0, 0.0, 5.0, 1L, null);
                when(walletJournalRepository.selectOverviewAggregate(eq((long) CORP), eq(div), eq(777L), isNull(), isNull()))
                        .thenReturn(agg);
                when(walletJournalRepository.selectOverviewCategories(any(), any(), any(), any(), any()))
                        .thenReturn(List.of());
                when(walletJournalRepository.selectOverviewTrend(any(), any(), any(), any(), any(), any()))
                        .thenReturn(List.of());

                WalletOverviewVO vo = applicationService.getCorporationOverview(CID, div, null, null, null);

                verify(walletJournalRepository)
                        .selectOverviewAggregate(eq((long) CORP), eq(div), eq(777L), isNull(), isNull());
                // 单分账 divisions 恒 null,绝不查分账分布
                verify(walletJournalRepository, never()).selectOverviewDivisionBalances(any(), any());
                verify(walletJournalRepository, never()).selectOverviewDivisionFlow(any(), any(), any(), any());
                assertThat(vo.divisions()).isNull();
                // 016:余额来自 ESI
                assertThat(vo.currentBalance()).isEqualTo(10.0);
            }
        }
    }

    /* ============================ VO 组装 ============================ */

    @Nested
    @DisplayName("VO 组装")
    class Assembly {

        @Test
        @DisplayName("仓储聚合(收支/类目/趋势)+ ESI 余额 → VO 字段组装,divisions=null(016)")
        void repoReturnsAggregate_voPassesThrough() throws ParseException {
            // 016:currentBalance 改由 ESI 直查
            when(esiGateway.queryCharacterWalletBalance(eq(CID), any())).thenReturn(100.0);
            OffsetDateTime asOf = OffsetDateTime.now();
            WalletOverviewAggregate agg = new WalletOverviewAggregate(
                    100.0, 200.0, 50.0, 150.0, 7L, asOf);
            List<WalletOverviewVO.CategorySummary> cats = List.of(
                    new WalletOverviewVO.CategorySummary("bounty_prizes", 100.0, 0.0, 3L));
            List<WalletOverviewVO.TrendPoint> trend = List.of(
                    new WalletOverviewVO.TrendPoint("2026-08", 100.0, 0.0, 100.0));
            when(walletJournalRepository.selectOverviewAggregate(eq((long) CID), isNull(), isNull(), isNull(), isNull()))
                    .thenReturn(agg);
            when(walletJournalRepository.selectOverviewCategories(any(), any(), any(), any(), any())).thenReturn(cats);
            when(walletJournalRepository.selectOverviewTrend(any(), any(), any(), any(), any(), any())).thenReturn(trend);

            WalletOverviewVO vo = applicationService.getCharacterOverview(CID, null, null, null);

            // 016:currentBalance 来自 ESI,与 aggregate.currentBalance 解耦
            assertThat(vo.currentBalance()).isEqualTo(100.0);
            assertThat(vo.totalIncome()).isEqualTo(200.0);
            assertThat(vo.totalExpense()).isEqualTo(50.0);
            assertThat(vo.netFlow()).isEqualTo(150.0);
            assertThat(vo.journalCount()).isEqualTo(7L);
            assertThat(vo.asOfTime()).isEqualTo(asOf);
            assertThat(vo.categories()).isEqualTo(cats);
            assertThat(vo.trend()).isEqualTo(trend);
            assertThat(vo.divisions()).isNull();
        }

        @Test
        @DisplayName("空聚合(仓储零值 + 空类别/趋势)+ ESI 余额 0 → VO 零值,divisions=null")
        void repoEmptyAggregate_voZeroed() {
            // @BeforeEach 已 stub ESI balance 返回 0.0
            WalletOverviewAggregate emptyAgg = new WalletOverviewAggregate(0.0, 0.0, 0.0, 0.0, 0L, null);
            when(walletJournalRepository.selectOverviewAggregate(eq((long) CID), isNull(), isNull(), isNull(), isNull()))
                    .thenReturn(emptyAgg);
            when(walletJournalRepository.selectOverviewCategories(any(), any(), any(), any(), any()))
                    .thenReturn(List.of());
            when(walletJournalRepository.selectOverviewTrend(any(), any(), any(), any(), any(), any()))
                    .thenReturn(List.of());

            WalletOverviewVO vo = applicationService.getCharacterOverview(CID, null, null, null);

            assertThat(vo.currentBalance()).isZero();
            assertThat(vo.totalIncome()).isZero();
            assertThat(vo.totalExpense()).isZero();
            assertThat(vo.netFlow()).isZero();
            assertThat(vo.journalCount()).isZero();
            assertThat(vo.asOfTime()).isNull();
            assertThat(vo.categories()).isEmpty();
            assertThat(vo.trend()).isEmpty();
            assertThat(vo.divisions()).isNull();
        }
    }
}