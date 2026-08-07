package xyz.foolcat.eve.evehelper.application.service;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.application.dto.request.RefreshTokenRequest;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysUser;
import xyz.foolcat.eve.evehelper.domain.model.vo.TokenResult;
import xyz.foolcat.eve.evehelper.domain.service.security.TokenBlacklistService;
import xyz.foolcat.eve.evehelper.domain.service.security.TokenService;
import xyz.foolcat.eve.evehelper.domain.service.system.SysRoleService;
import xyz.foolcat.eve.evehelper.domain.service.system.SysUserService;
import xyz.foolcat.eve.evehelper.shared.kernel.constants.SecurityConstant;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;
import xyz.foolcat.eve.evehelper.shared.util.SensitiveDataMasker;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 认证应用服务
 * 负责登出、token 刷新等认证用例的编排
 *
 * @author Leojan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthApplicationService {

    /** Refresh Token 应为 UUID 格式 */
    private static final Pattern UUID_PATTERN =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    /** Token 长度上限(防 DoS) */
    private static final int MAX_TOKEN_LENGTH = 2048;

    private final TokenBlacklistService tokenBlacklistService;
    private final TokenService tokenService;
    private final SysUserService sysUserService;
    private final SysRoleService sysRoleService;

    /**
     * 用户登出:解析并校验 Authorization 头中的 Bearer Token,将其 jti 加入黑名单。
     *
     * @param request HTTP 请求
     */
    public void logout(HttpServletRequest request) {
        String token = request.getHeader(SecurityConstant.AUTHORIZATION_KEY);

        if (StrUtil.isEmpty(token)) {
            throw new EveHelperException("缺少Authorization头");
        }
        if (!token.startsWith(SecurityConstant.JWT_PREFIX)) {
            throw new EveHelperException("无效的Token格式");
        }
        if (token.length() > MAX_TOKEN_LENGTH) {
            log.warn("Token长度超出限制: length={}", token.length());
            throw new EveHelperException("Token长度超出限制");
        }

        token = token.replace(SecurityConstant.JWT_PREFIX, Strings.EMPTY);
        if (token.isEmpty()) {
            throw new EveHelperException("Token内容为空");
        }

        final TokenService.ParsedAccessToken parsed;
        try {
            parsed = tokenService.parseAccessToken(token);
        } catch (ParseException e) {
            log.error("Token解析失败", e);
            throw new EveHelperException("Token格式错误", e);
        }
        if (parsed == null) {
            log.error("Token解析结果为空");
            throw new EveHelperException("Token格式错误");
        }

        String jti = parsed.jti();
        Date expirationTime = parsed.expirationTime();

        if (jti == null || jti.isEmpty()) {
            throw new EveHelperException("Token缺少JTI");
        }
        if (expirationTime == null) {
            throw new EveHelperException("Token缺少过期时间");
        }

        tokenBlacklistService.addToBlacklist(jti, expirationTime);
        log.info("用户登出成功: userId={}", parsed.userIdClaim());
    }

    /**
     * 刷新 Access Token:校验 Refresh Token 有效性后重新加载用户并生成新 Token 对。
     *
     * @param request 刷新请求
     * @return 新的 Token 对
     */
    public TokenResult refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new EveHelperException("Refresh Token不能为空");
        }
        if (!UUID_PATTERN.matcher(refreshToken).matches()) {
            log.warn("Refresh Token格式错误: token={}", SensitiveDataMasker.maskToken(refreshToken));
            throw new EveHelperException("Refresh Token格式错误");
        }
        if (!tokenService.isRefreshTokenValid(refreshToken)) {
            log.warn("Refresh Token无效: refreshToken={}", SensitiveDataMasker.maskToken(refreshToken));
            throw new EveHelperException("Refresh Token无效或已过期");
        }

        Integer userId = tokenService.getUserIdFromRefreshToken(refreshToken);
        if (userId == null || userId <= 0) {
            log.error("无效的用户ID: userId={}", userId);
            throw new EveHelperException("无效的用户信息");
        }

        SysUser user = sysUserService.loadUserById(userId);
        if (user == null) {
            log.error("用户不存在: userId={}", userId);
            throw new EveHelperException("用户不存在");
        }

        // 重新加载用户角色,写入新 JWT authorities claim
        List<String> authorities = sysRoleService.queryRolesByUserId(userId);

        TokenResult tokenResult = tokenService.refreshAccessTokenWithUser(refreshToken, user, authorities);
        log.info("刷新Token成功: userId={}", userId);
        return tokenResult;
    }
}
