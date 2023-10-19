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
import xyz.foolcat.eve.evehelper.esi.model.AgentsResearchResponse;
import xyz.foolcat.eve.evehelper.esi.model.AuthTokenResponse;
import xyz.foolcat.eve.evehelper.esi.model.CharacterPublicInfoResponse;
import xyz.foolcat.eve.evehelper.service.system.EveAccountService;

import java.util.List;
import java.util.Objects;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("ESI Character Api Test")
class CharacterApiTest {

    @Autowired
    CharacterApi characterApi;

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
    void queryCharactersCalendarEventId() {
        CharacterPublicInfoResponse characterPublicInfoResponse = characterApi.queryCharacter(2112818290L, "serenity", at).block();
        System.out.println("characterPublicInfoResponse = " + characterPublicInfoResponse);
    }

    @Test
    void queryCharacterAgentsResearch() {
        List<AgentsResearchResponse> agentsResearchResponses = characterApi.queryCharacterAgentsResearch(2112818290L, "serenity", at).collectList().block();
        System.out.println("agentsResearchResponses = " + agentsResearchResponses);
    }
}