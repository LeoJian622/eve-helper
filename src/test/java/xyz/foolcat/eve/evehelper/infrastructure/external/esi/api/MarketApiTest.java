package xyz.foolcat.eve.evehelper.infrastructure.external.esi.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * MarketApi Mock Unit Test
 *
 * This test class uses Mockito to mock WebClient responses,
 * eliminating dependencies on real ESI API, database, and Redis.
 *
 * @author Leojan
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ESI Market Api Mock Unit Test")
class MarketApiTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Mock
    private PageTotalApi pageTotalApi;

    private MarketApi marketApi;

    private ObjectMapper objectMapper;

    private static final Integer TEST_CHARACTER_ID = 2112818290;
    private static final Integer TEST_CORPORATION_ID = 656880659;
    private static final Integer TEST_REGION_ID = 10000002;
    private static final Integer TEST_TYPE_ID = 77738;
    private static final Integer TEST_MARKET_GROUP_ID = 2783;
    private static final Long TEST_STRUCTURE_ID = 1015148880281L;
    private static final String TEST_DATASOURCE = "serenity";
    private static final String TEST_LANGUAGE = "zh";
    private static final String TEST_ACCESS_TOKEN = "Bearer test_token";

    @BeforeEach
    void setUp() {
        marketApi = new MarketApi(webClient, pageTotalApi);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Load mock data from JSON file
     */
    private String loadMockData(String filename) throws IOException {
        ClassPathResource resource = new ClassPathResource("esi-mock-data/" + filename);
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("queryCharacterOrders should return character market orders")
    void queryCharacterOrders_shouldReturnCharacterMarketOrders() throws Exception {
        // Given - Load mock data
        String mockJson = loadMockData("market-region-orders.json");
        List<MarketOrderResponse> expectedList = objectMapper.readValue(mockJson,
                new TypeReference<List<MarketOrderResponse>>() {});

        // When - Configure mock behavior
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(Predicate.class), any(Function.class))).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(MarketOrderResponse.class)).thenReturn(Flux.fromIterable(expectedList));

        // Then - Execute test
        List<MarketOrderResponse> actualList = marketApi.queryCharacterOrders(
                TEST_CHARACTER_ID, TEST_DATASOURCE, TEST_ACCESS_TOKEN).collectList().block();

        // Verify
        assertNotNull(actualList);
        assertEquals(expectedList.size(), actualList.size());
        assertEquals(expectedList.get(0).getOrderId(), actualList.get(0).getOrderId());
        assertEquals(expectedList.get(0).getPrice(), actualList.get(0).getPrice());

        // Verify authorization header was set
        verify(requestHeadersSpec).header(eq("Authorization"), eq(TEST_ACCESS_TOKEN));
    }

    @Test
    @DisplayName("queryCharacterOrders should return empty list when no orders")
    void queryCharacterOrders_shouldReturnEmptyListWhenNoOrders() {
        // When - Configure mock to return empty flux
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(Predicate.class), any(Function.class))).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(MarketOrderResponse.class)).thenReturn(Flux.empty());

        // Then - Execute test
        List<MarketOrderResponse> actualList = marketApi.queryCharacterOrders(
                TEST_CHARACTER_ID, TEST_DATASOURCE, TEST_ACCESS_TOKEN).collectList().block();

        // Verify
        assertNotNull(actualList);
        assertTrue(actualList.isEmpty());
    }

    @Test
    @DisplayName("queryCharacterOrdersHistory should return order history")
    void queryCharacterOrdersHistory_shouldReturnOrderHistory() throws Exception {
        // Given - Load mock data
        String mockJson = loadMockData("market-region-orders.json");
        List<MarketOrderResponse> expectedList = objectMapper.readValue(mockJson,
                new TypeReference<List<MarketOrderResponse>>() {});

        // When - Configure mock behavior
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(Predicate.class), any(Function.class))).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(MarketOrderResponse.class)).thenReturn(Flux.fromIterable(expectedList));

        // Then - Execute test
        List<MarketOrderResponse> actualList = marketApi.queryCharacterOrdersHistory(
                TEST_CHARACTER_ID, TEST_DATASOURCE, TEST_ACCESS_TOKEN).collectList().block();

        // Verify
        assertNotNull(actualList);
        assertFalse(actualList.isEmpty());
    }

    @Test
    @DisplayName("queryCharacterOrdersHistoryMaxPage should return max page number")
    void queryCharacterOrdersHistoryMaxPage_shouldReturnMaxPageNumber() {
        // Given
        Integer expectedMaxPage = 3;
        when(pageTotalApi.queryMaxPage(anyString(), anyString(), any(WebClient.class))).thenReturn(expectedMaxPage);

        // Then - Execute test
        Integer actualMaxPage = marketApi.queryCharacterOrdersHistoryMaxPage(
                TEST_CHARACTER_ID, TEST_DATASOURCE, TEST_ACCESS_TOKEN);

        // Verify
        assertEquals(expectedMaxPage, actualMaxPage);
        verify(pageTotalApi).queryMaxPage(eq(TEST_ACCESS_TOKEN), contains("/characters/" + TEST_CHARACTER_ID + "/orders/history/"), eq(webClient));
    }

    @Test
    @DisplayName("queryCorporationOrders should return corporation orders with pagination")
    void queryCorporationOrders_shouldReturnCorporationOrders() throws Exception {
        // Given - Load mock data
        String mockJson = loadMockData("market-region-orders.json");
        List<MarketOrderResponse> expectedList = objectMapper.readValue(mockJson,
                new TypeReference<List<MarketOrderResponse>>() {});
        Integer page = 1;

        // When - Configure mock behavior
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(Predicate.class), any(Function.class))).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(MarketOrderResponse.class)).thenReturn(Flux.fromIterable(expectedList));

        // Then - Execute test
        List<MarketOrderResponse> actualList = marketApi.queryCorporationOrders(
                TEST_CORPORATION_ID, TEST_DATASOURCE, page, TEST_ACCESS_TOKEN).collectList().block();

        // Verify
        assertNotNull(actualList);
        assertFalse(actualList.isEmpty());
    }

    @Test
    @DisplayName("queryCorporationOrdersMaxPage should return max page number")
    void queryCorporationOrdersMaxPage_shouldReturnMaxPageNumber() {
        // Given
        Integer expectedMaxPage = 5;
        when(pageTotalApi.queryMaxPage(anyString(), anyString(), any(WebClient.class))).thenReturn(expectedMaxPage);

        // Then - Execute test
        Integer actualMaxPage = marketApi.queryCorporationOrdersMaxPage(
                TEST_CORPORATION_ID, TEST_DATASOURCE, TEST_ACCESS_TOKEN);

        // Verify
        assertEquals(expectedMaxPage, actualMaxPage);
    }

    @Test
    @DisplayName("queryCorporationOrdersHistory should return corporation order history")
    void queryCorporationOrdersHistory_shouldReturnCorporationOrderHistory() throws Exception {
        // Given - Load mock data
        String mockJson = loadMockData("market-region-orders.json");
        List<MarketOrderResponse> expectedList = objectMapper.readValue(mockJson,
                new TypeReference<List<MarketOrderResponse>>() {});

        // When - Configure mock behavior
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(Predicate.class), any(Function.class))).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(MarketOrderResponse.class)).thenReturn(Flux.fromIterable(expectedList));

        // Then - Execute test
        List<MarketOrderResponse> actualList = marketApi.queryCorporationOrdersHistory(
                TEST_CORPORATION_ID, TEST_DATASOURCE, TEST_ACCESS_TOKEN).collectList().block();

        // Verify
        assertNotNull(actualList);
        assertFalse(actualList.isEmpty());
    }

    @Test
    @DisplayName("queryMarketRegionHistory should return historical market statistics")
    void queryMarketRegionHistory_shouldReturnHistoricalMarketStatistics() throws Exception {
        // Given - Load mock data
        String mockJson = loadMockData("market-region-history.json");
        List<HistoricalMarketStatisticsResponse> expectedList = objectMapper.readValue(mockJson,
                new TypeReference<List<HistoricalMarketStatisticsResponse>>() {});

        // When - Configure mock behavior
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(Predicate.class), any(Function.class))).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(HistoricalMarketStatisticsResponse.class)).thenReturn(Flux.fromIterable(expectedList));

        // Then - Execute test
        List<HistoricalMarketStatisticsResponse> actualList = marketApi.queryMarketRegionHistory(
                TEST_REGION_ID, TEST_DATASOURCE, TEST_TYPE_ID).collectList().block();

        // Verify
        assertNotNull(actualList);
        assertEquals(expectedList.size(), actualList.size());
        assertEquals(expectedList.get(0).getAverage(), actualList.get(0).getAverage());
        assertEquals(expectedList.get(0).getVolume(), actualList.get(0).getVolume());
    }

    @Test
    @DisplayName("queryMarketRegionHistory should return empty list for new item")
    void queryMarketRegionHistory_shouldReturnEmptyListForNewItem() {
        // When - Configure mock to return empty flux
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(Predicate.class), any(Function.class))).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(HistoricalMarketStatisticsResponse.class)).thenReturn(Flux.empty());

        // Then - Execute test
        List<HistoricalMarketStatisticsResponse> actualList = marketApi.queryMarketRegionHistory(
                TEST_REGION_ID, TEST_DATASOURCE, 99999999).collectList().block();

        // Verify
        assertNotNull(actualList);
        assertTrue(actualList.isEmpty());
    }

    @Test
    @DisplayName("queryRegionOrders should return region orders with pagination")
    void queryRegionOrders_shouldReturnRegionOrders() throws Exception {
        // Given - Load mock data
        String mockJson = loadMockData("market-region-orders.json");
        List<MarketOrderResponse> expectedList = objectMapper.readValue(mockJson,
                new TypeReference<List<MarketOrderResponse>>() {});
        Integer page = 1;

        // When - Configure mock behavior
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(Predicate.class), any(Function.class))).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(MarketOrderResponse.class)).thenReturn(Flux.fromIterable(expectedList));

        // Then - Execute test
        List<MarketOrderResponse> actualList = marketApi.queryRegionOrders(
                TEST_REGION_ID, TEST_DATASOURCE, TEST_TYPE_ID, page).collectList().block();

        // Verify
        assertNotNull(actualList);
        assertEquals(expectedList.size(), actualList.size());
        // Verify buy and sell orders are present
        assertTrue(actualList.stream().anyMatch(o -> Boolean.TRUE.equals(o.getIsBuyOrder())));
        assertTrue(actualList.stream().anyMatch(o -> Boolean.FALSE.equals(o.getIsBuyOrder())));
    }

    @Test
    @DisplayName("queryRegionOrdersMaxPage should return max page number")
    void queryRegionOrdersMaxPage_shouldReturnMaxPageNumber() {
        // Given
        Integer expectedMaxPage = 10;
        when(pageTotalApi.queryMaxPage(anyString(), anyString(), any(WebClient.class))).thenReturn(expectedMaxPage);

        // Then - Execute test
        Integer actualMaxPage = marketApi.queryRegionOrdersMaxPage(
                TEST_REGION_ID, TEST_TYPE_ID, TEST_DATASOURCE);

        // Verify
        assertEquals(expectedMaxPage, actualMaxPage);
    }

    @Test
    @DisplayName("queryRegionTypes should return type IDs with active orders")
    void queryRegionTypes_shouldReturnTypeIds() {
        // Given
        List<Integer> expectedTypeIds = List.of(77738, 77739, 77740, 36951);
        Integer page = 1;

        // When - Configure mock behavior
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(Predicate.class), any(Function.class))).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(Integer.class)).thenReturn(Flux.fromIterable(expectedTypeIds));

        // Then - Execute test
        List<Integer> actualTypeIds = marketApi.queryRegionTypes(
                TEST_REGION_ID, TEST_DATASOURCE, page).collectList().block();

        // Verify
        assertNotNull(actualTypeIds);
        assertEquals(expectedTypeIds.size(), actualTypeIds.size());
        assertTrue(actualTypeIds.containsAll(expectedTypeIds));
    }

    @Test
    @DisplayName("queryRegionTypesMaxPage should return max page number")
    void queryRegionTypesMaxPage_shouldReturnMaxPageNumber() {
        // Given
        Integer expectedMaxPage = 15;
        when(pageTotalApi.queryMaxPage(anyString(), anyString(), any(WebClient.class))).thenReturn(expectedMaxPage);

        // Then - Execute test
        Integer actualMaxPage = marketApi.queryRegionTypesMaxPage(TEST_REGION_ID, TEST_DATASOURCE);

        // Verify
        assertEquals(expectedMaxPage, actualMaxPage);
    }

    @Test
    @DisplayName("queryMarketGroup should return market group IDs")
    void queryMarketGroup_shouldReturnMarketGroupIds() {
        // Given
        List<Integer> expectedGroupIds = List.of(1, 2, 3, 2783, 2784);

        // When - Configure mock behavior
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(Predicate.class), any(Function.class))).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(Integer.class)).thenReturn(Flux.fromIterable(expectedGroupIds));

        // Then - Execute test
        List<Integer> actualGroupIds = marketApi.queryMarketGroup(TEST_DATASOURCE).collectList().block();

        // Verify
        assertNotNull(actualGroupIds);
        assertEquals(expectedGroupIds.size(), actualGroupIds.size());
    }

    @Test
    @DisplayName("queryMarketGroupInfo should return market group information")
    void queryMarketGroupInfo_shouldReturnMarketGroupInfo() throws Exception {
        // Given - Load mock data
        String mockJson = loadMockData("market-group-info.json");
        GroupItemResponse expectedResponse = objectMapper.readValue(mockJson, GroupItemResponse.class);

        // When - Configure mock behavior
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(Predicate.class), any(Function.class))).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(GroupItemResponse.class)).thenReturn(Mono.just(expectedResponse));

        // Then - Execute test
        GroupItemResponse actualResponse = marketApi.queryMarketGroupInfo(
                TEST_MARKET_GROUP_ID, TEST_DATASOURCE, TEST_LANGUAGE).block();

        // Verify
        assertNotNull(actualResponse);
        assertEquals(expectedResponse.getMarketGroupId(), actualResponse.getMarketGroupId());
        assertEquals(expectedResponse.getName(), actualResponse.getName());
        assertNotNull(actualResponse.getTypes());
    }

    @Test
    @DisplayName("queryMarketGroupInfo should handle null response")
    void queryMarketGroupInfo_shouldHandleNullResponse() {
        // When - Configure mock to return empty
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(Predicate.class), any(Function.class))).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(GroupItemResponse.class)).thenReturn(Mono.empty());

        // Then - Execute test
        GroupItemResponse actualResponse = marketApi.queryMarketGroupInfo(
                99999, TEST_DATASOURCE, TEST_LANGUAGE).block();

        // Verify
        assertNull(actualResponse);
    }

    @Test
    @DisplayName("queryMarketPrices should return market prices")
    void queryMarketPrices_shouldReturnMarketPrices() throws Exception {
        // Given - Load mock data
        String mockJson = loadMockData("market-prices.json");
        List<PriceResponse> expectedList = objectMapper.readValue(mockJson,
                new TypeReference<List<PriceResponse>>() {});

        // When - Configure mock behavior
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(Predicate.class), any(Function.class))).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(PriceResponse.class)).thenReturn(Flux.fromIterable(expectedList));

        // Then - Execute test
        List<PriceResponse> actualList = marketApi.queryMarketPrices(TEST_DATASOURCE).collectList().block();

        // Verify
        assertNotNull(actualList);
        assertEquals(expectedList.size(), actualList.size());
        assertEquals(expectedList.get(0).getTypeId(), actualList.get(0).getTypeId());
        assertEquals(expectedList.get(0).getAveragePrice(), actualList.get(0).getAveragePrice());
    }

    @Test
    @DisplayName("queryStructureOrders should return structure orders with authorization")
    void queryStructureOrders_shouldReturnStructureOrders() throws Exception {
        // Given - Load mock data
        String mockJson = loadMockData("market-region-orders.json");
        List<MarketOrderResponse> expectedList = objectMapper.readValue(mockJson,
                new TypeReference<List<MarketOrderResponse>>() {});
        Integer page = 1;

        // When - Configure mock behavior
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(Predicate.class), any(Function.class))).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(MarketOrderResponse.class)).thenReturn(Flux.fromIterable(expectedList));

        // Then - Execute test
        List<MarketOrderResponse> actualList = marketApi.queryStructureOrders(
                TEST_STRUCTURE_ID, TEST_DATASOURCE, page, TEST_ACCESS_TOKEN).collectList().block();

        // Verify
        assertNotNull(actualList);
        assertFalse(actualList.isEmpty());

        // Verify authorization header was set
        verify(requestHeadersSpec).header(eq("Authorization"), eq(TEST_ACCESS_TOKEN));
    }

    @Test
    @DisplayName("queryStructureOrdersMaxPage should return max page number")
    void queryStructureOrdersMaxPage_shouldReturnMaxPageNumber() {
        // Given
        Integer expectedMaxPage = 8;
        when(pageTotalApi.queryMaxPage(anyString(), anyString(), any(WebClient.class))).thenReturn(expectedMaxPage);

        // Then - Execute test
        Integer actualMaxPage = marketApi.queryStructureOrdersMaxPage(
                TEST_STRUCTURE_ID, TEST_DATASOURCE, TEST_ACCESS_TOKEN);

        // Verify
        assertEquals(expectedMaxPage, actualMaxPage);
    }

    @Test
    @DisplayName("queryRegionOrders should correctly identify buy and sell orders")
    void queryRegionOrders_shouldCorrectlyIdentifyBuyAndSellOrders() throws Exception {
        // Given - Create specific mock data for buy/sell verification
        MarketOrderResponse sellOrder = new MarketOrderResponse();
        sellOrder.setOrderId(1L);
        sellOrder.setIsBuyOrder(false);
        sellOrder.setPrice(1500000.0);

        MarketOrderResponse buyOrder = new MarketOrderResponse();
        buyOrder.setOrderId(2L);
        buyOrder.setIsBuyOrder(true);
        buyOrder.setPrice(1400000.0);

        List<MarketOrderResponse> mockOrders = List.of(sellOrder, buyOrder);

        // When - Configure mock behavior
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(Predicate.class), any(Function.class))).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(MarketOrderResponse.class)).thenReturn(Flux.fromIterable(mockOrders));

        // Then - Execute test
        List<MarketOrderResponse> actualList = marketApi.queryRegionOrders(
                TEST_REGION_ID, TEST_DATASOURCE, TEST_TYPE_ID, 1).collectList().block();

        // Verify
        assertNotNull(actualList);
        assertEquals(2, actualList.size());

        // Verify sell order
        MarketOrderResponse actualSellOrder = actualList.stream()
                .filter(o -> Boolean.FALSE.equals(o.getIsBuyOrder()))
                .findFirst()
                .orElse(null);
        assertNotNull(actualSellOrder);
        assertEquals(1500000.0, actualSellOrder.getPrice());

        // Verify buy order
        MarketOrderResponse actualBuyOrder = actualList.stream()
                .filter(o -> Boolean.TRUE.equals(o.getIsBuyOrder()))
                .findFirst()
                .orElse(null);
        assertNotNull(actualBuyOrder);
        assertEquals(1400000.0, actualBuyOrder.getPrice());
    }
}
