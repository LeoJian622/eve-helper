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
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("ESI Corporation Api Test")
class CorporationApiTest {

    @Autowired
    CorporationApi corporationApi;

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
    void queryCorporation() {
        CorporationResponse corporationResponse = corporationApi.queryCorporation(98061457, "serenity").block();
        System.out.println("corporationResponse = " + corporationResponse);
    }

    @Test
    void queryCorporationAllianceHistory() {
        List<AllianceHistoryResponse> allianceHistoryResponses = corporationApi.queryCorporationAllianceHistory(656880659, "serenity").collectList().block();
        System.out.println("allianceHistoryResponses = " + allianceHistoryResponses);
    }

    @Test
    void queryCorporationBlueprints() {
        List<BlueprintResponse> blueprintResponses = corporationApi.queryCorporationBlueprints(656880659, "serenity", 1, at).collectList().block();
        System.out.println("blueprintResponses = " + blueprintResponses);
    }

    @Test
    void queryCorporationContainersLogs() {
        List<ContainersLogsResponse> containersLogsResponses = corporationApi.queryCorporationContainersLogs(656880659, "serenity", 1, at).collectList().block();
        System.out.println("containersLogsResponses = " + containersLogsResponses);
    }

    @Test
    void queryCorporationDivisions() {
        List<DivisionNamesResponse> divisionNamesResponses = corporationApi.queryCorporationDivisions(656880659, "serenity", at).collectList().block();
        System.out.println("divisionNamesResponses = " + divisionNamesResponses);
    }

    @Test
    void queryCorporationFacilities() {
        List<CorporationFacilitiesResponse> corporationFacilitiesRespons = corporationApi.queryCorporationFacilities(656880659, "serenity", at).collectList().block();
        System.out.println("facilitiesResponses = " + corporationFacilitiesRespons);
    }

    @Test
    void queryCorporationIcons() {
        IconResponse iconResponse = corporationApi.queryCorporationIcons(656880659, "serenity").block();
        System.out.println("iconResponse = " + iconResponse);
    }

    @Test
    void queryCorporationMedals() {
        List<MedalResponse> medalResponses = corporationApi.queryCorporationMedals(656880659, "serenity", 1, at).collectList().block();
        System.out.println("medalResponses = " + medalResponses);
    }

    @Test
    void queryCorporationIssuedMedals() {
        List<MedalResponse> medalResponses = corporationApi.queryCorporationIssuedMedals(656880659, "serenity", 1, at).collectList().block();
        System.out.println("medalResponses = " + medalResponses);
    }

    @Test
    void queryCorporationMembers() {
        List<Long> longs = corporationApi.queryCorporationMembers(656880659, "serenity", at).collectList().block();
        System.out.println("longs = " + longs);
    }

    @Test
    void queryCorporationMembersLimit() {
        Integer membersLimit = corporationApi.queryCorporationMembersLimit(656880659, "serenity", at).block();
        System.out.println("membersLimit = " + membersLimit);
    }

    @Test
    void queryCorporationMembersTitles() {
        List<MemberTitleResponse> memberTitleResponses = corporationApi.queryCorporationMembersTitles(656880659, "serenity", at).collectList().block();
        System.out.println("memberTitleResponses = " + memberTitleResponses);
    }

    @Test
    void queryCorporationMembersTracking() {
        List<MemberTrackingResponse> memberTrackingResponses = corporationApi.queryCorporationMembersTracking(656880659, "serenity", at).collectList().block();
        System.out.println("memberTrackingResponses = " + memberTrackingResponses);
    }

    @Test
    void queryCorporationMembersRoles() {
        List<MemberRolesResponse> memberRolesResponses = corporationApi.queryCorporationMembersRoles(656880659, "serenity", at).collectList().block();
        System.out.println("memberRolesResponses = " + memberRolesResponses);
    }

    @Test
    void queryCorporationMembersRolesHistory() {
    }

    @Test
    void queryCorporationShareHolders() {
    }

    @Test
    void queryCorporationStanding() {
    }

    @Test
    void queryCorporationStarBases() {
    }

    @Test
    void queryCorporationStarBase() {
    }

    @Test
    void queryCorporationStructures() {
        List<StructuresInformationResponse> structuresInformationResponses = corporationApi.queryCorporationStructures(656880659, "serenity", "zh", 1, at).collectList().block();
        System.out.println("structuresInformationResponses = " + structuresInformationResponses);
    }

    @Test
    void queryCorporationTitles() {
    }

    @Test
    void queryNpcCorporation() {
    }
}