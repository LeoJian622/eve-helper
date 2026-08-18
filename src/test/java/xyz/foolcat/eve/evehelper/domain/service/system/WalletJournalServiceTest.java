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
import xyz.foolcat.eve.evehelper.domain.model.entity.system.WalletJournal;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.domain.repository.system.WalletJournalRepository;
import xyz.foolcat.eve.evehelper.domain.util.AuthorizeUtil;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * WalletJournalService 单元测试(TDD 红-绿)。
 *
 * <p>纯 Mockito 单测(不起 Spring 上下文),mock {@link EsiGateway}/{@link AuthorizeUtil}/
 * {@link WalletJournalRepository},聚焦核心同步逻辑:</p>
 * <ul>
 *     <li>军团 division 1..7 循环 + 页码翻页 + 回填 ownerId/division</li>
 *     <li>军团 division 级失败隔离:某分账失败不影响其它成功分账落库,汇总抛 EveHelperException</li>
 *     <li>人物同步回填 division=0</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WalletJournalService 钱包流水同步")
class WalletJournalServiceTest {

    @Mock
    private EsiGateway esiApiService;

    @Mock
    private AuthorizeUtil authorizeUtil;

    @Mock
    private WalletJournalRepository walletJournalRepository;

    @InjectMocks
    private WalletJournalService walletJournalService;

    /* ---------- 私有构造辅助 ---------- */

    private static WalletJournal journal(long id) {
        WalletJournal j = new WalletJournal();
        j.setId(id);
        return j;
    }

    private EveAccount account(int userId, int corpId) {
        EveAccount a = new EveAccount();
        a.setUserId(userId);
        a.setCorpId(corpId);
        return a;
    }

    /* ============================ 军团同步 ============================ */

    @Nested
    @DisplayName("syncCorporationJournal 军团同步")
    class CorporationSync {

        @Test
        @DisplayName("七个分账均成功:每 division 独立 maxPage→翻页→回填 ownerId/division→保存")
        void allDivisionsSucceed_eachDivisionBackfilled() throws Exception {
            Integer corpId = 5001;
            String token = "Bearer corp-token";
            when(authorizeUtil.authorizeInternal(anyInt(), eq(corpId))).thenReturn(account(100, corpId));
            when(esiApiService.getAccessToken(corpId, 100)).thenReturn(token);

            for (int d = 1; d <= 7; d++) {
                when(esiApiService.queryCorporationWalletJournalMaxPage(corpId, d, token)).thenReturn(1);
                when(esiApiService.queryCorporationWalletJournal(corpId, d, 1, token))
                        .thenReturn(Flux.just(journal((long) (d * 100))));
            }

            walletJournalService.syncCorporationJournal(corpId);

            ArgumentCaptor<List<WalletJournal>> captor = ArgumentCaptor.forClass(List.class);
            verify(walletJournalRepository, times(7)).saveOrUpdateBatch(captor.capture());
            assertThat(captor.getAllValues()).hasSize(7);
            for (int d = 1; d <= 7; d++) {
                WalletJournal saved = captor.getAllValues().get(d - 1).get(0);
                assertThat(saved.getId()).isEqualTo((long) (d * 100));
                assertThat(saved.getOwnerId()).isEqualTo(corpId.longValue());
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
                if (d == 3) {
                    when(esiApiService.queryCorporationWalletJournalMaxPage(corpId, d, token))
                            .thenThrow(new RuntimeException("esi boom"));
                } else {
                    when(esiApiService.queryCorporationWalletJournalMaxPage(corpId, d, token)).thenReturn(1);
                    when(esiApiService.queryCorporationWalletJournal(corpId, d, 1, token))
                            .thenReturn(Flux.just(journal((long) (d * 100))));
                }
            }

            assertThatThrownBy(() -> walletJournalService.syncCorporationJournal(corpId))
                    .isInstanceOf(EveHelperException.class)
                    .hasMessageContaining("3");

            verify(walletJournalRepository, times(6)).saveOrUpdateBatch(any());
        }
    }

    /* ============================ 人物同步 ============================ */

    @Nested
    @DisplayName("syncCharacterJournal 人物同步")
    class CharacterSync {

        @Test
        @DisplayName("人物同步回填 division=0")
        void backfillsDivisionZero() throws Exception {
            Integer characterId = 9001;
            String token = "Bearer char-token";
            when(authorizeUtil.authorizeInternal(anyInt(), eq(characterId))).thenReturn(account(100, 5001));
            when(esiApiService.getAccessToken(characterId, 100)).thenReturn(token);
            when(esiApiService.queryCharacterWalletJournalMaxPage(characterId, token)).thenReturn(1);
            when(esiApiService.queryCharacterWalletJournal(characterId, 1, token))
                    .thenReturn(Flux.just(journal(1001L), journal(1002L)));

            walletJournalService.syncCharacterJournal(characterId);

            ArgumentCaptor<List<WalletJournal>> captor = ArgumentCaptor.forClass(List.class);
            verify(walletJournalRepository).saveOrUpdateBatch(captor.capture());
            List<WalletJournal> saved = captor.getValue();
            assertThat(saved).hasSize(2);
            assertThat(saved).allSatisfy(j -> {
                assertThat(j.getDivision()).isEqualTo(0);
            });
        }
    }
}
