package xyz.foolcat.eve.evehelper.interfaces.web.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.AppNotification;
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
import xyz.foolcat.eve.evehelper.domain.service.system.AppNotificationDetectService;
import xyz.foolcat.eve.evehelper.domain.util.AuthorizeUtil;
import xyz.foolcat.eve.evehelper.infrastructure.config.security.RbacAuthorizationManager;
import xyz.foolcat.eve.evehelper.shared.kernel.base.PageQuery;
import xyz.foolcat.eve.evehelper.shared.kernel.base.PageResult;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;


import static org.mockito.ArgumentMatchers.any;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 安全回归矩阵（T044，SC-006/Review Focus 5）。
 *
 * <p>固化跨属主/未认证安全矩阵：18 端点 401、10 端点 403、通知归属、
 * 燃料扇出零权限、解绑越权。零生产代码，仅测试。</p>
 *
 * <p>IT 范式源＝UserControllerAuthStatusIT：@SpringBootTest + @ActiveProfiles("test")
 * + @AutoConfigureMockMvc 完整过滤器链；@MockitoBean RbacAuthorizationManager
 * 条件式 mock（匿名请求拒绝、已认证请求放行）；mockAuthenticated＝
 * UsernamePasswordAuthenticationToken(userId,null,List.of(SimpleGrantedAuthority("ROLE_USER")))
 * 入 SecurityContextHolder；@AfterEach clearContext。</p>
 *
 * @author Leojian
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DisplayName("T044 安全回归矩阵（SC-006/Review Focus 5）")
class SecurityRegressionTest {

    private static final int USER_A = 888;
    private static final int CID_B = 9990001;
    private static final int CORP_B = 98000001;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    RbacAuthorizationManager rbacAuthorizationManager;

    @MockitoBean
    EsiGateway esiGateway;

    @MockitoBean
    EveAccountRepository eveAccountRepository;

    @MockitoBean
    AppNotificationRepository appNotificationRepository;

    @BeforeEach
    void setUp() {
        // 复刻真实 RBAC 语义：匿名/未认证拒绝(→ExceptionTranslationFilter→EntryPoint 401)，
        // 已认证放行(→控制器归属链 403)。恒 true 会让匿名穿透应用层守卫产生 403，
        // 使「未认证 401」用例不可达(GlobalExceptionHandler 无 401 映射，401 仅出自 Security 层)。
        lenient().when(rbacAuthorizationManager.check(any(), any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Supplier<Authentication> supplier = invocation.getArgument(0);
                    Authentication auth = supplier.get();
                    boolean denied = auth == null
                            || !auth.isAuthenticated()
                            || auth instanceof AnonymousAuthenticationToken;
                    return new AuthorizationDecision(!denied);
                });
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockAuthenticated(int userId) {
        var auth = new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // ────────────────────────────────────────────────────────────────
    //  用例 1: unauthenticated_all18Endpoints_401
    // ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("用例 1: 未认证访问全部 18 端点 → 401")
    void unauthenticated_all18Endpoints_401() throws Exception {
        // 18 端点清单（rest-api.md + 022_rbac_permissions.sql 逐字）
        List<MockHttpServletRequestBuilder> requests = List.of(
                // US1 角色面板 3
                get("/character/{cid}/panel", CID_B),
                get("/character/{cid}/skills", CID_B),
                get("/character/{cid}/skill-queue", CID_B),
                // US2 邮件 6
                get("/mail/{cid}/inbox", CID_B),
                get("/mail/{cid}/labels", CID_B),
                get("/mail/{cid}/{mid}", CID_B, 123),
                put("/mail/{cid}/{mid}/read", CID_B, 123)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"read\":true}"),
                post("/mail/{cid}/send", CID_B)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recipients\":[{\"recipientId\":1,\"recipientType\":\"character\"}],\"subject\":\"s\",\"body\":\"b\"}"),
                post("/mail/recipients/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"names\":[\"x\"]}"),
                // US3 工业直查 1
                get("/job/char/{cid}", CID_B),
                // US5 团员追踪 1
                get("/corporation/{corpId}/member-tracking?characterId={cid}", CORP_B, CID_B),
                // US4 通知 4
                get("/notification"),
                get("/notification/unread-count"),
                put("/notification/{id}/read", 5L),
                post("/notification/ws-ticket"),
                // US6 账号自助 3
                put("/user/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"OldPass123!\",\"newPassword\":\"NewPass456!\"}"),
                delete("/user/characters/{cid}", CID_B),
                get("/character/auth-url")
        );

        for (MockHttpServletRequestBuilder req : requests) {
            mockMvc.perform(req)
                    .andExpect(status().isUnauthorized());
        }
    }

    // ────────────────────────────────────────────────────────────────
    //  用例 2: crossOwner_characterScopedEndpoints_403
    // ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("用例 2: 用户 A 访问用户 B 角色 10 端点 → 403（归属链 null→USER_ACCOUNT_NOT_EXIST→ACCESS_UNAUTHORIZED→403）")
    void crossOwner_characterScopedEndpoints_403() throws Exception {
        mockAuthenticated(USER_A);
        // EveAccountRepository 未桩：getAccount(userId,cid)→null→403 链天然触发

        // 10 端点：panel/skills/skill-queue/inbox/labels/邮件详情/标记已读/send（8）+ job + member-tracking（2）
        List<MockHttpServletRequestBuilder> requests = List.of(
                // US1 角色面板 3
                get("/character/{cid}/panel", CID_B),
                get("/character/{cid}/skills", CID_B),
                get("/character/{cid}/skill-queue", CID_B),
                // US2 邮件 5
                get("/mail/{cid}/inbox", CID_B),
                get("/mail/{cid}/labels", CID_B),
                get("/mail/{cid}/{mid}", CID_B, 123),
                put("/mail/{cid}/{mid}/read", CID_B, 123)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"read\":true}"),
                post("/mail/{cid}/send", CID_B)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recipients\":[{\"recipientId\":1,\"recipientType\":\"character\"}],\"subject\":\"s\",\"body\":\"b\"}"),
                // US3 工业直查 1
                get("/job/char/{cid}", CID_B),
                // US5 团员追踪 1
                get("/corporation/{corpId}/member-tracking?characterId={cid}", CORP_B, CID_B)
        );

        for (MockHttpServletRequestBuilder req : requests) {
            mockMvc.perform(req)
                    .andExpect(status().isForbidden());
        }
    }

    // ────────────────────────────────────────────────────────────────
    //  用例 3: notificationOwnership_isolatedByCurrentUser
    // ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("用例 3: 通知归属隔离——当前用户分页+已读标记正常（mock pageByUser/markRead 按 userId=888）")
    void notificationOwnership_isolatedByCurrentUser() throws Exception {
        mockAuthenticated(USER_A);

        // Arrange: pageByUser 返回 1 行 id=5/userId=888 的 AppNotification fixture
        Date now = new Date();
        AppNotification entity = new AppNotification();
        entity.setId(5L);
        entity.setUserId(USER_A);
        entity.setEventType("MAIL_RECEIVED");
        entity.setTitle("测试邮件");
        entity.setContent("内容");
        entity.setCharacterId(CID_B);
        entity.setCorpId(CORP_B);
        entity.setIsRead(false);
        entity.setGmtCreate(now);

        PageResult<AppNotification> pageResult = PageResult.<AppNotification>builder()
                .records(List.of(entity))
                .total(1L)
                .current(1L)
                .size(20L)
                .pages(1L)
                .hasNext(false)
                .hasPrevious(false)
                .build();

        when(appNotificationRepository.pageByUser(eq(USER_A), any(), any(PageQuery.class)))
                .thenReturn(pageResult);

        // Act + Assert: GET /notification → 200 + 数据正确（服务层自动 AppNotification→NotificationItem 映射）
        mockMvc.perform(get("/notification"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.records[0].id").value(5));

        verify(appNotificationRepository).pageByUser(eq(USER_A), any(), any(PageQuery.class));

        // Arrange: markRead(5L, 888)→1（归属内）
        when(appNotificationRepository.markRead(eq(5L), eq(USER_A))).thenReturn(1);

        // Act + Assert: PUT /notification/5/read → 200
        mockMvc.perform(put("/notification/{id}/read", 5L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));

        verify(appNotificationRepository).markRead(eq(5L), eq(USER_A));
    }

    // ────────────────────────────────────────────────────────────────
    //  用例 4: notificationMarkRead_otherUsersRow_rejected
    // ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("用例 4: 标记他人通知已读 → 403（markRead 返回 0→EveHelperException ACCESS_UNAUTHORIZED）")
    void notificationMarkRead_otherUsersRow_rejected() throws Exception {
        mockAuthenticated(USER_A);

        // Arrange: markRead(9L, 888)→0（他人行，归属外）
        when(appNotificationRepository.markRead(eq(9L), eq(USER_A))).thenReturn(0);

        // Act + Assert: PUT /notification/9/read → 403
        // 注：NotificationApplicationService.markRead:83-88 rows==0 抛 EveHelperException(ACCESS_UNAUTHORIZED)
        // GlobalExceptionHandler 映射为 403。
        mockMvc.perform(put("/notification/{id}/read", 9L))
                .andExpect(status().isForbidden());

        verify(appNotificationRepository).markRead(eq(9L), eq(USER_A));
    }

    // ────────────────────────────────────────────────────────────────
    //  用例 5: unbind_crossOwner_rejected
    // ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("用例 5: 解绑他人角色 → 403（authorizeUtil.authorize 内部归属校验失败）")
    void unbind_crossOwner_rejected() throws Exception {
        mockAuthenticated(USER_A);

        // EveAccountRepository 未桩：getAccount(userId,cid)→null→403 链天然触发
        // authorizeUtil.authorize(characterId) 内部调用 EveAccountService.getAccountOne
        // → repository.getAccount null → USER_ACCOUNT_NOT_EXIST → ACCESS_UNAUTHORIZED → 403

        // Act + Assert
        mockMvc.perform(delete("/user/characters/{cid}", CID_B))
                .andExpect(status().isForbidden());

        // 验证仓储层 deleteByUserIdAndCharacterId 从未被调用（归属校验在前）
        verify(eveAccountRepository, never()).deleteByUserIdAndCharacterId(any(), any());
    }

    // ────────────────────────────────────────────────────────────────
    //  用例 6: fuelFanout_noPermission_zeroNotifications
    // ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("用例 6: 燃料扇出——无权限用户零通知（SC-006，复用 T034 fixture）")
    void fuelFanout_noPermission_zeroNotifications() throws Exception {
        // 本地 plain mock 7 依赖，不经 Spring（brief 事实 5 + 控制器补充 5）
        AppNotificationRepository repo = mock(AppNotificationRepository.class);
        AppNotificationPushGateway pushGateway = mock(AppNotificationPushGateway.class);
        EveAccountRepository accountRepo = mock(EveAccountRepository.class);
        StructureRepository structureRepo = mock(StructureRepository.class);
        EsiGateway esiGw = mock(EsiGateway.class);
        CacheGateway cacheGw = mock(CacheGateway.class);
        AuthorizeUtil authUtil = mock(AuthorizeUtil.class);

        AppNotificationDetectService service = new AppNotificationDetectService(
                repo, pushGateway, accountRepo, structureRepo, esiGw, cacheGw, authUtil
        );

        // accountA: userId 77, characterId 1001, corpId 500（T034 fixture）
        EveAccount accountA = new EveAccount();
        accountA.setUserId(77);
        accountA.setCharacterId(1001);
        accountA.setCorpId(500);
        accountA.setCharacterName("T034-A");

        // structure: structureId 9001L, typeId 100L, fuelExpires now+12h
        Structure structure = new Structure();
        structure.setStructureId(9001L);
        structure.setTypeId(100L);
        structure.setName("T034-Structure");
        structure.setFuelExpires(OffsetDateTime.now().plusHours(12));

        // 锁获取成功
        when(cacheGw.setIfAbsent(eq("app:notify:lock:FUEL_LOW"), any(), eq(600L), eq(TimeUnit.SECONDS)))
                .thenReturn(true);

        // listAll→[A]
        when(accountRepo.listAll()).thenReturn(List.of(accountA));

        // selectFuelExpiresList(24,500)→[structure 9001L/typeId 100L/fuelExpires now+12h]
        when(structureRepo.selectFuelExpiresList(24, 500)).thenReturn(List.of(structure));

        // existsByDedupKeyWithinHours→false（未去重）
        when(repo.existsByDedupKeyWithinHours("FUEL_LOW:9001", 24)).thenReturn(false);

        // listByCorpId(500)→[A]
        when(accountRepo.listByCorpId(500)).thenReturn(List.of(accountA));

        // getAccessTokenWithExpiry(1001,77)→Bearer t-sec
        CharacterAccessTokenResult tokenResult = new CharacterAccessTokenResult("Bearer t-sec", 1001, 1140L);
        when(esiGw.getAccessTokenWithExpiry(1001, 77)).thenReturn(tokenResult);

        // queryCharacterRoles→无权限 fixture（List.of(),List.of(),List.of()）
        CharacterRolesInfo noPermRoles = new CharacterRolesInfo(List.of(), List.of(), List.of());
        when(esiGw.queryCharacterRoles(1001, "Bearer t-sec")).thenReturn(noPermRoles);

        // Act
        service.detectFuelLow();

        // Assert: insertBatch never + pushToUser never
        verify(repo, never()).insertBatch(any());
        verify(pushGateway, never()).pushToUser(any(), anyString());
    }
}
