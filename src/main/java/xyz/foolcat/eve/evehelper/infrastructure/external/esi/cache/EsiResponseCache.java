package xyz.foolcat.eve.evehelper.infrastructure.external.esi.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import xyz.foolcat.eve.evehelper.domain.port.cache.CacheGateway;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * ESI 业务响应的本地缓存操作(Redis)。
 * <p>
 * 键格式 {@code esi:resp:{datasource}:{userId|pub}:{normalized-uri}:{page}}：
 * <ul>
 *     <li>鉴权数据按 userId 隔离（FR-004），杜绝跨用户读到他人数据（IDOR）；</li>
 *     <li>公开数据（类型/ID→名称）userId 段置 {@code pub} 全局共享；</li>
 *     <li>键不含 accessToken（FR-005），token 20 分钟轮换不驱逐业务缓存。</li>
 * </ul>
 * <p>
 * 全局 {@code RedisTemplate} 用 {@code Jackson2JsonRedisSerializer<Object>}，反序列化到
 * {@code Object} 无类型头时列表会还原为 {@code LinkedHashMap}。故列表读写在此处**显式按具体
 * 领域类型往返**（参数化 {@link JavaType}），避免 cast 失败。
 * <p>
 * fail-open（FR-009）：读/反序列化/写任一异常均被吞掉并记日志，返回 null，由上游真实请求 ESI，
 * 不因缓存故障阻断业务。
 *
 * @author Leojan
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EsiResponseCache {

    private static final String KEY_PREFIX = "esi:resp";
    private static final String PUB = "pub";

    private final CacheGateway cacheGateway;
    private final ObjectMapper objectMapper;

    /**
     * 构造缓存键。
     *
     * @param datasource ESI 数据源（serenity/tranquility）
     * @param userId     当前用户 ID；null 表示公开数据（置 pub）
     * @param uri        归一化请求 URI（含路径参数，不含 page）
     * @param page       页码；非分页接口传 0
     */
    public String cacheKey(String datasource, Integer userId, String uri, int page) {
        String scope = userId == null ? PUB : String.valueOf(userId);
        return KEY_PREFIX + ":" + datasource + ":" + scope + ":" + uri + ":" + page;
    }

    /**
     * 读取并按具体元素类型还原列表。未命中或反序列化失败 → null（fail-open）。
     *
     * @param elementType 列表元素的具体领域类型（如 {@code Assets.class}）
     */
    public <T> List<T> getList(String key, JavaType elementType) {
        try {
            Object raw = cacheGateway.get(key);
            if (raw == null) {
                return null;
            }
            String json = stringify(raw);
            if (json == null) {
                return null;
            }
            JavaType listType = objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, elementType);
            return objectMapper.readValue(json, listType);
        } catch (Exception e) {
            log.warn("ESI 响应缓存列表读取失败,按未命中处理: key={}", key, e);
            return null;
        }
    }

    /**
     * 读取 Integer（maxPage）。未命中或非整数 → null（fail-open）。
     */
    public Integer getInt(String key) {
        try {
            Object raw = cacheGateway.get(key);
            if (raw instanceof Number n) {
                return n.intValue();
            }
            if (raw instanceof String s) {
                try {
                    return Integer.valueOf(s.trim());
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
            return null;
        } catch (Exception e) {
            log.warn("ESI 响应缓存 Integer 读取失败,按未命中处理: key={}", key, e);
            return null;
        }
    }

    /**
     * 写入缓存。
     *
     * @param ttl 缓存失效时长；null 或非正 → 不写（FR-003，与提取器 null 语义一致）
     */
    public void put(String key, Object value, Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            return;
        }
        try {
            Object serialized = value;
            if (value instanceof List<?> || value instanceof String) {
                serialized = objectMapper.writeValueAsString(value);
            }
            cacheGateway.set(key, serialized, ttl.getSeconds(), TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            log.warn("ESI 响应缓存序列化失败,跳过本次缓存: key={}", key, e);
        } catch (Exception e) {
            log.warn("ESI 响应缓存写入失败,跳过本次缓存: key={}", key, e);
        }
    }

    /**
     * 防并发穿透：tryClaim 获取本次真实调用的"占位锁"。
     * <p>
     * 用 {@code key:lock} 的 {@code setIfAbsent} 原子占位：返回 true 表示本请求承担实时真调并回填；
     * 返回 false 表示已有其他请求在填，调用方应尝试等待复用其回填结果（FR-008）。
     * <p>
     * fail-open：Redis 异常返回 false（视为未抢到锁），不阻断业务真调路径。
     *
     * @param ttl 占位锁存活时长，应覆盖真调 + 回填的最坏耗时
     */
    public boolean tryClaim(String key, Duration ttl) {
        try {
            return Boolean.TRUE.equals(cacheGateway.setIfAbsent(key + ":lock", true,
                    ttl.toMillis(), TimeUnit.MILLISECONDS));
        } catch (Exception e) {
            log.warn("ESI 响应缓存防穿透占位失败,按未抢到锁处理: key={}", key, e);
            return false;
        }
    }

    /**
     * 防并发穿透：真调完成并回填后释放占位锁。
     */
    public void releaseClaim(String key) {
        try {
            cacheGateway.delete(key + ":lock");
        } catch (Exception e) {
            log.warn("ESI 响应缓存防穿透释放占位失败(锁有 TTL 兜底): key={}", key, e);
        }
    }

    /**
     * 将缓存返回值统一为 JSON 字符串；已是字符串直接返回，否则序列化。
     */
    private String stringify(Object raw) {
        try {
            if (raw instanceof String s) {
                return s;
            }
            return objectMapper.writeValueAsString(raw);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}