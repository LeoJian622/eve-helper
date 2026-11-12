package xyz.foolcat.eve.evehelper.domain.service.system;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Structure;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterAccessTokenResult;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterRolesInfo;
import xyz.foolcat.eve.evehelper.domain.port.cache.CacheGateway;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.domain.repository.system.EveAccountRepository;
import xyz.foolcat.eve.evehelper.domain.repository.system.StructureRepository;
import xyz.foolcat.eve.evehelper.domain.util.AuthorizeUtil;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AppStructureSyncService 单元测试（T030,US4 建筑同步领域服务）。
 * <p>
 * 纯单元测试,mock 五依赖(EveAccountRepository/StructureRepository/EsiGateway/AuthorizeUtil/CacheGateway),
 * 无需 DB/Spring context。
 *
 * @author Leojan
 */
@ExtendWith(MockitoExtension.class)
class AppStructureSyncServiceTest {

    @Mock
    private EveAccountRepository eveAccountRepository;

    @Mock
    private StructureRepository structureRepository;

    @Mock
    private EsiGateway esiGateway;

    @Mock
    private AuthorizeUtil authorizeUtil;

    @Mock
    private CacheGateway cacheGateway;

    private AppStructureSyncService buildService() {
        return new AppStructureSyncService(
                eveAccountRepository, structureRepository, esiGateway, authorizeUtil, cacheGateway);
    }

    // ── 辅助 fixture ──

    private static EveAccount account(Integer userId, Integer characterId, Integer corpId) {
        EveAccount a = new EveAccount();
        a.setUserId(userId);
        a.setCharacterId(characterId);
        a.setCorpId(corpId);
        return a;
    }

    private static CharacterRolesInfo permittedRoles() {
        return new CharacterRolesInfo(List.of("Director"), List.of(), List.of());
    }

    private static CharacterRolesInfo noPermissionRoles() {
        return new CharacterRolesInfo(List.of(), List.of(), List.of());
    }

    private static Structure structure(Long structureId) {
        Structure s = new Structure();
        s.setStructureId(structureId);
        return s;
    }

    // ── 用例 ──

    @Test
    void sync_throttled_secondCallWithinHour_skips() throws Exception {
        // Arrange
        EveAccount a = account(77, 1001, 500);
        when(eveAccountRepository.listAll()).thenReturn(List.of(a));
        when(cacheGateway.setIfAbsent(eq("app:struct:sync:500"), eq("1"), eq(3600L), eq(TimeUnit.SECONDS)))
                .thenReturn(true, false);

        when(eveAccountRepository.listByCorpId(500)).thenReturn(List.of(a));
        when(authorizeUtil.authorizeInternal(77, 1001)).thenReturn(a);
        when(esiGateway.getAccessTokenWithExpiry(1001, 77))
                .thenReturn(new CharacterAccessTokenResult("Bearer test-token", 1001, 1140L));
        when(esiGateway.queryCharacterRoles(1001, "Bearer test-token")).thenReturn(permittedRoles());
        when(esiGateway.queryCorporationStructuresMaxPage(500, "Bearer test-token")).thenReturn(1);

        Structure s1 = structure(9001L);
        when(esiGateway.queryCorporationStructures(500, "zh", 1, "Bearer test-token"))
                .thenReturn(Flux.just(s1));
        when(structureRepository.batchInsertOrUpdate(anyList())).thenReturn(1);

        AppStructureSyncService service = buildService();

        // Act:调用两次
        service.syncAllCorps();
        service.syncAllCorps();

        // Assert:第二次被节流跳过
        verify(structureRepository, times(1)).batchInsertOrUpdate(anyList());
        verify(eveAccountRepository, times(1)).listByCorpId(500);
    }

    @Test
    void sync_noPermittedCharacter_skipsCorp() throws Exception {
        // Arrange
        EveAccount a1 = account(77, 1001, 500);
        EveAccount a2 = account(78, 1002, 500);
        when(eveAccountRepository.listAll()).thenReturn(List.of(a1));
        when(cacheGateway.setIfAbsent(eq("app:struct:sync:500"), eq("1"), eq(3600L), eq(TimeUnit.SECONDS)))
                .thenReturn(true);

        when(eveAccountRepository.listByCorpId(500)).thenReturn(List.of(a1, a2));
        when(authorizeUtil.authorizeInternal(77, 1001)).thenReturn(a1);
        when(authorizeUtil.authorizeInternal(78, 1002)).thenReturn(a2);
        when(esiGateway.getAccessTokenWithExpiry(1001, 77))
                .thenReturn(new CharacterAccessTokenResult("Bearer test-token", 1001, 1140L));
        when(esiGateway.getAccessTokenWithExpiry(1002, 78))
                .thenReturn(new CharacterAccessTokenResult("Bearer test-token-2", 1002, 1140L));
        when(esiGateway.queryCharacterRoles(1001, "Bearer test-token")).thenReturn(noPermissionRoles());
        when(esiGateway.queryCharacterRoles(1002, "Bearer test-token-2")).thenReturn(noPermissionRoles());

        AppStructureSyncService service = buildService();

        // Act
        service.syncAllCorps();

        // Assert:无权限角色 → 不落库
        verify(structureRepository, never()).batchInsertOrUpdate(anyList());
    }

    @Test
    void sync_writesWithOwnerUserId() throws Exception {
        // Arrange
        EveAccount a = account(77, 1001, 500);
        when(eveAccountRepository.listAll()).thenReturn(List.of(a));
        when(cacheGateway.setIfAbsent(eq("app:struct:sync:500"), eq("1"), eq(3600L), eq(TimeUnit.SECONDS)))
                .thenReturn(true);

        when(eveAccountRepository.listByCorpId(500)).thenReturn(List.of(a));
        when(authorizeUtil.authorizeInternal(77, 1001)).thenReturn(a);
        when(esiGateway.getAccessTokenWithExpiry(1001, 77))
                .thenReturn(new CharacterAccessTokenResult("Bearer test-token", 1001, 1140L));
        when(esiGateway.queryCharacterRoles(1001, "Bearer test-token")).thenReturn(permittedRoles());
        when(esiGateway.queryCorporationStructuresMaxPage(500, "Bearer test-token")).thenReturn(1);

        Structure s1 = structure(9001L); // userId 初始 null
        when(esiGateway.queryCorporationStructures(500, "zh", 1, "Bearer test-token"))
                .thenReturn(Flux.just(s1));
        when(structureRepository.batchInsertOrUpdate(anyList())).thenReturn(1);

        AppStructureSyncService service = buildService();

        // Act
        service.syncAllCorps();

        // Assert:落库时 userId 必须设为同步角色的属主(77L)
        ArgumentCaptor<List<Structure>> captor = ArgumentCaptor.forClass(List.class);
        verify(structureRepository).batchInsertOrUpdate(captor.capture());
        List<Structure> saved = captor.getValue();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getUserId()).isEqualTo(77L);
    }

    @Test
    void sync_oneCorpFails_othersContinue() throws Exception {
        // Arrange
        EveAccount corpAAccount = account(77, 1001, 501);
        EveAccount corpBAccount = account(77, 1001, 502);
        when(eveAccountRepository.listAll()).thenReturn(List.of(corpAAccount, corpBAccount));

        when(cacheGateway.setIfAbsent(eq("app:struct:sync:501"), eq("1"), eq(3600L), eq(TimeUnit.SECONDS)))
                .thenReturn(true);
        when(cacheGateway.setIfAbsent(eq("app:struct:sync:502"), eq("1"), eq(3600L), eq(TimeUnit.SECONDS)))
                .thenReturn(true);

        // corpA 异常:listByCorpId 抛异常
        when(eveAccountRepository.listByCorpId(501)).thenThrow(new RuntimeException("模拟异常"));

        // corpB 正常
        when(eveAccountRepository.listByCorpId(502)).thenReturn(List.of(corpBAccount));
        when(authorizeUtil.authorizeInternal(77, 1001)).thenReturn(corpBAccount);
        when(esiGateway.getAccessTokenWithExpiry(1001, 77))
                .thenReturn(new CharacterAccessTokenResult("Bearer test-token", 1001, 1140L));
        when(esiGateway.queryCharacterRoles(1001, "Bearer test-token")).thenReturn(permittedRoles());
        when(esiGateway.queryCorporationStructuresMaxPage(502, "Bearer test-token")).thenReturn(1);

        Structure s1 = structure(9001L);
        when(esiGateway.queryCorporationStructures(502, "zh", 1, "Bearer test-token"))
                .thenReturn(Flux.just(s1));
        when(structureRepository.batchInsertOrUpdate(anyList())).thenReturn(1);

        AppStructureSyncService service = buildService();

        // Act
        service.syncAllCorps();

        // Assert:corpA 异常被吞,corpB 成功落库一次
        verify(structureRepository, times(1)).batchInsertOrUpdate(anyList());
    }
}
