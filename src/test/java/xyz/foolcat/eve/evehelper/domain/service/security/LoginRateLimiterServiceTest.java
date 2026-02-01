package xyz.foolcat.eve.evehelper.domain.service.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * LoginRateLimiterService 单元测试
 * 使用 Mockito 模拟 Redis 操作
 *
 * @author Leojan
 * date 2026-02-01
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("登录限流服务单元测试")
class LoginRateLimiterServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private LoginRateLimiterService loginRateLimiterService;

    private static final String TEST_USERNAME = "testuser";
    private static final String KEY_PREFIX = "login:attempt:";

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("首次登录失败应记录失败次数为1")
    void testRecordFailedAttempt_FirstAttempt() {
        // Given
        when(valueOperations.get(anyString())).thenReturn(null);

        // When
        boolean isLocked = loginRateLimiterService.recordFailedAttempt(TEST_USERNAME);

        // Then
        assertFalse(isLocked, "首次失败不应被锁定");
        verify(valueOperations).set(eq(KEY_PREFIX + TEST_USERNAME), eq(1), eq(30L), eq(TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("连续失败4次不应被锁定")
    void testRecordFailedAttempt_FourthAttempt() {
        // Given
        when(valueOperations.get(anyString())).thenReturn(3);

        // When
        boolean isLocked = loginRateLimiterService.recordFailedAttempt(TEST_USERNAME);

        // Then
        assertFalse(isLocked, "失败4次不应被锁定");
        verify(valueOperations).set(eq(KEY_PREFIX + TEST_USERNAME), eq(4), eq(30L), eq(TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("连续失败5次应被锁定")
    void testRecordFailedAttempt_FifthAttempt() {
        // Given
        when(valueOperations.get(anyString())).thenReturn(4);

        // When
        boolean isLocked = loginRateLimiterService.recordFailedAttempt(TEST_USERNAME);

        // Then
        assertTrue(isLocked, "失败5次应被锁定");
        verify(valueOperations).set(eq(KEY_PREFIX + TEST_USERNAME), eq(5), eq(30L), eq(TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("超过5次失败应保持锁定状态")
    void testRecordFailedAttempt_MoreThanFiveAttempts() {
        // Given
        when(valueOperations.get(anyString())).thenReturn(5);

        // When
        boolean isLocked = loginRateLimiterService.recordFailedAttempt(TEST_USERNAME);

        // Then
        assertTrue(isLocked, "超过5次失败应保持锁定");
        verify(valueOperations).set(eq(KEY_PREFIX + TEST_USERNAME), eq(6), eq(30L), eq(TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("检查未失败的用户不应被锁定")
    void testIsLocked_NoAttempts() {
        // Given
        when(valueOperations.get(anyString())).thenReturn(null);

        // When
        boolean isLocked = loginRateLimiterService.isLocked(TEST_USERNAME);

        // Then
        assertFalse(isLocked, "未失败的用户不应被锁定");
    }

    @Test
    @DisplayName("检查失败4次的用户不应被锁定")
    void testIsLocked_FourAttempts() {
        // Given
        when(valueOperations.get(anyString())).thenReturn(4);

        // When
        boolean isLocked = loginRateLimiterService.isLocked(TEST_USERNAME);

        // Then
        assertFalse(isLocked, "失败4次的用户不应被锁定");
    }

    @Test
    @DisplayName("检查失败5次的用户应被锁定")
    void testIsLocked_FiveAttempts() {
        // Given
        when(valueOperations.get(anyString())).thenReturn(5);

        // When
        boolean isLocked = loginRateLimiterService.isLocked(TEST_USERNAME);

        // Then
        assertTrue(isLocked, "失败5次的用户应被锁定");
    }

    @Test
    @DisplayName("获取剩余尝试次数 - 未失败")
    void testGetRemainingAttempts_NoAttempts() {
        // Given
        when(valueOperations.get(anyString())).thenReturn(null);

        // When
        int remaining = loginRateLimiterService.getRemainingAttempts(TEST_USERNAME);

        // Then
        assertEquals(5, remaining, "未失败应有5次剩余机会");
    }

    @Test
    @DisplayName("获取剩余尝试次数 - 失败2次")
    void testGetRemainingAttempts_TwoAttempts() {
        // Given
        when(valueOperations.get(anyString())).thenReturn(2);

        // When
        int remaining = loginRateLimiterService.getRemainingAttempts(TEST_USERNAME);

        // Then
        assertEquals(3, remaining, "失败2次应有3次剩余机会");
    }

    @Test
    @DisplayName("获取剩余尝试次数 - 已锁定")
    void testGetRemainingAttempts_Locked() {
        // Given
        when(valueOperations.get(anyString())).thenReturn(5);

        // When
        int remaining = loginRateLimiterService.getRemainingAttempts(TEST_USERNAME);

        // Then
        assertEquals(0, remaining, "已锁定应无剩余机会");
    }

    @Test
    @DisplayName("清除失败记录")
    void testClearAttempts() {
        // When
        loginRateLimiterService.clearAttempts(TEST_USERNAME);

        // Then
        verify(redisTemplate).delete(KEY_PREFIX + TEST_USERNAME);
    }

    @Test
    @DisplayName("获取锁定剩余时间")
    void testGetLockRemainingTime() {
        // Given
        long expectedTtl = 1800L; // 30分钟
        when(redisTemplate.getExpire(anyString(), eq(TimeUnit.SECONDS))).thenReturn(expectedTtl);

        // When
        long remainingTime = loginRateLimiterService.getLockRemainingTime(TEST_USERNAME);

        // Then
        assertEquals(expectedTtl, remainingTime, "应返回正确的剩余时间");
        verify(redisTemplate).getExpire(KEY_PREFIX + TEST_USERNAME, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("获取锁定剩余时间 - 未锁定")
    void testGetLockRemainingTime_NotLocked() {
        // Given
        when(redisTemplate.getExpire(anyString(), eq(TimeUnit.SECONDS))).thenReturn(null);

        // When
        long remainingTime = loginRateLimiterService.getLockRemainingTime(TEST_USERNAME);

        // Then
        assertEquals(-1, remainingTime, "未锁定应返回-1");
    }

    @Test
    @DisplayName("处理Long类型的失败次数")
    void testRecordFailedAttempt_LongValue() {
        // Given
        when(valueOperations.get(anyString())).thenReturn(2L);

        // When
        boolean isLocked = loginRateLimiterService.recordFailedAttempt(TEST_USERNAME);

        // Then
        assertFalse(isLocked, "失败3次不应被锁定");
        verify(valueOperations).set(eq(KEY_PREFIX + TEST_USERNAME), eq(3), eq(30L), eq(TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("处理String类型的失败次数")
    void testRecordFailedAttempt_StringValue() {
        // Given
        when(valueOperations.get(anyString())).thenReturn("3");

        // When
        boolean isLocked = loginRateLimiterService.recordFailedAttempt(TEST_USERNAME);

        // Then
        assertFalse(isLocked, "失败4次不应被锁定");
        verify(valueOperations).set(eq(KEY_PREFIX + TEST_USERNAME), eq(4), eq(30L), eq(TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("处理无效的失败次数格式")
    void testRecordFailedAttempt_InvalidValue() {
        // Given
        when(valueOperations.get(anyString())).thenReturn("invalid");

        // When
        boolean isLocked = loginRateLimiterService.recordFailedAttempt(TEST_USERNAME);

        // Then
        assertFalse(isLocked, "无效值应视为0次失败");
        verify(valueOperations).set(eq(KEY_PREFIX + TEST_USERNAME), eq(1), eq(30L), eq(TimeUnit.MINUTES));
    }
}
