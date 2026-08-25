package xyz.foolcat.eve.evehelper.interfaces.web.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.model.vo.WalletDivisionBalance;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.domain.repository.system.EveAccountRepository;
import xyz.foolcat.eve.evehelper.infrastructure.config.security.RbacAuthorizationManager;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 用户钱包余额聚合端点集成测试（018）。
 *
 * <p>MockMvc 走完整过滤器链 + 控制器 + 应用服务。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DisplayName("用户钱包余额聚合端点集成测试")
class WalletBalanceControllerIT {

    private static final int USER_ID = 777;
    private static final int CID_1 = 2112832425;
    private static final int CORP_A = 98454654;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    RbacAuthorizationManager rbacAuthorizationManager;

    @MockitoBean
    EsiGateway esiGateway;

    @MockitoBean
    EveAccountRepository eveAccountRepository;

    @BeforeEach
    void setUp() {
        // RBAC 默认放行
        lenient().when(rbacAuthorizationManager.check(any(), any()))
                .thenReturn(new org.springframework.security.authorization.AuthorizationDecision(true));
    }

    @AfterEach
    void tearDown() {
        // 清理安全上下文，避免测试间状态污染
        SecurityContextHolder.clearContext();
    }

    private void mockAuthenticated() {
        var auth = new UsernamePasswordAuthenticationToken(
                USER_ID, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("未认证 → 401")
    void unauthenticated_returns401() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/wallet/balance"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("正常查询 → 返回正确结构")
    void normalQuery_returnsCorrectStructure() throws Exception {
        mockAuthenticated();

        EveAccount account = new EveAccount();
        account.setCharacterId(CID_1);
        account.setCharacterName("TestChar");
        account.setCorpId(CORP_A);
        account.setCorpName("TestCorp");
        account.setUserId(USER_ID);

        when(eveAccountRepository.getAccountList(USER_ID)).thenReturn(List.of(account));

        // stub ESI
        lenient().when(esiGateway.getAccessToken(eq(CID_1), anyInt())).thenReturn("Bearer token");
        when(esiGateway.queryCharacterWalletBalance(eq(CID_1), any())).thenReturn(1000000.0);
        when(esiGateway.queryCorporationWalletBalances(eq(CORP_A), any()))
                .thenReturn(List.of(new WalletDivisionBalance(1, 500000.0)));

        mockMvc.perform(get("/wallet/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000000"))
                .andExpect(jsonPath("$.data.characters").isArray())
                .andExpect(jsonPath("$.data.characters[0].characterId").value(CID_1))
                .andExpect(jsonPath("$.data.characters[0].characterName").value("TestChar"))
                .andExpect(jsonPath("$.data.characters[0].balance").value(1000000.0))
                .andExpect(jsonPath("$.data.corporations").isArray())
                .andExpect(jsonPath("$.data.corporations[0].corpId").value(CORP_A))
                .andExpect(jsonPath("$.data.corporations[0].corpName").value("TestCorp"))
                .andExpect(jsonPath("$.data.corporations[0].totalBalance").value(500000.0));
    }

    @Test
    @DisplayName("无绑定人物 → 返回空结构")
    void noAccounts_returnsEmptyStructure() throws Exception {
        mockAuthenticated();
        when(eveAccountRepository.getAccountList(USER_ID)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/wallet/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000000"))
                .andExpect(jsonPath("$.data.characters").isEmpty())
                .andExpect(jsonPath("$.data.corporations").isEmpty());
    }
}
