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

import java.util.Arrays;
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
    void queryCharacter() {
        CharacterPublicInfoResponse characterPublicInfoResponse = characterApi.queryCharacter(2112818290L, "serenity", at).block();
        System.out.println("characterPublicInfoResponse = " + characterPublicInfoResponse);
    }

    @Test
    void queryCharacterAgentsResearch() {
        List<AgentsResearchResponse> agentsResearchResponses = characterApi.queryCharacterAgentsResearch(2112818290L, "serenity", at).collectList().block();
        System.out.println("agentsResearchResponses = " + agentsResearchResponses);
    }

    @Test
    void queryCharacterBlueprint() {
        List<BlueprintResponse> blueprintResponses = characterApi.queryCharacterBlueprint(2112818290L, "serenity", 1, at).collectList().block();
        System.out.println("blueprintResponses = " + blueprintResponses);
    }

    @Test
    void queryCharacterCorporationHistory() {
        List<CorporationHistoryResponse> corporationHistoryResponses = characterApi.queryCharacterCorporationHistory(2112818290L, "serenity", at).collectList().block();
        System.out.println("corporationHistoryResponses = " + corporationHistoryResponses);
    }

    @Test
    void queryCharacterCspa() {
        Float aFloat = characterApi.queryCharacterCspa(2112818290L, "serenity", List.of(2112832425, 2112965519, 91424410), at).block();
        System.out.println("aFloat = " + aFloat);
    }

    @Test
    void queryCharacterFatigue() {
        FatigueResponse fatigueResponse = characterApi.queryCharacterFatigue(2112818290L, "serenity", at).block();
        System.out.println("fatigueResponse = " + fatigueResponse);
    }

    @Test
    void queryCharacterMedals() {
        List<MedalResponse> fatigueResponses = characterApi.queryCharacterMedals(2112818290L, "serenity", at).collectList().block();
        System.out.println("fatigueResponses = " + fatigueResponses);
    }

    @Test
    void queryCharacterNotification() {
        List<NotificationResponse> notificationResponses = characterApi.queryCharacterNotification(2112818290L, "serenity", at).collectList().block();
        System.out.println("notificationResponses = " + notificationResponses);
    }

    @Test
    void queryCharacterNotificationContact() {
        List<NotificationContactResponse> notificationContactResponses = characterApi.queryCharacterNotificationContact(2112818290L, "serenity", at).collectList().block();
        System.out.println("notificationContactResponses = " + notificationContactResponses);
    }

    @Test
    void queryCharacterPortrait() {
        IconResponse iconResponse = characterApi.queryCharacterPortrait(2112818290L, "serenity", at).block();
        System.out.println("iconResponse = " + iconResponse);
    }

    @Test
    void queryCharacterRoles() {
        RoleResponse roleResponse = characterApi.queryCharacterRoles(2112818290L, "serenity", at).block();
        System.out.println("roleResponse = " + roleResponse);
    }

    @Test
    void queryCharacterStanding() {
        List<StandingResponse> standingResponses = characterApi.queryCharacterStanding(2112818290L, "serenity", at).collectList().block();
        System.out.println("standingResponses = " + standingResponses);
    }

    @Test
    void queryCharacterTitle() {
        List<TitleResponse> titleResponses = characterApi.queryCharacterTitle(2112818290L, "serenity", at).collectList().block();
        System.out.println("titleResponses = " + titleResponses);
    }

    @Test
    void queryCharacterAffiliation() {
        List<AffiliationResponse> affiliationResponses = characterApi.queryCharacterAffiliation("serenity", List.of(2112832425, 2112965519, 91424410), at).collectList().block();
        System.out.println("affiliationResponses = " + affiliationResponses);
    }
}