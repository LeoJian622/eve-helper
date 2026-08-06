package xyz.foolcat.eve.evehelper.infrastructure.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import xyz.foolcat.eve.evehelper.domain.port.cache.CacheGateway;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Redis 缓存端口适配器,实现 {@link CacheGateway}。
 * <p>
 * 封装 RedisTemplate 的字符串键/对象值操作,供领域层通过端口访问缓存。
 *
 * @author Leojan
 */
@Component
@RequiredArgsConstructor
public class RedisCacheGateway implements CacheGateway {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void set(String key, Object value, long ttl, TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(key, value, ttl, timeUnit);
    }

    @Override
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public Boolean setIfAbsent(String key, Object value, long ttl, TimeUnit timeUnit) {
        return redisTemplate.opsForValue().setIfAbsent(key, value, ttl, timeUnit);
    }

    @Override
    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    @Override
    public Long delete(Collection<String> keys) {
        return redisTemplate.delete(keys);
    }

    @Override
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    @Override
    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    @Override
    public Boolean expire(String key, long ttl, TimeUnit timeUnit) {
        return redisTemplate.expire(key, ttl, timeUnit);
    }

    @Override
    public Long getExpire(String key, TimeUnit timeUnit) {
        return redisTemplate.getExpire(key, timeUnit);
    }

    @Override
    public void putAllHash(String key, Map<String, ?> map) {
        redisTemplate.opsForHash().putAll(key, map);
    }

    @Override
    public void convertAndSend(String channel, Object message) {
        redisTemplate.convertAndSend(channel, message);
    }
}