package xyz.foolcat.eve.evehelper.infrastructure.external.esi.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.api.KillMailApi;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.auth.AuthorizeOAuth;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.auth.GrantType;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.AuthTokenResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.KillMailResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.KillMailsIdAndHashResponse;
import xyz.foolcat.eve.evehelper.shared.util.AuthorizeUtil;

import java.util.List;
import java.util.Objects;


@ActiveProfiles("test")
@SpringBootTest
@DisplayName("ESI Industry Api Test")
class KillMailApiTest {

    @Autowired
    KillMailApi killMailApi;

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
    void queryCharacterKillMail() {
        List<KillMailsIdAndHashResponse> killMailListResponse = killMailApi.queryCharacterKillMail(2112818290, "serenity", 1, at).collectList().block();
        System.out.println("killMailsResponses = " + killMailListResponse);
    }

    @Test
    void queryCorporationKillMail() {
        List<KillMailsIdAndHashResponse> killMailListResponse = killMailApi.queryCorporationKillMail(656880659, "serenity", 1, at).collectList().block();
        System.out.println("killMailsResponses = " + killMailListResponse);
    }

    @Test
    void queryKillMailDetail() {
        KillMailResponse killMailResponse = killMailApi.queryKillMailDetail(19594691, "serenity", "0c12e5673682875fcb0234b218143e4a9e9c7197").block();
        System.out.println("killMailResponse = " + killMailResponse);
    }
}