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
import xyz.foolcat.eve.evehelper.esi.model.DogmaAttributeResponse;
import xyz.foolcat.eve.evehelper.esi.model.DogmaEffectResponse;
import xyz.foolcat.eve.evehelper.service.system.EveAccountService;

import java.util.List;
import java.util.Objects;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("ESI Dogma Api Test")
class DogmaApiTest {

    @Autowired
    DogmaApi dogmaApi;

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
    void queryAttributes() {
        List<Integer> integers = dogmaApi.queryAttributes("serenity").collectList().block();
        System.out.println("integers = " + integers);
    }

    @Test
    void queryAttribute() {
        DogmaAttributeResponse dogmaAttributeResponse = dogmaApi.queryAttribute(5474, "serenity").block();
        System.out.println("dogmaAttributeResponse = " + dogmaAttributeResponse);
    }


    @Test
    void queryEffects() {
        List<Integer> integers = dogmaApi.queryEffects("serenity").collectList().block();
        System.out.println("integers = " + integers);
    }

    @Test
    void queryEffect() {
        DogmaEffectResponse dogmaEffectResponse = dogmaApi.queryEffect(6511, "serenity").block();
        System.out.println("dogmaEffectResponse = " + dogmaEffectResponse);
    }
}