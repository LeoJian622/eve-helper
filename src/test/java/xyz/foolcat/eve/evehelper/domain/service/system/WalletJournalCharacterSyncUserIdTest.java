package xyz.foolcat.eve.evehelper.domain.service.system;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import reactor.core.publisher.Flux;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.WalletJournal;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.domain.repository.system.WalletJournalRepository;
import xyz.foolcat.eve.evehelper.domain.util.AuthorizeUtil;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * US1 T008:人物钱包流水写路径落 user_id。
 *
 * <p>同构于 {@link WalletJournalServiceSyncTest} 的请求路径构造:SecurityContext 设 Number principal
 * 令 {@code UserUtil.getUserId()>0} → {@code authorize(cId)};人物走 {@link #syncCharacterJournal},
 * ownerId=cId、division=0,外加同步者 user_id 随 saveOrUpdateBatch 落库。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WalletJournalService.syncCharacterJournal 人物写路径落 userId")
class WalletJournalCharacterSyncUserIdTest {

    private static final Integer CHARACTER_ID = 1008601;

    @Mock
    private EsiGateway esiApiService;

    @Mock
    private AuthorizeUtil authorizeUtil;

    @Mock
    private WalletJournalRepository walletJournalRepository;

    @InjectMocks
    private WalletJournalService walletJournalService;

    @BeforeEach
    void setUpAuthenticated() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(100L, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("人物钱包流水同步后,传入 saveOrUpdateBatch 的每条记录 userId 回填为同步用户 id")
    void syncCharacterJournal_backfillsUserId() throws Exception {
        // ── Arrange ──
        Integer systemUserId = 55;
        EveAccount account = new EveAccount();
        account.setUserId(systemUserId);
        when(authorizeUtil.authorize(CHARACTER_ID)).thenReturn(account);
        when(esiApiService.getAccessToken(CHARACTER_ID, systemUserId)).thenReturn("Bearer t");

        when(esiApiService.queryCharacterWalletJournalMaxPage(CHARACTER_ID, "Bearer t")).thenReturn(1);
        WalletJournal j1 = new WalletJournal();
        j1.setId(111L);
        when(esiApiService.queryCharacterWalletJournal(CHARACTER_ID, 1, "Bearer t")).thenReturn(Flux.just(j1));

        // ── Act ──
        walletJournalService.syncCharacterJournal(CHARACTER_ID);

        // ── Assert:ownerId/division/userId 回填发生在传入 saveOrUpdateBatch 之前 ──
        ArgumentCaptor<List<WalletJournal>> captor = ArgumentCaptor.forClass(List.class);
        verify(walletJournalRepository).saveOrUpdateBatch(captor.capture());
        assertThat(captor.getValue()).isNotEmpty();
        captor.getValue().forEach(j -> {
            assertThat(j.getOwnerId()).isEqualTo(CHARACTER_ID.longValue());
            assertThat(j.getDivision()).isZero();
            assertThat(j.getUserId()).isEqualTo(Long.valueOf(systemUserId));
        });
    }
}