package xyz.foolcat.eve.evehelper.esi.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import xyz.foolcat.eve.evehelper.domain.system.EveAccount;
import xyz.foolcat.eve.evehelper.esi.auth.AuthorizeOAuth;
import xyz.foolcat.eve.evehelper.esi.auth.GrantType;
import xyz.foolcat.eve.evehelper.esi.model.*;
import xyz.foolcat.eve.evehelper.service.system.EveAccountService;

import java.util.List;
import java.util.Objects;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("ESI Market Api Test")
class MarketApiTest {

    @Autowired
    MarketApi marketApi;

    @Autowired
    EveAccountService eveAccountService;

    @Autowired
    AuthorizeOAuth authorizeOAuth;

    String at = "Bearer ";

    @BeforeEach
    void initAccessToken() {
        EveAccount entity = eveAccountService.lambdaQuery().eq(EveAccount::getCharacterId, 2112818290).one();
        Mono<AuthTokenResponse> authTokenResponseMono = authorizeOAuth.updateAccessToken(GrantType.REFRESH_TOKEN, entity.getRefreshToken());
        at = at + Objects.requireNonNull(authTokenResponseMono.block()).getAccessToken();
        System.out.println("at = " + at);
    }

    @Test
    void queryCharacterOrders() {
        List<MarketOrderResponse> marketOrderResponses = marketApi.queryCharacterOrders(2112818290, "serenity", at).collectList().block();
        System.out.println("marketOrderResponses = " + marketOrderResponses);
    }

    @Test
    void queryCharacterOrdersHistory() {
        List<MarketOrderResponse> marketOrderResponses = marketApi.queryCharacterOrdersHistory(2112818290, "serenity", at).collectList().block();
        System.out.println("marketOrderResponses = " + marketOrderResponses);
    }

    @Test
    void queryCorporationOrders() {
        List<MarketOrderResponse> marketOrderResponses = marketApi.queryCorporationOrders(656880659, "serenity", 1, at).collectList().block();
        System.out.println("marketOrderResponses = " + marketOrderResponses);
    }

    @Test
    void queryCorporationOrdersHistory() {
        List<MarketOrderResponse> marketOrderResponses = marketApi.queryCorporationOrdersHistory(656880659, "serenity", at).collectList().block();
        System.out.println("marketOrderResponses = " + marketOrderResponses);
    }

    @Test
    void queryMarketRegionHistory() {
        List<HistoricalMarketStatisticsResponse> historicalMarketStatisticsResponses = marketApi.queryMarketRegionHistory(10000002, "serenity", 77738).collectList().block();
        System.out.println("historicalMarketStatisticsResponses = " + historicalMarketStatisticsResponses);
    }

    @Test
    void queryRegionOrders() {
        List<MarketOrderResponse> marketOrderResponses = marketApi.queryRegionOrders(10000002, "serenity", 77738, 1).collectList().block();
        System.out.println("marketOrderResponses = " + marketOrderResponses);
    }

    @Test
    void queryRegionTypes() {
        List<Integer> integers = marketApi.queryRegionTypes(10000002, "serenity", 1).collectList().block();
        System.out.println("integers = " + integers);
    }

    @Test
    void queryMarketGroup() {
        List<Integer> integers = marketApi.queryMarketGroup("serenity").collectList().block();
        System.out.println("integers = " + integers);
    }

    @Test
    void queryMarketGroupInfo() {
        GroupItemResponse groupItemResponse = marketApi.queryMarketGroupInfo(2783, "serenity", "zh").block();
        System.out.println("groupItemResponse = " + groupItemResponse);
    }

    @Test
    void queryMarketPrices() {
        List<PriceResponse> priceResponses = marketApi.queryMarketPrices("serenity").collectList().block();
        System.out.println("priceResponses = " + priceResponses);
    }

    @Test
    void queryStructureOrders() {
        List<MarketOrderResponse> marketOrderResponses = marketApi.queryStructureOrders(1015148880281L, "serenity", 1, at).collectList().block();
        System.out.println("marketOrderResponses = " + marketOrderResponses);
    }
}