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
import reactor.core.publisher.Flux;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.WalletJournal;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.domain.service.security.ResourceOwnershipPolicy;
import xyz.foolcat.eve.evehelper.domain.service.system.EveAccountService;
import xyz.foolcat.eve.evehelper.infrastructure.config.security.RbacAuthorizationManager;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * US2b(014 T021)军团维度数据私有性集成测试:军团读=同步者私有。
 * 甲同步过军团 X 钱包见其片段;同团非同步者乙查 = 空 200 非 403(FR-007 不泄漏);
 * 甲只见自己同步片段、不并入他人数据(FR-003 AC3)。
 *
 * <p>基建完全仿 {@link CharacterPrivacyIT}:
 * MockMvc 走完整过滤器链 + 控制器 + 应用服务 + 领域服务 + 真实仓储/mapper(落测试 MySQL 的 wallet_journal 表)。</p>
 * <ul>
 *   <li><b>真实写路径</b>:甲经真实 {@code WalletJournalService.syncCorporationJournal}(T013 已 setUserId)
 *       把军团流水落库,断言军团行 {@code owner_id=corpId, division=1} 之 {@code user_id}=甲(M-US1-1 复验)。</li>
 *   <li><b>RBAC</b>(RbacAuthorizationManager)mock 放行以规避对 Redis 依赖(既有 IT 惯例)。
 *       每次仅注入一个非 ROOT(USER)主体现身,令归属决策确定性。</li>
 *   <li><b>EsiGateway / EveAccountService mock</b>:同步用例注入受控军团流水与角色→军团派生,不触发真实 ESI。</li>
 *   <li><b>军团读裁决</b>(queryCorporationPage → corporationScope):非 ROOT 返回当前 userId,
 *       仓储按 user_id 过滤 —— 乙(未同步)自然拿空 200,生产代码不抛 403。</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DisplayName("军团维度私有读:同步者见 / 非同步者空200 / 不并入他人 集成测试")
class CorporationPrivacyIT {

    /** 甲:持有角色 OWNER_CHARACTER 的系统用户(军团 X 同步者) */
    private static final int OWNER_USER_ID = 55;

    /** 乙:已登录但从未同步过军团 X 的系统用户(同阵营非同步者) */
    private static final int FOREIGNER_USER_ID = 8888;

    /** 甲持有的角色 ID,其 eve_account 派生关联军团 CORP_ID */
    private static final int OWNER_CHARACTER = 2112999980;

    /** 军团 X(合成 id,避开既有真实数据;owner_id 落库目标) */
    private static final long CORP_ID = 2111990005L;

    /** 军团分账:本测试所有用例统一用分账 1 */
    private static final int DIVISION = 1;

    /** 各用例独立的主键 id,避免跨用例 PK 冲突 */
    private static final long JOURNAL_ID_OWN = 900100001L;
    private static final long JOURNAL_ID_FOREIGN = 900200002L;

    /** 甲片段的可识别内容 */
    private static final double OWN_AMOUNT = 1234.5;
    private static final String OWN_DESC = "T021-OWN-SEGMENT";

    /** 乙(他人)片段的可识别内容,验证不并入 */
    private static final double FOREIGN_AMOUNT = 999.25;
    private static final String FOREIGN_DESC = "T021-FOREIGN-SEGMENT";

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
        // RBAC 依赖 Redis,此处 mock 放行以聚焦军团私有读写语义(既有 IT 惯例)
        when(rbacAuthorizationManager.authorize(any(), any()))
                .thenReturn(new AuthorizationDecision(true));
        when(rbacAuthorizationManager.check(any(), any()))
                .thenReturn(new AuthorizationDecision(true));
        // 清理测试军团 X 的流水行,保证断言确定性
        jdbcTemplate.update("delete from wallet_journal where owner_id = ?", CORP_ID);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        jdbcTemplate.update("delete from wallet_journal where owner_id = ?", CORP_ID);
    }

    /** 注入指定用户主体现身;authority=USER(非 ROOT/ADMIN),令军团读走同步者私有过滤而非 ROOT 豁免 */
    private void loginAs(int userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        (long) userId, null, List.of(new SimpleGrantedAuthority("USER"))));
    }

    /** 供同步 mock 的受控军团流水(division 由服务统一回填) */
    private static WalletJournal journal(long id) {
        WalletJournal j = new WalletJournal();
        j.setId(id);
        j.setAmount(OWN_AMOUNT);
        j.setDescription(OWN_DESC);
        j.setDate(OffsetDateTime.of(2026, 1, 2, 3, 4, 5, 0, ZoneOffset.UTC));
        return j;
    }

    /** 直接按生产写路径的可落库列集合 seed 一条「甲拥有的军团 X 分账1 流水行」(含 user_id=甲) */
    private void seedJournal(long journalId, double amount, String desc, long userId) {
        jdbcTemplate.update(
                "insert into wallet_journal (id, amount, description, `date`, owner_id, division, user_id) "
                        + "values (?,?,?,?,?,?,?)",
                journalId, amount, desc, Timestamp.valueOf("2026-01-02 03:04:05"),
                CORP_ID, DIVISION, userId);
    }

    // ────────── 用例1:写路径落 user_id(军团表,M-US1-1 复验) ──────────

    @Test
    @DisplayName("甲同步军团X钱包:军团行(owner_id=X,division=1) user_id 落库为甲(M-US1-1)")
    void ownerSyncs_writesUserIdIntoCorpRow() throws Exception {
        loginAs(OWNER_USER_ID);
        // 应用服务 requireOwnership:角色 OWNER_CHARACTER 归属甲
        when(resourceOwnershipPolicy.isOwnedBy(OWNER_USER_ID, String.valueOf(OWNER_CHARACTER))).thenReturn(true);

        // 授权:甲持有 OWNER_CHARACTER 且该角色派生军团 CORP_ID;ESI 返回受控流水(仅 division=1 有数据)
        EveAccount acc = new EveAccount();
        acc.setCharacterId(OWNER_CHARACTER);
        acc.setUserId(OWNER_USER_ID);
        acc.setCorpId((int) CORP_ID);
        when(eveAccountService.getAccountOne(OWNER_USER_ID, OWNER_CHARACTER)).thenReturn(acc);
        when(esiGateway.getAccessToken(OWNER_CHARACTER, OWNER_USER_ID)).thenReturn("tk");
        when(esiGateway.queryCorporationWalletJournalMaxPage((int) CORP_ID, DIVISION, "tk")).thenReturn(1);
        when(esiGateway.queryCorporationWalletJournal(eq((int) CORP_ID), eq(DIVISION), eq(1), eq("tk")))
                .thenReturn(Flux.just(journal(JOURNAL_ID_OWN)));

        mockMvc.perform(post("/wallet/journal/corp/" + OWNER_CHARACTER + "/sync")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // 真实库存断言:军团 X 分账1 的流水行 user_id 已随写路径落库并等于甲;owner_id 与派生 corpId 一致
        Long persistedUserId = jdbcTemplate.queryForObject(
                "select user_id from wallet_journal where owner_id = ? and division = ? limit 1",
                Long.class, CORP_ID, DIVISION);
        assertThat(persistedUserId).isEqualTo((long) OWNER_USER_ID);
        Long persistedOwner = jdbcTemplate.queryForObject(
                "select owner_id from wallet_journal where id = ?", Long.class, JOURNAL_ID_OWN);
        assertThat(persistedOwner).isEqualTo(CORP_ID);
    }

    // ────────── 用例2:甲见片段 ──────────

    @Test
    @DisplayName("甲(军团X分账1同步者)查该分账:200,records 含甲的片段")
    void ownerReads_returnsOwnSegment() throws Exception {
        seedJournal(JOURNAL_ID_OWN, OWN_AMOUNT, OWN_DESC, OWNER_USER_ID);
        loginAs(OWNER_USER_ID);

        mockMvc.perform(get("/wallet/journal/corp/" + CORP_ID)
                        .param("division", String.valueOf(DIVISION))
                        .param("current", "1").param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                // 甲见:返回甲自己的同步片段
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(JOURNAL_ID_OWN))
                .andExpect(jsonPath("$.data.records[0].amount").value(OWN_AMOUNT));
    }

    // ────────── 用例3:同团非同步者乙 = 空 200 非 403(FR-007) ──────────

    @Test
    @DisplayName("乙(从未同步军团X)查军团X分账1:200 空 records,响应不含甲的片段,且非 403(不泄漏)")
    void foreignerReads_empty200_not403() throws Exception {
        // 军团 X 分账1 确有甲同步的数据 —— 验证乙即便面对「存在数据」也不得见其规模/内容
        seedJournal(JOURNAL_ID_OWN, OWN_AMOUNT, OWN_DESC, OWNER_USER_ID);
        loginAs(FOREIGNER_USER_ID);

        mockMvc.perform(get("/wallet/journal/corp/" + CORP_ID)
                        .param("division", String.valueOf(DIVISION))
                        .param("current", "1").param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                // 核心裁决:空 200,绝非 403
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records.length()").value(0))
                // 不泄漏:响应不含甲的 journal 明细(金额/描述/主键)
                .andExpect(content().string(not(containsString(String.valueOf(OWN_AMOUNT)))))
                .andExpect(content().string(not(containsString(OWN_DESC))))
                .andExpect(content().string(not(containsString(String.valueOf(JOURNAL_ID_OWN)))));
    }

    // ────────── 用例4:不并入他人(FR-003 AC3) ──────────

    @Test
    @DisplayName("甲查军团X分账1:只见自己的片段,他人(乙)的片段不出现(按 user_id 切分,不并入)")
    void ownerReads_doesNotMergeOthersSegment() throws Exception {
        // 同一军团 X 同一分账1,并存甲、乙两行不同 user_id 的流水
        seedJournal(JOURNAL_ID_OWN, OWN_AMOUNT, OWN_DESC, OWNER_USER_ID);
        seedJournal(JOURNAL_ID_FOREIGN, FOREIGN_AMOUNT, FOREIGN_DESC, FOREIGNER_USER_ID);
        loginAs(OWNER_USER_ID);

        mockMvc.perform(get("/wallet/journal/corp/" + CORP_ID)
                        .param("division", String.valueOf(DIVISION))
                        .param("current", "1").param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                // 甲只见自己的 1 条,他人数据被 user_id 过滤(CORP 该分账实际有 2 条)
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(JOURNAL_ID_OWN))
                // 甲片段可见,他人片段不可见
                .andExpect(jsonPath("$.data.records[0].description").value(OWN_DESC))
                .andExpect(content().string(not(containsString(FOREIGN_DESC))));
    }
}