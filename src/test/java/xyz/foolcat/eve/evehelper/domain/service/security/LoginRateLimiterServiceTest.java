package xyz.foolcat.eve.evehelper.domain.service.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.ActiveProfiles;
import xyz.foolcat.eve.evehelper.domain.port.cache.CacheGateway;


import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 登录限流服务测试
 *
 * @author Leojan
 * date 2026-02-01
 */
@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
@DisplayName("登录限流服务测试")
class LoginRateLimiterServiceTest {



    @MockBean
    private CacheGateway cacheGateway;

    @Autowired
    private LoginRateLimiterService loginRateLimiterService;

    private static final String TEST_USERNAME = "testuser";
    private static final String LOGIN_ATTEMPT_PREFIX = "login:attempt:";

    @Test
    @DisplayName("首次登录失败应记录并返回未锁定")
    void testRecordFailedAttempt_FirstAttempt() {
        // Given
        String key = LOGIN_ATTEMPT_PREFIX + TEST_USERNAME;
        when(cacheGateway.increment(key)).thenReturn(1L);
        when(cacheGateway.expire(eq(key), eq(30L), eq(TimeUnit.MINUTES))).thenReturn(true);

        // When
        boolean isLocked = loginRateLimiterService.recordFailedAttempt(TEST_USERNAME);

        // Then
        assertFalse(isLocked, "首次失败不应被锁定");
        verify(cacheGateway).increment(key);
        verify(cacheGateway).expire(key, 30L, TimeUnit.MINUTES);
    }

    @Test
    @DisplayName("第5次登录失败应返回锁定状态")
    void testRecordFailedAttempt_FifthAttempt() {
        // Given
        String key = LOGIN_ATTEMPT_PREFIX + TEST_USERNAME;
        when(cacheGateway.increment(key)).thenReturn(5L);

        // When
        boolean isLocked = loginRateLimiterService.recordFailedAttempt(TEST_USERNAME);

        // Then
        assertTrue(isLocked, "第5次失败应被锁定");
        verify(cacheGateway).increment(key);
        verify(cacheGateway, never()).expire(any(), anyLong(), any());
    }

    @Test
    @DisplayName("超过5次登录失败应保持锁定状态")
    void testRecordFailedAttempt_ExceedMaxAttempts() {
        // Given
        String key = LOGIN_ATTEMPT_PREFIX + TEST_USERNAME;
        when(cacheGateway.increment(key)).thenReturn(6L);

        // When
        boolean isLocked = loginRateLimiterService.recordFailedAttempt(TEST_USERNAME);

        // Then
        assertTrue(isLocked, "超过最大次数应保持锁定");
        verify(cacheGateway).increment(key);
    }

    @Test
    @DisplayName("Redis操作失败应返回未锁定")
    void testRecordFailedAttempt_RedisFailure() {
        // Given
        String key = LOGIN_ATTEMPT_PREFIX + TEST_USERNAME;
        when(cacheGateway.increment(key)).thenReturn(null);

        // When
        boolean isLocked = loginRateLimiterService.recordFailedAttempt(TEST_USERNAME);

        // Then
        assertFalse(isLocked, "Redis操作失败应返回未锁定");
        verify(cacheGateway).increment(key);
    }

    @Test
    @DisplayName("检查未锁定账户应返回false")
    void testIsLocked_NotLocked() {
        // Given
        String key = LOGIN_ATTEMPT_PREFIX + TEST_USERNAME;
        when(cacheGateway.get(key)).thenReturn(3);

        // When
        boolean isLocked = loginRateLimiterService.isLocked(TEST_USERNAME);

        // Then
        assertFalse(isLocked, "失败次数小于5应返回未锁定");
        verify(cacheGateway).get(key);
    }

    @Test
    @DisplayName("检查已锁定账户应返回true")
    void testIsLocked_Locked() {
        // Given
        String key = LOGIN_ATTEMPT_PREFIX + TEST_USERNAME;
        when(cacheGateway.get(key)).thenReturn(5);

        // When
        boolean isLocked = loginRateLimiterService.isLocked(TEST_USERNAME);

        // Then
        assertTrue(isLocked, "失败次数达到5应返回锁定");
        verify(cacheGateway).get(key);
    }

    @Test
    @DisplayName("检查无记录账户应返回未锁定")
    void testIsLocked_NoRecord() {
        // Given
        String key = LOGIN_ATTEMPT_PREFIX + TEST_USERNAME;
        when(cacheGateway.get(key)).thenReturn(null);

        // When
        boolean isLocked = loginRateLimiterService.isLocked(TEST_USERNAME);

        // Then
        assertFalse(isLocked, "无记录应返回未锁定");
        verify(cacheGateway).get(key);
    }

    @Test
    @DisplayName("获取剩余尝试次数 - 无失败记录")
    void testGetRemainingAttempts_NoAttempts() {
        // Given
        String key = LOGIN_ATTEMPT_PREFIX + TEST_USERNAME;
        when(cacheGateway.get(key)).thenReturn(null);

        // When
        int remaining = loginRateLimiterService.getRemainingAttempts(TEST_USERNAME);

        // Then
        assertEquals(5, remaining, "无失败记录应返回5次");
        verify(cacheGateway).get(key);
    }

    @Test
    @DisplayName("获取剩余尝试次数 - 已失败2次")
    void testGetRemainingAttempts_TwoAttempts() {
        // Given
        String key = LOGIN_ATTEMPT_PREFIX + TEST_USERNAME;
        when(cacheGateway.get(key)).thenReturn(2);

        // When
        int remaining = loginRateLimiterService.getRemainingAttempts(TEST_USERNAME);

        // Then
        assertEquals(3, remaining, "失败2次应剩余3次");
        verify(cacheGateway).get(key);
    }

    @Test
    @DisplayName("获取剩余尝试次数 - 已锁定")
    void testGetRemainingAttempts_Locked() {
        // Given
        String key = LOGIN_ATTEMPT_PREFIX + TEST_USERNAME;
        when(cacheGateway.get(key)).thenReturn(5);

        // When
        int remaining = loginRateLimiterService.getRemainingAttempts(TEST_USERNAME);

        // Then
        assertEquals(0, remaining, "已锁定应返回0次");
        verify(cacheGateway).get(key);
    }

    @Test
    @DisplayName("获取剩余尝试次数 - 超过最大次数")
    void testGetRemainingAttempts_ExceedMax() {
        // Given
        String key = LOGIN_ATTEMPT_PREFIX + TEST_USERNAME;
        when(cacheGateway.get(key)).thenReturn(10);

        // When
        int remaining = loginRateLimiterService.getRemainingAttempts(TEST_USERNAME);

        // Then
        assertEquals(0, remaining, "超过最大次数应返回0次");
        verify(cacheGateway).get(key);
    }

    @Test
    @DisplayName("清除失败记录应删除Redis键")
    void testClearAttempts() {
        // Given
        String key = LOGIN_ATTEMPT_PREFIX + TEST_USERNAME;
        when(cacheGateway.delete(key)).thenReturn(true);

        // When
        loginRateLimiterService.clearAttempts(TEST_USERNAME);

        // Then
        verify(cacheGateway).delete(key);
    }

    @Test
    @DisplayName("获取锁定剩余时间 - 有TTL")
    void testGetLockRemainingTime_WithTTL() {
        // Given
        String key = LOGIN_ATTEMPT_PREFIX + TEST_USERNAME;
        when(cacheGateway.getExpire(key, TimeUnit.SECONDS)).thenReturn(1800L);

        // When
        long remainingTime = loginRateLimiterService.getLockRemainingTime(TEST_USERNAME);

        // Then
        assertEquals(1800L, remainingTime, "应返回正确的TTL");
        verify(cacheGateway).getExpire(key, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("获取锁定剩余时间 - 无TTL")
    void testGetLockRemainingTime_NoTTL() {
        // Given
        String key = LOGIN_ATTEMPT_PREFIX + TEST_USERNAME;
        when(cacheGateway.getExpire(key, TimeUnit.SECONDS)).thenReturn(null);

        // When
        long remainingTime = loginRateLimiterService.getLockRemainingTime(TEST_USERNAME);

        // Then
        assertEquals(-1L, remainingTime, "无TTL应返回-1");
        verify(cacheGateway).getExpire(key, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("处理Long类型的失败次数")
    void testGetCurrentAttempts_LongType() {
        // Given
        String key = LOGIN_ATTEMPT_PREFIX + TEST_USERNAME;
        when(cacheGateway.get(key)).thenReturn(3L);

        // When
        boolean isLocked = loginRateLimiterService.isLocked(TEST_USERNAME);

        // Then
        assertFalse(isLocked, "应正确处理Long类型");
        verify(cacheGateway).get(key);
    }

    @Test
    @DisplayName("处理String类型的失败次数")
    void testGetCurrentAttempts_StringType() {
        // Given
        String key = LOGIN_ATTEMPT_PREFIX + TEST_USERNAME;
        when(cacheGateway.get(key)).thenReturn("4");

        // When
        boolean isLocked = loginRateLimiterService.isLocked(TEST_USERNAME);

        // Then
        assertFalse(isLocked, "应正确处理String类型");
        verify(cacheGateway).get(key);
    }

    @Test
    @DisplayName("处理无效的失败次数格式")
    void testGetCurrentAttempts_InvalidFormat() {
        // Given
        String key = LOGIN_ATTEMPT_PREFIX + TEST_USERNAME;
        when(cacheGateway.get(key)).thenReturn("invalid");

        // When
        boolean isLocked = loginRateLimiterService.isLocked(TEST_USERNAME);

        // Then
        assertFalse(isLocked, "无效格式应返回未锁定");
        verify(cacheGateway).get(key);
    }

    @Test
    @DisplayName("完整流程测试 - 从失败到锁定再到清除")
    void testCompleteFlow() {
        String key = LOGIN_ATTEMPT_PREFIX + TEST_USERNAME;

        // 第1次失败
        when(cacheGateway.increment(key)).thenReturn(1L);
        when(cacheGateway.expire(eq(key), eq(30L), eq(TimeUnit.MINUTES))).thenReturn(true);
        assertFalse(loginRateLimiterService.recordFailedAttempt(TEST_USERNAME));

        // 检查剩余次数
        when(cacheGateway.get(key)).thenReturn(1);
        assertEquals(4, loginRateLimiterService.getRemainingAttempts(TEST_USERNAME));

        // 第5次失败 - 锁定
        when(cacheGateway.increment(key)).thenReturn(5L);
        assertTrue(loginRateLimiterService.recordFailedAttempt(TEST_USERNAME));

        // 检查锁定状态
        when(cacheGateway.get(key)).thenReturn(5);
        assertTrue(loginRateLimiterService.isLocked(TEST_USERNAME));
        assertEquals(0, loginRateLimiterService.getRemainingAttempts(TEST_USERNAME));

        // 清除记录
        when(cacheGateway.delete(key)).thenReturn(true);
        loginRateLimiterService.clearAttempts(TEST_USERNAME);

        // 验证清除后状态
        when(cacheGateway.get(key)).thenReturn(null);
        assertFalse(loginRateLimiterService.isLocked(TEST_USERNAME));
        assertEquals(5, loginRateLimiterService.getRemainingAttempts(TEST_USERNAME));
    }

    // ---- 日志脱敏(008 T014 / R5 MEDIUM-2)----

    @Test
    @DisplayName("INCR 失败日志不含原文 username(maskUsername)")
    void recordFailedAttempt_redisIncrNull_logsMaskedUsername(CapturedOutput output) {
        when(cacheGateway.increment(anyString())).thenReturn(null);

        assertFalse(loginRateLimiterService.recordFailedAttempt(TEST_USERNAME));

        assertFalse(output.getAll().contains(TEST_USERNAME), "日志不得含原文 username");
        assertTrue(output.getAll().contains("te****r"), "日志应含 maskUsername 后形式");
    }

    @Test
    @DisplayName("锁定日志不含原文 username(maskUsername)")
    void recordFailedAttempt_locked_logsMaskedUsername(CapturedOutput output) {
        String key = LOGIN_ATTEMPT_PREFIX + TEST_USERNAME;
        when(cacheGateway.increment(key)).thenReturn(5L);

        assertTrue(loginRateLimiterService.recordFailedAttempt(TEST_USERNAME));

        assertFalse(output.getAll().contains(TEST_USERNAME), "日志不得含原文 username");
        assertTrue(output.getAll().contains("te****r"), "锁定日志应含 maskUsername 后形式");
    }

    @Test
    @DisplayName("清除记录日志不含原文 username(maskUsername)")
    void clearAttempts_logsMaskedUsername(CapturedOutput output) {
        String key = LOGIN_ATTEMPT_PREFIX + TEST_USERNAME;
        when(cacheGateway.delete(key)).thenReturn(true);

        loginRateLimiterService.clearAttempts(TEST_USERNAME);

        assertFalse(output.getAll().contains(TEST_USERNAME), "日志不得含原文 username");
        assertTrue(output.getAll().contains("te****r"), "清除日志应含 maskUsername 后形式");
    }
}
