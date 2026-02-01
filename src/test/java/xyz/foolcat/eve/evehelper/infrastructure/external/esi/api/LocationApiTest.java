package xyz.foolcat.eve.evehelper.infrastructure.external.esi.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.auth.AuthorizeOAuth;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.auth.GrantType;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.AuthTokenResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.CurrentShipResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.LocationResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.OnlineStatusResponse;
import xyz.foolcat.eve.evehelper.shared.util.AuthorizeUtil;

import java.util.Objects;


@ActiveProfiles("test")
@SpringBootTest
@DisplayName("ESI Location Api Test")
class LocationApiTest {

    @Autowired
    LocationApi locationApi;

    @Autowired
    AuthorizeUtil authorizeUtil;

    @Autowired
    AuthorizeOAuth authorizeOAuth;

    String at = "Bearer ";

    @BeforeEach
    void initAccessToken() {
        EveAccount entity = authorizeUtil.authorize( 2112818290);
        Mono<AuthTokenResponse> authTokenResponseMono = authorizeOAuth.updateAccessToken(GrantType.REFRESH_TOKEN, entity.getRefreshToken());
        at = at + Objects.requireNonNull(authTokenResponseMono.block()).getAccessToken();
        System.out.println("at = " + at);
    }

    @Test
    void queryCharacterLocation() {
        LocationResponse locationResponse = locationApi.queryCharacterLocation(2112818290, "serenity", at).block();
        System.out.println("locationResponse = " + locationResponse);
    }

    @Test
    void queryCharacterOnline() {
        OnlineStatusResponse onlineStatusResponse = locationApi.queryCharacterOnline(2112818290, "serenity", at).block();
        System.out.println("onlineStatusResponse = " + onlineStatusResponse);
    }

    @Test
    void queryCharacterShip() {
        CurrentShipResponse currentShipResponse = locationApi.queryCharacterShip(2112818290, "serenity", at).block();
        System.out.println("currentShipResponse = " + currentShipResponse);
    }
}