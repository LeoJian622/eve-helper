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
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.EsiException;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.ResultCode;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * WalletJournalService.syncCorporationJournal 签名统一契约测试(TDD 红-绿)。
 *
 * <p>针对 013 US4 双重语义 bug:同一入参此前兼作「角色ID(取 token)」与「军团ID(ESI 查询)」。
 * 本测试驱动改造后 orale:入参恒为<b>角色ID(characterId)</b>,内部经
 * {@code authorize(characterId)}（请求路径）/ {@code authorizeInternal}(内部路径) 从同一
 * eve_account 角色行解析 {@code corpId = eveAccount.getCorpId()},ESI 军团查询及 ownerId 回填
 * <b>必须用解析出的 corpId</b>,绝不与入参复用。</p>
 *
 * <p>走到请求路径分支:通过 SecurityContextHolder 设 Number principal,令
 * {@code UserUtil.getUserId()>0},从而走 {@code authorize(characterId)}。(内部路径分支见
 * {@link WalletJournalServiceInternalAuthTest}。)示例仿 {@link AssetsServiceTest}。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WalletJournalService.syncCorporationJournal 角色ID驱动")
class WalletJournalServiceSyncTest {

    @Mock
    private EsiGateway esiApiService;

    @Mock
    private AuthorizeUtil authorizeUtil;

    @Mock
    private WalletJournalRepository walletJournalRepository;

    @InjectMocks
    private WalletJournalService walletJournalService;

    @BeforeEach
    void setUpAuthenticatedRequestContext() {
        // 令 UserUtil.getUserId()>0 → 请求路径 authorize(characterId)
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(100L, null, List.of()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private static WalletJournal journal(long id) {
        WalletJournal j = new WalletJournal();
        j.setId(id);
        return j;
    }

    private EveAccount account(Integer userId, Integer corpId) {
        EveAccount a = new EveAccount();
        a.setUserId(userId);
        a.setCorpId(corpId);
        return a;
    }

    @Test
    @DisplayName("传角色ID:authorize(角色ID)取 token,ESI 军团查询与 ownerId 用角色行解析的 corpId,绝不复用入参")
    void syncUsesResolvedCorpId_notReuseCharacterId() throws Exception {
        Integer characterId = 9001;
        Integer userId = 100;
        Integer corpId = 5001; // 从 eve_account 角色行解析
        String token = "Bearer corp-token";
        when(authorizeUtil.authorize(characterId)).thenReturn(account(userId, corpId));
        when(esiApiService.getAccessToken(characterId, userId)).thenReturn(token);

        for (int d = 1; d <= 7; d++) {
            when(esiApiService.queryCorporationWalletJournalMaxPage(corpId, d, token)).thenReturn(1);
            when(esiApiService.queryCorporationWalletJournal(corpId, d, 1, token))
                    .thenReturn(Flux.just(journal(d * 100L)));
        }

        walletJournalService.syncCorporationJournal(characterId);

        // ── token 身份 = 角色ID ──
        verify(authorizeUtil).authorize(characterId);
        verify(authorizeUtil, never()).authorizeInternal(anyInt(), any());
        verify(esiApiService).getAccessToken(characterId, userId);

        // ── ESI 军团查询目标 = 角色行派生的 corpId,而非入参 characterId ──
        verify(esiApiService, times(7)).queryCorporationWalletJournalMaxPage(eq(corpId), anyInt(), eq(token));
        verify(esiApiService, times(7)).queryCorporationWalletJournal(eq(corpId), anyInt(), anyInt(), eq(token));

        // ── ownerId 回填用解析 corpId ──
        ArgumentCaptor<List<WalletJournal>> captor = ArgumentCaptor.forClass(List.class);
        verify(walletJournalRepository, times(7)).saveOrUpdateBatch(captor.capture());
        assertThat(captor.getAllValues()).hasSize(7);
        for (int d = 1; d <= 7; d++) {
            assertThat(captor.getAllValues().get(d - 1).get(0).getOwnerId()).isEqualTo(corpId.longValue());
        }
    }

    @Test
    @DisplayName("corpId 为空(角色不在任何军团):映射 ESI_AUTH_PERMISSION_LOW(403),不触发 ESI")
    void nullCorpId_mapsPermissionLowNotEsi() throws Exception {
        Integer characterId = 9002;
        Integer userId = 100;
        when(authorizeUtil.authorize(characterId)).thenReturn(account(userId, null));

        assertThatThrownBy(() -> walletJournalService.syncCorporationJournal(characterId))
                .isInstanceOf(EsiException.class)
                .satisfies(e -> assertThat(((EsiException) e).getResultCode())
                        .isEqualTo(ResultCode.ESI_AUTH_PERMISSION_LOW));

        verify(esiApiService, never()).queryCorporationWalletJournal(anyInt(), anyInt(), anyInt(), any());
    }

    @Test
    @DisplayName("军团同步:每行 setUserId=同步者 userId.longValue()（US2a T013 军团写路径落 user_id）")
    void corpSync_setsUserId() throws Exception {
        Integer characterId = 9003;
        Integer userId = 100;
        Integer corpId = 5001;
        String token = "Bearer corp-token";
        when(authorizeUtil.authorize(characterId)).thenReturn(account(userId, corpId));
        when(esiApiService.getAccessToken(characterId, userId)).thenReturn(token);

        for (int d = 1; d <= 7; d++) {
            when(esiApiService.queryCorporationWalletJournalMaxPage(corpId, d, token)).thenReturn(1);
            when(esiApiService.queryCorporationWalletJournal(corpId, d, 1, token))
                    .thenReturn(Flux.just(journal(d * 100L)));
        }

        walletJournalService.syncCorporationJournal(characterId);

        Long syncUserId = userId.longValue();
        ArgumentCaptor<List<WalletJournal>> captor = ArgumentCaptor.forClass(List.class);
        verify(walletJournalRepository, times(7)).saveOrUpdateBatch(captor.capture());
        captor.getAllValues().forEach(list -> assertThat(list).allSatisfy(j ->
                assertThat(j.getUserId()).isEqualTo(syncUserId)));
    }
}