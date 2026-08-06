package xyz.foolcat.eve.evehelper.domain.service.security;

import cn.hutool.core.lang.UUID;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.shared.kernel.config.JwtTokenProperties;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysUser;
import xyz.foolcat.eve.evehelper.domain.model.vo.TokenResult;
import xyz.foolcat.eve.evehelper.domain.port.cache.CacheGateway;
import xyz.foolcat.eve.evehelper.shared.kernel.constants.SecurityConstant;
import xyz.foolcat.eve.evehelper.shared.util.SensitiveDataMasker;

import java.security.KeyPair;
import java.text.ParseException;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Token管理服务
 * 负责生成和刷新Access Token和Refresh Token
 *
 * @author Leojan
 * date 2026-01-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final KeyPair keyPair;
    private final JwtTokenProperties jwtTokenProperties;
    private final CacheGateway cacheGateway;

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";

    /**
     * 生成Token对(Access Token + Refresh Token)
     *
     * @param user 用户信息
     * @return Token对(领域读模型)
     */
    public TokenResult generateTokenPair(SysUser user) {
        try {
            // 生成Access Token
            String accessToken = generateAccessToken(user);

            // 生成Refresh Token
            String refreshToken = generateRefreshToken(user);

            return new TokenResult(
                    SecurityConstant.JWT_PREFIX + accessToken,
                    refreshToken,
                    jwtTokenProperties.getAccessTokenExpirationTime(),
                    "Bearer");

        } catch (JOSEException e) {
            log.error("生成Token失败", e);
            throw new RuntimeException("生成Token失败", e);
        }
    }

    /**
     * 生成Access Token
     *
     * @param user 用户信息
     * @return JWT字符串
     */
    private String generateAccessToken(SysUser user) throws JOSEException {
        long expirationTime = jwtTokenProperties.getAccessTokenExpirationTime() * 1000;

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(jwtTokenProperties.getSubject())
                .issuer(jwtTokenProperties.getIssuer())
                .jwtID(UUID.randomUUID().toString())
                .claim(SecurityConstant.USER_ID_KEY, user.getId())
                .claim(SecurityConstant.USER_NAME_KEY, user.getUsername())
                .claim(SecurityConstant.JWT_AUTHORITIES_KEY,
                        user.getAuthorities().stream()
                                .map(GrantedAuthority::getAuthority)
                                .collect(Collectors.toList()))
                .expirationTime(new Date(System.currentTimeMillis() + expirationTime))
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).build(),
                claimsSet);

        JWSSigner signer = new RSASSASigner(keyPair.getPrivate());
        signedJWT.sign(signer);

        return signedJWT.serialize();
    }

    /**
     * 生成Refresh Token
     *
     * @param user 用户信息
     * @return Refresh Token ID
     */
    private String generateRefreshToken(SysUser user) {
        String refreshTokenId = UUID.randomUUID().toString();
        String key = REFRESH_TOKEN_PREFIX + refreshTokenId;

        // 存储到Redis: refresh_token:{jti} -> userId
        long ttl = jwtTokenProperties.getRefreshTokenExpirationTime();
        cacheGateway.set(key, user.getId(), ttl, TimeUnit.SECONDS);

        log.info("生成Refresh Token: userId={}, refreshTokenId={}, ttl={}s",
                user.getId(), SensitiveDataMasker.maskToken(refreshTokenId), ttl);

        return refreshTokenId;
    }

    /**
     * 从Refresh Token获取用户ID
     *
     * @param refreshToken Refresh Token
     * @return 用户ID
     */
    public Integer getUserIdFromRefreshToken(String refreshToken) {
        String key = REFRESH_TOKEN_PREFIX + refreshToken;

        Object userIdObj = cacheGateway.get(key);
        if (userIdObj == null) {
            throw new IllegalArgumentException("Refresh Token无效或已过期");
        }

        if (userIdObj instanceof Integer number) {
            return number;
        } else {
            return Integer.parseInt(userIdObj.toString());
        }
    }

    /**
     * 验证并刷新Access Token (完整版)
     * 实现Refresh Token轮换机制,防止token重放攻击
     *
     * @param refreshToken Refresh Token
     * @param user         用户信息(从数据库重新加载)
     * @return 新的Token对(领域读模型)
     */
    public TokenResult refreshAccessTokenWithUser(String refreshToken, SysUser user) {
        String key = REFRESH_TOKEN_PREFIX + refreshToken;

        // 验证Refresh Token是否存在
        Object userIdObj = cacheGateway.get(key);
        if (userIdObj == null) {
            log.warn("Refresh Token不存在或已过期: refreshToken={}", SensitiveDataMasker.maskToken(refreshToken));
            throw new IllegalArgumentException("Refresh Token无效或已过期");
        }

        // 验证用户ID是否匹配
        Integer storedUserId;
        if (userIdObj instanceof Integer number) {
            storedUserId = number;
        } else {
            storedUserId = Integer.parseInt(userIdObj.toString());
        }

        if (!storedUserId.equals(user.getId())) {
            log.error("Refresh Token用户ID不匹配: expected={}, actual={}",
                    storedUserId, user.getId());
            throw new IllegalArgumentException("Refresh Token无效");
        }

        log.info("刷新Access Token: userId={}, refreshToken={}", user.getId(), SensitiveDataMasker.maskToken(refreshToken));

        // 撤销旧的Refresh Token(轮换机制)
        revokeRefreshToken(refreshToken);

        // 生成新的Token对(包含新的Refresh Token)
        return generateTokenPair(user);
    }

    /**
     * 撤销Refresh Token
     *
     * @param refreshToken Refresh Token
     */
    public void revokeRefreshToken(String refreshToken) {
        String key = REFRESH_TOKEN_PREFIX + refreshToken;
        cacheGateway.delete(key);
        log.info("撤销Refresh Token: refreshToken={}", SensitiveDataMasker.maskToken(refreshToken));
    }

    /**
     * 验证Refresh Token是否有效
     *
     * @param refreshToken Refresh Token
     * @return true-有效, false-无效
     */
    public boolean isRefreshTokenValid(String refreshToken) {
        String key = REFRESH_TOKEN_PREFIX + refreshToken;
        return Boolean.TRUE.equals(cacheGateway.hasKey(key));
    }

    /**
     * 解析 Access Token 并提取关键声明(jti / 过期时间 / 用户ID)。
     * 将 JWT 解析细节收拢到领域层,供登出等流程使用。
     *
     * @param token 原始 Access Token(不含 "Bearer " 前缀)
     * @return 解析出的关键声明
     * @throws ParseException token 格式非法
     */
    public ParsedAccessToken parseAccessToken(String token) throws ParseException {
        SignedJWT signedJWT = SignedJWT.parse(token);
        JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
        return new ParsedAccessToken(
                claimsSet.getJWTID(),
                claimsSet.getExpirationTime(),
                claimsSet.getClaim(SecurityConstant.USER_ID_KEY));
    }

    /**
     * Access Token 解析结果
     *
     * @param jti            Token 唯一标识
     * @param expirationTime 过期时间
     * @param userIdClaim    用户ID声明
     */
    public record ParsedAccessToken(String jti, Date expirationTime, Object userIdClaim) {
    }
}
