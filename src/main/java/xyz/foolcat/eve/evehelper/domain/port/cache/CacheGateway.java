package xyz.foolcat.eve.evehelper.domain.port.cache;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 缓存访问端口(缓存/限流/黑名单/权限契约)。
 * <p>
 * 领域层仅依赖本接口读写缓存,不直接操作 RedisTemplate。
 * 由基础设施层 {@code infrastructure/cache/RedisCacheGateway} 实现。
 *
 * @author Leojan
 */
public interface CacheGateway {

    /**
     * 写入带 TTL 的键值。
     */
    void set(String key, Object value, long ttl, TimeUnit timeUnit);

    /**
     * 读取键值,不存在返回 null。
     */
    Object get(String key);

    /**
     * 仅当键不存在时写入(SETNX),避免竞态投毒。
     *
     * @return true 表示写入成功(原先不存在)
     */
    Boolean setIfAbsent(String key, Object value, long ttl, TimeUnit timeUnit);

    /**
     * 删除单键。
     *
     * @return true 表示键存在并已删除
     */
    Boolean delete(String key);

    /**
     * 批量删除键。
     *
     * @return 删除的键数量
     */
    Long delete(Collection<String> keys);

    /**
     * 判断键是否存在。
     */
    Boolean hasKey(String key);

    /**
     * 自增计数并返回新值。
     */
    Long increment(String key);

    /**
     * 为键设置新 TTL。
     *
     * @return true 表示设置成功
     */
    Boolean expire(String key, long ttl, TimeUnit timeUnit);

    /**
     * 查询键剩余 TTL。
     *
     * @return 剩余时间,键不存在返回 -2、无 TTL 返回 -1
     */
    Long getExpire(String key, TimeUnit timeUnit);

    /**
     * 批量写入 Hash 字段。
     */
    void putAllHash(String key, Map<String, ?> map);

    /**
     * 发布消息到指定通道。
     */
    void convertAndSend(String channel, Object message);
}