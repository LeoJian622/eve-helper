package xyz.foolcat.eve.evehelper.domain.service.system;

import cn.hutool.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.AppNotification;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.IndustryJob;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterAccessTokenResult;
import xyz.foolcat.eve.evehelper.domain.model.vo.SkillQueueItem;
import xyz.foolcat.eve.evehelper.domain.port.appws.AppNotificationPushGateway;
import xyz.foolcat.eve.evehelper.domain.port.cache.CacheGateway;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.domain.repository.system.AppNotificationRepository;
import xyz.foolcat.eve.evehelper.domain.repository.system.EveAccountRepository;
import xyz.foolcat.eve.evehelper.domain.repository.system.StructureRepository;
import xyz.foolcat.eve.evehelper.domain.util.AuthorizeUtil;

import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;
import xyz.foolcat.eve.evehelper.shared.result.ResultCode;

import reactor.core.publisher.Flux;

import java.time.OffsetDateTime;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AppNotificationDetectService 工业 + 技能队列 + 清理单元测试（T032）。
 * <p>
 * 10 用例覆盖 US4 工业完成检测、技能队列空检测、过期清理核心流程，纯单元 mock 7 依赖。
 *
 * @author Leojian
 * date 2026-11-13
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("T032 工业+技能队列+清理 领域服务单元测试")
class AppNotificationDetectServiceIndustrySkillTest {

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
    private CharacterAccessTokenResult tokenA;
    private CharacterAccessTokenResult tokenB;

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

        // accountA: userId 77, characterId 1001, corpId 500, name "T032TEST-A"
        accountA = new EveAccount();
        accountA.setUserId(77);
        accountA.setCharacterId(1001);
        accountA.setCorpId(500);
        accountA.setCharacterName("T032TEST-A");

        // accountB: userId 78, characterId 1002, corpId 500, name "T032TEST-B"
        accountB = new EveAccount();
        accountB.setUserId(78);
        accountB.setCharacterId(1002);
        accountB.setCorpId(500);
        accountB.setCharacterName("T032TEST-B");

        tokenA = new CharacterAccessTokenResult("Bearer t-ind", 1001, 1140L);
        tokenB = new CharacterAccessTokenResult("Bearer t-ind-b", 1002, 1140L);
    }

    // ─── 用例 1: industry_activeWithin24h_notifies ──────────

    @Test
    @DisplayName("工业完成：active 状态 24h 内结束 → 触发通知")
    void industry_activeWithin24h_notifies() throws Exception {
        // Arrange
        when(cacheGateway.setIfAbsent(eq("app:notify:lock:INDUSTRY_COMPLETE"), anyString(), eq(600L), eq(TimeUnit.SECONDS)))
                .thenReturn(true);
        when(eveAccountRepository.listAll()).thenReturn(List.of(accountA));
        when(authorizeUtil.authorizeInternal(77, 1001)).thenReturn(accountA);
        when(esiGateway.getAccessTokenWithExpiry(1001, 77)).thenReturn(tokenA);

        IndustryJob job = new IndustryJob();
        job.setJobId(8001L);
        job.setStatus("active");
        job.setEndDate(OffsetDateTime.now().plusHours(12));

        when(esiGateway.queryCharacterIndustryJobs(eq(1001), eq(true), eq("Bearer t-ind")))
                .thenReturn(Flux.just(job));
        when(appNotificationRepository.existsByDedupKey("INDUSTRY_COMPLETE:8001")).thenReturn(false);
        when(appNotificationRepository.insertBatch(any())).thenAnswer(invocation -> {
            List<?> rows = invocation.getArgument(0);
            for (int i = 0; i < rows.size(); i++) {
                var row = rows.get(i);
                if (row instanceof AppNotification n) {
                    n.setId(6000L + i);
                }
            }
            return rows.size();
        });
        when(pushGateway.isOnline(77)).thenReturn(true);

        // Act
        service.detectIndustryComplete();

        // Assert
        ArgumentCaptor<List<AppNotification>> captor = ArgumentCaptor.forClass(List.class);
        verify(appNotificationRepository).insertBatch(captor.capture());
        List<AppNotification> rows = captor.getValue();
        assertThat(rows).hasSize(1);

        AppNotification row = rows.get(0);
        assertThat(row.getUserId()).isEqualTo(77);
        assertThat(row.getCharacterId()).isEqualTo(1001);
        assertThat(row.getCorpId()).isEqualTo(500);
        assertThat(row.getEventType()).isEqualTo("INDUSTRY_COMPLETE");
        assertThat(row.getDedupKey()).isEqualTo("INDUSTRY_COMPLETE:8001");
        assertThat(row.getContent()).contains("工业订单 8001 结束时间: ");
        assertThat(row.getIsRead()).isFalse();

        // 推送断言
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(pushGateway).pushToUser(eq(77), payloadCaptor.capture());
        String payload = payloadCaptor.getValue();
        assertThat(payload).contains("\"type\":\"NOTIFICATION\"");

        verify(cacheGateway).compareAndDelete(eq("app:notify:lock:INDUSTRY_COMPLETE"), anyString());
    }

    // ─── 用例 2: industry_deliveredOlderThan24h_skipped ─────

    @Test
    @DisplayName("工业完成：delivered 状态超过 24h → 跳过（谓词先于去重）")
    void industry_deliveredOlderThan24h_skipped() throws Exception {
        // Arrange
        when(cacheGateway.setIfAbsent(eq("app:notify:lock:INDUSTRY_COMPLETE"), anyString(), eq(600L), eq(TimeUnit.SECONDS)))
                .thenReturn(true);
        when(eveAccountRepository.listAll()).thenReturn(List.of(accountA));
        when(authorizeUtil.authorizeInternal(77, 1001)).thenReturn(accountA);
        when(esiGateway.getAccessTokenWithExpiry(1001, 77)).thenReturn(tokenA);

        IndustryJob job = new IndustryJob();
        job.setJobId(8002L);
        job.setStatus("delivered");
        job.setEndDate(OffsetDateTime.now().minusHours(48));

        when(esiGateway.queryCharacterIndustryJobs(eq(1001), eq(true), eq("Bearer t-ind")))
                .thenReturn(Flux.just(job));

        // Act
        service.detectIndustryComplete();

        // Assert: 谓词不满足 → 不查去重、不插入
        verify(appNotificationRepository, never()).existsByDedupKey(anyString());
        verify(appNotificationRepository, never()).insertBatch(any());
        verify(cacheGateway).compareAndDelete(eq("app:notify:lock:INDUSTRY_COMPLETE"), anyString());
    }

    // ─── 用例 3: industry_deliveredWithin24h_notifies ───────

    @Test
    @DisplayName("工业完成：delivered 状态 24h 内 → 触发通知")
    void industry_deliveredWithin24h_notifies() throws Exception {
        // Arrange
        when(cacheGateway.setIfAbsent(eq("app:notify:lock:INDUSTRY_COMPLETE"), anyString(), eq(600L), eq(TimeUnit.SECONDS)))
                .thenReturn(true);
        when(eveAccountRepository.listAll()).thenReturn(List.of(accountA));
        when(authorizeUtil.authorizeInternal(77, 1001)).thenReturn(accountA);
        when(esiGateway.getAccessTokenWithExpiry(1001, 77)).thenReturn(tokenA);

        IndustryJob job = new IndustryJob();
        job.setJobId(8003L);
        job.setStatus("delivered");
        job.setEndDate(OffsetDateTime.now().minusHours(1));

        when(esiGateway.queryCharacterIndustryJobs(eq(1001), eq(true), eq("Bearer t-ind")))
                .thenReturn(Flux.just(job));
        when(appNotificationRepository.existsByDedupKey("INDUSTRY_COMPLETE:8003")).thenReturn(false);
        when(appNotificationRepository.insertBatch(any())).thenAnswer(invocation -> {
            List<?> rows = invocation.getArgument(0);
            for (int i = 0; i < rows.size(); i++) {
                var row = rows.get(i);
                if (row instanceof AppNotification n) {
                    n.setId(6000L + i);
                }
            }
            return rows.size();
        });
        when(pushGateway.isOnline(77)).thenReturn(true);

        // Act
        service.detectIndustryComplete();

        // Assert
        ArgumentCaptor<List<AppNotification>> captor = ArgumentCaptor.forClass(List.class);
        verify(appNotificationRepository).insertBatch(captor.capture());
        List<AppNotification> rows = captor.getValue();
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getDedupKey()).isEqualTo("INDUSTRY_COMPLETE:8003");
        assertThat(rows.get(0).getEventType()).isEqualTo("INDUSTRY_COMPLETE");

        verify(cacheGateway).compareAndDelete(eq("app:notify:lock:INDUSTRY_COMPLETE"), anyString());
    }

    // ─── 用例 4: industry_oneShotDedup_everWithinRetention ──

    @Test
    @DisplayName("工业完成：一次性去重（不限时间）→ 已存在则跳过")
    void industry_oneShotDedup_everWithinRetention() throws Exception {
        // Arrange
        when(cacheGateway.setIfAbsent(eq("app:notify:lock:INDUSTRY_COMPLETE"), anyString(), eq(600L), eq(TimeUnit.SECONDS)))
                .thenReturn(true);
        when(eveAccountRepository.listAll()).thenReturn(List.of(accountA));
        when(authorizeUtil.authorizeInternal(77, 1001)).thenReturn(accountA);
        when(esiGateway.getAccessTokenWithExpiry(1001, 77)).thenReturn(tokenA);

        IndustryJob job = new IndustryJob();
        job.setJobId(8001L);
        job.setStatus("active");
        job.setEndDate(OffsetDateTime.now().plusHours(1));

        when(esiGateway.queryCharacterIndustryJobs(eq(1001), eq(true), eq("Bearer t-ind")))
                .thenReturn(Flux.just(job));
        // 去重已存在 → 跳过
        when(appNotificationRepository.existsByDedupKey("INDUSTRY_COMPLETE:8001")).thenReturn(true);

        // Act
        service.detectIndustryComplete();

        // Assert: 不插入、不推送
        verify(appNotificationRepository, never()).insertBatch(any());
        verify(pushGateway, never()).pushToUser(any(), anyString());
        verify(cacheGateway).compareAndDelete(eq("app:notify:lock:INDUSTRY_COMPLETE"), anyString());
    }

    // ─── 用例 5: skill_emptyQueue_notifies ──────────────────

    @Test
    @DisplayName("技能队列空：空队列 → 触发通知")
    void skill_emptyQueue_notifies() throws Exception {
        // Arrange
        when(cacheGateway.setIfAbsent(eq("app:notify:lock:SKILL_QUEUE_EMPTY"), anyString(), eq(600L), eq(TimeUnit.SECONDS)))
                .thenReturn(true);
        when(eveAccountRepository.listAll()).thenReturn(List.of(accountA));
        when(authorizeUtil.authorizeInternal(77, 1001)).thenReturn(accountA);
        when(esiGateway.getAccessTokenWithExpiry(1001, 77)).thenReturn(tokenA);
        when(esiGateway.queryCharacterSkillQueue(1001, "Bearer t-ind")).thenReturn(List.of());
        when(appNotificationRepository.existsByDedupKeyWithinHours("SKILL_QUEUE_EMPTY:1001", 24)).thenReturn(false);
        when(appNotificationRepository.insertBatch(any())).thenAnswer(invocation -> {
            List<?> rows = invocation.getArgument(0);
            for (int i = 0; i < rows.size(); i++) {
                var row = rows.get(i);
                if (row instanceof AppNotification n) {
                    n.setId(6000L + i);
                }
            }
            return rows.size();
        });
        when(pushGateway.isOnline(77)).thenReturn(true);

        // Act
        service.detectSkillQueueEmpty();

        // Assert
        ArgumentCaptor<List<AppNotification>> captor = ArgumentCaptor.forClass(List.class);
        verify(appNotificationRepository).insertBatch(captor.capture());
        List<AppNotification> rows = captor.getValue();
        assertThat(rows).hasSize(1);

        AppNotification row = rows.get(0);
        assertThat(row.getCharacterId()).isEqualTo(1001);
        assertThat(row.getDedupKey()).isEqualTo("SKILL_QUEUE_EMPTY:1001");
        assertThat(row.getEventType()).isEqualTo("SKILL_QUEUE_EMPTY");
        assertThat(row.getContent()).isEqualTo("角色 T032TEST-A 技能队列已空，请及时安排训练");

        verify(pushGateway).pushToUser(eq(77), anyString());
        verify(cacheGateway).compareAndDelete(eq("app:notify:lock:SKILL_QUEUE_EMPTY"), anyString());
    }

    // ─── 用例 6: skill_nonEmptyQueue_noNotification ─────────

    @Test
    @DisplayName("技能队列非空：不触发通知")
    void skill_nonEmptyQueue_noNotification() throws Exception {
        // Arrange
        when(cacheGateway.setIfAbsent(eq("app:notify:lock:SKILL_QUEUE_EMPTY"), anyString(), eq(600L), eq(TimeUnit.SECONDS)))
                .thenReturn(true);
        when(eveAccountRepository.listAll()).thenReturn(List.of(accountA));
        when(authorizeUtil.authorizeInternal(77, 1001)).thenReturn(accountA);
        when(esiGateway.getAccessTokenWithExpiry(1001, 77)).thenReturn(tokenA);
        when(esiGateway.queryCharacterSkillQueue(1001, "Bearer t-ind"))
                .thenReturn(List.of(mock(SkillQueueItem.class)));

        // Act
        service.detectSkillQueueEmpty();

        // Assert: 非空队列 → 不查去重、不插入
        verify(appNotificationRepository, never()).existsByDedupKeyWithinHours(anyString(), anyInt());
        verify(appNotificationRepository, never()).insertBatch(any());
        verify(cacheGateway).compareAndDelete(eq("app:notify:lock:SKILL_QUEUE_EMPTY"), anyString());
    }

    // ─── 用例 7: skill_24hRepeat_afterWindow ────────────────

    @Test
    @DisplayName("技能队列空 24h 重复抑制：窗口内跳过，窗口后再提醒")
    void skill_24hRepeat_afterWindow() throws Exception {
        // Arrange: 两次调用，第一次窗口内存在，第二次窗口外不存在
        when(cacheGateway.setIfAbsent(eq("app:notify:lock:SKILL_QUEUE_EMPTY"), anyString(), eq(600L), eq(TimeUnit.SECONDS)))
                .thenReturn(true);
        when(eveAccountRepository.listAll()).thenReturn(List.of(accountA));
        when(authorizeUtil.authorizeInternal(77, 1001)).thenReturn(accountA);
        when(esiGateway.getAccessTokenWithExpiry(1001, 77)).thenReturn(tokenA);
        when(esiGateway.queryCharacterSkillQueue(1001, "Bearer t-ind")).thenReturn(List.of());
        when(appNotificationRepository.existsByDedupKeyWithinHours("SKILL_QUEUE_EMPTY:1001", 24))
                .thenReturn(true, false);
        when(appNotificationRepository.insertBatch(any())).thenAnswer(invocation -> {
            List<?> rows = invocation.getArgument(0);
            for (int i = 0; i < rows.size(); i++) {
                var row = rows.get(i);
                if (row instanceof AppNotification n) {
                    n.setId(6000L + i);
                }
            }
            return rows.size();
        });
        when(pushGateway.isOnline(77)).thenReturn(true);

        // Act: 调用两次
        service.detectSkillQueueEmpty();
        service.detectSkillQueueEmpty();

        // Assert: 仅第二次触发插入
        verify(appNotificationRepository, times(1)).insertBatch(any());
    }

    // ─── 用例 8: detect_oneCharacterAuthFails_othersContinue ─

    @Test
    @DisplayName("多角色隔离：一角色授权失败不阻断其他角色")
    void detect_oneCharacterAuthFails_othersContinue() throws Exception {
        // Arrange
        when(cacheGateway.setIfAbsent(eq("app:notify:lock:INDUSTRY_COMPLETE"), anyString(), eq(600L), eq(TimeUnit.SECONDS)))
                .thenReturn(true);
        when(eveAccountRepository.listAll()).thenReturn(List.of(accountA, accountB));

        // accountA 授权失败
        when(authorizeUtil.authorizeInternal(77, 1001))
                .thenThrow(new EveHelperException(ResultCode.ACCESS_UNAUTHORIZED));

        // accountB 授权成功 + 有命中
        when(authorizeUtil.authorizeInternal(78, 1002)).thenReturn(accountB);
        when(esiGateway.getAccessTokenWithExpiry(1002, 78)).thenReturn(tokenB);

        IndustryJob jobB = new IndustryJob();
        jobB.setJobId(8004L);
        jobB.setStatus("active");
        jobB.setEndDate(OffsetDateTime.now().plusHours(2));

        when(esiGateway.queryCharacterIndustryJobs(eq(1002), eq(true), eq("Bearer t-ind-b")))
                .thenReturn(Flux.just(jobB));
        when(appNotificationRepository.existsByDedupKey("INDUSTRY_COMPLETE:8004")).thenReturn(false);
        when(appNotificationRepository.insertBatch(any())).thenAnswer(invocation -> {
            List<?> rows = invocation.getArgument(0);
            for (int i = 0; i < rows.size(); i++) {
                var row = rows.get(i);
                if (row instanceof AppNotification n) {
                    n.setId(6000L + i);
                }
            }
            return rows.size();
        });
        when(pushGateway.isOnline(78)).thenReturn(true);

        // Act
        service.detectIndustryComplete();

        // Assert: 恰 1 行，且属主为 accountB(userId=78)
        ArgumentCaptor<List<AppNotification>> captor = ArgumentCaptor.forClass(List.class);
        verify(appNotificationRepository, times(1)).insertBatch(captor.capture());
        List<AppNotification> rows = captor.getValue();
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getUserId()).isEqualTo(78);

        verify(cacheGateway).compareAndDelete(eq("app:notify:lock:INDUSTRY_COMPLETE"), anyString());
    }

    // ─── 用例 9: detect_unboundCharacter_excluded ───────────

    @Test
    @DisplayName("未绑定角色天然排除：listAll 不含则不查询")
    void detect_unboundCharacter_excluded() throws Exception {
        // Arrange: listAll 仅 [A]，cid 2002 未绑定
        when(cacheGateway.setIfAbsent(eq("app:notify:lock:INDUSTRY_COMPLETE"), anyString(), eq(600L), eq(TimeUnit.SECONDS)))
                .thenReturn(true);
        when(eveAccountRepository.listAll()).thenReturn(List.of(accountA));
        when(authorizeUtil.authorizeInternal(77, 1001)).thenReturn(accountA);
        when(esiGateway.getAccessTokenWithExpiry(1001, 77)).thenReturn(tokenA);
        when(esiGateway.queryCharacterIndustryJobs(eq(1001), eq(true), eq("Bearer t-ind")))
                .thenReturn(Flux.empty());

        // Act
        service.detectIndustryComplete();

        // Assert: cid 1001 查询一次，cid 2002 从未查询
        verify(esiGateway, times(1)).queryCharacterIndustryJobs(eq(1001), eq(true), eq("Bearer t-ind"));
        verify(esiGateway, never()).queryCharacterIndustryJobs(eq(2002), anyBoolean(), anyString());

        verify(cacheGateway).compareAndDelete(eq("app:notify:lock:INDUSTRY_COMPLETE"), anyString());
    }

    // ─── 用例 10: cleanup_batchesUntilZero ──────────────────

    @Test
    @DisplayName("过期清理：分批删除直至 0，无锁")
    void cleanup_batchesUntilZero() {
        // Arrange
        when(appNotificationRepository.deleteExpired(30, 500))
                .thenReturn(500, 500, 120, 0);

        // Act
        int total = service.cleanupExpired();

        // Assert
        assertThat(total).isEqualTo(1120);
        verify(appNotificationRepository, times(4)).deleteExpired(30, 500);
        // 无锁：cacheGateway.setIfAbsent 从未调用
        verify(cacheGateway, never()).setIfAbsent(anyString(), anyString(), anyLong(), any());
    }
}
