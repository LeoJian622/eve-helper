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
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Assets;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.domain.repository.system.AssetsRepository;
import xyz.foolcat.eve.evehelper.domain.util.AuthorizeUtil;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * US1 T006:资产写路径落 user_id。
 *
 * <p>同构于 {@link AssetsServiceTest} 的 ownerId 回填契约,额外断言同步用户 id 随 save 落库
 * (人物数据只对自己可见的关键前置)。走到请求路径分支:SecurityContext 设 Number principal,
 * 令 {@code UserUtil.getUserId()>0} → {@code authorize(characterId)}。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AssetsService.saveAndUpdateAsserts 人物写路径落 userId")
class AssetsWriteUserIdTest {

    private static final int CHARACTER_ID = 1008601;

    @Mock
    private EsiGateway esiGateway;

    @Mock
    private AssetsRepository assetsRepository;

    @Mock
    private AuthorizeUtil authorizeUtil;

    @InjectMocks
    private AssetsService assetsService;

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
    @DisplayName("同步角色资产后,传入 batchInsertOrUpdate 的每条资产 userId 回填为同步用户 id")
    void saveAndUpdateAsserts_backfillsUserId() throws Exception {
        // ── Arrange ──
        Integer systemUserId = 55;
        EveAccount account = new EveAccount();
        account.setCharacterId(CHARACTER_ID);
        account.setUserId(systemUserId);
        when(authorizeUtil.authorize(CHARACTER_ID)).thenReturn(account);
        when(esiGateway.getAccessToken(CHARACTER_ID, systemUserId)).thenReturn("Bearer t");

        when(esiGateway.queryCharactersAssetsMaxPage(CHARACTER_ID, "Bearer t")).thenReturn(1);
        Assets a1 = new Assets();
        a1.setItemId(1L);
        Assets a2 = new Assets();
        a2.setItemId(2L);
        when(esiGateway.queryCharactersAssets(CHARACTER_ID, 1, "Bearer t")).thenReturn(Flux.just(a1, a2));
        when(assetsRepository.findByOwnerId(CHARACTER_ID)).thenReturn(List.of());

        // ── Act ──
        assetsService.saveAndUpdateAsserts(CHARACTER_ID);

        // ── Assert:userId 回填发生在传入 batchInsertOrUpdate 之前 ──
        ArgumentCaptor<List<Assets>> captor = ArgumentCaptor.forClass(List.class);
        verify(assetsRepository).batchInsertOrUpdate(captor.capture());
        assertThat(captor.getValue()).isNotEmpty();
        captor.getValue().forEach(a -> assertThat(a.getUserId()).isEqualTo(Long.valueOf(systemUserId)));
    }
}