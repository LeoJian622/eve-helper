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
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Assets;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.domain.service.security.ResourceOwnershipPolicy;
import xyz.foolcat.eve.evehelper.domain.service.system.EveAccountService;
import xyz.foolcat.eve.evehelper.infrastructure.config.security.RbacAuthorizationManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * US1(014 T010)人物维度数据私有性集成测试:甲见 / 乙挡,且不泄漏数据存在性(FR-007)。
 *
 * <p>MockMvc 走完整过滤器链 + 控制器 + 应用服务 + 领域服务 + 真实仓储/mapper(落测试 MySQL 的 assets 表)。</p>
 * <ul>
 *   <li><b>真实写路径</b>:甲经真实 {@code AssetsService.saveAndUpdateAsserts} 把 asset 落库,
 *       断言 person 行 {@code user_id}=甲(证明写路径已落 user_id)。</li>
 *   <li><b>RBAC</b>(RbacAuthorizationManager)mock 放行以规避对 Redis 依赖(既有 IT 惯例)。
 *       每次仅注入一个非 ROOT(USER)主体现身,令归属决策确定性。</li>
 *   <li><b>EsiGateway mock</b>:同步用例注入受控资产,不触发真实 ESI 网络调用。</li>
 *   <li><b>归属决策</b>(ResourceOwnershipPolicy / EveAccountService)mock:甲→own,乙→not own,
 *       确定性驱动「甲返完整 / 乙被拒」两路径。</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DisplayName("人物维度数据私有性:甲见/乙挡集成测试")
class CharacterPrivacyIT {

    /** 甲:持有角色 A 的系统用户 */
    private static final int OWNER_USER_ID = 55;

    /** 乙:已登录但未持有角色 A */
    private static final int FOREIGNER_USER_ID = 8888;

    /** 角色 A(甲持有) */
    private static final int CHARACTER_A = 2112832425;

    /** 角色 A 名下资产主键 item_id(唯一,区分测试数据) */
    private static final long ASSET_ITEM_ID = 66012345011L;

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
        // RBAC 依赖 Redis,此处 mock 放行以聚焦人物私有读写语义(既有 IT 惯例)
        when(rbacAuthorizationManager.authorize(any(), any()))
                .thenReturn(new AuthorizationDecision(true));
        when(rbacAuthorizationManager.check(any(), any()))
                .thenReturn(new AuthorizationDecision(true));
        // 清理测试人物 A 的资产行,保证断言确定性
        jdbcTemplate.update("delete from assets where owner_id = ?", (long) CHARACTER_A);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        jdbcTemplate.update("delete from assets where owner_id = ?", (long) CHARACTER_A);
    }

    /** 注入指定用户主体现身;authority=USER(非 ROOT/ADMIN),令 requireOwnership 走归属校验而非豁免 */
    private void loginAs(int userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        (long) userId, null, List.of(new SimpleGrantedAuthority("USER"))));
    }

    private static Assets asset() {
        Assets a = new Assets();
        a.setItemId(ASSET_ITEM_ID);
        a.setTypeId(34);
        a.setLocationId(60003466L);
        a.setLocationType("item");
        a.setLocationFlag("Hangar");
        a.setIsSingleton(false);
        a.setIsBlueprintCopy(false);
        a.setQuantity(1L);
        return a;
    }

    /** 直接按生产写路径的列集合 seed 一条「甲拥有的 A 资产行」(含 user_id=甲) */
    private void seedAssetForA() {
        jdbcTemplate.update(
                "insert into assets "
                        + "(item_id, type_id, location_id, location_type, location_flag, is_singleton, "
                        + " is_blueprint_copy, quantity, owner_id, user_id) "
                        + "values (?,?,?,?,?,?,?,?,?,?)",
                ASSET_ITEM_ID, 34, 60003466L, "item", "Hangar", false, false, 1L,
                (long) CHARACTER_A, (long) OWNER_USER_ID);
    }

    // ────────── 甲见:写路径落 user_id + 读返完整 ──────────

    @Test
    @DisplayName("甲同步角色A资产:person 行 user_id 落库为甲(user_id=甲) 且 owner_id=A")
    void ownerSyncs_writesUserIdIntoAssetsRow() throws Exception {
        loginAs(OWNER_USER_ID);
        when(resourceOwnershipPolicy.isOwnedBy(OWNER_USER_ID, String.valueOf(CHARACTER_A))).thenReturn(true);

        EveAccount acc = new EveAccount();
        acc.setCharacterId(CHARACTER_A);
        acc.setUserId(OWNER_USER_ID);
        when(eveAccountService.getAccountOne(OWNER_USER_ID, CHARACTER_A)).thenReturn(acc);
        when(esiGateway.getAccessToken(CHARACTER_A, OWNER_USER_ID)).thenReturn("tk");
        when(esiGateway.queryCharactersAssetsMaxPage(CHARACTER_A, "tk")).thenReturn(1);
        when(esiGateway.queryCharactersAssets(CHARACTER_A, 1, "tk")).thenReturn(Flux.just(asset()));

        mockMvc.perform(post("/assets/" + CHARACTER_A + "/sync").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // 真实库存断言:person 资产行 user_id 已随写路径落库并等于甲
        Long persistedUserId = jdbcTemplate.queryForObject(
                "select user_id from assets where owner_id = ? limit 1", Long.class, (long) CHARACTER_A);
        assertThat(persistedUserId).isEqualTo((long) OWNER_USER_ID);
    }

    @Test
    @DisplayName("甲(持角色A)查A资产:返回完整数据,含 A 的单条资产")
    void ownerReads_returnsFullAssets() throws Exception {
        seedAssetForA();
        Long seededCount = jdbcTemplate.queryForObject(
                "select count(*) from assets where owner_id = ?", Long.class, (long) CHARACTER_A);
        assertThat(seededCount).as("seed 应可见").isEqualTo(1L);
        loginAs(OWNER_USER_ID);
        when(resourceOwnershipPolicy.isOwnedBy(OWNER_USER_ID, String.valueOf(CHARACTER_A))).thenReturn(true);

        mockMvc.perform(get("/assets/" + CHARACTER_A)
                        .param("current", "1").param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                // 甲见:完整返回 A 名下这条资产(records 含该行,itemId 可见)
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].itemId").value(ASSET_ITEM_ID));
    }

    // ────────── 乙挡:被拒且响应不泄漏 A 的数据规模/条数(FR-007) ──────────

    @Test
    @DisplayName("乙(未持A)查A资产:403 被拒,响应不含 A 的 data(records/total/itemId 均不出现)")
    void foreignerReads_denied_withoutLeakingAssetScale() throws Exception {
        // A 名下确有甲同步的数据 —— 验证乙即便面对「存在数据」也不得见其规模/条数
        seedAssetForA();
        loginAs(FOREIGNER_USER_ID);
        when(resourceOwnershipPolicy.isOwnedBy(FOREIGNER_USER_ID, String.valueOf(CHARACTER_A))).thenReturn(false);

        mockMvc.perform(get("/assets/" + CHARACTER_A)
                        .param("current", "1").param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                // 不泄漏 A 数据:不出现资产分页结构的任何痕迹
                .andExpect(content().string(not(containsString("records"))))
                .andExpect(content().string(not(containsString("total"))))
                .andExpect(content().string(not(containsString(String.valueOf(ASSET_ITEM_ID)))));
    }
}