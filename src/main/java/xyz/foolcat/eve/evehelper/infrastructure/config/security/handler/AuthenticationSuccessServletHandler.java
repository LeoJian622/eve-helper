package xyz.foolcat.eve.evehelper.infrastructure.config.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import xyz.foolcat.eve.evehelper.application.dto.response.TokenPair;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysUser;
import xyz.foolcat.eve.evehelper.domain.service.security.LoginRateLimiterService;
import xyz.foolcat.eve.evehelper.domain.service.security.TokenService;
import xyz.foolcat.eve.evehelper.shared.result.Result;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 认证成功处理器
 * 生成JWT token并清除登录失败记录
 *
 * @author Leojan
 * date 2022-01-12 14:57
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationSuccessServletHandler implements AuthenticationSuccessHandler {

    private final TokenService tokenService;
    private final LoginRateLimiterService loginRateLimiterService;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        SysUser sysUser = (SysUser) authentication.getPrincipal();

        // 清除登录失败记录
        loginRateLimiterService.clearAttempts(sysUser.getUsername());

        // 生成Token对
        TokenPair tokenPair = tokenService.generateTokenPair(sysUser);

        log.info("用户登录成功: userId={}, username={}", sysUser.getId(), sysUser.getUsername());

        // 返回JSON响应
        response.setStatus(HttpStatus.OK.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Result<TokenPair> result = Result.success(tokenPair);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
