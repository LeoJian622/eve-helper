package xyz.foolcat.eve.evehelper.esi.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.api.UserInterfaceApi;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.auth.AuthorizeOAuth;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.auth.GrantType;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.AuthTokenResponse;
import xyz.foolcat.eve.evehelper.shared.util.AuthorizeUtil;

import java.util.List;
import java.util.Objects;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("ESI UserInterface Api Test")
class UserInterfaceApiTest {

    @Autowired
    UserInterfaceApi userInterfaceApi;

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
    void addWaypoint() {
        userInterfaceApi.addWaypoint("serenity", true, true, 1014461781154L, at).block();
        userInterfaceApi.addWaypoint("serenity", false, false, 60003760L, at).block();
    }

    @Test
    void openContract() {
        userInterfaceApi.openContract("serenity", 54923597, at).block();
    }

    @Test
    void openInformation() {
        userInterfaceApi.openInformation("serenity", 2112818290, at).block();
    }

    @Test
    void openMarketDetails() {
        userInterfaceApi.openMarketDetails("serenity", 16274, at).block();
    }

    @Test
    void openNewMail() {
        xyz.foolcat.eve.evehelper.esi.model.send.NewMailUI newMailUI = new xyz.foolcat.eve.evehelper.esi.model.send.NewMailUI();
        newMailUI.setRecipients(List.of(2112818290));
        newMailUI.setSubject("test");
        newMailUI.setBody("测试");
        newMailUI.setToCorpOrAllianceId(656880659);
        userInterfaceApi.openNewMail("serenity", newMailUI, at).block();
    }
}