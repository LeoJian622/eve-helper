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
import xyz.foolcat.eve.evehelper.esi.model.AuthTokenResponse;
import xyz.foolcat.eve.evehelper.esi.model.ChunkTimersResponse;
import xyz.foolcat.eve.evehelper.esi.model.IndustryJobPlacedResponse;
import xyz.foolcat.eve.evehelper.esi.model.MiningLedgerResponse;
import xyz.foolcat.eve.evehelper.service.system.EveAccountService;

import java.util.List;
import java.util.Objects;

@ExtendWith(SpringExtension.class)
@SpringBootTest
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
        EveAccount entity = eveAccountService.lambdaQuery().eq(EveAccount::getId, 3).one();
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
}