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
import xyz.foolcat.eve.evehelper.domain.model.entity.system.WalletTransaction;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.domain.service.security.ResourceOwnershipPolicy;
import xyz.foolcat.eve.evehelper.domain.service.system.EveAccountService;
import xyz.foolcat.eve.evehelper.infrastructure.config.security.RbacAuthorizationManager;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 人物钱包交易端点集成测试(US1)。
 *
 * <p>MockMvc 走完整过滤器链 + 控制器 + 应用服务 + 领域服务 + 真实仓储/mapper
 * (落测试 MySQL 的 wallet_transaction 表)。</p>
 * <ul>
 *   <li>RBAC(RbacAuthorizationManager)mock 以自动规避权限映射对 Redis 的依赖,默认放行(仿 AssetsAggregateIT)。</li>
 *   <li>ESI(EsiGateway)mock 以在同步用例中注入受控交易,不触发真实 ESI 网络调用。</li>
 *   <li>AccessGuard 保持真实;仅 mock 其唯一 DB 依赖 ResourceOwnershipPolicy,以确定性驱动越权(403)路径。</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DisplayName("人物钱包交易端点集成测试")
class WalletTransactionControllerIT {

    private static final int CURRENT_USER_ID = 7;
    private static final int CID = 2112832425;
    private static final int FOREIGN_CID = 999999;

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
        // 清理该人物在测试库中的交易,保证分页/幂等断言确定性
        jdbcTemplate.update("delete from wallet_transaction where owner_id = ?", CID);
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

    private static WalletTransaction tx(long transactionId, OffsetDateTime date,
                                        Integer typeId, Integer quantity,
                                        Double unitPrice, Boolean isBuy) {
        WalletTransaction t = new WalletTransaction();
        t.setTransactionId(transactionId);
        t.setDate(date);
        t.setTypeId(typeId);
        t.setQuantity(quantity);
        t.setUnitPrice(unitPrice);
        t.setIsBuy(isBuy);
        return t;
    }

    private void stubSyncEsi(List<WalletTransaction> page) {
        EveAccount acc = new EveAccount();
        acc.setCharacterId(CID);
        acc.setUserId(CURRENT_USER_ID);
        when(eveAccountService.getAccountOne(CURRENT_USER_ID, CID)).thenReturn(acc);
        try {
            when(esiGateway.getAccessToken(CID, CURRENT_USER_ID)).thenReturn("tk");
        } catch (java.text.ParseException e) {
            throw new IllegalStateException(e);
        }
        // from_id 游标:首页(null)给一页,翻页(非 null)给空页终止
        when(esiGateway.queryCharacterWalletTransactions(eq(CID), any(), eq("tk")))
                .thenAnswer(inv -> inv.getArgument(1) == null ? Flux.fromIterable(page) : Flux.empty());
    }

    // ---------- T008 越权拒绝 ----------

    @Test
    @DisplayName("未认证访问分页 -> 401(拒绝)")
    void queryPage_unauthenticated_forbidden() throws Exception {
        SecurityContextHolder.clearContext();
        // 未认证:RBAC 拒绝 -> 认证入口点直写 401
        when(rbacAuthorizationManager.authorize(any(), any()))
                .thenReturn(new AuthorizationDecision(false));
        when(rbacAuthorizationManager.check(any(), any()))
                .thenReturn(new AuthorizationDecision(false));

        mockMvc.perform(get("/wallet/transaction/" + CID).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("已认证但无权查询他人交易 -> 403")
    void queryPage_authenticatedUnauthorized_denied() throws Exception {
        // 非 ROOT 主体,resourceOwnershipPolicy mock 默认返回 false -> AccessGuard 抛 ACCESS_UNAUTHORIZED
        loginAs(CURRENT_USER_ID, "USER");

        mockMvc.perform(get("/wallet/transaction/" + FOREIGN_CID).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("已认证但无权同步他人交易 -> 403")
    void sync_authenticatedUnauthorized_denied() throws Exception {
        loginAs(CURRENT_USER_ID, "USER");

        mockMvc.perform(post("/wallet/transaction/" + FOREIGN_CID + "/sync").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("分页参数越界(size<=0) -> 400 拒绝")
    void queryPage_invalidPaginationParams_rejected() throws Exception {
        loginAs(CURRENT_USER_ID, "ADMIN");

        mockMvc.perform(get("/wallet/transaction/" + CID)
                        .param("current", "1").param("size", "-1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ---------- T009 同步 + 分页 ----------

    @Test
    @DisplayName("同步交易 -> 分页倒序返回完整 VO 字段")
    void sync_thenQueryPage_returnsDescOrderFields() throws Exception {
        loginAs(CURRENT_USER_ID, "ADMIN");
        WalletTransaction newest = tx(3001L,
                OffsetDateTime.of(2026, 1, 2, 10, 0, 0, 0, ZoneOffset.UTC), 34, 5, 1234.5, true);
        WalletTransaction older = tx(2001L,
                OffsetDateTime.of(2026, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC), 303, 2, 900.0, false);
        stubSyncEsi(List.of(newest, older));

        mockMvc.perform(post("/wallet/transaction/" + CID + "/sync").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(get("/wallet/transaction/" + CID)
                        .param("current", "1").param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records.length()").value(2))
                // date 倒序:较新的在前
                .andExpect(jsonPath("$.data.records[0].transactionId").value(3001))
                .andExpect(jsonPath("$.data.records[0].date").exists())
                .andExpect(jsonPath("$.data.records[0].typeId").value(34))
                .andExpect(jsonPath("$.data.records[0].quantity").value(5))
                .andExpect(jsonPath("$.data.records[0].unitPrice").value(1234.5))
                .andExpect(jsonPath("$.data.records[0].isBuy").value(true))
                .andExpect(jsonPath("$.data.records[1].transactionId").value(2001))
                .andExpect(jsonPath("$.data.records[1].typeId").value(303))
                .andExpect(jsonPath("$.data.records[1].quantity").value(2))
                .andExpect(jsonPath("$.data.records[1].isBuy").value(false));
    }

    // ---------- T010 幂等 + 失败 ----------

    @Test
    @DisplayName("重复同步同一交易 -> 不产生重复行(幂等 upsert by 复合唯一键)")
    void syncTwice_idempotent_noDuplicateRows() throws Exception {
        loginAs(CURRENT_USER_ID, "ADMIN");
        WalletTransaction a = tx(4001L,
                OffsetDateTime.of(2026, 2, 1, 8, 0, 0, 0, ZoneOffset.UTC), 34, 1, 10.0, true);
        stubSyncEsi(List.of(a));

        mockMvc.perform(post("/wallet/transaction/" + CID + "/sync")).andExpect(status().isOk());
        mockMvc.perform(post("/wallet/transaction/" + CID + "/sync")).andExpect(status().isOk());

        // transaction_id 相同 => on duplicate key update 覆盖而非新增,始终仅 1 行
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from wallet_transaction where owner_id = ?", Integer.class, CID);
        org.assertj.core.api.Assertions.assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("同步时 ESI 失败 -> 不写脏数据,返回 500")
    void syncEsiFailure_noPartialData() throws Exception {
        loginAs(CURRENT_USER_ID, "ADMIN");
        EveAccount acc = new EveAccount();
        acc.setCharacterId(CID);
        acc.setUserId(CURRENT_USER_ID);
        when(eveAccountService.getAccountOne(CURRENT_USER_ID, CID)).thenReturn(acc);
        try {
            when(esiGateway.getAccessToken(CID, CURRENT_USER_ID)).thenReturn("tk");
        } catch (java.text.ParseException e) {
            throw new IllegalStateException(e);
        }
        when(esiGateway.queryCharacterWalletTransactions(eq(CID), any(), eq("tk")))
                .thenReturn(Flux.error(new RuntimeException("esi down")));

        mockMvc.perform(post("/wallet/transaction/" + CID + "/sync").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is5xxServerError());

        // block() 抛错 -> saveOrUpdateBatch 未执行 -> DB 无新增
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from wallet_transaction where owner_id = ?", Integer.class, CID);
        org.assertj.core.api.Assertions.assertThat(count).isZero();
    }
}