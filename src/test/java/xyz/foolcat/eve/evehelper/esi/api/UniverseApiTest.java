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
@DisplayName("ESI Universe Api Test")
class UniverseApiTest {

    @Autowired
    UniverseApi universeApi;

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
    void queryUniverseAncestries() {
        List<AncestriesResponse> ancestriesResponses = universeApi.queryUniverseAncestries("serenity", "zh").collectList().block();
        System.out.println("ancestriesResponses = " + ancestriesResponses);
    }

    @Test
    void queryUniverseAsteroidBelts() {
        AsteroidBeltInformationResponse asteroidBeltInformationResponse = universeApi.queryUniverseAsteroidBelts(40015737, "serenity").block();
        System.out.println("asteroidBeltInformationResponse = " + asteroidBeltInformationResponse);
    }

    @Test
    void queryUniverseBloodlines() {
        List<BloodlineResponse> bloodlineResponses = universeApi.queryUniverseBloodlines("serenity", "zh").collectList().block();
        System.out.println("bloodlineResponses = " + bloodlineResponses);
    }

    @Test
    void queryUniverseCategories() {
        List<Integer> integers = universeApi.queryUniverseCategories("serenity").collectList().block();
        System.out.println("integers = " + integers);
    }

    @Test
    void queryUniverseCategory() {
        CategoryInfoResponse categoryInfoResponse = universeApi.queryUniverseCategory(25, "serenity", "zh").block();
        System.out.println("categoryInfoResponse = " + categoryInfoResponse);
    }

    @Test
    void queryUniverseConstellations() {
        List<Integer> integers = universeApi.queryUniverseConstellations("serenity").collectList().block();
        System.out.println("integers = " + integers);
    }

    @Test
    void queryUniverseConstellation() {
        ConstellationInfoResponse constellationInfoResponse = universeApi.queryUniverseConstellation(22000006, "serenity", "zh").block();
        System.out.println("constellationInfoResponse = " + constellationInfoResponse);
    }

    @Test
    void queryUniverseFactions() {
        List<FactionResponse> factionResponses = universeApi.queryUniverseFactions("serenity", "zh").collectList().block();
        System.out.println("factionResponses = " + factionResponses);
    }

    @Test
    void queryUniverseGraphics() {
        List<Integer> integers = universeApi.queryUniverseGraphics("serenity").collectList().block();
        System.out.println("integers = " + integers);
    }

    @Test
    void queryUniverseGraphic() {
        GraphicResponse graphicResponse = universeApi.queryUniverseGraphic(11291, "serenity", "zh").block();
        System.out.println("graphicResponse = " + graphicResponse);
    }

    @Test
    void queryUniverseGroups() {
        List<Integer> integers = universeApi.queryUniverseGroups("serenity").collectList().block();
        System.out.println("integers = " + integers);
    }

    @Test
    void queryUniverseGroup() {
        GroupInfoResponse groupInfoResponse = universeApi.queryUniverseGroup(1150, "serenity", "zh").block();
        System.out.println("groupInfoResponse = " + groupInfoResponse);
    }

    @Test
    void queryUniverseIds() {
        Name2IdResponse name2IdResponse = universeApi.queryUniverseIds(List.of("Cat9QAQ", "CatTAT"), "serenity", "zh").block();
        System.out.println("name2IdResponse = " + name2IdResponse);
    }

    @Test
    void queryUniverseMoon() {
        MoonInfoResponse moonInfoResponse = universeApi.queryUniverseMoon(40009083, "serenity").block();
        System.out.println("moonInfoResponse = " + moonInfoResponse);
    }

    @Test
    void queryUniverseNames() {
        universeApi.queryUniverseNames(List.of(656880659), "serenity").collectList().block();
    }

    @Test
    void queryUniversePlanet() {
        PlanetInfoResponse planetInfoResponse = universeApi.queryUniversePlanet(40009078, "serenity").block();
        System.out.println("planetInfoResponse = " + planetInfoResponse);
    }

    @Test
    void queryUniverseRaces() {
        List<RaceInfoResponse> raceInfoResponses = universeApi.queryUniverseRaces("serenity", "zh").collectList().block();
        System.out.println("raceInfoResponses = " + raceInfoResponses);
    }

    @Test
    void queryUniverseRegions() {
        List<Integer> integers = universeApi.queryUniverseRegions("serenity").collectList().block();
        System.out.println("integers = " + integers);
    }

    @Test
    void queryUniverseRegion() {
        RegionInfoResponse regionInfoResponse = universeApi.queryUniverseRegion(10000065, "serenity", "zh").block();
        System.out.println("regionInfoResponse = " + regionInfoResponse);
    }

    @Test
    void queryUniverseStargate() {
        StargateInfoResponse stargateInfoResponse = universeApi.queryUniverseStargate(50001248, "serenity").block();
        System.out.println("stargateInfoResponse = " + stargateInfoResponse);
    }

    @Test
    void queryUniverseStar() {
        StarInfoResponse starInfoResponse = universeApi.queryUniverseStar(40009076, "serenity").block();
        System.out.println("starInfoResponse = " + starInfoResponse);
    }

    @Test
    void queryUniverseStation() {
        StationInfoResponse stationInfoResponse = universeApi.queryUniverseStation(60000361, "serenity").block();
        System.out.println("stationInfoResponse = " + stationInfoResponse);
    }

    @Test
    void queryUniverseStructures() {
        List<Long> ids = universeApi.queryUniverseStructures("serenity").collectList().block();
        System.out.println("integers = " + ids);

    }

    @Test
    void queryUniverseStructure() {
        StructureInfoResponse structureInfoResponse = universeApi.queryUniverseStructure(1015148880281L, "serenity", at).block();
        System.out.println("structureInfoResponse = " + structureInfoResponse);
    }

    @Test
    void queryUniverseSystemJumps() {
        List<SolarSystemJumpResponse> solarSystemJumpResponses = universeApi.queryUniverseSystemJumps("serenity").collectList().block();
        System.out.println("solarSystemJumpResponses = " + solarSystemJumpResponses);

    }

    @Test
    void queryUniverseSystemKills() {
        List<SolarSystemKillResponse> solarSystemKillResponses = universeApi.queryUniverseSystemKills("serenity").collectList().block();
        System.out.println("solarSystemKillResponses = " + solarSystemKillResponses);
    }

    @Test
    void queryUniverseSystems() {
        List<Integer> integers = universeApi.queryUniverseSystems("serenity").collectList().block();
        System.out.println("integers = " + integers);
    }

    @Test
    void queryUniverseSystem() {
        SolarSystemInfoResponse solarSystemInfoResponse = universeApi.queryUniverseSystem(30000142, "serenity", "zh").block();
        System.out.println("solarSystemInfoResponse = " + solarSystemInfoResponse);
    }

    @Test
    void queryUniverseTypes() {
        List<Integer> integers = universeApi.queryUniverseTypes("serenity", 1).collectList().block();
        System.out.println("integers = " + integers);
    }

    @Test
    void queryUniverseType() {
        TypeInfoResponse typeInfoResponse = universeApi.queryUniverseType(36951, "serenity", "zh").block();
        System.out.println("typeInfoResponse = " + typeInfoResponse);
    }
}