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
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.EsiException;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.ResultCode;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * WalletTransactionService.syncCorporationTransactions 签名统一契约测试(TDD 红-绿)。
 *
 * <p>同构于 {@link WalletJournalServiceSyncTest}:入参恒为<b>角色ID(characterId)</b>,内部从
 * eve_account 角色行解析 {@code corpId = eveAccount.getCorpId()},ESI 军团交易查询、ownerId
 * 回填一律用解析 corpId,绝不复用入参;corpId 为 null → {@link EsiException}(ESI00403, 403)。</p>
 *
 * <p>请求路径:SecurityContextHolder 设 Number principal → {@code UserUtil.getUserId()>0} →
 * {@code authorize(characterId)}。样式仿 {@link WalletJournalServiceSyncTest}。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WalletTransactionService.syncCorporationTransactions 角色ID驱动")
class WalletTransactionServiceSyncTest {

    @Mock
    private EsiGateway esiGateway;

    @Mock
    private AuthorizeUtil authorizeUtil;

    @Mock
    private WalletTransactionRepository walletTransactionRepository;

    @InjectMocks
    private WalletTransactionService walletTransactionService;

    @BeforeEach
    void setUpAuthenticatedRequestContext() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(100L, null, List.of()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private EveAccount account(Integer userId, Integer corpId) {
        EveAccount a = new EveAccount();
        a.setUserId(userId);
        a.setCorpId(corpId);
        return a;
    }

    private static WalletTransaction tx(long id) {
        WalletTransaction t = new WalletTransaction();
        t.setTransactionId(id);
        return t;
    }

    @Test
    @DisplayName("传角色ID:authorize(角色ID)取 token,ESI 军团交易查询与 owner 回填用角色行解析的 corpId")
    void syncUsesResolvedCorpId_notReuseCharacterId() throws Exception {
        Integer characterId = 9001;
        Integer userId = 100;
        Integer corpId = 5001;
        String token = "Bearer corp-token";
        when(authorizeUtil.authorize(characterId)).thenReturn(account(userId, corpId));
        when(esiGateway.getAccessToken(characterId, userId)).thenReturn(token);

        for (int d = 1; d <= 7; d++) {
            // 首页游标 isNull() → 有 1 条;游标页 fromId=末条 transactionId → 空,翻页终止
            when(esiGateway.queryCorporationWalletTransactions(eq(corpId), eq(d), isNull(), eq(token)))
                    .thenReturn(Flux.just(tx(d * 100L)));
            when(esiGateway.queryCorporationWalletTransactions(eq(corpId), eq(d), eq(d * 100L), eq(token)))
                    .thenReturn(Flux.empty());
        }

        Map<Integer, Boolean> results = walletTransactionService.syncCorporationTransactions(characterId);

        assertThat(results).hasSize(7).allSatisfy((d, ok) -> assertThat(ok).isTrue());

        // ── token 身份 = 角色ID;非内部通道 ──
        verify(authorizeUtil).authorize(characterId);
        verify(authorizeUtil, never()).authorizeInternal(anyInt(), any());
        verify(esiGateway).getAccessToken(characterId, userId);

        // ── ESI 军团交易查询首页目标 = 角色行派生的 corpId,而非入参 characterId ──
        verify(esiGateway, times(7)).queryCorporationWalletTransactions(eq(corpId), anyInt(), isNull(), eq(token));

        // ── owner 回填用解析 corpId(ownerType=corporation) ──
        ArgumentCaptor<List<WalletTransaction>> captor = ArgumentCaptor.forClass(List.class);
        verify(walletTransactionRepository, times(7)).saveOrUpdateBatch(captor.capture());
        assertThat(captor.getAllValues()).hasSize(7);
        captor.getAllValues().forEach(list -> assertThat(list).allSatisfy(t -> {
            assertThat(t.getOwnerId()).isEqualTo(corpId.longValue());
            assertThat(t.getOwnerType()).isEqualTo("corporation");
        }));
    }

    @Test
    @DisplayName("corpId 为空(角色不在任何军团):映射 ESI_AUTH_PERMISSION_LOW(403),不触发 ESI")
    void nullCorpId_mapsPermissionLowNotEsi() throws Exception {
        Integer characterId = 9002;
        Integer userId = 100;
        when(authorizeUtil.authorize(characterId)).thenReturn(account(userId, null));

        assertThatThrownBy(() -> walletTransactionService.syncCorporationTransactions(characterId))
                .isInstanceOf(EsiException.class)
                .satisfies(e -> assertThat(((EsiException) e).getResultCode())
                        .isEqualTo(ResultCode.ESI_AUTH_PERMISSION_LOW));

        verify(esiGateway, never()).queryCorporationWalletTransactions(anyInt(), anyInt(), any(), any());
    }

    @Test
    @DisplayName("军团同步:每行 setUserId=同步者 userId.longValue()（US2a T014 军团写路径落 user_id）")
    void corpSync_setsUserId() throws Exception {
        Integer characterId = 9003;
        Integer userId = 100;
        Integer corpId = 5001;
        String token = "Bearer corp-token";
        when(authorizeUtil.authorize(characterId)).thenReturn(account(userId, corpId));
        when(esiGateway.getAccessToken(characterId, userId)).thenReturn(token);

        for (int d = 1; d <= 7; d++) {
            when(esiGateway.queryCorporationWalletTransactions(eq(corpId), eq(d), isNull(), eq(token)))
                    .thenReturn(Flux.just(tx(d * 100L)));
            when(esiGateway.queryCorporationWalletTransactions(eq(corpId), eq(d), eq(d * 100L), eq(token)))
                    .thenReturn(Flux.empty());
        }

        walletTransactionService.syncCorporationTransactions(characterId);

        Long syncUserId = userId.longValue();
        ArgumentCaptor<List<WalletTransaction>> captor = ArgumentCaptor.forClass(List.class);
        verify(walletTransactionRepository, times(7)).saveOrUpdateBatch(captor.capture());
        captor.getAllValues().forEach(list -> assertThat(list).allSatisfy(t ->
                assertThat(t.getUserId()).isEqualTo(syncUserId)));
    }
}