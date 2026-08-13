package xyz.foolcat.eve.evehelper.application.service;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import xyz.foolcat.eve.evehelper.application.dto.request.RefreshTokenRequest;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysUser;
import xyz.foolcat.eve.evehelper.domain.model.vo.TokenResult;
import xyz.foolcat.eve.evehelper.domain.service.security.RefreshRateLimiterService;
import xyz.foolcat.eve.evehelper.domain.service.security.TokenBlacklistService;
import xyz.foolcat.eve.evehelper.domain.service.security.TokenService;
import xyz.foolcat.eve.evehelper.domain.service.system.SysRoleService;
import xyz.foolcat.eve.evehelper.domain.service.system.SysUserService;
import xyz.foolcat.eve.evehelper.shared.kernel.constants.SecurityConstant;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 认证应用服务单元测试。
 *
 * @author Leojan
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("认证应用服务单元测试")
class AuthApplicationServiceUnitTest {

    @MockBean
    TokenBlacklistService tokenBlacklistService;

    @MockBean
    TokenService tokenService;

    @MockBean
    SysUserService sysUserService;

    @MockBean
    SysRoleService sysRoleService;

    @MockBean
    RefreshRateLimiterService refreshRateLimiterService;

    @Autowired
    private AuthApplicationService authApplicationService;

    @Test
    @DisplayName("登出:缺少 Authorization 头 -> 抛异常")
    void logout_missingHeader_throws() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(SecurityConstant.AUTHORIZATION_KEY)).thenReturn(null);

        assertThrows(EveHelperException.class, () -> authApplicationService.logout(request));
        verify(tokenBlacklistService, never()).addToBlacklist(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("登出:非 Bearer 前缀 -> 抛异常")
    void logout_nonBearer_throws() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(SecurityConstant.AUTHORIZATION_KEY)).thenReturn("Basic abc");

        assertThrows(EveHelperException.class, () -> authApplicationService.logout(request));
    }

    @Test
    @DisplayName("登出:非法 JWT -> 抛异常")
    void logout_invalidJwt_throws() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(SecurityConstant.AUTHORIZATION_KEY))
                .thenReturn(SecurityConstant.JWT_PREFIX + "not-a-jwt");

        assertThrows(EveHelperException.class, () -> authApplicationService.logout(request));
    }

    @Test
    @DisplayName("刷新:Refresh Token 为空 -> 抛异常")
    void refreshToken_empty_throws() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("  ");

        assertThrows(EveHelperException.class, () -> authApplicationService.refreshToken(request));
    }

    @Test
    @DisplayName("刷新:Refresh Token 非 UUID 格式 -> 抛异常")
    void refreshToken_badFormat_throws() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("not-a-uuid");

        assertThrows(EveHelperException.class, () -> authApplicationService.refreshToken(request));
    }

    @Test
    @DisplayName("刷新:Refresh Token 无效 -> 抛异常")
    void refreshToken_invalid_throws() {
        // 007 T015 后:存在性校验与取 userId 合并为单次 get,无效即抛 IllegalArgumentException
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("123e4567-e89b-12d3-a456-426614174000");
        when(tokenService.getUserIdFromRefreshToken("123e4567-e89b-12d3-a456-426614174000"))
                .thenThrow(new IllegalArgumentException("Refresh Token无效或已过期"));

        assertThrows(EveHelperException.class, () -> authApplicationService.refreshToken(request));

        // 007 T017 接线断言:无效 token 失败路径触发 L2 观测(非硬拒,业务错误照常抛出)
        verify(refreshRateLimiterService).observeInvalidRefresh();
    }

    @Test
    @DisplayName("刷新:有效 -> 返回新 Token 对")
    void refreshToken_valid_returnsTokenPair() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("123e4567-e89b-12d3-a456-426614174000");
        when(tokenService.getUserIdFromRefreshToken("123e4567-e89b-12d3-a456-426614174000")).thenReturn(1);
        SysUser user = new SysUser();
        when(sysUserService.loadUserById(1)).thenReturn(user);
        List<String> authorities = List.of("ADMIN");
        when(sysRoleService.queryRolesByUserId(1)).thenReturn(authorities);
        TokenResult tokenResult = new TokenResult("Bearer accessToken", "refreshToken", 3600L, "Bearer");
        when(tokenService.refreshAccessTokenWithUser("123e4567-e89b-12d3-a456-426614174000", user, authorities)).thenReturn(tokenResult);

        TokenResult result = authApplicationService.refreshToken(request);

        assertEquals("Bearer accessToken", result.accessToken());
        assertEquals("refreshToken", result.refreshToken());
        assertEquals(3600L, result.expiresIn());
        assertEquals("Bearer", result.tokenType());
    }
}
