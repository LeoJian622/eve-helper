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
@DisplayName("ESI Industry Api Test")
class IndustryApiTest {

    @Autowired
    IndustryApi industryApi;

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
    void queryCharacterIndustryJobs() {
        List<IndustryJobPlacedResponse> industryJobPlacedResponses = industryApi.queryCharacterIndustryJobs(2112818290, "serenity", null, at).collectList().block();
        System.out.println("industryJobPlacedResponses = " + industryJobPlacedResponses);
    }

    @Test
    void queryCharacterMining() {
        List<MiningLedgerResponse> miningLedgerResponses = industryApi.queryCharacterMining(2112818290, "serenity", 1, at).collectList().block();
        System.out.println("miningLedgerResponses = " + miningLedgerResponses);
    }

    @Test
    void queryCorporationMiningExtractions() {
        List<ChunkTimersResponse> chunkTimersResponses = industryApi.queryCorporationMiningExtractions(656880659, "serenity", 1, at).collectList().block();
        System.out.println("chunkTimersResponses = " + chunkTimersResponses);
    }

    @Test
    void queryCorporationMiningObservers() {
        List<CorporationObserverResponse> corporationObserverResponses = industryApi.queryCorporationMiningObservers(656880659, "serenity", 1, at).collectList().block();
        System.out.println("corporationObserverResponses = " + corporationObserverResponses);
    }

    @Test
    void queryCorporationMiningObserver() {
        List<ObserverMiningLedgerResponse> observerMiningLedgerResponses = industryApi.queryCorporationMiningObserver(656880659, "serenity", 1014012885950L, 1,at).collectList().block();
        System.out.println("observerMiningLedgerResponses = " + observerMiningLedgerResponses);
    }

    @Test
    void queryCorporationIndustryJobs() {
        List<IndustryJobPlacedResponse> industryJobPlacedResponses = industryApi.queryCorporationIndustryJobs(98061457, "serenity", null, at).collectList().block();
        System.out.println("industryJobPlacedResponses = " + industryJobPlacedResponses);
    }

    @Test
    void queryIndustryFacilities() {
        List<FacilitiesResponse> facilitiesResponses = industryApi.queryIndustryFacilities("serenity").collectList().block();
        System.out.println("facilitiesResponses = " + facilitiesResponses);
    }

    @Test
    void queryIndustrySystems() {
        List<CostIndiciesResponse> costIndiciesResponses = industryApi.queryIndustrySystems("serenity").collectList().block();
        System.out.println("costIndiciesResponses = " + costIndiciesResponses);
    }
}