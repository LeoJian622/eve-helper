package xyz.foolcat.eve.evehelper.esi.auth;

import cn.hutool.core.lang.Assert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import xyz.foolcat.eve.evehelper.esi.model.AuthTokenResponse;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("ESI AuthorizeOAuth Test")
class AuthorizeOAuthTest {

    @Autowired
    AuthorizeOAuth authorizeOAuth;

    @Test
    void authorizeUrl() {
        String authorizeUrl = authorizeOAuth.authorizeUrl(SsoScopes.ALL);
        Assert.notNull(authorizeUrl);
        System.out.println("authorizeUrl = " + authorizeUrl);
    }

    @Test
    void updateAccessTokenAuthorizationCode() throws InterruptedException {
        AuthTokenResponse block = authorizeOAuth.updateAccessToken(GrantType.AUTHORIZATION_CODE, "Reh4eZtbX0-RCyzWzekq2A").log().block();
        System.out.println("block = " + block);
    }

    @Test
    void updateAccessTokenRefreshToken() throws InterruptedException {
        AuthTokenResponse block = authorizeOAuth.updateAccessToken(GrantType.REFRESH_TOKEN, "odZ2dRCzHUOJCa9KZqILQQ==").log().block();
        System.out.println("block = " + block);
    }

}