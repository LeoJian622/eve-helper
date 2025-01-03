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
@SpringBootTest
@DisplayName("ESI PlanetaryInteraction Api Test")
class PlanetaryInteractionApiTest {

    @Autowired
    PlanetaryInteractionApi planetaryInteractionApi;

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
    void queryCharacterPlanets() {
        List<ColonyResponse> colonyResponses = planetaryInteractionApi.queryCharacterPlanets(2112818290, "serenity", at).collectList().block();
        System.out.println("colonyResponses = " + colonyResponses);
    }

    @Test
    void queryCharacterPlanet() {
        ColonyLayoutResponse colonyLayoutResponse = planetaryInteractionApi.queryCharacterPlanet(2112818290, "serenity", 40015632, at).block();
        System.out.println("colonyLayoutResponse = " + colonyLayoutResponse);
    }

    @Test
    void queryCorporationCustomsOffices() {
        List<CustomsOfficesSettingResponse> customsOfficesSettingResponses = planetaryInteractionApi.queryCorporationCustomsOffices(656880659, "serenity", at).collectList().block();
        System.out.println("customsOfficesSettingResponses = " + customsOfficesSettingResponses);
    }

    @Test
    void queryUniverseSchematic() {
        FactorySchematicResponse factorySchematicResponse = planetaryInteractionApi.queryUniverseSchematic(122, "serenity").block();
        System.out.println("factorySchematicResponse = " + factorySchematicResponse);
    }
}