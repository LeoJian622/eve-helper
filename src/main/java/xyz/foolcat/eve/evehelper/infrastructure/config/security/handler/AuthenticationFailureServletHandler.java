package xyz.foolcat.eve.evehelper.infrastructure.config.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import xyz.foolcat.eve.evehelper.domain.service.security.LoginRateLimiterService;
import xyz.foolcat.eve.evehelper.shared.result.Result;

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
        log.warn("登录失败: username={}, reason={}", username, exception.getMessage());

        // 记录失败次数
        boolean isLocked = loginRateLimiterService.recordFailedAttempt(username);

        // 构建响应消息
        String message;
        if (isLocked) {
            long remainingTime = loginRateLimiterService.getLockRemainingTime(username);
            message = "账户已锁定，请在%d分钟后重试".formatted(remainingTime / 60);
        } else {
            int remainingAttempts = loginRateLimiterService.getRemainingAttempts(username);
            if (exception instanceof AccountExpiredException) {
                message = "账号已过期";
            } else if (exception instanceof UsernameNotFoundException) {
                message = "用户名或密码错误";
            } else if (exception instanceof BadCredentialsException) {
                message = "用户名或密码错误，剩余尝试次数: %d".formatted(remainingAttempts);
            } else if (exception instanceof CredentialsExpiredException) {
                message = "密码已过期";
            } else if (exception instanceof DisabledException) {
                message = "账号不可用";
            } else if (exception instanceof LockedException) {
                message = "账户已被锁定";
            } else if (exception instanceof InternalAuthenticationServiceException) {
                message = "用户账号不存在";
            // 007 T018:原 InvalidCookieException 分支已删除 —— 其唯一来源
            // JwtAuthorizationTokenFilter 在 T013 重写后不再抛该异常(失败统一
            // 走 rejectOrPass 直写 401),此分支不可达
            } else {
                message = "登录失败: " + exception.getMessage();
            }
        }

        // 返回JSON响应
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Result result = Result.failed(message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
