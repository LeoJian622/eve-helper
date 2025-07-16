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
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.AuthTokenResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.LoyaltyPointsResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.OfferResponse;
import xyz.foolcat.eve.evehelper.domain.service.system.EveAccountService;

import java.util.List;
import java.util.Objects;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("ESI Loyalty Api Test")
class LoyaltyApiTest {

    @Autowired
    LoyaltyApi loyaltyApi;

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
    void queryCharacterLoyaltyPoints() {
        List<LoyaltyPointsResponse> loyaltyPointsResponses = loyaltyApi.queryCharacterLoyaltyPoints(2112818290, "serenity", at).collectList().block();
        System.out.println("loyaltyPointsResponses = " + loyaltyPointsResponses);
    }

    @Test
    void queryCorporationLoyaltyPoints() {
        List<OfferResponse> offerResponses = loyaltyApi.queryCorporationLoyaltyPoints(1000035, "serenity").collectList().block();
        System.out.println("offerResponses = " + offerResponses);
    }
}