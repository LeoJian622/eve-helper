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
import xyz.foolcat.eve.evehelper.domain.service.security.RefreshRateLimiterService;
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
    /** 007 T017:refresh 两层限流(仅失败路径调用,成功路径零影响) */
    private final RefreshRateLimiterService refreshRateLimiterService;

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

        // 007 T048:先撤销 refresh token,再拉黑 access token —— 顺序不可颠倒。
        // 若先拉黑成功而撤销失败,用户重试登出时 access token 已在黑名单 → filter 直接 401
        // → 再也无法登出,refresh token 留存 7 天。反序则重试幂等且安全。
        // 撤销失败不阻断登出(见 revokeRefreshTokenBySession 的 fail-open 说明),
        // 但会打 [SECURITY_ALERT:LOGOUT_REVOKE_MISS] 以便发现绕过。
        tokenService.revokeRefreshTokenBySession(parsed.sessionId());

        // 007 T049-A:无条件设置 session_revoked:<sid> tombstone(仅当 sid 非空)。
        // 不置于 revokeRefreshTokenBySession 内部 -- 该方法 5 条 early-return(索引驱逐等)会跳过它,
        // 而那恰是 fail-open 路径(评审 HIGH-1)。tombstone 让并发刷新在 claim 后、generate 前
        // check 到会话已登出而拒绝,闭合 T048 的并发缺口。markSessionRevoked 内部对 null/空 sid no-op。
        tokenService.markSessionRevoked(parsed.sessionId());

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
            // 007 T017:格式非法属洪泛主向量(此阶段拿不到 userId)→ L2 观测告警(T036:不施加时延)
            refreshRateLimiterService.observeInvalidRefresh();
            throw new EveHelperException("Refresh Token格式错误");
        }
        // 007 T015:原「③ 存在性校验(hasKey)+ ④ 取 userId(get)」两次 Redis 操作
        // 合并为单次 get(null 即无效),消除两次操作间 token 恰好过期的 TOCTOU 窗口。
        // 该失败路径尚未解析出 userId,属 L2 洪泛观测范围(T017 接线)。
        final Integer userId;
        try {
            userId = tokenService.getUserIdFromRefreshToken(refreshToken);
        } catch (IllegalArgumentException e) {
            log.warn("Refresh Token无效: refreshToken={}", SensitiveDataMasker.maskToken(refreshToken));
            // 007 T017:随机 UUID 洪泛在此被挡(解析不出 userId)→ L2 观测告警(T036:不施加时延)
            refreshRateLimiterService.observeInvalidRefresh();
            throw new EveHelperException("Refresh Token无效或已过期");
        }
        if (userId == null || userId <= 0) {
            log.error("无效的用户ID: userId={}", userId);
            throw new EveHelperException("无效的用户信息");
        }

        SysUser user = sysUserService.loadUserById(userId);
        if (user == null) {
            log.error("用户不存在: userId={}", userId);
            // 007 T017:已有 userId 的失败 → L1 按用户观测(仅计数,禁止锁定)
            refreshRateLimiterService.recordUserFailure(userId);
            throw new EveHelperException("用户不存在");
        }

        // 重新加载用户角色,写入新 JWT authorities claim
        List<String> authorities = sysRoleService.queryRolesByUserId(userId);

        TokenResult tokenResult = tokenService.refreshAccessTokenWithUser(refreshToken, user, authorities);
        log.info("刷新Token成功: userId={}", userId);
        return tokenResult;
    }
}
