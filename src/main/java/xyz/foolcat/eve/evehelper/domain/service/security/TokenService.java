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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.application.dto.response.TokenPair;
import xyz.foolcat.eve.evehelper.config.security.JwtTokenProperties;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysUser;
import xyz.foolcat.eve.evehelper.shared.kernel.constants.SecurityConstant;
import xyz.foolcat.eve.evehelper.shared.util.SensitiveDataMasker;

import java.security.KeyPair;
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
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";

    /**
     * 生成Token对(Access Token + Refresh Token)
     *
     * @param user 用户信息
     * @return Token对
     */
    public TokenPair generateTokenPair(SysUser user) {
        try {
            // 生成Access Token
            String accessToken = generateAccessToken(user);

            // 生成Refresh Token
            String refreshToken = generateRefreshToken(user);

            return TokenPair.builder()
                    .accessToken(SecurityConstant.JWT_PREFIX + accessToken)
                    .refreshToken(refreshToken)
                    .expiresIn(jwtTokenProperties.getAccessTokenExpirationTime())
                    .tokenType("Bearer")
                    .build();

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
        redisTemplate.opsForValue().set(key, user.getId(), ttl, TimeUnit.SECONDS);

        log.info("生成Refresh Token: userId={}, refreshTokenId={}, ttl={}s",
                user.getId(), SensitiveDataMasker.maskToken(refreshTokenId), ttl);

        return refreshTokenId;
    }

    /**
     * 验证并刷新Access Token
     *
     * @param refreshToken Refresh Token
     * @return 新的Token对
     */
    public TokenPair refreshAccessToken(String refreshToken) {
        String key = REFRESH_TOKEN_PREFIX + refreshToken;

        // 验证Refresh Token是否存在并获取用户ID
        Object userIdObj = redisTemplate.opsForValue().get(key);
        if (userIdObj == null) {
            log.warn("Refresh Token不存在或已过期: refreshToken={}", SensitiveDataMasker.maskToken(refreshToken));
            throw new IllegalArgumentException("Refresh Token无效或已过期");
        }

        Integer userId;
        if (userIdObj instanceof Integer) {
            userId = (Integer) userIdObj;
        } else {
            userId = Integer.parseInt(userIdObj.toString());
        }

        log.info("刷新Access Token: userId={}, refreshToken={}", userId, SensitiveDataMasker.maskToken(refreshToken));

        // 返回用户ID,由调用方加载用户信息
        return null; // 需要在Controller中完成
    }

    /**
     * 从Refresh Token获取用户ID
     *
     * @param refreshToken Refresh Token
     * @return 用户ID
     */
    public Integer getUserIdFromRefreshToken(String refreshToken) {
        String key = REFRESH_TOKEN_PREFIX + refreshToken;

        Object userIdObj = redisTemplate.opsForValue().get(key);
        if (userIdObj == null) {
            throw new IllegalArgumentException("Refresh Token无效或已过期");
        }

        if (userIdObj instanceof Integer) {
            return (Integer) userIdObj;
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
     * @return 新的Token对
     */
    public TokenPair refreshAccessTokenWithUser(String refreshToken, SysUser user) {
        String key = REFRESH_TOKEN_PREFIX + refreshToken;

        // 验证Refresh Token是否存在
        Object userIdObj = redisTemplate.opsForValue().get(key);
        if (userIdObj == null) {
            log.warn("Refresh Token不存在或已过期: refreshToken={}", SensitiveDataMasker.maskToken(refreshToken));
            throw new IllegalArgumentException("Refresh Token无效或已过期");
        }

        // 验证用户ID是否匹配
        Integer storedUserId;
        if (userIdObj instanceof Integer) {
            storedUserId = (Integer) userIdObj;
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
        redisTemplate.delete(key);
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
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
