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
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.api.FleetApi;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.auth.AuthorizeOAuth;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.auth.GrantType;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.*;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.send.FleetInvitationDetails;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.send.FleetNewSetting;
import xyz.foolcat.eve.evehelper.shared.util.AuthorizeUtil;

import java.util.List;
import java.util.Objects;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("ESI Fleet Api Test")
class FleetApiTest {

    @Autowired
    FleetApi fleetApi;

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
    void queryCharacterFittings() {
        CharacterFleetResponse characterFleetResponse = fleetApi.queryCharacterFittings(2112818290, "serenity", at).block();
        System.out.println("characterFleetResponse = " + characterFleetResponse);
    }

    @Test
    void queryFleet() {
        FleetDetailResponse fleetDetailResponse = fleetApi.queryFleet(1022810945368L, "serenity", at).block();
        System.out.println("fleetDetailResponse = " + fleetDetailResponse);
    }

    @Test
    void updateFleet() {
        FleetNewSetting fleetNewSetting = new FleetNewSetting();
        fleetNewSetting.setIsFreeMove(true);
        FleetDetailResponse fleetDetailResponse = fleetApi.updateFleet(1022810945368L, "serenity", fleetNewSetting, at).block();
        System.out.println("fleetDetailResponse = " + fleetDetailResponse);
    }

    @Test
    void queryFleetMember() {
        List<FleetMemberResponse> fleetMemberResponseList = fleetApi.queryFleetMember(1022810945368L, "serenity", "zh", at).collectList().block();
        System.out.println("fleetMemberResponseList = " + fleetMemberResponseList);
    }

    @Test
    void addFleetMember() {
        FleetInvitationDetails fleetInvitationDetails = new FleetInvitationDetails();
        fleetInvitationDetails.setCharacterId(2112832425);
        fleetInvitationDetails.setRole("squad_member");
//        fleetInvitationDetails.setSquadId(1L);
//        fleetInvitationDetails.setWingId(1L);
        Object o = fleetApi.addFleetMember(1022810945368L, "serenity", fleetInvitationDetails, at).block();
        System.out.println("o = " + o);
    }

    @Test
    void deleteFleetMember() {
        Object o = fleetApi.deleteFleetMember(1022810945368L, "serenity", 2112832425, at).block();
        System.out.println("o = " + o);
    }

    @Test
    void updateFleetMember() {
        FleetInvitationDetails fleetInvitationDetails = new FleetInvitationDetails();
        fleetInvitationDetails.setRole("squad_commander");
        fleetInvitationDetails.setSquadId(3052010945368L);
        fleetInvitationDetails.setWingId(2027110945368L);
        Object o = fleetApi.updateFleetMember(1022810945368L, "serenity", 2112832425, fleetInvitationDetails, at).block();
        System.out.println("o = " + o);
    }

    @Test
    void deleteFleetSquad() {
        Object block = fleetApi.deleteFleetSquad(1022810945368L, "serenity", 3057910945368L, at).block();
        System.out.println("block = " + block);
    }

    @Test
    void updateFleetSquadRename() {
        Object block = fleetApi.updateFleetSquadRename(1022810945368L, "serenity", 3052010945368L, "ABC", at).block();
        System.out.println("block = " + block);
    }

    @Test
    void queryFleetWings() {
        List<WingResponse> wingResponses = fleetApi.queryFleetWings(1022810945368L, "serenity", at).collectList().block();
        System.out.println("wingResponses = " + wingResponses);
    }

    @Test
    void addFleetWing() {
        NewWingResponse newWingResponse = fleetApi.addFleetWing(1022810945368L, "serenity", at).block();
        System.out.println("newWingResponse = " + newWingResponse);

    }

    @Test
    void deleteFleetWing() {
        Object block = fleetApi.deleteFleetWing(1022810945368L, "serenity", 2029210945368L, at).block();
        System.out.println("block = " + block);
    }

    @Test
    void updateFleetWingRename() {
        Object block = fleetApi.updateFleetWingRename(1022810945368L, "serenity", 2029510945368L, "ABC", at).block();
        System.out.println("block = " + block);
    }

    @Test
    void addFleetWingSquad() {
        NewSquadResponse block = fleetApi.addFleetWingSquad(1022810945368L, "serenity", 2029510945368L, at).block();
        System.out.println("block = " + block);
    }
}