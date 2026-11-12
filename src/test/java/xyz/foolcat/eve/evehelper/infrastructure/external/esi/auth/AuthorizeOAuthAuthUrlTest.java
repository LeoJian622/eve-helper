package xyz.foolcat.eve.evehelper.infrastructure.external.esi.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.EsiClientProperties;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;
import xyz.foolcat.eve.evehelper.shared.result.ResultCode;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * 授权 URL 校验内聚单元测试（T043，3 用例）。
 * <p>
 * 纯单元：直接 new EsiClientProperties（默认 datasource=serenity）+ new AuthorizeOAuth。
 *
 * @author Leojian
 */
@DisplayName("授权 URL 校验内聚测试(T043)")
class AuthorizeOAuthAuthUrlTest {

    private AuthorizeOAuth authorizeOAuth;

    @BeforeEach
    void setUp() {
        EsiClientProperties props = new EsiClientProperties();
        authorizeOAuth = new AuthorizeOAuth(props, mock(WebClient.class));
    }

    // ── AC: 含所有必需 scopes ──

    @Test
    @DisplayName("authUrl_containsAllRequiredScopes: URL 含 state=eve-helper + scope 解码后==SsoScopes.ALL")
    void authUrl_containsAllRequiredScopes() {
        String url = authorizeOAuth.authorizeUrlForServer("serenity", SsoScopes.ALL);

        assertThat(url).contains("state=eve-helper");

        // 从 URL query 中解析 scope 参数（用 URLDecoder 解析，不依赖 encode 实现细节）
        String query = url.substring(url.indexOf('?') + 1);
        String scopeParam = Arrays.stream(query.split("&"))
                .filter(p -> p.startsWith("scope="))
                .map(p -> p.substring("scope=".length()))
                .findFirst()
                .orElseThrow();
        String decoded = URLDecoder.decode(scopeParam, StandardCharsets.UTF_8);
        Set<String> actualScopes = new HashSet<>(Arrays.asList(decoded.split(" ")));
        assertThat(actualScopes).isEqualTo(SsoScopes.ALL);
    }

    // ── AC: 默认服务器使用配置 datasource ──

    @Test
    @DisplayName("authUrl_defaultServer_usesConfiguredDatasource: null/空/serenity 三者返回相等且不抛")
    void authUrl_defaultServer_usesConfiguredDatasource() {
        String urlNull = authorizeOAuth.authorizeUrlForServer(null, SsoScopes.ALL);
        String urlEmpty = authorizeOAuth.authorizeUrlForServer("", SsoScopes.ALL);
        String urlSerenity = authorizeOAuth.authorizeUrlForServer("serenity", SsoScopes.ALL);

        assertThat(urlNull).isEqualTo(urlEmpty).isEqualTo(urlSerenity);
    }

    // ── AC: 服务器不匹配友好错误 ──

    @Test
    @DisplayName("authUrl_serverMismatch_friendlyError: tranquility → PARAM_ERROR")
    void authUrl_serverMismatch_friendlyError() {
        assertThatThrownBy(() -> authorizeOAuth.authorizeUrlForServer("tranquility", SsoScopes.ALL))
                .isInstanceOf(EveHelperException.class)
                .satisfies(e -> assertThat(((EveHelperException) e).getResultCode())
                        .isEqualTo(ResultCode.PARAM_ERROR));
    }
}
