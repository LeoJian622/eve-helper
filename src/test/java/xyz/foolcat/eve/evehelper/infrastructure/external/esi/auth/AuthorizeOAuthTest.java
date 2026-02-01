package xyz.foolcat.eve.evehelper.infrastructure.external.esi.auth;

import cn.hutool.core.lang.Assert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.auth.AuthorizeOAuth;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.auth.GrantType;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.AuthTokenResponse;

import java.util.Set;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("ESI AuthorizeOAuth Test")
@WithUserDetails("user1")
class AuthorizeOAuthTest {

    @Autowired
    AuthorizeOAuth authorizeOAuth;

    @Test
    void authorizeUrl() {
//        String authorizeUrl = authorizeOAuth.authorizeUrl(SsoScopes.ALL);
        String authorizeUrl = authorizeOAuth.authorizeUrl(Set.of("esi-clones.read_clones.v1",
                "esi-location.read_location.v1",
                "esi-location.read_online.v1",
                "esi-location.read_ship_type.v1"));
        Assert.notNull(authorizeUrl);
        System.out.println("authorizeUrl = " + authorizeUrl);
    }
    @Test
    void updateAccessTokenAuthorizationCode() {
        AuthTokenResponse block = authorizeOAuth.updateAccessToken(GrantType.AUTHORIZATION_CODE, "cBhCan6IUEK97cmhWN5lJg").log().block();
        System.out.println("block = " + block);
    }

    @Test
    void updateAccessTokenRefreshToken() {
        AuthTokenResponse block = authorizeOAuth.updateAccessToken(GrantType.REFRESH_TOKEN, "odZ2dRCzHUOJCa9KZqILQQ==").log().block();
        System.out.println("block = " + block);
    }

}