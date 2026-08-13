package xyz.foolcat.eve.evehelper.infrastructure.config.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import xyz.foolcat.eve.evehelper.domain.service.security.LoginRateLimiterService;
import xyz.foolcat.eve.evehelper.shared.result.Result;
import xyz.foolcat.eve.evehelper.shared.util.SensitiveDataMasker;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 认证失败处理器
 * 处理登录失败,记录失败次数,实现账户锁定
 *
 * @author Leojan
 * date 2026-01-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationFailureServletHandler implements AuthenticationFailureHandler {

    private final LoginRateLimiterService loginRateLimiterService;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        String username = request.getParameter("username");
        String maskedUsername = SensitiveDataMasker.maskUsername(username);

        // 限流副作用守护(评审 LOW-1):Redis 故障时降级为仅日志,恒写统一 401,不退化为 500
        boolean isLocked = false;
        int remainingAttempts = -1;
        try {
            isLocked = loginRateLimiterService.recordFailedAttempt(username);
            remainingAttempts = loginRateLimiterService.getRemainingAttempts(username);
        } catch (Exception e) {
            log.warn("限流服务不可用,降级为统一响应: username={}", maskedUsername, e);
        }

        // 对外单一措辞(SC-001 / FR-004 / FR-005):凭证类失败对外不可区分,细节仅服务端日志
        // (maskUsername 已剔除控制字符,防 CRLF 注入;CWE-117)
        log.warn("登录失败: username={}, reason={}, locked={}, remainingAttempts={}",
                maskedUsername, exception.getClass().getSimpleName(), isLocked, remainingAttempts);

        // 返回JSON响应:统一「用户名或密码错误」,不回显内部异常消息(LOW-7j)
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Result result = Result.failed("用户名或密码错误");
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
