package xyz.foolcat.eve.evehelper.domain.service.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 登录限流服务
 * 防止暴力破解攻击
 *
 * @author Leojan
 * date 2026-01-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginRateLimiterService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String LOGIN_ATTEMPT_PREFIX = "login:attempt:";
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MINUTES = 30;

    /**
     * 记录登录失败
     * 使用Redis INCR命令保证原子性
     *
     * @param username 用户名
     * @return 是否已被锁定
     */
    public boolean recordFailedAttempt(String username) {
        String key = LOGIN_ATTEMPT_PREFIX + username;

        // 使用INCR命令原子性递增失败次数
        Long attempts = redisTemplate.opsForValue().increment(key);

        if (attempts == null) {
            log.error("Redis INCR操作失败: username={}", username);
            return false;
        }

        // 首次失败时设置过期时间
        if (attempts == 1) {
            redisTemplate.expire(key, LOCK_DURATION_MINUTES, TimeUnit.MINUTES);
        }

        boolean isLocked = attempts >= MAX_ATTEMPTS;
        if (isLocked) {
            log.warn("账户已锁定: username={}, attempts={}", username, attempts);
        } else {
            log.info("登录失败记录: username={}, attempts={}/{}", username, attempts, MAX_ATTEMPTS);
        }

        return isLocked;
    }

    /**
     * 获取当前失败次数
     *
     * @param username 用户名
     * @return 当前失败次数
     */
    private Integer getCurrentAttempts(String username) {
        String key = LOGIN_ATTEMPT_PREFIX + username;
        Object attemptsObj = redisTemplate.opsForValue().get(key);

        if (attemptsObj == null) {
            return 0;
        }

        if (attemptsObj instanceof Integer) {
            return (Integer) attemptsObj;
        } else if (attemptsObj instanceof Long) {
            return ((Long) attemptsObj).intValue();
        } else {
            try {
                return Integer.parseInt(attemptsObj.toString());
            } catch (NumberFormatException e) {
                log.warn("无法解析失败次数: username={}, value={}", username, attemptsObj);
                return 0;
            }
        }
    }

    /**
     * 检查是否被锁定
     *
     * @param username 用户名
     * @return true-已锁定, false-未锁定
     */
    public boolean isLocked(String username) {
        Integer attempts = getCurrentAttempts(username);
        return attempts >= MAX_ATTEMPTS;
    }

    /**
     * 获取剩余尝试次数
     *
     * @param username 用户名
     * @return 剩余次数
     */
    public int getRemainingAttempts(String username) {
        Integer attempts = getCurrentAttempts(username);
        return Math.max(0, MAX_ATTEMPTS - attempts);
    }

    /**
     * 清除失败记录(登录成功后调用)
     *
     * @param username 用户名
     */
    public void clearAttempts(String username) {
        String key = LOGIN_ATTEMPT_PREFIX + username;
        redisTemplate.delete(key);
        log.info("清除登录失败记录: username={}", username);
    }

    /**
     * 获取锁定剩余时间(秒)
     *
     * @param username 用户名
     * @return 剩余秒数, -1表示未锁定
     */
    public long getLockRemainingTime(String username) {
        String key = LOGIN_ATTEMPT_PREFIX + username;
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return ttl != null ? ttl : -1;
    }
}
