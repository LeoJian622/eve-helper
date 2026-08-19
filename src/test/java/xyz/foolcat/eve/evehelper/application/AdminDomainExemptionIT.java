package xyz.foolcat.eve.evehelper.application;

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
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.domain.service.security.ResourceOwnershipPolicy;
import xyz.foolcat.eve.evehelper.domain.service.system.EveAccountService;
import xyz.foolcat.eve.evehelper.infrastructure.config.security.RbacAuthorizationManager;
import xyz.foolcat.eve.evehelper.shared.kernel.constants.GlobalConstants;

import java.sql.Timestamp;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * US3(014 T022-T023)ROOT 管理域豁免集成测试(FR-006 / SC-003 对照)。
 *
 * <p>核心语义:ROOT(系统管理域,ADMIN role)看全量军团数据,不受同步者 {@code user_id} 私有过滤;
 * 非 ROOT 普通用户豁免不扩散 —— 非属主查非自同步军团仍返回空 200 且不泄漏(FR-007,与 T021 乙路径一致)。</p>
 *
 * <p>基建完全仿 {@link CharacterPrivacyIT} / {@link CorporationPrivacyIT}:
 * MockMvc 走完整过滤器链 + 控制器 + 应用服务 + 领域服务 + 真实仓储/mapper(落测试 MySQL 的 wallet_journal 表)。</p>
 * <ul>
 *   <li><b>ROOT 主体注入</b>:{@code AccessGuard.isCurrentUserRoot()} 要求 {@code auth.isAuthenticated()} 且
 *        authority 含 {@link GlobalConstants#ROOT_ROLE_CODE}("ADMIN")。loginAsRoot 注入
 *        {@code new UsernamePasswordAuthenticationToken((long) id, null,
 *        List.of(new SimpleGrantedAuthority(GlobalConstants.ROOT_ROLE_CODE)))} —— 3 参构造已认证,
 *        令 {@code corporationScope} 走 ROOT→null→不过滤看全量路径。</li>
 *   <li><b>RBAC</b>(RbacAuthorizationManager)mock 放行以规避对 Redis 依赖(既有 IT 惯例)。</li>
 *   <li><b>seed</b> 用合成 id:军团 2111XXX、用户 甲=55/乙=8888/丙=9999、流水主键 91/92XXXXXXX;
 *        setUp/tearDown 按 corp(owner_id) 清理,保证确定性。</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DisplayName("ROOT 管理域豁免:ROOT 看全量 / 普通用户豁免不扩散 集成测试")
class AdminDomainExemptionIT {

    /** ROOT(系统管理域 ADMIN):不受 corporationScope 私有过滤,仅作 loginAs 主体 id */
    private static final int ROOT_USER_ID = 1;

    /** 甲:军团 X 分账1 的同步者,user_id=55 */
    private static final int OWNER_USER_ID = 55;

    /** 乙:同阵营另一同步者,user_id=8888(亦在军团 Y 有数据) */
    private static final int FOREIGNER_USER_ID = 8888;

    /** 丙:普通用户,从未同步任何军团(豁免不得扩散到普通用户,SC-003 反例) */
    private static final int THIRD_USER_ID = 9999;

    /** 军团 X(合成 id):甲、乙两人各同步部分流水到同一分账1 */
    private static final long CORP_X = 2111990006L;

    /** 军团 Y(合成 id):仅乙一个人同步,验证 ROOT 对任意军团都看全量 */
    private static final long CORP_Y = 2111990007L;

    /** 所有用例统一用军团分账1 */
    private static final int DIVISION = 1;

    /** 各流水行唯一主键,避免跨用例 PK 冲突 */
    private static final long JID_A = 910100001L; // 军团 X / 分账1 / user_id=甲
    private static final long JID_B = 910200002L; // 军团 X / 分账1 / user_id=乙
    private static final long JID_Y = 920300003L; // 军团 Y / 分账1 / user_id=乙

    /** 可识别内容:甲片段 / 乙片段 / 乙在 Y 的片段 */
    private static final double AMOUNT_A = 1234.5;
    private static final String DESC_A = "US3-ROOT-SEG-A";
    private static final double AMOUNT_B = 8888.8;
    private static final String DESC_B = "US3-ROOT-SEG-B";
    private static final double AMOUNT_Y = 777.7;
    private static final String DESC_Y = "US3-ROOT-SEG-Y";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @MockBean
    RbacAuthorizationManager rbacAuthorizationManager;

    @MockBean
    EveAccountService eveAccountService;

    @MockBean
    ResourceOwnershipPolicy resourceOwnershipPolicy;

    @MockBean
    EsiGateway esiGateway;

    @BeforeEach
    void setUp() {
        // RBAC 依赖 Redis,此处 mock 放行以聚焦 ROOT 豁免读语义(既有 IT 惯例)
        when(rbacAuthorizationManager.authorize(any(), any()))
                .thenReturn(new AuthorizationDecision(true));
        when(rbacAuthorizationManager.check(any(), any()))
                .thenReturn(new AuthorizationDecision(true));
        // 清理两个测试军团的流水行,保证断言确定性
        jdbcTemplate.update("delete from wallet_journal where owner_id in (?, ?)", CORP_X, CORP_Y);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        jdbcTemplate.update("delete from wallet_journal where owner_id in (?, ?)", CORP_X, CORP_Y);
    }

    /** 注入 ROOT 主体现身;authority=ROOT_ROLE_CODE("ADMIN")+3 参已认证,令 corporationScope 走 ROOT→null 全量 */
    private void loginAsRoot() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        (long) ROOT_USER_ID, null,
                        List.of(new SimpleGrantedAuthority(GlobalConstants.ROOT_ROLE_CODE))));
    }

    /** 注入普通用户主体现身;authority=USER(非 ROOT/ADMIN),令军团读走同步者私有过滤而非 ROOT 豁免 */
    private void loginAsUser(int userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        (long) userId, null, List.of(new SimpleGrantedAuthority("USER"))));
    }

    /** 直接按生产写路径的可落库列集合 seed 一条军团流水行(owner_id=corpId, division=1, 含 user_id) */
    private void seedJournal(long journalId, long corpId, double amount, String desc, long userId) {
        jdbcTemplate.update(
                "insert into wallet_journal (id, amount, description, `date`, owner_id, division, user_id) "
                        + "values (?,?,?,?,?,?,?)",
                journalId, amount, desc, Timestamp.valueOf("2026-03-02 04:05:06"),
                corpId, DIVISION, userId);
    }

    // ────────── 用例1:ROOT 豁免全量(FR-006) ──────────

    @Test
    @DisplayName("ROOT 查军团X分账1:甲、乙各自同步的数据都可见(合并全量,records=2,total=2)")
    void root_readsFullAcrossBothSyncers() throws Exception {
        // 军团 X 分账1 并存两条不同 user_id 的流水:甲同步一条 + 乙同步一条
        seedJournal(JID_A, CORP_X, AMOUNT_A, DESC_A, OWNER_USER_ID);
        seedJournal(JID_B, CORP_X, AMOUNT_B, DESC_B, FOREIGNER_USER_ID);

        Long seededCount = jdbcTemplate.queryForObject(
                "select count(*) from wallet_journal where owner_id = ? and division = ?",
                Long.class, CORP_X, DIVISION);
        assertThat(seededCount).as("seed 应见军团X分账1共2条").isEqualTo(2L);

        loginAsRoot(); // authority=ADMIN 已认证 → corporationScope null 看全量

        mockMvc.perform(get("/wallet/journal/corp/" + CORP_X)
                        .param("division", String.valueOf(DIVISION))
                        .param("current", "1").param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                // ROOT 全量:不含过滤,甲+乙两条都可见(记录序由 DB 返回决定,断言以「两主键/两金额/两描述均在响应」为准)
                .andExpect(jsonPath("$.data.records.length()").value(2))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(content().string(containsString(String.valueOf(JID_A))))
                .andExpect(content().string(containsString(String.valueOf(JID_B))))
                .andExpect(content().string(containsString(String.valueOf(AMOUNT_A))))
                .andExpect(content().string(containsString(String.valueOf(AMOUNT_B))))
                // 两片段内容均不缺席
                .andExpect(content().string(containsString(DESC_A)))
                .andExpect(content().string(containsString(DESC_B)));
    }

    // ────────── 用例2:ROOT 豁免复验任意军团 ──────────

    @Test
    @DisplayName("ROOT 查军团Y分账1:仅乙同步的数据 ROOT 同样可见(任意军团看全量)")
    void root_readsFullOnAnyCorp() throws Exception {
        // 军团 Y 分账1 仅乙一个人有数据
        seedJournal(JID_Y, CORP_Y, AMOUNT_Y, DESC_Y, FOREIGNER_USER_ID);

        loginAsRoot();

        mockMvc.perform(get("/wallet/journal/corp/" + CORP_Y)
                        .param("division", String.valueOf(DIVISION))
                        .param("current", "1").param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                // ROOT 只见全量,乙那部分可见
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(JID_Y))
                .andExpect(jsonPath("$.data.records[0].description").value(DESC_Y));
    }

    // ────────── 用例3:反例 —— 普通用户豁免不扩散(SC-003) ──────────

    @Test
    @DisplayName("丙(普通用户,非属主、从未同步)查军团X分账1:200 空 records 且 total=0,非403,不泄漏甲/乙数据")
    void normalUser_readsEmpty200_scopeNotDiffused() throws Exception {
        // 军团 X 分账1 确有甲、乙同步的数据 —— 验证普通用户即便面对「存在数据」也不得其见规模/内容
        seedJournal(JID_A, CORP_X, AMOUNT_A, DESC_A, OWNER_USER_ID);
        seedJournal(JID_B, CORP_X, AMOUNT_B, DESC_B, FOREIGNER_USER_ID);

        Long seededCount = jdbcTemplate.queryForObject(
                "select count(*) from wallet_journal where owner_id = ? and division = ?",
                Long.class, CORP_X, DIVISION);
        assertThat(seededCount).as("seed 应见军团X分账1共2条(躲开因空库而误绿)").isEqualTo(2L);

        loginAsUser(THIRD_USER_ID); // 丙:普通 USER,非属主

        mockMvc.perform(get("/wallet/journal/corp/" + CORP_X)
                        .param("division", String.valueOf(DIVISION))
                        .param("current", "1").param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                // 核心裁决:空 200,绝非 403(T021 已定语义,豁免不扩散给普通用户)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records.length()").value(0))
                // 严封 FR-007:total 同 0,杜绝「records 空但 total 泄漏规模」
                .andExpect(jsonPath("$.data.total").value(0))
                // 不泄漏:丙的响应不含甲/乙任一流水明细(金额/描述/主键)
                .andExpect(content().string(not(containsString(String.valueOf(AMOUNT_A)))))
                .andExpect(content().string(not(containsString(DESC_A))))
                .andExpect(content().string(not(containsString(String.valueOf(JID_A)))))
                .andExpect(content().string(not(containsString(String.valueOf(AMOUNT_B)))))
                .andExpect(content().string(not(containsString(DESC_B))))
                .andExpect(content().string(not(containsString(String.valueOf(JID_B)))));
    }
}