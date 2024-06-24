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
import xyz.foolcat.eve.evehelper.esi.model.AssertResponse;
import xyz.foolcat.eve.evehelper.esi.model.AssetsLocationResponse;
import xyz.foolcat.eve.evehelper.esi.model.AssetsNameResponse;
import xyz.foolcat.eve.evehelper.esi.model.AuthTokenResponse;
import xyz.foolcat.eve.evehelper.service.system.EveAccountService;

import java.util.List;
import java.util.Objects;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("ESI Assets Api Test")
class AssetsApiTest {

    @Autowired
    AssetsApi assetsApi;

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
    void queryCharacterAssetsById() {
        List<AssertResponse> block = assetsApi.queryCharactersAssets(2112818290, "serenity", 1, at).collectList().block();
        System.out.println("block = " + block);
    }

    @Test
    void queryCharacterAssetsLocations() {
        List<Long> itemIds = List.of(1005714787537L, 1006072740228L, 1006142100083L);

        List<AssetsLocationResponse> block = assetsApi.queryCharactersAssetsLocations(2112818290, "serenity", itemIds, at).collectList().block();
        System.out.println("block = " + block);
    }

    @Test
    void queryCharacterAssetsNames() {
        List<Long> itemIds = List.of(1005714787537L, 1006072740228L, 1006142100083L);

        List<AssetsNameResponse> block = assetsApi.queryCharactersAssetsNames(2112818290, "serenity", itemIds, at).collectList().block();
        System.out.println("block = " + block);
    }

    @Test
    void queryCharactersAssets() {
    }

    @Test
    void queryCharactersAssetsLocations() {
    }

    @Test
    void queryCharactersAssetsNames() {
    }

    @Test
    void queryCorporationsAssets() {
        List<AssertResponse> assertResponseList = assetsApi.queryCorporationsAssets(656880659, "serenity", 1, at).collectList().block();
        System.out.println("assertResponseList = " + assertResponseList);
    }

    @Test
    void queryCorporationsAssetsLocations() {
    }

    @Test
    void queryCorporationsAssetsNames() {
    }
}