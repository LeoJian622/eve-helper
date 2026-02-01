package xyz.foolcat.eve.evehelper.domain.service.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.shared.kernel.constants.SecurityConstant;
import xyz.foolcat.eve.evehelper.shared.util.SensitiveDataMasker;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Token黑名单服务
 * 用于撤销JWT token,实现登出功能
 *
 * @author Leojan
 * date 2026-01-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 将token加入黑名单
     * 使用SETNX确保幂等性
     *
     * @param jti            JWT ID
     * @param expirationTime token过期时间
     * @return true-成功加入, false-已存在或过期
     */
    public boolean addToBlacklist(String jti, Date expirationTime) {
        long ttl = expirationTime.getTime() - System.currentTimeMillis();
        if (ttl <= 0) {
            log.warn("Token已过期,无需加入黑名单: jti={}", jti);
            return false;
        }

        String key = SecurityConstant.TOKEN_BLACKLIST_PREFIX + jti;

        // 使用setIfAbsent确保幂等性(相当于SETNX)
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, "revoked", ttl, TimeUnit.MILLISECONDS);

        if (Boolean.TRUE.equals(success)) {
            log.info("Token已加入黑名单: jti={}, ttl={}ms", SensitiveDataMasker.maskToken(jti), ttl);
            return true;
        } else {
            log.warn("Token已在黑名单中: jti={}", jti);
            return false;
        }
    }

    /**
     * 检查token是否在黑名单中
     * 此方法使用Redis的hasKey命令,是原子操作
     *
     * @param jti JWT ID
     * @return true-已撤销, false-有效
     */
    public boolean isBlacklisted(String jti) {
        if (jti == null || jti.isEmpty()) {
            log.warn("检查黑名单时jti为空");
            return false;
        }

        String key = SecurityConstant.TOKEN_BLACKLIST_PREFIX + jti;
        Boolean exists = redisTemplate.hasKey(key);

        if (Boolean.TRUE.equals(exists)) {
            log.debug("Token在黑名单中: jti={}", SensitiveDataMasker.maskToken(jti));
        }

        return Boolean.TRUE.equals(exists);
    }

    /**
     * 从黑名单中移除token(仅用于测试或特殊场景)
     *
     * @param jti JWT ID
     */
    public void removeFromBlacklist(String jti) {
        String key = SecurityConstant.TOKEN_BLACKLIST_PREFIX + jti;
        redisTemplate.delete(key);
        log.info("Token已从黑名单移除: jti={}", jti);
    }
}
