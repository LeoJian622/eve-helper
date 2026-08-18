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
import xyz.foolcat.eve.evehelper.domain.service.security.ResourceOwnershipPolicy;
import xyz.foolcat.eve.evehelper.infrastructure.config.security.RbacAuthorizationManager;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 军团钱包流水端点集成测试(T008)。
 *
 * <p>MockMvc 走完整过滤器链 + 控制器 + 应用服务 + 领域服务 + 真实仓储/mapper。</p>
 * <ul>
 *   <li>RBAC(RbacAuthorizationManager)mock 以自动规避权限映射对 Redis 的依赖,默认放行。</li>
 *   <li>AccessGuard 保持真实;仅 mock 其唯一 DB 依赖 ResourceOwnershipPolicy。</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DisplayName("军团钱包流水端点集成测试")
class WalletJournalControllerCorpIT {

    private static final int CURRENT_USER_ID = 7;
    private static final int CORP_ID = 1000001;
    private static final int FOREIGN_CORP_ID = 999998;

    @Autowired
    MockMvc mockMvc;

    @MockBean
    RbacAuthorizationManager rbacAuthorizationManager;

    @MockBean
    ResourceOwnershipPolicy resourceOwnershipPolicy;

    @BeforeEach
    void setUp() {
        when(rbacAuthorizationManager.authorize(any(), any()))
                .thenReturn(new AuthorizationDecision(true));
        when(rbacAuthorizationManager.check(any(), any()))
                .thenReturn(new AuthorizationDecision(true));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(int userId, String authority) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        (long) userId, null, List.of(new SimpleGrantedAuthority(authority))));
    }

    // ---------- 认证/授权拒绝 ----------

    @Test
    @DisplayName("未认证访问军团分页 -> 401(拒绝)")
    void queryCorporationPage_unauthenticated_forbidden() throws Exception {
        SecurityContextHolder.clearContext();
        when(rbacAuthorizationManager.authorize(any(), any()))
                .thenReturn(new AuthorizationDecision(false));
        when(rbacAuthorizationManager.check(any(), any()))
                .thenReturn(new AuthorizationDecision(false));

        mockMvc.perform(get("/wallet/journal/corp/" + CORP_ID)
                        .param("division", "2").param("current", "1").param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("已认证但无权查询他人军团 -> 403")
    void queryCorporationPage_authenticatedUnauthorized_denied() throws Exception {
        loginAs(CURRENT_USER_ID, "USER");

        mockMvc.perform(get("/wallet/journal/corp/" + FOREIGN_CORP_ID)
                        .param("division", "1").param("current", "1").param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("已认证但无权同步他人军团 -> 403")
    void syncCorporation_authenticatedUnauthorized_denied() throws Exception {
        loginAs(CURRENT_USER_ID, "USER");

        mockMvc.perform(post("/wallet/journal/corp/" + FOREIGN_CORP_ID + "/sync")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    // ---------- division 参数校验 ----------

    @Test
    @DisplayName("division=0 -> 400(参数不合法)")
    void queryCorporationPage_divisionZero_badRequest() throws Exception {
        loginAs(CURRENT_USER_ID, "ADMIN");

        mockMvc.perform(get("/wallet/journal/corp/" + CORP_ID)
                        .param("division", "0").param("current", "1").param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("division=8 -> 400(参数不合法)")
    void queryCorporationPage_divisionEight_badRequest() throws Exception {
        loginAs(CURRENT_USER_ID, "ADMIN");

        mockMvc.perform(get("/wallet/journal/corp/" + CORP_ID)
                        .param("division", "8").param("current", "1").param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
