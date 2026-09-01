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
import xyz.foolcat.eve.evehelper.domain.model.vo.EsiAuthorizationDetail;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.domain.repository.system.EveAccountRepository;
import xyz.foolcat.eve.evehelper.infrastructure.config.security.RbacAuthorizationManager;
import xyz.foolcat.eve.evehelper.shared.kernel.enums.EsiAuthStatus;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 用户角色列表(含 ESI 授权状态)集成测试(003)。
 *
 * <p>MockMvc 走完整过滤器链 + 控制器 + 应用服务。
 * 外部依赖(EsiGateway/EveAccountRepository)通过 MockitoBean mock,
 * 避免依赖真实 ESI 服务和测试库数据。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DisplayName("用户角色列表(含 ESI 授权状态)集成测试")
class UserControllerAuthStatusIT {

    private static final int USER_ID = 888;
    private static final int CID_1 = 1000001;
    private static final int CID_2 = 1000002;
    private static final int CID_3 = 1000003;
    private static final int CID_4 = 1000004;
    private static final int CORP_ID = 98000001;

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
        SecurityContextHolder.clearContext();
    }

    private void mockAuthenticated() {
        var auth = new UsernamePasswordAuthenticationToken(
                USER_ID, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private EveAccount buildAccount(int characterId, String characterName) {
        EveAccount account = new EveAccount();
        account.setCharacterId(characterId);
        account.setCharacterName(characterName);
        account.setCorpId(CORP_ID);
        account.setUserId(USER_ID);
        return account;
    }

    // ── T013: 用户无绑定角色 → data=[] 且成功 ──

    @Test
    @DisplayName("T013: 用户无绑定角色 → data=[] 且 200 成功")
    void noBoundRoles_returnsEmptyDataList() throws Exception {
        mockAuthenticated();
        when(eveAccountRepository.getAccountList(USER_ID)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/user/{userId}", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    // ── T014: 多角色混合状态端到端验证(021 扩展:AUTHORIZED 携带 scopes,降级三态 scopes=null) ──

    @Test
    @DisplayName("T014: 多角色混合状态(AUTHORIZED/EXPIRED/NOT_AUTHORIZED/UNKNOWN)端到端验证,AUTHORIZED 角色 scopes 数组可见")
    void multipleRoles_mixedAuthStatuses() throws Exception {
        mockAuthenticated();

        EveAccount account1 = buildAccount(CID_1, "Char-Authorized");
        EveAccount account2 = buildAccount(CID_2, "Char-Expired");
        EveAccount account3 = buildAccount(CID_3, "Char-NotAuthorized");
        EveAccount account4 = buildAccount(CID_4, "Char-Unknown");

        when(eveAccountRepository.getAccountList(USER_ID))
                .thenReturn(List.of(account1, account2, account3, account4));

        // 每个角色返回不同的授权详情(021:状态 + scopes)
        when(esiGateway.getAuthorizationDetail(account1)).thenReturn(new EsiAuthorizationDetail(
                EsiAuthStatus.AUTHORIZED, List.of("esi-assets.read_assets.v1", "esi-wallet.read_character_wallet.v1")));
        when(esiGateway.getAuthorizationDetail(account2)).thenReturn(new EsiAuthorizationDetail(EsiAuthStatus.EXPIRED, null));
        when(esiGateway.getAuthorizationDetail(account3)).thenReturn(new EsiAuthorizationDetail(EsiAuthStatus.NOT_AUTHORIZED, null));
        when(esiGateway.getAuthorizationDetail(account4)).thenReturn(new EsiAuthorizationDetail(EsiAuthStatus.UNKNOWN, null));

        mockMvc.perform(get("/user/{userId}", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(4))
                // 逐角色断言 authStatus
                .andExpect(jsonPath("$.data[0].characterId").value(CID_1))
                .andExpect(jsonPath("$.data[0].authStatus").value("AUTHORIZED"))
                .andExpect(jsonPath("$.data[1].characterId").value(CID_2))
                .andExpect(jsonPath("$.data[1].authStatus").value("EXPIRED"))
                .andExpect(jsonPath("$.data[2].characterId").value(CID_3))
                .andExpect(jsonPath("$.data[2].authStatus").value("NOT_AUTHORIZED"))
                .andExpect(jsonPath("$.data[3].characterId").value(CID_4))
                .andExpect(jsonPath("$.data[3].authStatus").value("UNKNOWN"))
                // AUTHORIZED 角色 scopes 端到端可见(顺序与 stub 一致)
                .andExpect(jsonPath("$.data[0].scopes").isArray())
                .andExpect(jsonPath("$.data[0].scopes.length()").value(2))
                .andExpect(jsonPath("$.data[0].scopes[0]").value("esi-assets.read_assets.v1"))
                .andExpect(jsonPath("$.data[0].scopes[1]").value("esi-wallet.read_character_wallet.v1"))
                // 非 AUTHORIZED 三态 scopes=null(UserAccountDTO 无 @JsonInclude,Jackson 默认输出 "scopes":null)
                .andExpect(jsonPath("$.data[1].scopes").value(nullValue()))
                .andExpect(jsonPath("$.data[2].scopes").value(nullValue()))
                .andExpect(jsonPath("$.data[3].scopes").value(nullValue()));
    }

    // ── 021 FR-005: 单角色 detail 判定异常被隔离为 UNKNOWN,不影响其他角色 ──

    @Test
    @DisplayName("FR-005: 单角色 detail 判定抛异常 → 该角色降级为 UNKNOWN 且 scopes=null,其他角色不受影响")
    void detailFailure_isolatedAsUnknown() throws Exception {
        mockAuthenticated();

        EveAccount account1 = buildAccount(CID_1, "Char-Boom");
        EveAccount account2 = buildAccount(CID_2, "Char-Ok");

        when(eveAccountRepository.getAccountList(USER_ID)).thenReturn(List.of(account1, account2));

        when(esiGateway.getAuthorizationDetail(account1)).thenThrow(new RuntimeException("detail failed"));
        when(esiGateway.getAuthorizationDetail(account2)).thenReturn(new EsiAuthorizationDetail(
                EsiAuthStatus.AUTHORIZED, List.of("esi-assets.read_assets.v1")));

        mockMvc.perform(get("/user/{userId}", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.length()").value(2))
                // 角色 1:future 异常经 resolveDetail 兜底为 UNKNOWN + scopes null
                .andExpect(jsonPath("$.data[0].characterId").value(CID_1))
                .andExpect(jsonPath("$.data[0].authStatus").value("UNKNOWN"))
                .andExpect(jsonPath("$.data[0].scopes").value(nullValue()))
                // 角色 2:正常判定不受影响
                .andExpect(jsonPath("$.data[1].characterId").value(CID_2))
                .andExpect(jsonPath("$.data[1].authStatus").value("AUTHORIZED"))
                .andExpect(jsonPath("$.data[1].scopes[0]").value("esi-assets.read_assets.v1"));
    }
}
