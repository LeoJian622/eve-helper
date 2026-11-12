package xyz.foolcat.eve.evehelper.domain.service.system;

import cn.hutool.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Structure;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterAccessTokenResult;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterRolesInfo;
import xyz.foolcat.eve.evehelper.domain.port.appws.AppNotificationPushGateway;
import xyz.foolcat.eve.evehelper.domain.port.cache.CacheGateway;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.domain.repository.system.AppNotificationRepository;
import xyz.foolcat.eve.evehelper.domain.repository.system.EveAccountRepository;
import xyz.foolcat.eve.evehelper.domain.repository.system.StructureRepository;
import xyz.foolcat.eve.evehelper.domain.util.AuthorizeUtil;

import java.text.ParseException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AppNotificationDetectService 燃料检测单元测试（T031）。
 * <p>
 * 6 用例覆盖 US4 燃料事件检测与权限扇出核心流程，纯单元 mock 6 依赖。
 *
 * @author Leojian
 * date 2026-11-13
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("T031 燃料检测领域服务单元测试")
class AppNotificationDetectServiceFuelTest {

    @Mock
    private AppNotificationRepository appNotificationRepository;
    @Mock
    private AppNotificationPushGateway pushGateway;
    @Mock
    private EveAccountRepository eveAccountRepository;
    @Mock
    private StructureRepository structureRepository;
    @Mock
    private EsiGateway esiGateway;
    @Mock
    private CacheGateway cacheGateway;
    @Mock
    private AuthorizeUtil authorizeUtil;

    private AppNotificationDetectService service;

    // ─── 公共 Fixture ─────────────────────────────────────

    private EveAccount accountA;
    private EveAccount accountB;
    private Structure structure1;

    @BeforeEach
    void setUp() {
        service = new AppNotificationDetectService(
                appNotificationRepository,
                pushGateway,
                eveAccountRepository,
                structureRepository,
                esiGateway,
                cacheGateway,
                authorizeUtil
        );

        // accountA: userId 77, characterId 1001, corpId 500
        accountA = new EveAccount();
        accountA.setUserId(77);
        accountA.setCharacterId(1001);
        accountA.setCorpId(500);
        accountA.setCharacterName("T031TEST-A");

        // accountB: userId 78, characterId 1002, corpId 500
        accountB = new EveAccount();
        accountB.setUserId(78);
        accountB.setCharacterId(1002);
        accountB.setCorpId(500);
        accountB.setCharacterName("T031TEST-B");

        // structure1: structureId 9001L, typeId 100L, fuelExpires now+12h
        structure1 = new Structure();
        structure1.setStructureId(9001L);
        structure1.setTypeId(100L);
        structure1.setName("T031TEST-Structure");
        structure1.setFuelExpires(OffsetDateTime.now().plusHours(12));
    }

    // ─── 用例 1: fuel_globalDedup_anyRecipientRowInWindow_skipsWholeRound ───

    @Test
    @DisplayName("全局去重：窗口内已有任一接收者行 → 整轮跳过")
    void fuel_globalDedup_anyRecipientRowInWindow_skipsWholeRound() throws ParseException {
        // Arrange: 锁获取成功
        when(cacheGateway.setIfAbsent(eq("app:notify:lock:FUEL_LOW"), any(), eq(600L), eq(TimeUnit.SECONDS)))
                .thenReturn(true);
        when(eveAccountRepository.listAll()).thenReturn(List.of(accountA, accountB));
        when(structureRepository.selectFuelExpiresList(24, 500)).thenReturn(List.of(structure1));
        // 全局去重：窗口内已存在
        when(appNotificationRepository.existsByDedupKeyWithinHours("FUEL_LOW:9001", 24)).thenReturn(true);

        // Act
        service.detectFuelLow();

        // Assert: 整轮跳过，不插入、不推送、不查角色
        verify(appNotificationRepository, never()).insertBatch(any());
        verify(pushGateway, never()).pushToUser(any(), anyString());
        verify(esiGateway, never()).queryCharacterRoles(anyInt(), anyString());
        verify(cacheGateway).compareAndDelete(eq("app:notify:lock:FUEL_LOW"), any());
    }

    // ─── 用例 2: fuel_fanout_onlyPermittedUsers ──────────

    @Test
    @DisplayName("权限扇出：仅 Director/Station_Manager 接收通知")
    void fuel_fanout_onlyPermittedUsers() throws ParseException {
        // Arrange
        when(cacheGateway.setIfAbsent(eq("app:notify:lock:FUEL_LOW"), any(), eq(600L), eq(TimeUnit.SECONDS)))
                .thenReturn(true);
        when(eveAccountRepository.listAll()).thenReturn(List.of(accountA, accountB));
        when(structureRepository.selectFuelExpiresList(24, 500)).thenReturn(List.of(structure1));
        when(appNotificationRepository.existsByDedupKeyWithinHours("FUEL_LOW:9001", 24)).thenReturn(false);

        // accountA: Director (有权)
        CharacterAccessTokenResult tokenA = new CharacterAccessTokenResult("Bearer t1", 1001, 1140L);
        when(esiGateway.getAccessTokenWithExpiry(1001, 77)).thenReturn(tokenA);
        CharacterRolesInfo rolesA = new CharacterRolesInfo(List.of("Director"), List.of(), List.of());
        when(esiGateway.queryCharacterRoles(1001, "Bearer t1")).thenReturn(rolesA);

        // accountB: 无权
        CharacterAccessTokenResult tokenB = new CharacterAccessTokenResult("Bearer t2", 1002, 1140L);
        when(esiGateway.getAccessTokenWithExpiry(1002, 78)).thenReturn(tokenB);
        CharacterRolesInfo rolesB = new CharacterRolesInfo(List.of(), List.of(), List.of());
        when(esiGateway.queryCharacterRoles(1002, "Bearer t2")).thenReturn(rolesB);

        when(eveAccountRepository.listByCorpId(500)).thenReturn(List.of(accountA, accountB));

        // insertBatch 模拟 id 回填
        when(appNotificationRepository.insertBatch(any())).thenAnswer(invocation -> {
            List<?> rows = invocation.getArgument(0);
            for (int i = 0; i < rows.size(); i++) {
                var row = rows.get(i);
                if (row instanceof xyz.foolcat.eve.evehelper.domain.model.entity.system.AppNotification n) {
                    n.setId(5000L + i);
                }
            }
            return rows.size();
        });

        when(pushGateway.isOnline(77)).thenReturn(true);

        // Act
        service.detectFuelLow();

        // Assert: 恰 1 行（仅 accountA）
        ArgumentCaptor<List<xyz.foolcat.eve.evehelper.domain.model.entity.system.AppNotification>> captor = ArgumentCaptor.forClass(List.class);
        verify(appNotificationRepository).insertBatch(captor.capture());
        List<xyz.foolcat.eve.evehelper.domain.model.entity.system.AppNotification> rows = captor.getValue();
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getUserId()).isEqualTo(77);
        assertThat(rows.get(0).getDedupKey()).isEqualTo("FUEL_LOW:9001");
        assertThat(rows.get(0).getCharacterId()).isNull();
        assertThat(rows.get(0).getCorpId()).isEqualTo(500);
        assertThat(rows.get(0).getIsRead()).isFalse();
        assertThat(rows.get(0).getTitle()).isEqualTo("建筑燃料不足");

        // 推送断言
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(pushGateway).pushToUser(eq(77), payloadCaptor.capture());
        verify(pushGateway, never()).pushToUser(eq(78), anyString());

        // 信封断言
        String payload = payloadCaptor.getValue();
        JSONObject envelope = new JSONObject(payload);
        assertThat(envelope.getStr("type")).isEqualTo("NOTIFICATION");
        JSONObject data = envelope.getJSONObject("data");
        assertThat(data.getLong("id")).isEqualTo(5000L);
        assertThat(data.getStr("eventType")).isEqualTo("FUEL_LOW");

        verify(cacheGateway).compareAndDelete(eq("app:notify:lock:FUEL_LOW"), any());
    }

    // ─── 用例 3: fuel_excludesFactionCitadels ────────────

    @Test
    @DisplayName("势力铁壁排除：typeId 47513-47516 不触发通知")
    void fuel_excludesFactionCitadels() throws ParseException {
        // Arrange
        when(cacheGateway.setIfAbsent(eq("app:notify:lock:FUEL_LOW"), any(), eq(600L), eq(TimeUnit.SECONDS)))
                .thenReturn(true);
        when(eveAccountRepository.listAll()).thenReturn(List.of(accountA));
        Structure factionCitadel = new Structure();
        factionCitadel.setStructureId(9003L);
        factionCitadel.setTypeId(47513L);
        factionCitadel.setName("FactionCitadel");
        factionCitadel.setFuelExpires(OffsetDateTime.now().plusHours(12));

        when(structureRepository.selectFuelExpiresList(24, 500)).thenReturn(List.of(factionCitadel, structure1));
        when(appNotificationRepository.existsByDedupKeyWithinHours("FUEL_LOW:9001", 24)).thenReturn(false);

        // accountA 有权
        CharacterAccessTokenResult tokenA = new CharacterAccessTokenResult("Bearer t1", 1001, 1140L);
        when(esiGateway.getAccessTokenWithExpiry(1001, 77)).thenReturn(tokenA);
        CharacterRolesInfo rolesA = new CharacterRolesInfo(List.of("Director"), List.of(), List.of());
        when(esiGateway.queryCharacterRoles(1001, "Bearer t1")).thenReturn(rolesA);
        when(eveAccountRepository.listByCorpId(500)).thenReturn(List.of(accountA));

        when(appNotificationRepository.insertBatch(any())).thenAnswer(invocation -> {
            List<?> rows = invocation.getArgument(0);
            for (int i = 0; i < rows.size(); i++) {
                var row = rows.get(i);
                if (row instanceof xyz.foolcat.eve.evehelper.domain.model.entity.system.AppNotification n) {
                    n.setId(5000L + i);
                }
            }
            return rows.size();
        });
        when(pushGateway.isOnline(77)).thenReturn(true);

        // Act
        service.detectFuelLow();

        // Assert: 仅 structure1 触发（9003 零行）
        ArgumentCaptor<List<xyz.foolcat.eve.evehelper.domain.model.entity.system.AppNotification>> captor = ArgumentCaptor.forClass(List.class);
        verify(appNotificationRepository).insertBatch(captor.capture());
        List<xyz.foolcat.eve.evehelper.domain.model.entity.system.AppNotification> rows = captor.getValue();
        assertThat(rows).allMatch(r -> "FUEL_LOW:9001".equals(r.getDedupKey()));

        verify(cacheGateway).compareAndDelete(eq("app:notify:lock:FUEL_LOW"), any());
    }

    // ─── 用例 4: fuel_nullFuelExpires_treatedAsExhausted ─

    @Test
    @DisplayName("燃料耗尽：fuelExpires=null 视为已耗尽")
    void fuel_nullFuelExpires_treatedAsExhausted() throws ParseException {
        // Arrange
        structure1.setFuelExpires(null);
        when(cacheGateway.setIfAbsent(eq("app:notify:lock:FUEL_LOW"), any(), eq(600L), eq(TimeUnit.SECONDS)))
                .thenReturn(true);
        when(eveAccountRepository.listAll()).thenReturn(List.of(accountA));
        when(structureRepository.selectFuelExpiresList(24, 500)).thenReturn(List.of(structure1));
        when(appNotificationRepository.existsByDedupKeyWithinHours("FUEL_LOW:9001", 24)).thenReturn(false);

        CharacterAccessTokenResult tokenA = new CharacterAccessTokenResult("Bearer t1", 1001, 1140L);
        when(esiGateway.getAccessTokenWithExpiry(1001, 77)).thenReturn(tokenA);
        CharacterRolesInfo rolesA = new CharacterRolesInfo(List.of("Director"), List.of(), List.of());
        when(esiGateway.queryCharacterRoles(1001, "Bearer t1")).thenReturn(rolesA);
        when(eveAccountRepository.listByCorpId(500)).thenReturn(List.of(accountA));

        when(appNotificationRepository.insertBatch(any())).thenAnswer(invocation -> {
            List<?> rows = invocation.getArgument(0);
            for (int i = 0; i < rows.size(); i++) {
                var row = rows.get(i);
                if (row instanceof xyz.foolcat.eve.evehelper.domain.model.entity.system.AppNotification n) {
                    n.setId(5000L + i);
                }
            }
            return rows.size();
        });
        when(pushGateway.isOnline(77)).thenReturn(true);

        // Act
        service.detectFuelLow();

        // Assert: content 含 "燃料已耗尽"
        ArgumentCaptor<List<xyz.foolcat.eve.evehelper.domain.model.entity.system.AppNotification>> captor = ArgumentCaptor.forClass(List.class);
        verify(appNotificationRepository).insertBatch(captor.capture());
        assertThat(captor.getValue()).anyMatch(r -> r.getContent().contains("燃料已耗尽"));

        verify(cacheGateway).compareAndDelete(eq("app:notify:lock:FUEL_LOW"), any());
    }

    // ─── 用例 5: fuel_offlineUser_persistedNotPushed ─────

    @Test
    @DisplayName("离线用户：仅入库不推送")
    void fuel_offlineUser_persistedNotPushed() throws ParseException {
        // Arrange
        when(cacheGateway.setIfAbsent(eq("app:notify:lock:FUEL_LOW"), any(), eq(600L), eq(TimeUnit.SECONDS)))
                .thenReturn(true);
        when(eveAccountRepository.listAll()).thenReturn(List.of(accountA));
        when(structureRepository.selectFuelExpiresList(24, 500)).thenReturn(List.of(structure1));
        when(appNotificationRepository.existsByDedupKeyWithinHours("FUEL_LOW:9001", 24)).thenReturn(false);

        CharacterAccessTokenResult tokenA = new CharacterAccessTokenResult("Bearer t1", 1001, 1140L);
        when(esiGateway.getAccessTokenWithExpiry(1001, 77)).thenReturn(tokenA);
        CharacterRolesInfo rolesA = new CharacterRolesInfo(List.of("Director"), List.of(), List.of());
        when(esiGateway.queryCharacterRoles(1001, "Bearer t1")).thenReturn(rolesA);
        when(eveAccountRepository.listByCorpId(500)).thenReturn(List.of(accountA));

        when(appNotificationRepository.insertBatch(any())).thenAnswer(invocation -> {
            List<?> rows = invocation.getArgument(0);
            for (int i = 0; i < rows.size(); i++) {
                var row = rows.get(i);
                if (row instanceof xyz.foolcat.eve.evehelper.domain.model.entity.system.AppNotification n) {
                    n.setId(5000L + i);
                }
            }
            return rows.size();
        });
        // 离线
        when(pushGateway.isOnline(77)).thenReturn(false);

        // Act
        service.detectFuelLow();

        // Assert: 入库但未推送
        verify(appNotificationRepository).insertBatch(any());
        verify(pushGateway, never()).pushToUser(any(), anyString());

        verify(cacheGateway).compareAndDelete(eq("app:notify:lock:FUEL_LOW"), any());
    }

    // ─── 用例 6: fuel_lockHeld_skipsRun ──────────────────

    @Test
    @DisplayName("锁未获取：立即返回不执行")
    void fuel_lockHeld_skipsRun() {
        // Arrange: 锁获取失败
        when(cacheGateway.setIfAbsent(eq("app:notify:lock:FUEL_LOW"), any(), eq(600L), eq(TimeUnit.SECONDS)))
                .thenReturn(false);

        // Act
        service.detectFuelLow();

        // Assert: 未获锁立即返回，不执行任何逻辑
        verify(eveAccountRepository, never()).listAll();
        verify(cacheGateway, never()).compareAndDelete(any(), any());
    }
}
