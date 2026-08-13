package xyz.foolcat.eve.evehelper.infrastructure.external.esi.auth;

import cn.hutool.core.lang.Assert;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.AuthTokenResponse;

import java.util.Set;


@SpringBootTest
@ActiveProfiles("test")
@DisplayName("ESI AuthorizeOAuth Test")
@WithUserDetails("admin")
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
    /**
     * 禁用：此测试用硬编码的一次性授权码真实调 ESI，授权码已失效返回 400，
     * 无法在测试环境稳定通过。需真实有效的一次性授权码时，替换 code 后临时启用。
     */
    @Test
    @Disabled("需真实有效的一次性授权码，硬编码 code 已失效")
    void updateAccessTokenAuthorizationCode() {
        AuthTokenResponse block = authorizeOAuth.updateAccessToken(GrantType.AUTHORIZATION_CODE, "cBhCan6IUEK97cmhWN5lJg").log().block();
        System.out.println("block = " + block);
    }

    /**
     * 禁用：此测试用硬编码 refresh token 真实调 ESI，凭据无效返回 400，
     * 无法在测试环境稳定通过。需真实凭据时替换后临时启用。
     */
    @Test
    @Disabled("需真实有效的 refresh token，硬编码 token 无效")
    void updateAccessTokenRefreshToken() {
        AuthTokenResponse block = authorizeOAuth.updateAccessToken(GrantType.REFRESH_TOKEN, "odZ2dRCzHUOJCa9KZqILQQ==").log().block();
        System.out.println("block = " + block);
    }

}