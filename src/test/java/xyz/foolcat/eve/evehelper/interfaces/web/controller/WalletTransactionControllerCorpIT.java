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
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.EsiException;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.ResultCode;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 军团钱包交易端点集成测试(US2)。
 *
 * <p>MockMvc 走完整过滤器链 + 控制器 + 应用服务 + 领域服务 + 真实仓储/mapper
 * (落测试 MySQL 的 wallet_transaction 表,按 owner_id=军团ID 隔离)。</p>
 * <ul>
 *   <li>RBAC(RbacAuthorizationManager)mock 以自动规避权限映射对 Redis 的依赖,默认放行。</li>
 *   <li>ESI(EsiGateway)mock:queryCorporationWalletTransactions 按 division 注入受控交易;
 *       from_id 游标首页(null)给数据、翻页(非 null)给空页终止,不触发真实 ESI 网络调用。</li>
 *   <li>AccessGuard 保持真实;仅 mock 其唯一 DB 依赖 ResourceOwnershipPolicy,以确定性驱动越权(403)路径。</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DisplayName("军团钱包交易端点集成测试")
class WalletTransactionControllerCorpIT {

    private static final int CURRENT_USER_ID = 7;
    private static final int CORP_ID = 1000001;
    private static final int FOREIGN_CORP_ID = 999998;

    /** 军团钱包分账区间 1..7。 */
    private static final int MIN_DIVISION = 1;
    private static final int MAX_DIVISION = 7;

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
        // 清理该军团在测试库中的交易,保证分页/幂等断言确定性
        jdbcTemplate.update("delete from wallet_transaction where owner_id = ?", CORP_ID);
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

    /**
     * 装配军团同步:mock getAccountOne/getAccessToken,并按 division 注入各分账交易。
     *
     * @param succeeded division -> 该分账首页交易(未列出 division 按"无交易=空页"成功处理)
     * @param failed    需要模拟 ESI 抛错的分账集合(success/empty 之外的增长扩展值)
     */
    private void stubCorpSync(Integer corpId, Map<Integer, List<WalletTransaction>> succeeded,
                              Set<Integer> failed) {
        EveAccount acc = new EveAccount();
        acc.setCharacterId(corpId);
        acc.setCorpId(corpId); // 013 US4:解析 corpId 须非空,否则服务在 ESI 前抛 403(validate stub 语义)
        acc.setUserId(CURRENT_USER_ID);
        when(eveAccountService.getAccountOne(CURRENT_USER_ID, corpId)).thenReturn(acc);
        try {
            when(esiGateway.getAccessToken(corpId, CURRENT_USER_ID)).thenReturn("tk");
        } catch (java.text.ParseException e) {
            throw new IllegalStateException(e);
        }
        when(esiGateway.queryCorporationWalletTransactions(eq(corpId), anyInt(), any(), eq("tk")))
                .thenAnswer(inv -> {
                    Integer division = inv.getArgument(1);
                    Long fromId = inv.getArgument(2);
                    if (failed.contains(division)) {
                        return Flux.error(new RuntimeException("esi division " + division + " down"));
                    }
                    List<WalletTransaction> page = succeeded.getOrDefault(division, List.of());
                    // from_id 游标:首页(null)给该分账数据,翻页(非 null)给空页终止
                    return fromId == null ? Flux.fromIterable(page) : Flux.empty();
                });
    }

    /**
     * stub 军团同步,使 ESI 军团交易查询直接抛指定 ESI 异常(013 US3:验证 403/5xx 透传,
     * 不被 division 级失败隔离折叠成笼统 400)。
     */
    @SuppressWarnings("unchecked")
    private void stubCorpSyncEsiError(Integer corpId, EsiException esiException) {
        EveAccount acc = new EveAccount();
        acc.setCharacterId(corpId);
        acc.setCorpId(corpId);
        acc.setUserId(CURRENT_USER_ID);
        when(eveAccountService.getAccountOne(CURRENT_USER_ID, corpId)).thenReturn(acc);
        try {
            when(esiGateway.getAccessToken(corpId, CURRENT_USER_ID)).thenReturn("tk");
        } catch (java.text.ParseException e) {
            throw new IllegalStateException(e);
        }
        // 任一分账首次翻页(from_id=null)即抛 ESI 异常
        when(esiGateway.queryCorporationWalletTransactions(eq(corpId), anyInt(), isNull(), eq("tk")))
                .thenThrow(esiException);
    }

    private Integer countByDivision(Integer corpId, Integer division) {
        return jdbcTemplate.queryForObject(
                "select count(*) from wallet_transaction where owner_id = ? and division = ?",
                Integer.class, corpId, division);
    }

    // ---------- T018 越权拒绝 ----------

    @Test
    @DisplayName("未认证访问军团分页 -> 401(拒绝)")
    void queryCorporationPage_unauthenticated_forbidden() throws Exception {
        SecurityContextHolder.clearContext();
        when(rbacAuthorizationManager.authorize(any(), any()))
                .thenReturn(new AuthorizationDecision(false));
        when(rbacAuthorizationManager.check(any(), any()))
                .thenReturn(new AuthorizationDecision(false));

        mockMvc.perform(get("/wallet/transaction/corp/" + CORP_ID)
                        .param("division", "2").param("current", "1").param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("已认证但无权查询他人军团 -> 403")
    void queryCorporationPage_authenticatedUnauthorized_denied() throws Exception {
        loginAs(CURRENT_USER_ID, "USER"); // resourceOwnershipPolicy mock 默认 false -> ACCESS_UNAUTHORIZED

        mockMvc.perform(get("/wallet/transaction/corp/" + FOREIGN_CORP_ID)
                        .param("division", "1").param("current", "1").param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("已认证但无权同步他人军团 -> 403")
    void syncCorporation_authenticatedUnauthorized_denied() throws Exception {
        loginAs(CURRENT_USER_ID, "USER");

        mockMvc.perform(post("/wallet/transaction/corp/" + FOREIGN_CORP_ID + "/sync")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    // ---------- T016 全 division 同步 + 分页 ----------

    @Test
    @DisplayName("全 division 同步 -> 按 division 分页倒序返回;空分账返回空页")
    void syncAllDivisions_thenQueryByDivision() throws Exception {
        loginAs(CURRENT_USER_ID, "ADMIN");
        WalletTransaction div2Newest = tx(7101L,
                OffsetDateTime.of(2026, 3, 2, 10, 0, 0, 0, ZoneOffset.UTC), 34, 5, 1234.5, true);
        WalletTransaction div2Older = tx(7102L,
                OffsetDateTime.of(2026, 3, 1, 10, 0, 0, 0, ZoneOffset.UTC), 303, 2, 900.0, false);
        stubCorpSync(CORP_ID, Map.of(2, List.of(div2Newest, div2Older)), Set.of());

        // 同步 1..7 全部分账,全部成功(div 2 有数据,其余无交易视为成功)
        mockMvc.perform(post("/wallet/transaction/corp/" + CORP_ID + "/sync")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data['2']").value(true));

        // division=2 有数据,date 倒序返回完整 VO 字段
        mockMvc.perform(get("/wallet/transaction/corp/" + CORP_ID)
                        .param("division", "2").param("current", "1").param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records.length()").value(2))
                .andExpect(jsonPath("$.data.records[0].transactionId").value(7101))
                .andExpect(jsonPath("$.data.records[0].date").exists())
                .andExpect(jsonPath("$.data.records[0].typeId").value(34))
                .andExpect(jsonPath("$.data.records[0].quantity").value(5))
                .andExpect(jsonPath("$.data.records[0].unitPrice").value(1234.5))
                .andExpect(jsonPath("$.data.records[0].isBuy").value(true))
                .andExpect(jsonPath("$.data.records[0].ownerId").value(CORP_ID))
                .andExpect(jsonPath("$.data.records[0].division").value(2))
                .andExpect(jsonPath("$.data.records[1].transactionId").value(7102));

        // division=5 无交易 -> 空页而非报错
        mockMvc.perform(get("/wallet/transaction/corp/" + CORP_ID)
                        .param("division", "5").param("current", "1").param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.records.length()").value(0));
    }

    // ---------- T017 单 division 失败隔离 ----------

    @Test
    @DisplayName("单 division ESI 失败 -> 已成功 division 数据保留不整批回滚;错误响应含失败 division")
    void sync_corporationDivisionFailure_isolation() throws Exception {
        loginAs(CURRENT_USER_ID, "ADMIN");
        WalletTransaction div2 = tx(7201L,
                OffsetDateTime.of(2026, 4, 1, 9, 0, 0, 0, ZoneOffset.UTC), 34, 1, 10.0, true);
        // division 3 抛错,division 2 正常
        stubCorpSync(CORP_ID, Map.of(2, List.of(div2)), new HashSet<>(List.of(3)));

        mockMvc.perform(post("/wallet/transaction/corp/" + CORP_ID + "/sync")
                        .contentType(MediaType.APPLICATION_JSON))
                // 领域服务汇总抛 EveHelperException(null ResultCode) -> 400;错误消息含失败 division
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.msg", containsString("[3]")));

        // 已成功 division=2 的数据已落库且保留(失败隔离,不整体回滚)
        org.assertj.core.api.Assertions.assertThat(countByDivision(CORP_ID, 2)).isEqualTo(1);
        // 失败 division=3 无脏数据
        org.assertj.core.api.Assertions.assertThat(countByDivision(CORP_ID, 3)).isZero();
    }

    // ---------- T018 幂等 ----------

    @Test
    @DisplayName("重复同步同一军团分账 -> 不产生重复行(幂等 upsert by 复合唯一键)")
    void syncCorporationTwice_idempotent_noDuplicateRows() throws Exception {
        loginAs(CURRENT_USER_ID, "ADMIN");
        WalletTransaction div2 = tx(7301L,
                OffsetDateTime.of(2026, 5, 1, 8, 0, 0, 0, ZoneOffset.UTC), 34, 1, 10.0, true);
        stubCorpSync(CORP_ID, Map.of(2, List.of(div2)), Set.of());

        mockMvc.perform(post("/wallet/transaction/corp/" + CORP_ID + "/sync"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/wallet/transaction/corp/" + CORP_ID + "/sync"))
                .andExpect(status().isOk());

        // transaction_id 相同 => on duplicate key update 覆盖而非新增,division=2 始终仅 1 行
        org.assertj.core.api.Assertions.assertThat(countByDivision(CORP_ID, 2)).isEqualTo(1);
    }

    // ---------- 013 US3:ESI 数据接口 403 透传(FR-005) ----------

    @Test
    @DisplayName("ESI 军团交易查询返回 403 → 前端收 HTTP 403 + ESI00403 + 友好文案(非 ACCESS_UNAUTHORIZED 折叠)")
    void syncCorporation_esi403_forbidden_friendlyMessage() throws Exception {
        loginAs(CURRENT_USER_ID, "ADMIN");
        stubCorpSyncEsiError(CORP_ID, new EsiException(ResultCode.ESI_AUTH_PERMISSION_LOW));

        mockMvc.perform(post("/wallet/transaction/corp/" + CORP_ID + "/sync")
                        .contentType(MediaType.APPLICATION_JSON))
                // FR-005:数据权限 403 MUST 映射 HTTP 403 + 专属码,不被 division 隔离折叠成 400
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ESI00403"))
                .andExpect(jsonPath("$.msg", containsString("Director")));
    }

    @Test
    @DisplayName("ESI 军团交易查询 5xx → 返回异于 403 的错误(ESI00500),不与权限问题混淆")
    void syncCorporation_esi5xx_distinctFrom403() throws Exception {
        loginAs(CURRENT_USER_ID, "ADMIN");
        stubCorpSyncEsiError(CORP_ID, new EsiException(ResultCode.ESI_SERVER_FAILURE));

        mockMvc.perform(post("/wallet/transaction/corp/" + CORP_ID + "/sync")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest()) // 非 403,异于权限混淆(FR-006 可区分)
                .andExpect(jsonPath("$.code").value("ESI00500"));
    }
}