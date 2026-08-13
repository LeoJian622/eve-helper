package xyz.foolcat.eve.evehelper.infrastructure.config.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import xyz.foolcat.eve.evehelper.domain.service.security.LoginRateLimiterService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 登录失败响应枚举面回归(008 T008 / SC-001)。
 *
 * <p>验证:所有凭证类登录失败对外单一措辞「用户名或密码错误」+ 401,不可枚举有效账号(FR-004/005);
 * 不回显内部异常消息(LOW-7j);限流服务故障时仍统一 401,不退化为 500(评审 LOW-1)。
 */
class AuthenticationFailureHandlerEnumerationTest {

    private LoginRateLimiterService rateLimiter;
    private AuthenticationFailureServletHandler handler;

    @BeforeEach
    void setUp() {
        rateLimiter = mock(LoginRateLimiterService.class);
        handler = new AuthenticationFailureServletHandler(rateLimiter, new ObjectMapper());
    }

    private MockHttpServletResponse invoke(AuthenticationException ex, String username) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setParameter("username", username);
        MockHttpServletResponse res = new MockHttpServletResponse();
        handler.onAuthenticationFailure(req, res, ex);
        return res;
    }

    @Test
    void credentialFailureMessagesAreIndistinguishable() throws Exception {
        // SC-001:「账号存在+口令错」与「账号不存在+任意口令」响应逐字节一致
        when(rateLimiter.recordFailedAttempt(any())).thenReturn(false);
        MockHttpServletResponse a = invoke(new BadCredentialsException("bad password"), "realuser");
        MockHttpServletResponse b = invoke(new UsernameNotFoundException("no such account"), "nosuchuser");

        assertEquals(401, a.getStatus());
        assertEquals(401, b.getStatus());
        assertEquals(a.getContentAsString(), b.getContentAsString(), "两种失败响应必须逐字节一致");
        assertTrue(a.getContentAsString().contains("用户名或密码错误"));
    }

    @Test
    void lockedAccountUsesSameMessage_NoRemainingAttempts() throws Exception {
        // FR-004:锁定账号同一措辞,不泄露「剩余次数」
        when(rateLimiter.recordFailedAttempt(any())).thenReturn(true);
        MockHttpServletResponse res = invoke(new LockedException("locked account"), "user");

        assertEquals(401, res.getStatus());
        assertTrue(res.getContentAsString().contains("用户名或密码错误"));
        assertFalse(res.getContentAsString().contains("剩余"), "不得泄露剩余尝试次数");
    }

    @Test
    void doesNotEchoExceptionMessage() throws Exception {
        // LOW-7j:不回显内部异常消息
        when(rateLimiter.recordFailedAttempt(any())).thenReturn(false);
        MockHttpServletResponse res = invoke(
                new InternalAuthenticationServiceException("secret-internal-detail"), "user");

        assertFalse(res.getContentAsString().contains("secret-internal-detail"),
                "不得回显内部异常消息");
    }

    @Test
    void rateLimiterFailureStillReturnsUniform401() throws Exception {
        // 评审 LOW-1:限流 Redis 故障时仍统一 401,不退化为 500
        when(rateLimiter.recordFailedAttempt(any())).thenThrow(new RuntimeException("redis down"));
        MockHttpServletResponse res = invoke(new BadCredentialsException("bad"), "user");

        assertEquals(401, res.getStatus(), "限流故障不得使统一 401 退化为 500");
        assertTrue(res.getContentAsString().contains("用户名或密码错误"));
    }

    @Test
    void responseDoesNotLeakRawUsername() throws Exception {
        // 剩余尝试/锁定细节仅日志,响应不含原文 username
        when(rateLimiter.recordFailedAttempt(any())).thenReturn(false);
        MockHttpServletResponse res = invoke(new BadCredentialsException("bad"), "alice");

        assertFalse(res.getContentAsString().contains("alice"), "响应不得含原始 username");
    }
}