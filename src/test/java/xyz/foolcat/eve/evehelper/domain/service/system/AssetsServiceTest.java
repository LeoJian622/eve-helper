package xyz.foolcat.eve.evehelper.domain.service.system;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Assets;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.domain.repository.system.AssetsRepository;
import xyz.foolcat.eve.evehelper.domain.util.AuthorizeUtil;

import java.text.ParseException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 资产同步 ownerId 回填测试。
 * <p>
 * 现状缺陷(T001):EsiAssetsConverter 将 owner_id 置为 ignore,且
 * {@code saveAndUpdateAsserts} 未回填 ownerId,导致全库资产 owner_id=NULL,
 * 资产按角色聚合/归属鉴权/stale 删除全部失效。
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("资产服务 ownerId 回填单元测试")
class AssetsServiceTest {

    /**
     * 角色ID(CharacterID),回填后应转为 Long 落在 owner_id 上
     */
    private static final int CHARACTER_ID = 1008601;

    @MockBean
    private EsiGateway esiGateway;

    @MockBean
    private AssetsRepository assetsRepository;

    @MockBean
    private AuthorizeUtil authorizeUtil;

    @Autowired
    private AssetsService assetsService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 模拟生产环境 JWT 认证主体(AuthorizeUtil 被 mock,仅供真实路径可被安全上下文识别)。
     */
    private void loginAs() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(1L, null,
                        List.of(new SimpleGrantedAuthority("USER"))));
    }

    @Test
    @DisplayName("同步角色资产后,写入数据库的每条资产 ownerId 均回填为该角色 characterId")
    void saveAndUpdateAsserts_backfillsOwnerId() throws ParseException {
        // ── Arrange ──
        loginAs();

        EveAccount account = new EveAccount();
        account.setCharacterId(CHARACTER_ID);
        account.setUserId(1);
        when(authorizeUtil.authorize(CHARACTER_ID)).thenReturn(account);
        when(esiGateway.getAccessToken(CHARACTER_ID, 1)).thenReturn("Bearer test-token");

        Assets asset1 = new Assets();
        asset1.setItemId(111L);
        Assets asset2 = new Assets();
        asset2.setItemId(222L);

        when(esiGateway.queryCharactersAssetsMaxPage(CHARACTER_ID, "Bearer test-token")).thenReturn(1);
        when(esiGateway.queryCharactersAssets(CHARACTER_ID, 1, "Bearer test-token")).thenReturn(Flux.just(asset1, asset2));

        // 无 stale:removeBatchByIds 不应被触发
        when(assetsRepository.findByOwnerId(CHARACTER_ID)).thenReturn(List.of());

        // ── Act ──
        assetsService.saveAndUpdateAsserts(CHARACTER_ID);

        // ── Assert:回填发生在传入 batchInsertOrUpdate 之前 ──
        ArgumentCaptor<List<Assets>> captor = ArgumentCaptor.forClass(List.class);
        verify(assetsRepository).batchInsertOrUpdate(captor.capture());

        List<Assets> persisted = captor.getValue();
        assertFalse(persisted.isEmpty(), "写入批次不应为空");
        for (Assets a : persisted) {
            assertEquals(Long.valueOf(CHARACTER_ID), a.getOwnerId(),
                    "资产 itemId=" + a.getItemId() + " 的 ownerId 未回填为 characterId=" + CHARACTER_ID);
        }

        // stale 删除基于回填后的 ownerId 查询 → owner_id 非 NULL 时才有效
        verify(assetsRepository).findByOwnerId(CHARACTER_ID);
    }
}