package xyz.foolcat.eve.evehelper.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.foolcat.eve.evehelper.application.security.AccessGuard;
import xyz.foolcat.eve.evehelper.domain.model.vo.WalletOverviewAggregate;
import xyz.foolcat.eve.evehelper.domain.model.vo.WalletOverviewVO;
import xyz.foolcat.eve.evehelper.domain.repository.system.WalletJournalRepository;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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

    private static final long DAY_MS = 86400000L;

    @Mock
    private AccessGuard accessGuard;

    @Mock
    private WalletJournalRepository walletJournalRepository;

    @InjectMocks
    private WalletOverviewApplicationService applicationService;

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

            verify(walletJournalRepository)
                    .selectOverviewAggregate(eq((long) CID), isNull(), startCaptor.capture(), endCaptor.capture());
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
                    .selectOverviewAggregate(eq((long) CID), isNull(), isNull(), isNull());
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
                    eq((long) CID), isNull(), isNull(), isNull(), eq("%Y-%m"));
        }

        @Test
        @DisplayName("有界区间 ≤92 天 → 日粒度 %Y-%m-%d")
        void boundedSmallRange_dayGranularity() {
            Date end = new Date();
            Date start = new Date(end.getTime() - 30L * DAY_MS);

            applicationService.getCharacterOverview(CID, null, start, end);

            verify(walletJournalRepository).selectOverviewTrend(
                    eq((long) CID), isNull(), eq(start), eq(end), eq("%Y-%m-%d"));
        }

        @Test
        @DisplayName("有界区间 >92 天 → 月粒度 %Y-%m")
        void boundedWideRange_monthGranularity() {
            Date end = new Date();
            Date start = new Date(end.getTime() - 200L * DAY_MS);

            applicationService.getCharacterOverview(CID, null, start, end);

            verify(walletJournalRepository).selectOverviewTrend(
                    eq((long) CID), isNull(), eq(start), eq(end), eq("%Y-%m"));
        }
    }

    /* ============================ 组装 ============================ */

    @Nested
    @DisplayName("VO 组装")
    class Assembly {

        @Test
        @DisplayName("仓储聚合 → VO 字段透传,divisions=null,categories/trend 透传")
        void repoReturnsAggregate_voPassesThrough() {
            OffsetDateTime asOf = OffsetDateTime.now();
            WalletOverviewAggregate agg = new WalletOverviewAggregate(
                    100.0, 200.0, 50.0, 150.0, 7L, asOf);
            List<WalletOverviewVO.CategorySummary> cats = List.of(
                    new WalletOverviewVO.CategorySummary("bounty_prizes", 100.0, 0.0, 3L));
            List<WalletOverviewVO.TrendPoint> trend = List.of(
                    new WalletOverviewVO.TrendPoint("2026-08", 100.0, 0.0, 100.0));
            when(walletJournalRepository.selectOverviewAggregate(eq((long) CID), isNull(), isNull(), isNull()))
                    .thenReturn(agg);
            when(walletJournalRepository.selectOverviewCategories(any(), any(), any(), any())).thenReturn(cats);
            when(walletJournalRepository.selectOverviewTrend(any(), any(), any(), any(), any())).thenReturn(trend);

            WalletOverviewVO vo = applicationService.getCharacterOverview(CID, null, null, null);

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
        @DisplayName("空聚合(仓储零值 + 空类别/趋势)→ VO 零值,divisions=null")
        void repoEmptyAggregate_voZeroed() {
            WalletOverviewAggregate emptyAgg = new WalletOverviewAggregate(0.0, 0.0, 0.0, 0.0, 0L, null);
            when(walletJournalRepository.selectOverviewAggregate(eq((long) CID), isNull(), isNull(), isNull()))
                    .thenReturn(emptyAgg);
            when(walletJournalRepository.selectOverviewCategories(any(), any(), any(), any()))
                    .thenReturn(List.of());
            when(walletJournalRepository.selectOverviewTrend(any(), any(), any(), any(), any()))
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