package xyz.foolcat.eve.evehelper.infrastructure.external.esi.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.auth.AuthorizeOAuth;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.auth.GrantType;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.*;
import xyz.foolcat.eve.evehelper.domain.service.system.EveAccountService;

import java.util.List;
import java.util.Objects;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("ESI FactionWarfare Api Test")
class FactionWarfareApiTest {

    @Autowired
    FactionWarfareApi factionWarfareApi;

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
    void queryCharacterFactionWarfareStats() {
        FactionWarfareStatisticsResponse factionWarfareStatisticsResponse = factionWarfareApi.queryCharacterFactionWarfareStats(2112818290, "serenity", at).block();
        System.out.println("factionWarfareStatisticsResponse = " + factionWarfareStatisticsResponse);
    }

    @Test
    void queryCorporationFactionWarfareStats() {
        FactionWarfareStatisticsResponse factionWarfareStatisticsResponse = factionWarfareApi.queryCorporationFactionWarfareStats(656880659, "serenity", at).block();
        System.out.println("factionWarfareStatisticsResponse = " + factionWarfareStatisticsResponse);
    }

    @Test
    void queryFactionWarfareLeaderboards() {
        LeaderboardResponse leaderboardResponse = factionWarfareApi.queryFactionWarfareLeaderboards("serenity").block();
        System.out.println("leaderboardResponse = " + leaderboardResponse);
    }

    @Test
    void queryFactionWarfareCharacterLeaderboards() {
        LeaderboardCharacterResponse leaderboardCharacterResponse = factionWarfareApi.queryFactionWarfareCharacterLeaderboards("serenity").block();
        System.out.println("leaderboardCharacterResponse = " + leaderboardCharacterResponse);
    }

    @Test
    void queryFactionWarfareCorporationLeaderboards() {
        LeaderboardCorporationResponse leaderboardCorporationResponse = factionWarfareApi.queryFactionWarfareCorporationLeaderboards("serenity").block();
        System.out.println("leaderboardCorporationResponse = " + leaderboardCorporationResponse);
    }

    @Test
    void queryFactionWarfareStats() {
        List<FactionWarfareStatisticsResponse> factionWarfareStatisticsResponses = factionWarfareApi.queryFactionWarfareStats("serenity").collectList().block();
        System.out.println("factionWarfareStatisticsResponses = " + factionWarfareStatisticsResponses);
    }

    @Test
    void queryFactionWarfareSystems() {
        List<FactionWarfareSolarSystemsResponse> factionWarfareSolarSystemsResponses = factionWarfareApi.queryFactionWarfareSystems("serenity").collectList().block();
        System.out.println("factionWarfareSolarSystemsResponses = " + factionWarfareSolarSystemsResponses);
    }

    @Test
    void queryFactionWarfareWars() {
        List<NpcFactionsAtWarResponse> npcFactionsAtWarResponses = factionWarfareApi.queryFactionWarfareWars("serenity").collectList().block();
        System.out.println("npcFactionsAtWarResponses = " + npcFactionsAtWarResponses);
    }
}