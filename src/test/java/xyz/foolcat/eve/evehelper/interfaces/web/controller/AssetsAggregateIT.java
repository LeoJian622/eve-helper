package xyz.foolcat.eve.evehelper.interfaces.web.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.model.vo.AssetsAggregateVO;
import xyz.foolcat.eve.evehelper.domain.service.system.AssetsService;
import xyz.foolcat.eve.evehelper.domain.service.system.EveAccountService;
import xyz.foolcat.eve.evehelper.infrastructure.config.security.RbacAuthorizationManager;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 资产多角色聚合端点集成测试。
 *
 * <p>MockMvc 走完整过滤器链 + 控制器 + 应用服务,注入 {@code EveAccountService}/{@code AssetsService}
 * 以及 {@code RbacAuthorizationManager}(UBAC 依赖 Redis,此处规避以聚焦聚合端点语义)。
 * 安全上下文注入 userId 主体,UserUtil#getUserId 据此返回该值。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DisplayName("资产多角色聚合端点集成测试")
class AssetsAggregateIT {

    private static final int CURRENT_USER_ID = 7;
    private static final int BOUND_ROLE_A = 2112832425;
    private static final int BOUND_ROLE_B = 2112832426;
    private static final int NO_ASSET_ROLE = 2112832427;
    private static final int UNBOUND_ROLE = 9999;

    @Autowired
    MockMvc mockMvc;

    @MockBean
    EveAccountService eveAccountService;

    @MockBean
    AssetsService assetsService;

    @MockBean
    RbacAuthorizationManager rbacAuthorizationManager;

    @BeforeEach
    void setUp() throws Exception {
        loginAs(CURRENT_USER_ID);
        when(rbacAuthorizationManager.authorize(any(), any()))
                .thenReturn(new AuthorizationDecision(true));
        when(rbacAuthorizationManager.check(any(), any()))
                .thenReturn(new AuthorizationDecision(true));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(int userId) {
        // principal 为 userId claim(Number),UserUtil.getUserId() 返回其 int 值
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        (long) userId, null, List.of(new SimpleGrantedAuthority("ADMIN"))));
    }

    private static EveAccount account(int characterId) {
        EveAccount a = new EveAccount();
        a.setCharacterId(characterId);
        return a;
    }

    @Test
    @DisplayName("多角色 -> 按角色分组返回聚合,含 ownerId/assetCount/assetValue/categoryCount")
    void aggregateE2E_multiRole_groupedWithAllFields() throws Exception {
        when(eveAccountService.getAccountList(CURRENT_USER_ID))
                .thenReturn(List.of(account(BOUND_ROLE_A), account(BOUND_ROLE_B)));
        when(assetsService.getAggregateByOwnerId(BOUND_ROLE_A))
                .thenReturn(new AssetsAggregateVO(BOUND_ROLE_A, 100L, 1500.5, 3L));
        when(assetsService.getAggregateByOwnerId(BOUND_ROLE_B))
                .thenReturn(new AssetsAggregateVO(BOUND_ROLE_B, 50L, 99.0, 1L));

        mockMvc.perform(get("/assets/aggregate").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].ownerId").value(BOUND_ROLE_A))
                .andExpect(jsonPath("$.data[0].assetCount").value(100))
                .andExpect(jsonPath("$.data[0].assetValue").value(1500.5))
                .andExpect(jsonPath("$.data[0].categoryCount").value(3))
                .andExpect(jsonPath("$.data[1].ownerId").value(BOUND_ROLE_B));
    }

    @Test
    @DisplayName("无资产角色 -> 0 值聚合视图,不报错")
    void aggregateE2E_emptyRole_zeroValueView() throws Exception {
        when(eveAccountService.getAccountList(CURRENT_USER_ID))
                .thenReturn(List.of(account(NO_ASSET_ROLE)));
        when(assetsService.getAggregateByOwnerId(NO_ASSET_ROLE)).thenReturn(null);

        mockMvc.perform(get("/assets/aggregate").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].ownerId").value(NO_ASSET_ROLE))
                .andExpect(jsonPath("$.data[0].assetCount").value(0))
                .andExpect(jsonPath("$.data[0].assetValue").value(0))
                .andExpect(jsonPath("$.data[0].categoryCount").value(0));
    }

    @Test
    @DisplayName("无绑定角色 -> 空列表")
    void aggregateE2E_noRoles_emptyList() throws Exception {
        when(eveAccountService.getAccountList(CURRENT_USER_ID)).thenReturn(List.of());

        mockMvc.perform(get("/assets/aggregate").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("越权角色(未绑定)不出现且不被查询")
    void aggregateE2E_unboundRole_notQueried() throws Exception {
        when(eveAccountService.getAccountList(CURRENT_USER_ID))
                .thenReturn(List.of(account(BOUND_ROLE_A)));

        mockMvc.perform(get("/assets/aggregate").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].ownerId").value(BOUND_ROLE_A));

        // 未绑定角色未被枚举 -> 不会被查询、不会出现在响应
        verify(assetsService, never()).getAggregateByOwnerId(UNBOUND_ROLE);
    }

    @Test
    @DisplayName("未认证 -> 返回空列表")
    void aggregateE2E_unauthenticated_emptyList() throws Exception {
        SecurityContextHolder.clearContext();
        when(eveAccountService.getAccountList(-1)).thenReturn(List.of());

        mockMvc.perform(get("/assets/aggregate").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }
}