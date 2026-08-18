package xyz.foolcat.eve.evehelper.domain.service.system;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.WalletTransaction;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.domain.repository.system.WalletTransactionRepository;
import xyz.foolcat.eve.evehelper.domain.service.system.WalletTransactionService;
import xyz.foolcat.eve.evehelper.domain.util.AuthorizeUtil;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * WalletTransactionService 单元测试(TDD 红-绿)。
 *
 * <p>纯 Mockito 单测(不起 Spring 上下文),mock {@link EsiGateway}/{@link AuthorizeUtil}/
 * {@link WalletTransactionRepository},聚焦核心同步逻辑:</p>
 * <ul>
 *     <li>from_id 游标翻页(plan D2):首页 null → 末条 transactionId 续拉 → 空页终止</li>
 *     <li>归属回填(plan):人物 ownerType=character/ownerId=cId/division=0;军团按当前 division</li>
 *     <li>军团 division 1..7 循环失败隔离(plan)：某分账失败不影响其它成功分账落库,且汇总抛 EveHelperException</li>
 *     <li>游标上限保护 MAX_CURSOR_PAGES,防死循环</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WalletTransactionService 钱包交易同步")
class WalletTransactionServiceTest {

    @Mock
    private EsiGateway esiApiService;

    @Mock
    private AuthorizeUtil authorizeUtil;

    @Mock
    private WalletTransactionRepository walletTransactionRepository;

    @InjectMocks
    private WalletTransactionService walletTransactionService;

    /* ---------- 私有构造辅助 ---------- */

    private static WalletTransaction tx(long transactionId) {
        WalletTransaction t = new WalletTransaction();
        t.setTransactionId(transactionId);
        return t;
    }

    private EveAccount account(int userId, int corpId) {
        EveAccount a = new EveAccount();
        a.setUserId(userId);
        a.setCorpId(corpId);
        return a;
    }

    /**
     * 模拟有限分页:fromId 为 null 时给一页,否则给空页(模拟一次翻页后终止)。
     */
    private void stubFiniteCharacterPages(Integer characterId, List<Long> pageIds, String token) {
        when(esiApiService.queryCharacterWalletTransactions(eq(characterId), any(), eq(token)))
                .thenAnswer(inv -> {
                    Long fromId = inv.getArgument(1);
                    return fromId == null ? Flux.fromIterable(pageIds.stream().map(WalletTransactionServiceTest::tx).toList()) : Flux.empty();
                });
    }

    /* ============================ 人物同步 ============================ */

    @Nested
    @DisplayName("syncCharacterTransactions 人物同步")
    class CharacterSync {

        @Test
        @DisplayName("游标翻页:首页null→末条id续拉→空页终止,逐页回填并入参保存")
        void cursorPagination_pullsAllPages_untilEmpty() throws Exception {
            Integer characterId = 9001;
            String token = "Bearer char-token";
            when(authorizeUtil.authorizeInternal(anyInt(), eq(characterId))).thenReturn(account(100, 5001));
            when(esiApiService.getAccessToken(characterId, 100)).thenReturn(token);

            // 第一页(最新两条),第二页(更旧两条),第三页空
            when(esiApiService.queryCharacterWalletTransactions(eq(characterId), isNull(), eq(token)))
                    .thenReturn(Flux.just(tx(100L), tx(50L)));
            when(esiApiService.queryCharacterWalletTransactions(eq(characterId), eq(50L), eq(token)))
                    .thenReturn(Flux.just(tx(25L), tx(10L)));
            when(esiApiService.queryCharacterWalletTransactions(eq(characterId), eq(10L), eq(token)))
                    .thenReturn(Flux.empty());

            walletTransactionService.syncCharacterTransactions(characterId);

            // 保存一次,含全部 4 条,顺序保留(100,50,25,10)
            ArgumentCaptor<List<WalletTransaction>> captor = ArgumentCaptor.forClass(List.class);
            verify(walletTransactionRepository).saveOrUpdateBatch(captor.capture());
            List<WalletTransaction> saved = captor.getValue();
            assertThat(saved).extracting(WalletTransaction::getTransactionId)
                    .containsExactly(100L, 50L, 25L, 10L);

            // 归属回填
            assertThat(saved).allSatisfy(t -> {
                assertThat(t.getOwnerType()).isEqualTo("character");
                assertThat(t.getOwnerId()).isEqualTo(9001L);
                assertThat(t.getDivision()).isEqualTo(0);
            });
        }

        @Test
        @DisplayName("首页为空时直接终止,不调用保存")
        void emptyFirstPage_noSave() throws Exception {
            Integer characterId = 9002;
            String token = "Bearer t";
            when(authorizeUtil.authorizeInternal(anyInt(), eq(characterId))).thenReturn(account(100, 5001));
            when(esiApiService.getAccessToken(characterId, 100)).thenReturn(token);
            when(esiApiService.queryCharacterWalletTransactions(eq(characterId), isNull(), eq(token)))
                    .thenReturn(Flux.empty());

            walletTransactionService.syncCharacterTransactions(characterId);

            verify(walletTransactionRepository, times(0)).saveOrUpdateBatch(any());
        }

        @Test
        @DisplayName("游标上限保护:永不终止时 500 页后停止,防死循环")
        void cursorMaxPages_guard_stopsAfter500() throws Exception {
            Integer characterId = 9003;
            String token = "Bearer t";
            when(authorizeUtil.authorizeInternal(anyInt(), eq(characterId))).thenReturn(account(100, 5001));
            when(esiApiService.getAccessToken(characterId, 100)).thenReturn(token);
            // 每页恒返回同一条(游标不前进,永不空页) → 应被 MAX_CURSOR_PAGES 截断
            when(esiApiService.queryCharacterWalletTransactions(eq(characterId), any(), eq(token)))
                    .thenReturn(Flux.just(tx(1L)));

            walletTransactionService.syncCharacterTransactions(characterId);

            ArgumentCaptor<List<WalletTransaction>> captor = ArgumentCaptor.forClass(List.class);
            verify(walletTransactionRepository).saveOrUpdateBatch(captor.capture());
            assertThat(captor.getValue()).hasSize(500);
        }
    }

    /* ============================ 军团同步 ============================ */

    @Nested
    @DisplayName("syncCorporationTransactions 军团同步")
    class CorporationSync {

        @Test
        @DisplayName("七个分账均成功:每 division 独立拉取+回填+保存,按当前 division 回填")
        void allDivisionsSucceed_eachDivisionBackfilled() throws Exception {
            Integer corpId = 5001;
            String token = "Bearer corp-token";
            when(authorizeUtil.authorizeInternal(anyInt(), eq(corpId))).thenReturn(account(100, corpId));
            when(esiApiService.getAccessToken(corpId, 100)).thenReturn(token);
            // 每 division 首页给一条,翻页空页终止
            for (int d = 1; d <= 7; d++) {
                final int div = d;
                when(esiApiService.queryCorporationWalletTransactions(eq(corpId), eq(div), any(), eq(token)))
                        .thenAnswer(inv -> inv.getArgument(2) == null ? Flux.just(tx(div * 100L)) : Flux.empty());
            }

            walletTransactionService.syncCorporationTransactions(corpId);

            ArgumentCaptor<List<WalletTransaction>> captor = ArgumentCaptor.forClass(List.class);
            verify(walletTransactionRepository, times(7)).saveOrUpdateBatch(captor.capture());
            assertThat(captor.getAllValues()).hasSize(7);
            for (int d = 1; d <= 7; d++) {
                WalletTransaction saved = captor.getAllValues().get(d - 1).get(0);
                assertThat(saved.getTransactionId()).isEqualTo((long) (d * 100));
                assertThat(saved.getOwnerType()).isEqualTo("corporation");
                assertThat(saved.getOwnerId()).isEqualTo(5001L);
                assertThat(saved.getDivision()).isEqualTo(d);
            }
        }

        @Test
        @DisplayName("分账失败隔离:某 division 抛异常不影响其它成功分账落库,且汇总抛 EveHelperException")
        void oneDivisionFails_othersStillPersist_andAggregateThrows() throws Exception {
            Integer corpId = 5002;
            String token = "Bearer corp-token";
            when(authorizeUtil.authorizeInternal(anyInt(), eq(corpId))).thenReturn(account(100, corpId));
            when(esiApiService.getAccessToken(corpId, 100)).thenReturn(token);
            for (int d = 1; d <= 7; d++) {
                final int div = d;
                if (div == 2) {
                    // division 2 抛 ESI 运行异常
                    when(esiApiService.queryCorporationWalletTransactions(eq(corpId), eq(div), any(), eq(token)))
                            .thenThrow(new RuntimeException("esi boom"));
                } else {
                    when(esiApiService.queryCorporationWalletTransactions(eq(corpId), eq(div), any(), eq(token)))
                            .thenAnswer(inv -> inv.getArgument(2) == null ? Flux.just(tx(div * 100L)) : Flux.empty());
                }
            }

            assertThatThrownBy(() -> walletTransactionService.syncCorporationTransactions(corpId))
                    .isInstanceOf(EveHelperException.class)
                    .hasMessageContaining("2");

            // 成功分账(1,3,4,5,6,7)已落库,共 6 次
            verify(walletTransactionRepository, times(6)).saveOrUpdateBatch(any());
        }
    }
}