package xyz.foolcat.eve.evehelper.domain.service.security;

import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysUser;
import xyz.foolcat.eve.evehelper.domain.model.vo.TokenResult;
import xyz.foolcat.eve.evehelper.domain.port.cache.CacheGateway;
import xyz.foolcat.eve.evehelper.shared.kernel.config.JwtTokenProperties;

import java.security.KeyPair;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MIN-2 回归:refresh_user 反向索引撤销剪枝。
 * <p>
 * {@code refresh_user:<userId>} 集合此前只 sAdd+expire 从不 sRemove,
 * 登出/改密撤销后已死 sid 仍滞留集合(最长 7 天),改密全会话失效会枚举到死会话。
 * 本测试钉住:成功撤销 refresh token 后必须从用户集合剪枝该 sid;
 * 未撤销(索引缺失/无 sid)不得误剪。
 *
 * @author Leojan
 * date 2026-11-24
 */
@DisplayName("MIN-2 TokenService refresh_user 索引撤销剪枝")
class TokenServiceUserIndexPruneTest {

    private static final Integer USER_ID = 42;
    private static final String SESSION_INDEX_PREFIX = "refresh_session:";
    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    private static final String REFRESH_USER_PREFIX = "refresh_user:";
    private static final long REFRESH_TTL = 604800L;

    private CacheGateway cacheGateway;
    private TokenService tokenService;

    @BeforeEach
    void setUp() throws Exception {
        cacheGateway = Mockito.mock(CacheGateway.class);
        JwtTokenProperties props = new JwtTokenProperties();
        props.setAccessTokenExpirationTime(900L);
        props.setRefreshTokenExpirationTime(REFRESH_TTL);
        props.setIssuer("eve-helper-test");
        props.setSubject("eve-helper-test");

        KeyPair keyPair = new RSAKeyGenerator(2048).generate().toKeyPair();
        tokenService = new TokenService(keyPair, props, cacheGateway);

        when(cacheGateway.setIfAbsent(anyString(), any(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(cacheGateway.sAdd(anyString(), any())).thenReturn(1L);
        when(cacheGateway.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
    }

    private static SysUser user() {
        SysUser u = new SysUser();
        u.setId(USER_ID);
        u.setUsername("tester");
        return u;
    }

    /** 生成真实 token 对,返回 [sid, refreshTokenUuid]。 */
    private String[] generateSession() throws Exception {
        TokenResult result = tokenService.generateTokenPair(user(), List.of("ROLE_USER"));
        String raw = result.accessToken().replace("Bearer ", "");
        String sid = tokenService.parseAccessToken(raw).sessionId();

        ArgumentCaptor<Object> valueCaptor = ArgumentCaptor.forClass(Object.class);
        verify(cacheGateway).set(eq(SESSION_INDEX_PREFIX + sid), valueCaptor.capture(),
                eq(REFRESH_TTL), eq(TimeUnit.SECONDS));
        return new String[]{sid, valueCaptor.getValue().toString()};
    }

    @Test
    @DisplayName("登出撤销成功 → sRemove 从 refresh_user:<userId> 剪枝该 sid")
    void revokeSuccess_prunesUserSessionIndex() throws Exception {
        String[] session = generateSession();
        String sid = session[0];
        String refreshToken = session[1];

        when(cacheGateway.get(SESSION_INDEX_PREFIX + sid)).thenReturn(refreshToken);
        when(cacheGateway.get(REFRESH_TOKEN_PREFIX + refreshToken)).thenReturn(USER_ID);
        when(cacheGateway.delete(anyString())).thenReturn(true);

        boolean revoked = tokenService.revokeRefreshTokenBySession(sid);

        assertTrue(revoked, "索引与 refresh token 均在,撤销应成功");
        verify(cacheGateway).sRemove(eq(REFRESH_USER_PREFIX + USER_ID), eq(sid));
    }

    @Test
    @DisplayName("会话索引缺失 → 撤销失败且不误剪 refresh_user 集合")
    void revokeIndexMissing_noPrune() {
        when(cacheGateway.get(SESSION_INDEX_PREFIX + "ghost-sid")).thenReturn(null);

        boolean revoked = tokenService.revokeRefreshTokenBySession("ghost-sid");

        assertFalse(revoked);
        verify(cacheGateway, never()).sRemove(anyString(), any());
    }

    @Test
    @DisplayName("sid 为空(改造前遗留 token) → 降级仅拉黑,不触发剪枝")
    void revokeNullSid_noPrune() {
        boolean revoked = tokenService.revokeRefreshTokenBySession(null);

        assertFalse(revoked);
        verify(cacheGateway, never()).sRemove(anyString(), any());
    }
}