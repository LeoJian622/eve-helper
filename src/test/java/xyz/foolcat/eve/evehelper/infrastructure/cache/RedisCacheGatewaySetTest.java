package xyz.foolcat.eve.evehelper.infrastructure.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RedisCacheGateway Set 操作的单元测试(TDD 红-绿)。
 *
 * <p>纯 Mockito 单测(不起 Spring 上下文),mock {@link SetOperations},
 * 验证三个新增 Set 操作的委托行为:</p>
 * <ul>
 *     <li>{@link RedisCacheGateway#sAdd(String, Object...)} —— 返回新增元素数</li>
 *     <li>{@link RedisCacheGateway#sMembers(String)} —— 返回集合全部成员</li>
 *     <li>{@link RedisCacheGateway#sRemove(String, Object...)} —— 返回移除元素数</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RedisCacheGateway Set 操作")
class RedisCacheGatewaySetTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private SetOperations<String, Object> setOperations;

    private RedisCacheGateway gateway;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        gateway = new RedisCacheGateway(redisTemplate);
    }

    @Test
    @DisplayName("sAdd 返回新增元素数")
    void sAdd_returnsAddedCount() {
        String key = "test:set";
        when(setOperations.add(key, "a", "b")).thenReturn(2L);

        Long result = gateway.sAdd(key, "a", "b");

        assertThat(result).isEqualTo(2L);
        verify(setOperations).add(key, "a", "b");
    }

    @Test
    @DisplayName("sMembers 返回集合全部成员")
    void sMembers_returnsAllMembers() {
        String key = "test:set";
        Set<Object> expected = Set.of("a", "b", "c");
        when(setOperations.members(key)).thenReturn(expected);

        Set<Object> result = gateway.sMembers(key);

        assertThat(result).isEqualTo(expected);
        verify(setOperations).members(key);
    }

    @Test
    @DisplayName("sRemove 返回移除元素数")
    void sRemove_returnsRemovedCount() {
        String key = "test:set";
        when(setOperations.remove(key, "a")).thenReturn(1L);

        Long result = gateway.sRemove(key, "a");

        assertThat(result).isEqualTo(1L);
        verify(setOperations).remove(key, "a");
    }
}
