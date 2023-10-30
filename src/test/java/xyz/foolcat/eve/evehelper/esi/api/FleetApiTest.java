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
import xyz.foolcat.eve.evehelper.esi.model.send.FleetInvitationDetails;
import xyz.foolcat.eve.evehelper.esi.model.send.FleetNewSetting;
import xyz.foolcat.eve.evehelper.service.system.EveAccountService;

import java.util.List;
import java.util.Objects;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("ESI Fleet Api Test")
class FleetApiTest {

    @Autowired
    FleetApi fleetApi;

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
    void queryCharacterFittings() {
        CharacterFleetResponse characterFleetResponse = fleetApi.queryCharacterFittings(2112818290, "serenity", at).block();
        System.out.println("characterFleetResponse = " + characterFleetResponse);
    }

    @Test
    void queryFleet() {
        FleetDetailResponse fleetDetailResponse = fleetApi.queryFleet(1L, "serenity", at).block();
        System.out.println("fleetDetailResponse = " + fleetDetailResponse);
    }

    @Test
    void updateFleet() {
        FleetNewSetting fleetNewSetting = new FleetNewSetting();
        fleetNewSetting.setIsFreeMove(true);
        FleetDetailResponse fleetDetailResponse = fleetApi.updateFleet(1L, "serenity", fleetNewSetting, at).block();
        System.out.println("fleetDetailResponse = " + fleetDetailResponse);
    }

    @Test
    void queryFleetMember() {
        List<FleetMemberResponse> fleetMemberResponseList = fleetApi.queryFleetMember(1L, "serenity", "zh", at).collectList().block();
        System.out.println("fleetMemberResponseList = " + fleetMemberResponseList);
    }

    @Test
    void addFleetMember() {
        FleetInvitationDetails fleetInvitationDetails = new FleetInvitationDetails();
        fleetInvitationDetails.setCharacterId(1);
        fleetInvitationDetails.setRole("squad_member");
        Object o = fleetApi.addFleetMember(1L, "serenity", fleetInvitationDetails, at).block();
        System.out.println("o = " + o);
    }

    @Test
    void deleteFleetMember() {
        Object o = fleetApi.deleteFleetMember(1L, "serenity", 1, at).block();
        System.out.println("o = " + o);
    }

    @Test
    void updateFleetMember() {
        FleetInvitationDetails fleetInvitationDetails = new FleetInvitationDetails();
        fleetInvitationDetails.setRole("squad_commander");
        Object o = fleetApi.updateFleetMember(1L, "serenity", 1, fleetInvitationDetails, at).block();
        System.out.println("o = " + o);
    }

    @Test
    void deleteFleetSquad() {
        Object block = fleetApi.deleteFleetSquad(1L, "serenity", 1L, at).block();
        System.out.println("block = " + block);
    }

    @Test
    void updateFleetSquadRename() {
        Object block = fleetApi.updateFleetSquadRename(1L, "serenity", 1L, "测试中队", at).block();
        System.out.println("block = " + block);
    }

    @Test
    void queryFleetWings() {
        List<WingResponse> wingResponses = fleetApi.queryFleetWings(1L, "serenity", at).collectList().block();
        System.out.println("wingResponses = " + wingResponses);
    }

    @Test
    void addFleetWing() {
        Long block = fleetApi.addFleetWing(1L, "serenity", at).block();
        System.out.println("block = " + block);
    }

    @Test
    void deleteFleetWing() {
        Object block = fleetApi.deleteFleetWing(1L, "serenity", 1L, at).block();
        System.out.println("block = " + block);
    }

    @Test
    void updateFleetWingRename() {
        Object block = fleetApi.updateFleetWingRename(1L, "serenity", 1L, "测试中队", at).block();
        System.out.println("block = " + block);
    }

    @Test
    void addFleetWingSquad() {
        Long block = fleetApi.addFleetWingSquad(1L, "serenity", 1L, at).block();
        System.out.println("block = " + block);
    }
}