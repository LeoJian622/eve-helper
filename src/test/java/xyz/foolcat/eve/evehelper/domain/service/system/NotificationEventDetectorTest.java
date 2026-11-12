package xyz.foolcat.eve.evehelper.domain.service.system;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.AppNotification;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.IndustryJob;
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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * T034 去重与扇出场景套件（US4，SC-007/SC-006 边界固化）。
 * <p>
 * 跨轮次场景固化（characterization）：覆盖燃料 24h 去重 / 持续条件再提醒 / 条件解除停止 /
 * 工业一次性去重 / 权限丧失扇出（spec Edge Case SC-006）/ 同用户双角色隔离。
 * 纯单元测试，mock 7 依赖，不依赖 DB。
 *
 * @author Leojian
 * date 2026-11-13
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("T034 去重与扇出场景套件（US4，SC-007/SC-006 边界固化）")
class NotificationEventDetectorTest {

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
    private Structure structure1;
    private IndustryJob job1;

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

        // accountA: userId 77, characterId 1001, corpId 500, name "T034TEST-A"
        accountA = new EveAccount();
        accountA.setUserId(77);
        accountA.setCharacterId(1001);
        accountA.setCorpId(500);
        accountA.setCharacterName("T034TEST-A");

        // structure1: structureId 9001L, typeId 100L, fuelExpires now+12h
        structure1 = new Structure();
        structure1.setStructureId(9001L);
        structure1.setTypeId(100L);
        structure1.setName("T034TEST-Structure");
        structure1.setFuelExpires(OffsetDateTime.now().plusHours(12));

        // job1: jobId 8001L, status "active", endDate now+1h
        job1 = new IndustryJob();
        job1.setJobId(8001L);
        job1.setStatus("active");
        job1.setEndDate(OffsetDateTime.now().plusHours(1));

        // 公共锁桩：所有锁键均返回 true（lenient 以兼容不同检测方法的锁键）
        lenient().when(cacheGateway.setIfAbsent(anyString(), anyString(), eq(600L), eq(TimeUnit.SECONDS)))
                .thenReturn(true);
    }

    // ─── 场景 1: sameEvent_within24h_noDuplicate（燃料） ──────

    @Test
    @DisplayName("SC-007 燃料 24h 去重：同一建筑窗口内第二次调用跳过")
    void sameEvent_within24h_noDuplicate() throws Exception {
        // Arrange
        when(eveAccountRepository.listAll()).thenReturn(List.of(accountA));
        when(structureRepository.selectFuelExpiresList(24, 500))
                .thenReturn(List.of(structure1));
        // 第一轮窗口内无记录（触发），第二轮窗口内已有记录（跳过）
        when(appNotificationRepository.existsByDedupKeyWithinHours("FUEL_LOW:9001", 24))
                .thenReturn(false, true);

        // fanout 桩全通
        when(eveAccountRepository.listByCorpId(500)).thenReturn(List.of(accountA));
        when(esiGateway.getAccessTokenWithExpiry(1001, 77))
                .thenReturn(new CharacterAccessTokenResult("Bearer t-scn", 1001, 1140L));
        when(esiGateway.queryCharacterRoles(1001, "Bearer t-scn"))
                .thenReturn(new CharacterRolesInfo(List.of("Director"), List.of(), List.of()));

        when(appNotificationRepository.insertBatch(any())).thenAnswer(invocation -> {
            List<?> rows = invocation.getArgument(0);
            for (int i = 0; i < rows.size(); i++) {
                var row = rows.get(i);
                if (row instanceof AppNotification n) {
                    n.setId(7000L + i);
                }
            }
            return rows.size();
        });
        when(pushGateway.isOnline(77)).thenReturn(true);

        // Act: 同一 service 实例连续调用两次
        service.detectFuelLow();
        service.detectFuelLow();

        // Assert: 仅第一轮插入
        ArgumentCaptor<List<AppNotification>> captor = ArgumentCaptor.forClass(List.class);
        verify(appNotificationRepository, times(1)).insertBatch(captor.capture());
        List<AppNotification> rows = captor.getValue();
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getUserId()).isEqualTo(77);
        assertThat(rows.get(0).getDedupKey()).isEqualTo("FUEL_LOW:9001");
    }

    // ─── 场景 2: persistentCondition_after24h_newReminder（燃料） ─

    @Test
    @DisplayName("SC-007 持续条件 24h 后再提醒：窗口过后每轮均触发")
    void persistentCondition_after24h_newReminder() throws Exception {
        // Arrange
        when(eveAccountRepository.listAll()).thenReturn(List.of(accountA));
        when(structureRepository.selectFuelExpiresList(24, 500))
                .thenReturn(List.of(structure1));
        // 恒 false：模拟窗口已过（每轮均新窗口）
        when(appNotificationRepository.existsByDedupKeyWithinHours("FUEL_LOW:9001", 24))
                .thenReturn(false);

        when(eveAccountRepository.listByCorpId(500)).thenReturn(List.of(accountA));
        when(esiGateway.getAccessTokenWithExpiry(1001, 77))
                .thenReturn(new CharacterAccessTokenResult("Bearer t-scn", 1001, 1140L));
        when(esiGateway.queryCharacterRoles(1001, "Bearer t-scn"))
                .thenReturn(new CharacterRolesInfo(List.of("Director"), List.of(), List.of()));

        when(appNotificationRepository.insertBatch(any())).thenAnswer(invocation -> {
            List<?> rows = invocation.getArgument(0);
            for (int i = 0; i < rows.size(); i++) {
                var row = rows.get(i);
                if (row instanceof AppNotification n) {
                    n.setId(7000L + i);
                }
            }
            return rows.size();
        });
        when(pushGateway.isOnline(77)).thenReturn(true);

        // Act
        service.detectFuelLow();
        service.detectFuelLow();

        // Assert: 持续条件每 24h 再提醒（两轮均插入）
        ArgumentCaptor<List<AppNotification>> captor = ArgumentCaptor.forClass(List.class);
        verify(appNotificationRepository, times(2)).insertBatch(captor.capture());
        List<List<AppNotification>> allBatches = captor.getAllValues();
        assertThat(allBatches).hasSize(2);
        assertThat(allBatches.get(0)).hasSize(1);
        assertThat(allBatches.get(1)).hasSize(1);
        assertThat(allBatches.get(0).get(0).getUserId()).isEqualTo(77);
        assertThat(allBatches.get(1).get(0).getUserId()).isEqualTo(77);
    }

    // ─── 场景 3: persistentCondition_cleared_stops（燃料） ─────

    @Test
    @DisplayName("SC-007 条件解除后不再产生通知：第二轮无低燃料建筑")
    void persistentCondition_cleared_stops() throws Exception {
        // Arrange
        when(eveAccountRepository.listAll()).thenReturn(List.of(accountA));
        // 第一轮有低燃料建筑，第二轮条件解除（空列表）
        when(structureRepository.selectFuelExpiresList(24, 500))
                .thenReturn(List.of(structure1), List.of());
        when(appNotificationRepository.existsByDedupKeyWithinHours("FUEL_LOW:9001", 24))
                .thenReturn(false);

        when(eveAccountRepository.listByCorpId(500)).thenReturn(List.of(accountA));
        when(esiGateway.getAccessTokenWithExpiry(1001, 77))
                .thenReturn(new CharacterAccessTokenResult("Bearer t-scn", 1001, 1140L));
        when(esiGateway.queryCharacterRoles(1001, "Bearer t-scn"))
                .thenReturn(new CharacterRolesInfo(List.of("Director"), List.of(), List.of()));

        when(appNotificationRepository.insertBatch(any())).thenAnswer(invocation -> {
            List<?> rows = invocation.getArgument(0);
            for (int i = 0; i < rows.size(); i++) {
                var row = rows.get(i);
                if (row instanceof AppNotification n) {
                    n.setId(7000L + i);
                }
            }
            return rows.size();
        });
        when(pushGateway.isOnline(77)).thenReturn(true);

        // Act
        service.detectFuelLow();
        service.detectFuelLow();

        // Assert: 仅第一轮插入（第二轮条件解除→无建筑→提前返回）
        ArgumentCaptor<List<AppNotification>> captor = ArgumentCaptor.forClass(List.class);
        verify(appNotificationRepository, times(1)).insertBatch(captor.capture());
        List<AppNotification> rows = captor.getValue();
        assertThat(rows).hasSize(1);
        // T034-M1: 补字段断言（dedupKey/characterId/userId）
        AppNotification row = rows.get(0);
        assertThat(row.getDedupKey()).isEqualTo("FUEL_LOW:9001");
        assertThat(row.getUserId()).isEqualTo(77);
    }

    // ─── 场景 4: industryComplete_notifiedExactlyOnce_acrossRounds ─

    @Test
    @DisplayName("SC-007 工业完成一次性事件：跨轮次至多通知一次")
    void industryComplete_notifiedExactlyOnce_acrossRounds() throws Exception {
        // Arrange
        when(eveAccountRepository.listAll()).thenReturn(List.of(accountA));
        when(authorizeUtil.authorizeInternal(77, 1001)).thenReturn(accountA);
        when(esiGateway.getAccessTokenWithExpiry(1001, 77))
                .thenReturn(new CharacterAccessTokenResult("Bearer t-scn", 1001, 1140L));
        when(esiGateway.queryCharacterIndustryJobs(1001, true, "Bearer t-scn"))
                .thenReturn(Flux.just(job1));
        // 第一轮未通知（触发），第二轮已存在（跳过）
        when(appNotificationRepository.existsByDedupKey("INDUSTRY_COMPLETE:8001"))
                .thenReturn(false, true);

        when(appNotificationRepository.insertBatch(any())).thenAnswer(invocation -> {
            List<?> rows = invocation.getArgument(0);
            for (int i = 0; i < rows.size(); i++) {
                var row = rows.get(i);
                if (row instanceof AppNotification n) {
                    n.setId(7000L + i);
                }
            }
            return rows.size();
        });
        when(pushGateway.isOnline(77)).thenReturn(true);

        // Act
        service.detectIndustryComplete();
        service.detectIndustryComplete();

        // Assert: 一次性事件至多一次
        ArgumentCaptor<List<AppNotification>> captor = ArgumentCaptor.forClass(List.class);
        verify(appNotificationRepository, times(1)).insertBatch(captor.capture());
        List<AppNotification> rows = captor.getValue();
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getUserId()).isEqualTo(77);
        assertThat(rows.get(0).getCharacterId()).isEqualTo(1001);
        assertThat(rows.get(0).getDedupKey()).isEqualTo("INDUSTRY_COMPLETE:8001");
    }

    // ─── 场景 5: fanout_userLosesPermission_noFurtherFuelNotifications ─

    @Test
    @DisplayName("SC-006 权限丧失扇出：用户失去建筑权限后不再收到燃料通知")
    void fanout_userLosesPermission_noFurtherFuelNotifications() throws Exception {
        // Arrange
        when(eveAccountRepository.listAll()).thenReturn(List.of(accountA));
        when(structureRepository.selectFuelExpiresList(24, 500))
                .thenReturn(List.of(structure1));
        // 恒 false：每轮均为新窗口（排除去重干扰，聚焦权限变化）
        when(appNotificationRepository.existsByDedupKeyWithinHours("FUEL_LOW:9001", 24))
                .thenReturn(false);

        when(eveAccountRepository.listByCorpId(500)).thenReturn(List.of(accountA));
        when(esiGateway.getAccessTokenWithExpiry(1001, 77))
                .thenReturn(new CharacterAccessTokenResult("Bearer t-scn", 1001, 1140L));
        // 第一轮有权（Director），第二轮无权（空角色）
        when(esiGateway.queryCharacterRoles(1001, "Bearer t-scn"))
                .thenReturn(
                        new CharacterRolesInfo(List.of("Director"), List.of(), List.of()),
                        new CharacterRolesInfo(List.of(), List.of(), List.of())
                );

        when(appNotificationRepository.insertBatch(any())).thenAnswer(invocation -> {
            List<?> rows = invocation.getArgument(0);
            for (int i = 0; i < rows.size(); i++) {
                var row = rows.get(i);
                if (row instanceof AppNotification n) {
                    n.setId(7000L + i);
                }
            }
            return rows.size();
        });
        when(pushGateway.isOnline(77)).thenReturn(true);

        // Act
        service.detectFuelLow();
        service.detectFuelLow();

        // Assert: 仅第一轮插入（第二轮权限丧失→无接收者→零行）
        ArgumentCaptor<List<AppNotification>> captor = ArgumentCaptor.forClass(List.class);
        verify(appNotificationRepository, times(1)).insertBatch(captor.capture());
        List<AppNotification> rows = captor.getValue();
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getUserId()).isEqualTo(77);
        assertThat(rows.get(0).getDedupKey()).isEqualTo("FUEL_LOW:9001");
    }

    // ─── 场景 6: multiCharacterUser_notificationsIsolatedPerCharacter ─

    @Test
    @DisplayName("SC-007 同用户双角色隔离：按角色隔离通知，互不去重")
    void multiCharacterUser_notificationsIsolatedPerCharacter() throws Exception {
        // Arrange: accountB 与 accountA 同 userId 77，不同 characterId
        EveAccount accountB = new EveAccount();
        accountB.setUserId(77);
        accountB.setCharacterId(1002);
        accountB.setCorpId(500);
        accountB.setCharacterName("T034TEST-B");

        when(eveAccountRepository.listAll()).thenReturn(List.of(accountA, accountB));
        when(authorizeUtil.authorizeInternal(77, 1001)).thenReturn(accountA);
        when(authorizeUtil.authorizeInternal(77, 1002)).thenReturn(accountB);
        when(esiGateway.getAccessTokenWithExpiry(1001, 77))
                .thenReturn(new CharacterAccessTokenResult("Bearer t-scn", 1001, 1140L));
        when(esiGateway.getAccessTokenWithExpiry(1002, 77))
                .thenReturn(new CharacterAccessTokenResult("Bearer t-scn", 1002, 1140L));
        when(esiGateway.queryCharacterSkillQueue(eq(1001), eq("Bearer t-scn")))
                .thenReturn(List.of());
        when(esiGateway.queryCharacterSkillQueue(eq(1002), eq("Bearer t-scn")))
                .thenReturn(List.of());
        // 两键均 false（互不去重）
        lenient().when(appNotificationRepository.existsByDedupKeyWithinHours(
                        eq("SKILL_QUEUE_EMPTY:1001"), eq(24)))
                .thenReturn(false);
        lenient().when(appNotificationRepository.existsByDedupKeyWithinHours(
                        eq("SKILL_QUEUE_EMPTY:1002"), eq(24)))
                .thenReturn(false);

        when(appNotificationRepository.insertBatch(any())).thenAnswer(invocation -> {
            List<?> rows = invocation.getArgument(0);
            for (int i = 0; i < rows.size(); i++) {
                var row = rows.get(i);
                if (row instanceof AppNotification n) {
                    n.setId(7000L + i);
                }
            }
            return rows.size();
        });
        when(pushGateway.isOnline(77)).thenReturn(true);

        // Act: 单次调用，内部遍历两个角色各触发一次 insertBatch
        service.detectSkillQueueEmpty();

        // Assert: insertBatch 调用 2 次，各 1 行
        ArgumentCaptor<List<AppNotification>> captor = ArgumentCaptor.forClass(List.class);
        verify(appNotificationRepository, times(2)).insertBatch(captor.capture());
        List<List<AppNotification>> allBatches = captor.getAllValues();
        assertThat(allBatches).hasSize(2);

        List<AppNotification> batch1 = allBatches.get(0);
        List<AppNotification> batch2 = allBatches.get(1);
        assertThat(batch1).hasSize(1);
        assertThat(batch2).hasSize(1);

        // 按角色隔离：characterId 不同，dedupKey 不同，userId 相同
        assertThat(batch1.get(0).getCharacterId()).isEqualTo(1001);
        assertThat(batch1.get(0).getDedupKey()).isEqualTo("SKILL_QUEUE_EMPTY:1001");
        assertThat(batch1.get(0).getUserId()).isEqualTo(77);

        assertThat(batch2.get(0).getCharacterId()).isEqualTo(1002);
        assertThat(batch2.get(0).getDedupKey()).isEqualTo("SKILL_QUEUE_EMPTY:1002");
        assertThat(batch2.get(0).getUserId()).isEqualTo(77);
    }
}
