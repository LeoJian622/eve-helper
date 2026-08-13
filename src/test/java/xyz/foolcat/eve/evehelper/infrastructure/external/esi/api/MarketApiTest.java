package xyz.foolcat.eve.evehelper.infrastructure.external.esi.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.util.AuthorizeUtil;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.auth.AuthorizeOAuth;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.auth.GrantType;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.AuthTokenResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.GroupItemResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.HistoricalMarketStatisticsResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.MarketOrderResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.PriceResponse;
import xyz.foolcat.eve.evehelper.shared.kernel.constants.GlobalConstants;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * MarketApi 真实 ESI 集成测试。
 * <p>
 * 由 mock 单元测试改造而来：不再依赖 esi-mock-data 资源文件，改为真实调用 ESI。
 * 公开数据端点（region orders / prices / marketGroup / marketGroupInfo / 历史统计）
 * 无需 token；角色/军团/结构订单端点需真实 access token（initAccessToken 用应用方法换取）。
 *
 * @author Leojan
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("ESI Market Api 集成测试")
class MarketApiTest {

    @Autowired
    MarketApi marketApi;

    @Autowired
    AuthorizeUtil authorizeUtil;

    @Autowired
    AuthorizeOAuth authorizeOAuth;

    private static final Integer CHARACTER_ID = 2112818290;
    private static final Integer CORPORATION_ID = 656880659;
    private static final Integer REGION_ID = 10000002;
    private static final Integer TYPE_ID = 77738;
    private static final Integer MARKET_GROUP_ID = 2783;
    private static final Long STRUCTURE_ID = 1015148880281L;
    private static final String DATASOURCE = "serenity";
    private static final String LANGUAGE = "zh";

    private String at = "Bearer ";

    @BeforeEach
    void initAccessToken() {
        EveAccount entity = authorizeUtil.authorizeInternal(GlobalConstants.SYSTEM_USER_ID, CHARACTER_ID);
        Mono<AuthTokenResponse> resp = authorizeOAuth.updateAccessToken(GrantType.REFRESH_TOKEN, entity.getRefreshToken());
        at = at + Objects.requireNonNull(resp.block()).getAccessToken();
    }

    // ---------- 角色订单（需 token） ----------

    @Test
    @DisplayName("queryCharacterOrders 返回角色市场订单")
    void queryCharacterOrders_shouldReturnCharacterMarketOrders() {
        List<MarketOrderResponse> list = marketApi.queryCharacterOrders(CHARACTER_ID, DATASOURCE, at).collectList().block();
        assertNotNull(list);
        System.out.println("character orders count = " + list.size());
    }

    @Test
    @DisplayName("queryCharacterOrdersHistory 返回角色订单历史")
    void queryCharacterOrdersHistory_shouldReturnOrderHistory() {
        List<MarketOrderResponse> list = marketApi.queryCharacterOrdersHistory(CHARACTER_ID, DATASOURCE, at).collectList().block();
        assertNotNull(list);
        System.out.println("character orders history count = " + list.size());
    }

    // ---------- 军团订单（需 token） ----------

    @Test
    @DisplayName("queryCorporationOrders 返回军团市场订单")
    void queryCorporationOrders_shouldReturnCorporationOrders() {
        List<MarketOrderResponse> list = marketApi.queryCorporationOrders(CORPORATION_ID, DATASOURCE, 1, at).collectList().block();
        assertNotNull(list);
        System.out.println("corporation orders count = " + list.size());
    }

    @Test
    @DisplayName("queryCorporationOrdersHistory 返回军团订单历史")
    void queryCorporationOrdersHistory_shouldReturnCorporationOrderHistory() {
        List<MarketOrderResponse> list = marketApi.queryCorporationOrdersHistory(CORPORATION_ID, DATASOURCE, at).collectList().block();
        assertNotNull(list);
        System.out.println("corporation orders history count = " + list.size());
    }

    // ---------- 结构订单（需 token） ----------

    /**
     * 禁用：真实调 ESI 时硬编码结构 1015148880281 返回 403 Market access denied，
     * 该结构非角色可访问的市场。需替换为角色真实可访问的结构 ID 后恢复。
     */
    @Test
    @Disabled("需角色可访问的结构 ID，硬编码 1015148880281 无市场访问权(403)")
    @DisplayName("queryStructureOrders 返回结构市场订单")
    void queryStructureOrders_shouldReturnStructureOrders() {
        List<MarketOrderResponse> list = marketApi.queryStructureOrders(STRUCTURE_ID, DATASOURCE, 1, at).collectList().block();
        assertNotNull(list);
        System.out.println("structure orders count = " + list.size());
    }

    // ---------- 公开数据端点（无需 token） ----------

    @Test
    @DisplayName("queryRegionOrders 返回区域市场订单")
    void queryRegionOrders_shouldReturnRegionOrders() {
        List<MarketOrderResponse> list = marketApi.queryRegionOrders(REGION_ID, DATASOURCE, TYPE_ID, 1).collectList().block();
        assertNotNull(list);
        assertFalse(list.isEmpty());
        System.out.println("region orders count = " + list.size());
    }

    @Test
    @DisplayName("queryMarketRegionHistory 返回区域历史统计")
    void queryMarketRegionHistory_shouldReturnHistoricalMarketStatistics() {
        List<HistoricalMarketStatisticsResponse> list =
                marketApi.queryMarketRegionHistory(REGION_ID, DATASOURCE, TYPE_ID).collectList().block();
        assertNotNull(list);
        assertFalse(list.isEmpty());
        System.out.println("region history count = " + list.size());
    }

    @Test
    @DisplayName("queryMarketGroupInfo 返回市场组信息")
    void queryMarketGroupInfo_shouldReturnMarketGroupInfo() {
        GroupItemResponse resp = marketApi.queryMarketGroupInfo(MARKET_GROUP_ID, DATASOURCE, LANGUAGE).block();
        assertNotNull(resp);
        assertNotNull(resp.getMarketGroupId());
        assertNotNull(resp.getName());
        System.out.println("market group = " + resp.getMarketGroupId() + " " + resp.getName());
    }

    @Test
    @DisplayName("queryMarketGroup 返回市场组 ID 列表")
    void queryMarketGroup_shouldReturnMarketGroupIds() {
        List<Integer> ids = marketApi.queryMarketGroup(DATASOURCE).collectList().block();
        assertNotNull(ids);
        assertFalse(ids.isEmpty());
        System.out.println("market group count = " + ids.size());
    }

    @Test
    @DisplayName("queryMarketPrices 返回市场估价")
    void queryMarketPrices_shouldReturnMarketPrices() {
        List<PriceResponse> list = marketApi.queryMarketPrices(DATASOURCE).collectList().block();
        assertNotNull(list);
        assertFalse(list.isEmpty());
        System.out.println("prices count = " + list.size());
    }

    @Test
    @DisplayName("queryRegionTypes 返回区域在售类型 ID")
    void queryRegionTypes_shouldReturnTypeIds() {
        List<Integer> ids = marketApi.queryRegionTypes(REGION_ID, DATASOURCE, 1).collectList().block();
        assertNotNull(ids);
        assertFalse(ids.isEmpty());
        System.out.println("region types count = " + ids.size());
    }
}