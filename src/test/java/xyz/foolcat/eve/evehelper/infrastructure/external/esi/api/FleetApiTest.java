package xyz.foolcat.eve.evehelper.infrastructure.external.esi.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.auth.AuthorizeOAuth;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.auth.GrantType;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.*;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.send.FleetInvitationDetails;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.send.FleetNewSetting;
import xyz.foolcat.eve.evehelper.domain.util.AuthorizeUtil;
import xyz.foolcat.eve.evehelper.shared.kernel.constants.GlobalConstants;

import java.util.List;
import java.util.Objects;



@SpringBootTest
@ActiveProfiles("test")
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
        EveAccount entity = authorizeUtil.authorizeInternal(GlobalConstants.SYSTEM_USER_ID, 2112818290);
        Mono<AuthTokenResponse> authTokenResponseMono = authorizeOAuth.updateAccessToken(GrantType.REFRESH_TOKEN, entity.getRefreshToken());
        at = at + Objects.requireNonNull(authTokenResponseMono.block()).getAccessToken();
        System.out.println("at = " + at);
    }

    /**
     * 硬编码 ESI 资源(舰队/成员等)ID 不存在(404)，需真实数据，@Disabled 记环境数据依赖
     */
    @Test
    @Disabled("硬编码 ESI 资源(舰队/成员等)ID 不存在(404)，需真实数据，@Disabled 记环境数据依赖")
    void queryCharacterFittings() {
        CharacterFleetResponse characterFleetResponse = fleetApi.queryCharacterFittings(2112818290, "serenity", at).block();
        System.out.println("characterFleetResponse = " + characterFleetResponse);
    }

    /**
     * 硬编码 ESI 资源(舰队/成员等)ID 不存在(404)，需真实数据，@Disabled 记环境数据依赖
     */
    @Test
    @Disabled("硬编码 ESI 资源(舰队/成员等)ID 不存在(404)，需真实数据，@Disabled 记环境数据依赖")
    void queryFleet() {
        FleetDetailResponse fleetDetailResponse = fleetApi.queryFleet(1022810945368L, "serenity", at).block();
        System.out.println("fleetDetailResponse = " + fleetDetailResponse);
    }

    /**
     * 硬编码 ESI 资源(舰队/成员等)ID 不存在(404)，需真实数据，@Disabled 记环境数据依赖
     */
    @Test
    @Disabled("硬编码 ESI 资源(舰队/成员等)ID 不存在(404)，需真实数据，@Disabled 记环境数据依赖")
    void updateFleet() {
        FleetNewSetting fleetNewSetting = new FleetNewSetting();
        fleetNewSetting.setIsFreeMove(true);
        FleetDetailResponse fleetDetailResponse = fleetApi.updateFleet(1022810945368L, "serenity", fleetNewSetting, at).block();
        System.out.println("fleetDetailResponse = " + fleetDetailResponse);
    }

    /**
     * 硬编码 ESI 资源(舰队/成员等)ID 不存在(404)，需真实数据，@Disabled 记环境数据依赖
     */
    @Test
    @Disabled("硬编码 ESI 资源(舰队/成员等)ID 不存在(404)，需真实数据，@Disabled 记环境数据依赖")
    void queryFleetMember() {
        List<FleetMemberResponse> fleetMemberResponseList = fleetApi.queryFleetMember(1022810945368L, "serenity", "zh", at).collectList().block();
        System.out.println("fleetMemberResponseList = " + fleetMemberResponseList);
    }

    /**
     * 硬编码 ESI 资源(舰队/成员等)ID 不存在(404)，需真实数据，@Disabled 记环境数据依赖
     */
    @Test
    @Disabled("硬编码 ESI 资源(舰队/成员等)ID 不存在(404)，需真实数据，@Disabled 记环境数据依赖")
    void addFleetMember() {
        FleetInvitationDetails fleetInvitationDetails = new FleetInvitationDetails();
        fleetInvitationDetails.setCharacterId(2112832425);
        fleetInvitationDetails.setRole("squad_member");
//        fleetInvitationDetails.setSquadId(1L);
//        fleetInvitationDetails.setWingId(1L);
        Object o = fleetApi.addFleetMember(1022810945368L, "serenity", fleetInvitationDetails, at).block();
        System.out.println("o = " + o);
    }

    /**
     * 硬编码 ESI 资源(舰队/成员等)ID 不存在(404)，需真实数据，@Disabled 记环境数据依赖
     */
    @Test
    @Disabled("硬编码 ESI 资源(舰队/成员等)ID 不存在(404)，需真实数据，@Disabled 记环境数据依赖")
    void deleteFleetMember() {
        Object o = fleetApi.deleteFleetMember(1022810945368L, "serenity", 2112832425, at).block();
        System.out.println("o = " + o);
    }

    /**
     * 硬编码 ESI 资源(舰队/成员等)ID 不存在(404)，需真实数据，@Disabled 记环境数据依赖
     */
    @Test
    @Disabled("硬编码 ESI 资源(舰队/成员等)ID 不存在(404)，需真实数据，@Disabled 记环境数据依赖")
    void updateFleetMember() {
        FleetInvitationDetails fleetInvitationDetails = new FleetInvitationDetails();
        fleetInvitationDetails.setRole("squad_commander");
        fleetInvitationDetails.setSquadId(3052010945368L);
        fleetInvitationDetails.setWingId(2027110945368L);
        Object o = fleetApi.updateFleetMember(1022810945368L, "serenity", 2112832425, fleetInvitationDetails, at).block();
        System.out.println("o = " + o);
    }

    /**
     * 硬编码 ESI 资源(舰队/成员等)ID 不存在(404)，需真实数据，@Disabled 记环境数据依赖
     */
    @Test
    @Disabled("硬编码 ESI 资源(舰队/成员等)ID 不存在(404)，需真实数据，@Disabled 记环境数据依赖")
    void deleteFleetSquad() {
        Object block = fleetApi.deleteFleetSquad(1022810945368L, "serenity", 3057910945368L, at).block();
        System.out.println("block = " + block);
    }

    /**
     * 硬编码 ESI 资源(舰队/成员等)ID 不存在(404)，需真实数据，@Disabled 记环境数据依赖
     */
    @Test
    @Disabled("硬编码 ESI 资源(舰队/成员等)ID 不存在(404)，需真实数据，@Disabled 记环境数据依赖")
    void updateFleetSquadRename() {
        Object block = fleetApi.updateFleetSquadRename(1022810945368L, "serenity", 3052010945368L, "ABC", at).block();
        System.out.println("block = " + block);
    }

    /**
     * 硬编码 ESI 资源(舰队/成员等)ID 不存在(404)，需真实数据，@Disabled 记环境数据依赖
     */
    @Test
    @Disabled("硬编码 ESI 资源(舰队/成员等)ID 不存在(404)，需真实数据，@Disabled 记环境数据依赖")
    void queryFleetWings() {
        List<WingResponse> wingResponses = fleetApi.queryFleetWings(1022810945368L, "serenity", at).collectList().block();
        System.out.println("wingResponses = " + wingResponses);
    }

    /**
     * 硬编码 ESI 资源(舰队/成员等)ID 不存在(404)，需真实数据，@Disabled 记环境数据依赖
     */
    @Test
    @Disabled("硬编码 ESI 资源(舰队/成员等)ID 不存在(404)，需真实数据，@Disabled 记环境数据依赖")
    void addFleetWing() {
        NewWingResponse newWingResponse = fleetApi.addFleetWing(1022810945368L, "serenity", at).block();
        System.out.println("newWingResponse = " + newWingResponse);

    }

    /**
     * 硬编码 ESI 资源(舰队/成员等)ID 不存在(404)，需真实数据，@Disabled 记环境数据依赖
     */
    @Test
    @Disabled("硬编码 ESI 资源(舰队/成员等)ID 不存在(404)，需真实数据，@Disabled 记环境数据依赖")
    void deleteFleetWing() {
        Object block = fleetApi.deleteFleetWing(1022810945368L, "serenity", 2029210945368L, at).block();
        System.out.println("block = " + block);
    }

    /**
     * 硬编码 ESI 资源(舰队/成员等)ID 不存在(404)，需真实数据，@Disabled 记环境数据依赖
     */
    @Test
    @Disabled("硬编码 ESI 资源(舰队/成员等)ID 不存在(404)，需真实数据，@Disabled 记环境数据依赖")
    void updateFleetWingRename() {
        Object block = fleetApi.updateFleetWingRename(1022810945368L, "serenity", 2029510945368L, "ABC", at).block();
        System.out.println("block = " + block);
    }

    /**
     * 硬编码 ESI 资源(舰队/成员等)ID 不存在(404)，需真实数据，@Disabled 记环境数据依赖
     */
    @Test
    @Disabled("硬编码 ESI 资源(舰队/成员等)ID 不存在(404)，需真实数据，@Disabled 记环境数据依赖")
    void addFleetWingSquad() {
        NewSquadResponse block = fleetApi.addFleetWingSquad(1022810945368L, "serenity", 2029510945368L, at).block();
        System.out.println("block = " + block);
    }
}