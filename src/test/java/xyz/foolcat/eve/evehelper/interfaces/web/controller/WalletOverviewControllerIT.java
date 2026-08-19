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
import xyz.foolcat.eve.evehelper.domain.service.security.ResourceOwnershipPolicy;
import xyz.foolcat.eve.evehelper.infrastructure.config.security.RbacAuthorizationManager;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 人物钱包总览端点集成测试(US1)。
 *
 * <p>MockMvc 走完整过滤器链 + 控制器 + 应用服务 + 真实仓储/mapper(落测试 MySQL 的 wallet_journal 表)。</p>
 * <ul>
 *   <li>RBAC(RbacAuthorizationManager)mock 默认放行(参照 AssetsAggregateIT)。</li>
 *   <li>登录为 ADMIN(ROOT)直接构造 wallet_journal 行,断言人物总览聚合/类目/趋势。</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DisplayName("人物钱包总览端点集成测试")
class WalletOverviewControllerIT {

    private static final int CID = 2112832425;
    private static final int CORP = 98454654;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @MockBean
    RbacAuthorizationManager rbacAuthorizationManager;

    @MockBean
    ResourceOwnershipPolicy resourceOwnershipPolicy;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from wallet_journal where owner_id = ?", CID);
        jdbcTemplate.update("delete from wallet_journal where owner_id = ?", CORP);
        when(rbacAuthorizationManager.authorize(any(), any()))
                .thenReturn(new AuthorizationDecision(true));
        when(rbacAuthorizationManager.check(any(), any()))
                .thenReturn(new AuthorizationDecision(true));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void loginAsAdmin() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        (long) 7, null, List.of(new SimpleGrantedAuthority("ADMIN"))));
    }

    /** 直接落库一行流水(id 自增决定 balance 的"最新"取 id 最大)。 */
    private void insertJournal(long id, double amount, double balance, LocalDate date, String refType) {
        jdbcTemplate.update(
                "insert into wallet_journal (id, amount, balance, `date`, ref_type, owner_id) values (?,?,?,?,?,?)",
                id, amount, balance, java.sql.Date.valueOf(date), refType, (long) CID);
    }

    /** 军团侧落库:额外带 division。 */
    private void insertCorpJournal(long id, int division, double amount, double balance, LocalDate date, String refType) {
        jdbcTemplate.update(
                "insert into wallet_journal (id, amount, balance, division, `date`, ref_type, owner_id) values (?,?,?,?,?,?,?)",
                id, amount, balance, division, java.sql.Date.valueOf(date), refType, (long) CORP);
    }

    // ---------- 五个标量 + 类目 + 趋势 ----------

    @Test
    @DisplayName("有流水 → 返回五个标量/类目/趋势,divisions 为 null")
    void overview_withJournals_returnsAggregateCategoriesTrend() throws Exception {
        loginAsAdmin();
        // id 倒序取 balance 为 currentBalance;amount 决定 income/expense/net
        insertJournal(101L, 1000.0, 1500.0, LocalDate.of(2026, 1, 2), "bounty_prizes");
        insertJournal(102L, -200.0, 1300.0, LocalDate.of(2026, 1, 5), "ess_escrow_transfer");
        insertJournal(103L, 500.0, 1800.0, LocalDate.of(2026, 1, 8), "bounty_prizes");
        // 再一条更早期,验证趋势按月桶(全量月粒度)与类目按 refType 聚合
        insertJournal(104L, 300.0, 2100.0, LocalDate.of(2025, 12, 20), "market_sale");

        mockMvc.perform(get("/wallet/overview/" + CID).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                // 标量:income=1800, expense=200(取 -amount), net=1600, count=4, currentBalance=最新id=104的balance=2100
                .andExpect(jsonPath("$.data.totalIncome").value(1800.0))
                .andExpect(jsonPath("$.data.totalExpense").value(200.0))
                .andExpect(jsonPath("$.data.netFlow").value(1600.0))
                .andExpect(jsonPath("$.data.journalCount").value(4))
                .andExpect(jsonPath("$.data.currentBalance").value(2100.0))
                .andExpect(jsonPath("$.data.asOfTime").isNotEmpty())
                .andExpect(jsonPath("$.data.divisions").doesNotExist())
                // 类目:按 (income+expense) 降序,bounty_prizes(1500) 在前
                .andExpect(jsonPath("$.data.categories[0].refType").value("bounty_prizes"))
                .andExpect(jsonPath("$.data.categories[0].income").value(1500.0))
                .andExpect(jsonPath("$.data.categories[0].expense").value(0.0))
                .andExpect(jsonPath("$.data.categories[0].count").value(2))
                .andExpect(jsonPath("$.data.categories[1].refType").value("market_sale"))
                .andExpect(jsonPath("$.data.categories[2].refType").value("ess_escrow_transfer"))
                // 趋势:月粒度(全量),按月升序
                .andExpect(jsonPath("$.data.trend[0].bucket").value("2025-12"))
                .andExpect(jsonPath("$.data.trend[0].income").value(300.0))
                .andExpect(jsonPath("$.data.trend[1].bucket").value("2026-01"))
                .andExpect(jsonPath("$.data.trend[1].net").value(1300.0));
    }

    @Test
    @DisplayName("有界≤92天区间 → 趋势日粒度")
    void overview_boundedRange_dailyTrend() throws Exception {
        loginAsAdmin();
        insertJournal(201L, 100.0, 100.0, LocalDate.of(2026, 8, 1), "market_sale");
        insertJournal(202L, -50.0, 50.0, LocalDate.of(2026, 8, 3), "market_sale");

        mockMvc.perform(get("/wallet/overview/" + CID)
                        .param("start", "2026-08-01").param("end", "2026-08-03")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.journalCount").value(2))
                .andExpect(jsonPath("$.data.trend[0].bucket").value("2026-08-01"))
                .andExpect(jsonPath("$.data.trend[1].bucket").value("2026-08-03"))
                // 时间过滤作用于收支/类目/趋势,不影响 currentBalance(取最新 id balance)
                .andExpect(jsonPath("$.data.currentBalance").value(50.0));
    }

    // ---------- 越权 / 非法入参 / 空数据 ----------

    @Test
    @DisplayName("无权访问他人总览 -> 403")
    void overview_foreignCharacter_forbidden() throws Exception {
        loginAsAdmin();
        // ADMIN 为 ROOT 豁免归属校验,改回 USER 并 mock 归属为 false 驱动越权
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        (long) 7, null, List.of(new SimpleGrantedAuthority("USER"))));
        when(resourceOwnershipPolicy.isOwnedBy(any(), any())).thenReturn(false);

        mockMvc.perform(get("/wallet/overview/999999").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("非法 range -> 400")
    void overview_invalidRange_badRequest() throws Exception {
        loginAsAdmin();

        mockMvc.perform(get("/wallet/overview/" + CID)
                        .param("range", "bogus")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("start > end -> 400")
    void overview_startAfterEnd_badRequest() throws Exception {
        loginAsAdmin();

        mockMvc.perform(get("/wallet/overview/" + CID)
                        .param("start", "2026-08-01").param("end", "2026-01-01")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("无流水(空数据) -> 零值")
    void overview_emptyData_zeroed() throws Exception {
        loginAsAdmin();

        mockMvc.perform(get("/wallet/overview/" + CID).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalIncome").value(0.0))
                .andExpect(jsonPath("$.data.totalExpense").value(0.0))
                .andExpect(jsonPath("$.data.netFlow").value(0.0))
                .andExpect(jsonPath("$.data.journalCount").value(0))
                .andExpect(jsonPath("$.data.currentBalance").value(0.0))
                .andExpect(jsonPath("$.data.categories").isArray())
                .andExpect(jsonPath("$.data.trend").isArray());
    }

    // ---------- 军团总览(US2) ----------

    @Test
    @DisplayName("军团全量 -> 分账分布 1..7(有数据归并/无数据补零),currentBalance=各分账余额和")
    void corpOverview_full_divisionDistribution() throws Exception {
        loginAsAdmin();
        // div1:最新 id302 balance=800;income=1000,expense=200
        insertCorpJournal(301L, 1, 1000.0, 1000.0, LocalDate.of(2026, 1, 2), "bounty_prizes");
        insertCorpJournal(302L, 1, -200.0, 800.0, LocalDate.of(2026, 1, 3), "ess_escrow_transfer");
        // div3 单条
        insertCorpJournal(303L, 3, 500.0, 500.0, LocalDate.of(2026, 1, 4), "market_sale");
        // div5 单条
        insertCorpJournal(304L, 5, 300.0, 300.0, LocalDate.of(2026, 1, 5), "market_sale");

        mockMvc.perform(get("/wallet/overview/corp/" + CORP).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                // 全量标量(无 division 过滤):income=1800, expense=200, net=1600, count=4
                .andExpect(jsonPath("$.data.totalIncome").value(1800.0))
                .andExpect(jsonPath("$.data.totalExpense").value(200.0))
                .andExpect(jsonPath("$.data.netFlow").value(1600.0))
                .andExpect(jsonPath("$.data.journalCount").value(4))
                // currentBalance = 800(div1)+500(div3)+300(div5) = 1600
                .andExpect(jsonPath("$.data.currentBalance").value(1600.0))
                // divisions 固定 1..7
                .andExpect(jsonPath("$.data.divisions.length()").value(7))
                .andExpect(jsonPath("$.data.divisions[0].division").value(1))
                .andExpect(jsonPath("$.data.divisions[0].balance").value(800.0))
                .andExpect(jsonPath("$.data.divisions[0].income").value(1000.0))
                .andExpect(jsonPath("$.data.divisions[0].expense").value(200.0))
                .andExpect(jsonPath("$.data.divisions[2].division").value(3))
                .andExpect(jsonPath("$.data.divisions[2].balance").value(500.0))
                .andExpect(jsonPath("$.data.divisions[4].division").value(5))
                .andExpect(jsonPath("$.data.divisions[4].balance").value(300.0))
                // 无数据分账补零:div2
                .andExpect(jsonPath("$.data.divisions[1].division").value(2))
                .andExpect(jsonPath("$.data.divisions[1].balance").value(0.0))
                .andExpect(jsonPath("$.data.divisions[1].income").value(0.0))
                .andExpect(jsonPath("$.data.divisions[1].expense").value(0.0))
                // div6 补零
                .andExpect(jsonPath("$.data.divisions[5].division").value(6))
                .andExpect(jsonPath("$.data.divisions[5].balance").value(0.0));
    }

    @Test
    @DisplayName("军团单分账 -> 仅该 division 过滤,divisions 不出现")
    void corpOverview_singleDivision_filteredNoDivisions() throws Exception {
        loginAsAdmin();
        insertCorpJournal(401L, 1, 1000.0, 1000.0, LocalDate.of(2026, 1, 2), "bounty_prizes");
        insertCorpJournal(402L, 3, 500.0, 500.0, LocalDate.of(2026, 1, 4), "market_sale");

        mockMvc.perform(get("/wallet/overview/corp/" + CORP)
                        .param("division", "3")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.journalCount").value(1))
                .andExpect(jsonPath("$.data.totalIncome").value(500.0))
                .andExpect(jsonPath("$.data.totalExpense").value(0.0))
                .andExpect(jsonPath("$.data.currentBalance").value(500.0))
                .andExpect(jsonPath("$.data.divisions").doesNotExist());
    }

    @Test
    @DisplayName("军团单分账边界:division=1 与 7 合法,仅含该分账数据(其它分账不入),divisions 不出现")
    void corpOverview_singleDivision_boundary_div1And7Legal() throws Exception {
        loginAsAdmin();
        // div1、div7 有数据,div3、div5 为干扰数据(division=1/7 查询不得包含)
        insertCorpJournal(501L, 1, 1000.0, 1000.0, LocalDate.of(2026, 1, 2), "bounty_prizes");
        insertCorpJournal(502L, 3, 700.0, 700.0, LocalDate.of(2026, 1, 3), "market_sale");
        insertCorpJournal(503L, 7, 300.0, 300.0, LocalDate.of(2026, 1, 4), "market_sale");
        insertCorpJournal(504L, 5, 900.0, 900.0, LocalDate.of(2026, 1, 5), "market_sale");

        // division=1:仅含 div1 数据(div3/div5/div7 不入)
        mockMvc.perform(get("/wallet/overview/corp/" + CORP)
                        .param("division", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.journalCount").value(1))
                .andExpect(jsonPath("$.data.totalIncome").value(1000.0))
                .andExpect(jsonPath("$.data.totalExpense").value(0.0))
                .andExpect(jsonPath("$.data.currentBalance").value(1000.0))
                .andExpect(jsonPath("$.data.divisions").doesNotExist());

        // division=7:仅含 div7 数据
        mockMvc.perform(get("/wallet/overview/corp/" + CORP)
                        .param("division", "7")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.journalCount").value(1))
                .andExpect(jsonPath("$.data.totalIncome").value(300.0))
                .andExpect(jsonPath("$.data.currentBalance").value(300.0))
                .andExpect(jsonPath("$.data.divisions").doesNotExist());
    }

    @Test
    @DisplayName("军团 division=0/8 -> 400(PARAM_ERROR)")
    void corpOverview_invalidDivision_badRequest() throws Exception {
        loginAsAdmin();

        mockMvc.perform(get("/wallet/overview/corp/" + CORP).param("division", "0")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/wallet/overview/corp/" + CORP).param("division", "8")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("军团越权访问 -> 403")
    void corpOverview_foreignCorp_forbidden() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        (long) 7, null, List.of(new SimpleGrantedAuthority("USER"))));
        when(resourceOwnershipPolicy.isOwnedBy(any(), any())).thenReturn(false);

        mockMvc.perform(get("/wallet/overview/corp/999999").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("军团空分账零值 -> divisions 1..7 全零,currentBalance=0")
    void corpOverview_empty_zeroedDivisions() throws Exception {
        loginAsAdmin();

        mockMvc.perform(get("/wallet/overview/corp/" + CORP).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentBalance").value(0.0))
                .andExpect(jsonPath("$.data.journalCount").value(0))
                .andExpect(jsonPath("$.data.divisions.length()").value(7))
                .andExpect(jsonPath("$.data.divisions[0].division").value(1))
                .andExpect(jsonPath("$.data.divisions[0].balance").value(0.0))
                .andExpect(jsonPath("$.data.divisions[6].division").value(7))
                .andExpect(jsonPath("$.data.divisions[6].balance").value(0.0));
    }
}