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
import xyz.foolcat.eve.evehelper.domain.model.entity.system.WalletTransaction;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.domain.repository.system.WalletTransactionRepository;
import xyz.foolcat.eve.evehelper.domain.util.AuthorizeUtil;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * US1 T009:人物钱包交易写路径落 user_id。
 *
 * <p>同构于 {@link WalletTransactionServiceSyncTest} 请求路径构造:SecurityContext 设 Number principal
 * 令 {@code UserUtil.getUserId()>0} → {@code authorize(characterId)}。人物走
 * {@link #syncCharacterTransactions},ownerType=character、ownerId=characterId、division=0,
 * 外加同步者 user_id 随 saveOrUpdateBatch 落库。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WalletTransactionService.syncCharacterTransactions 人物写路径落 userId")
class WalletTransactionCharacterSyncUserIdTest {

    private static final Integer CHARACTER_ID = 1008601;

    @Mock
    private EsiGateway esiGateway;

    @Mock
    private AuthorizeUtil authorizeUtil;

    @Mock
    private WalletTransactionRepository walletTransactionRepository;

    @InjectMocks
    private WalletTransactionService walletTransactionService;

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
    @DisplayName("人物钱包交易同步后,传入 saveOrUpdateBatch 的每条记录 userId 回填为同步用户 id")
    void syncCharacterTransactions_backfillsUserId() throws Exception {
        // ── Arrange ──
        Integer systemUserId = 55;
        EveAccount account = new EveAccount();
        account.setUserId(systemUserId);
        when(authorizeUtil.authorize(CHARACTER_ID)).thenReturn(account);
        when(esiGateway.getAccessToken(CHARACTER_ID, systemUserId)).thenReturn("Bearer t");

        WalletTransaction tx = new WalletTransaction();
        tx.setTransactionId(9999L);
        // 首页游标 null → 有 1 条;游标页 fromId=末条 transactionId → 空,翻页终止
        when(esiGateway.queryCharacterWalletTransactions(eq(CHARACTER_ID), isNull(), eq("Bearer t")))
                .thenReturn(Flux.just(tx));
        when(esiGateway.queryCharacterWalletTransactions(eq(CHARACTER_ID), eq(9999L), eq("Bearer t")))
                .thenReturn(Flux.empty());

        // ── Act ──
        walletTransactionService.syncCharacterTransactions(CHARACTER_ID);

        // ── Assert:ownerType/ownerId/division/userId 回填发生在传入 saveOrUpdateBatch 之前 ──
        ArgumentCaptor<List<WalletTransaction>> captor = ArgumentCaptor.forClass(List.class);
        verify(walletTransactionRepository).saveOrUpdateBatch(captor.capture());
        assertThat(captor.getValue()).isNotEmpty();
        captor.getValue().forEach(t -> {
            assertThat(t.getOwnerType()).isEqualTo("character");
            assertThat(t.getOwnerId()).isEqualTo(CHARACTER_ID.longValue());
            assertThat(t.getDivision()).isZero();
            assertThat(t.getUserId()).isEqualTo(Long.valueOf(systemUserId));
        });
    }
}