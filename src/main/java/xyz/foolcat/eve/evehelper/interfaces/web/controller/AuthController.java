package xyz.foolcat.eve.evehelper.interfaces.web.controller;

import cn.hutool.core.util.StrUtil;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import xyz.foolcat.eve.evehelper.application.dto.request.RefreshTokenRequest;
import xyz.foolcat.eve.evehelper.application.dto.response.TokenPair;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysUser;
import xyz.foolcat.eve.evehelper.domain.service.security.TokenBlacklistService;
import xyz.foolcat.eve.evehelper.domain.service.security.TokenService;
import xyz.foolcat.eve.evehelper.domain.service.system.SysUserService;
import xyz.foolcat.eve.evehelper.shared.kernel.constants.SecurityConstant;
import xyz.foolcat.eve.evehelper.shared.result.Result;
import xyz.foolcat.eve.evehelper.shared.util.SensitiveDataMasker;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.text.ParseException;
import java.util.Date;

/**
 * 认证资源控制器 (RESTful)
 * 处理登出、token刷新等认证相关操作
 *
 * @author Leojan
 * date 2026-01-30
 */
@Tag(name = "认证")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@Validated
public class AuthController {

    private final TokenBlacklistService tokenBlacklistService;
    private final TokenService tokenService;
    private final SysUserService sysUserService;

    /**
     * 用户登出 (撤销token)
     * RESTful: DELETE /auth/sessions (删除当前会话)
     */
    @Operation(summary = "用户登出")
    @DeleteMapping("/sessions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Result logout(HttpServletRequest request) {
        try {
            String token = request.getHeader(SecurityConstant.AUTHORIZATION_KEY);

            // 增强输入验证
            if (StrUtil.isEmpty(token)) {
                return Result.failed("缺少Authorization头");
            }

            if (!token.startsWith(SecurityConstant.JWT_PREFIX)) {
                return Result.failed("无效的Token格式");
            }

            // Token长度限制(防止DoS攻击)
            if (token.length() > 2048) {
                log.warn("Token长度超出限制: length={}", token.length());
                return Result.failed("Token长度超出限制");
            }

            // 移除Bearer前缀
            token = token.replace(SecurityConstant.JWT_PREFIX, Strings.EMPTY);

            // 验证token不为空
            if (token.isEmpty()) {
                return Result.failed("Token内容为空");
            }

            // 解析JWT
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();

            // 提取jti和过期时间
            String jti = claimsSet.getJWTID();
            Date expirationTime = claimsSet.getExpirationTime();

            // 验证必要字段
            if (jti == null || jti.isEmpty()) {
                return Result.failed("Token缺少JTI");
            }

            if (expirationTime == null) {
                return Result.failed("Token缺少过期时间");
            }

            // 加入黑名单
            tokenBlacklistService.addToBlacklist(jti, expirationTime);

            log.info("用户登出成功: userId={}", claimsSet.getClaim(SecurityConstant.USER_ID_KEY));
            return Result.success("登出成功");

        } catch (ParseException e) {
            log.error("Token解析失败", e);
            return Result.failed("Token格式错误");
        } catch (Exception e) {
            log.error("登出失败", e);
            return Result.failed("登出失败");
        }
    }

    /**
     * 刷新Access Token
     * RESTful: POST /auth/tokens (创建新的token)
     */
    @Operation(summary = "刷新Access Token")
    @PostMapping("/tokens")
    public Result<TokenPair> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            String refreshToken = request.getRefreshToken();

            // 增强输入验证
            if (refreshToken == null || refreshToken.trim().isEmpty()) {
                return Result.failed("Refresh Token不能为空");
            }

            // UUID格式验证(Refresh Token应该是UUID格式)
            if (!refreshToken.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")) {
                log.warn("Refresh Token格式错误: token={}", SensitiveDataMasker.maskToken(refreshToken));
                return Result.failed("Refresh Token格式错误");
            }

            // 验证Refresh Token是否有效
            if (!tokenService.isRefreshTokenValid(refreshToken)) {
                log.warn("Refresh Token无效: refreshToken={}", SensitiveDataMasker.maskToken(refreshToken));
                return Result.failed("Refresh Token无效或已过期");
            }

            // 从Refresh Token获取用户ID
            Integer userId = tokenService.getUserIdFromRefreshToken(refreshToken);

            if (userId == null || userId <= 0) {
                log.error("无效的用户ID: userId={}", userId);
                return Result.failed("无效的用户信息");
            }

            // 重新加载用户信息(包含最新的权限)
            SysUser user = sysUserService.loadUserById(userId);

            if (user == null) {
                log.error("用户不存在: userId={}", userId);
                return Result.failed("用户不存在");
            }

            // 生成新的Token对
            TokenPair tokenPair = tokenService.refreshAccessTokenWithUser(refreshToken, user);

            log.info("刷新Token成功: userId={}", userId);
            return Result.success(tokenPair);

        } catch (IllegalArgumentException e) {
            log.warn("刷新Token失败: {}", e.getMessage());
            return Result.failed(e.getMessage());
        } catch (Exception e) {
            log.error("刷新Token失败", e);
            return Result.failed("刷新Token失败");
        }
    }
}

