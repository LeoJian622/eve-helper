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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Flux;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.WalletJournal;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.domain.service.security.ResourceOwnershipPolicy;
import xyz.foolcat.eve.evehelper.domain.service.system.EveAccountService;
import xyz.foolcat.eve.evehelper.infrastructure.config.security.RbacAuthorizationManager;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.EsiException;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.ResultCode;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 军团钱包流水同步端到端验收(013 US1 / T021)。
 *
 * <p>MockMvc 走完整过滤器链 + 控制器({@code /wallet/journal/corp/{characterId}/sync}) +
 * 应用服务 + 领域服务 + 真实仓储/mapper(落测试 MySQL 的 wallet_journal 表,按 owner_id=军团ID 隔离)。</p>
 *
 * <p><b>013 US1 核心断言</b>:入参为<b>角色ID(characterId)</b>,ESI 调用必须使用从该角色 eve_account
 * 解析出的<b>军团ID(corpId)</b>——即 ESI 收到 {@code eq(corpId)} 而非角色ID(消除双重语义)。</p>
 *
 * <p>与既有人物钱包流水 IT({@code WalletJournalControllerIT})的命名映射:013 T021 命名
 * {@code WalletJournalControllerIT},但该类名已被<em>人物</em>侧流水测试占用,故军团侧端到端验收
 * 独立落本类(军团同步走同一 controller 的 <code>/corp/</code> 分支)。</p>
 *
 * <p>驱动方式:<ul>
 *   <li>RBAC mock 放行,规避权限映射对 Redis 的依赖。</li>
 *   <li>ADMIN 登录 = ROOT(豁免 requireOwnership 归属校验),直达授权/ESI;USER 登录走归属校验
 *       (ResourceOwnershipPolicy mock 默认 false)驱动越权 403 路径。</li>
 *   <li>EsiGateway 全 mock:注入受控 maxPage/流水,不触发真实 ESI 网络。</li>
 * </ul></p>
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DisplayName("军团钱包流水同步-角色ID端到端(013 US1)")
class WalletJournalCorpSyncIT {

    private static final int USER_ID = 7;
    private static final int CHARACTER_ID = 10001;
    private static final int CORP_ID = 1000001;
    private static final int FOREIGN_CHARACTER_ID = 888999;

    private static final int DIVISION_1 = 1;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @MockBean
    RbacAuthorizationManager rbacAuthorizationManager;

    @MockBean
    ResourceOwnershipPolicy resourceOwnershipPolicy;

    @MockBean
    EveAccountService eveAccountService;

    @MockBean
    EsiGateway esiGateway;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from wallet_journal where owner_id = ?", CORP_ID);
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

    private static WalletJournal journal() {
        WalletJournal j = new WalletJournal();
        j.setId(123L);
        j.setAmount(1234.56);
        j.setBalance(1234.56);
        j.setDate(OffsetDateTime.of(2026, 1, 2, 10, 0, 0, 0, ZoneOffset.UTC));
        j.setRefType("bounty_prizes");
        j.setDescription("ops");
        j.setTax(0.0);
        return j;
    }

    /** stub 授权解析:角色ID→含 corpId 的 eve_account + 固定 token。 */
    private void stubAccountAndToken() throws Exception {
        EveAccount acc = new EveAccount();
        acc.setCharacterId(CHARACTER_ID);
        acc.setUserId(USER_ID);
        acc.setCorpId(CORP_ID);
        when(eveAccountService.getAccountOne(USER_ID, CHARACTER_ID)).thenReturn(acc);
        when(esiGateway.getAccessToken(CHARACTER_ID, USER_ID)).thenReturn("tk");
    }

    // ---------- US1 成功:角色ID → 解析 corpId → ESI 用 corpId ----------

    @Test
    @DisplayName("传角色ID → ESI 用解析出的军团ID拉取并落库;分页查询该军团返回数据")
    void sync_characterId_resolvesCorpId_persists_andVerifiesCorpIdUsage() throws Exception {
        loginAs(USER_ID, "ADMIN"); // ROOT 豁免归属校验,直达授权+ESI
        stubAccountAndToken();
        // maxPage:div1 有 1 页,div2..7 无数据(0 页)
        when(esiGateway.queryCorporationWalletJournalMaxPage(eq(CORP_ID), anyInt(), eq("tk")))
                .thenAnswer(inv -> ((Integer) inv.getArgument(1)).intValue() == DIVISION_1 ? 1 : 0);
        // query:div1 page1 返回一条,其余空页
        when(esiGateway.queryCorporationWalletJournal(eq(CORP_ID), anyInt(), anyInt(), eq("tk")))
                .thenAnswer(inv -> {
                    int div = ((Integer) inv.getArgument(1)).intValue();
                    int page = ((Integer) inv.getArgument(2)).intValue();
                    return (div == DIVISION_1 && page == 1) ? Flux.just(journal()) : Flux.empty();
                });

        mockMvc.perform(post("/wallet/journal/corp/" + CHARACTER_ID + "/sync")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // 核心 US1:ESI 调用使用解析 corpId 而非入参角色ID(7 个 division 全部用 corpId)
        verify(esiGateway, times(7))
                .queryCorporationWalletJournalMaxPage(eq(CORP_ID), anyInt(), eq("tk"));
        verify(esiGateway, times(1))
                .queryCorporationWalletJournal(eq(CORP_ID), eq(1), eq(1), eq("tk"));

        // 落库 + 分页查询该军团 div1 返回数据(ownerId=corpId, division=1)
        mockMvc.perform(get("/wallet/journal/corp/" + CORP_ID)
                        .param("division", "1").param("current", "1").param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(123))
                .andExpect(jsonPath("$.data.records[0].ownerId").value(CORP_ID))
                .andExpect(jsonPath("$.data.records[0].division").value(1));
    }

    // ---------- US1 越权:非关联角色 → 拒绝且不触发 ESI ----------

    @Test
    @DisplayName("传非关联角色ID → 403 拒绝,且不触发 ESI 军团调用")
    void sync_unrelatedCharacter_forbidden_noEsi() throws Exception {
        loginAs(USER_ID, "USER"); // 非 ROOT,requireOwnership 走 resourceOwnershipPolicy(mock 默认 false)
        mockMvc.perform(post("/wallet/journal/corp/" + FOREIGN_CHARACTER_ID + "/sync")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        // 归属校验失败在授权/ESI 之前 → 该非关联角色的账户不得因本次请求被解析,更不得触发 ESI。
        // 注意:精确匹配本用例 FOREIGN_CHARACTER_ID,避免被共享 context 中后台系统任务
        // (authorizeInternal(1, 其它真实角色))用 anyInt() 匹配而污染。
        verify(eveAccountService, never()).getAccountOne(eq(USER_ID), eq(FOREIGN_CHARACTER_ID));
        verify(esiGateway, never()).queryCorporationWalletJournalMaxPage(eq(FOREIGN_CHARACTER_ID), anyInt(), any());
        verify(esiGateway, never()).queryCorporationWalletJournal(eq(FOREIGN_CHARACTER_ID), anyInt(), anyInt(), any());
    }

    // ---------- US3:ESI 403 → 精确溢出为 HTTP 403 + 专属码(FR-005) ----------

    @Test
    @DisplayName("ESI 返回 403 → 前端收 HTTP 403 + ESI00403 + 友好文案(区别于 ACCESS_UNAUTHORIZED)")
    void sync_esi403_forbidden_friendlyMessage() throws Exception {
        loginAs(USER_ID, "ADMIN");
        stubAccountAndToken();
        // 模拟 ESI 防腐层已把 HTTP 403 转成 EsiException(ESI_AUTH_PERMISSION_LOW)
        when(esiGateway.queryCorporationWalletJournalMaxPage(anyInt(), anyInt(), any()))
                .thenThrow(new EsiException(ResultCode.ESI_AUTH_PERMISSION_LOW));

        mockMvc.perform(post("/wallet/journal/corp/" + CHARACTER_ID + "/sync")
                        .contentType(MediaType.APPLICATION_JSON))
                // FR-005:数据权限 403 MUST 映射 HTTP 403 + 专属码,不被折叠为笼统 400/5xx
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ESI00403"))
                .andExpect(jsonPath("$.msg", containsString("Director")));
    }

    // ---------- US3:5xx → 异于 403 的错误(SC-006 可区分) ----------

    @Test
    @DisplayName("ESI 5xx → 返回与 403 不同的错误,不与权限问题混淆")
    void sync_esi5xx_distinctFrom403() throws Exception {
        loginAs(USER_ID, "ADMIN");
        stubAccountAndToken();
        // 模拟 ESI 服务故障:5xx → EsiException(ESI_SERVER_FAILURE)
        when(esiGateway.queryCorporationWalletJournalMaxPage(anyInt(), anyInt(), any()))
                .thenThrow(new EsiException(ResultCode.ESI_SERVER_FAILURE));

        mockMvc.perform(post("/wallet/journal/corp/" + CHARACTER_ID + "/sync")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest()) // 非 403,异于权限混淆
                .andExpect(jsonPath("$.code").value("ESI00500"));
    }
}