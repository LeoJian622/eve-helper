package xyz.foolcat.eve.evehelper.infrastructure.external.esi.cache;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Assets;
import xyz.foolcat.eve.evehelper.domain.port.cache.CacheGateway;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * EsiResponseCache 单元测试(TDD 红-绿)。
 *
 * <p>覆盖 spec FR-004/FR-005/FR-008/FR-009 语义:</p>
 * <ul>
 *     <li>键构造:鉴权数据按 userId 隔离、公开数据置 pub、不含 accessToken</li>
 *     <li>getList:JSON 往返还原具体领域类型(防 LinkedHashMap)</li>
 *     <li>getInt:Integer 往返</li>
 *     <li>fail-open:序列化/Redis 异常 → 返回 null 不抛(由上游真调),不阻断业务</li>
 * </ul>
 */
@DisplayName("EsiResponseCache ESI 响应缓存")
class EsiResponseCacheTest {

    private CacheGateway cacheGateway;
    private ObjectMapper objectMapper;
    private EsiResponseCache cache;

    @BeforeEach
    void setUp() {
        cacheGateway = mock(CacheGateway.class);
        objectMapper = new ObjectMapper();
        cache = new EsiResponseCache(cacheGateway, objectMapper);
    }

    // ── 键构造 ──

    @Test
    @DisplayName("键构造:鉴权数据含 userId")
    void cacheKey_private_containsUserId() {
        String key = cache.cacheKey("serenity", 42, "/characters/7/assets/", 1);
        assertThat(key).isEqualTo("esi:resp:serenity:42:/characters/7/assets/:1");
    }

    @Test
    @DisplayName("键构造:公开数据置 pub 全局共享")
    void cacheKey_public_usesPub() {
        String key = cache.cacheKey("serenity", null, "/universe/types/34/", 0);
        assertThat(key).isEqualTo("esi:resp:serenity:pub:/universe/types/34/:0");
    }

    @Test
    @DisplayName("键构造:pageToken 字符串后缀(如 maxPage 独立键)")
    void cacheKey_maxPageToken() {
        String key = cache.cacheKey("serenity", 42, "/characters/7/assets/", "maxPage");
        assertThat(key).isEqualTo("esi:resp:serenity:42:/characters/7/assets/:maxPage");
    }

    // ── getList 类型往返 ──

    private JavaType assetsType() {
        return TypeFactory.defaultInstance().constructType(Assets.class);
    }

    @Test
    @DisplayName("getList:JSON 往返还原 List<Assets>(非 LinkedHashMap)")
    void getList_roundTripsToTypedList() {
        String json = "[{\"itemId\":100,\"quantity\":5}]";
        when(cacheGateway.get("k")).thenReturn(json);

        List<Assets> result = cache.getList("k", assetsType());
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isInstanceOf(Assets.class);
        assertThat(result.get(0).getItemId()).isEqualTo(100L);
        assertThat(result.get(0).getQuantity()).isEqualTo(5L);
    }

    @Test
    @DisplayName("getList:缓存 null → null")
    void getList_cacheMiss_returnsNull() {
        when(cacheGateway.get("k")).thenReturn(null);
        assertThat(cache.getList("k", assetsType())).isNull();
    }

    @Test
    @DisplayName("getList:损坏 JSON → null(fail-open 不抛)")
    void getList_corruptJson_returnsNull() {
        when(cacheGateway.get("k")).thenReturn("not-json");
        assertThat(cache.getList("k", assetsType())).isNull();
    }

    // ── getInt ──

    @Test
    @DisplayName("getInt:Integer 往返")
    void getInt_roundTrips() {
        when(cacheGateway.get("k")).thenReturn(20);
        assertThat(cache.getInt("k")).isEqualTo(20);
    }

    @Test
    @DisplayName("getInt:缓存不存在 → null")
    void getInt_miss_returnsNull() {
        when(cacheGateway.get("k")).thenReturn(null);
        assertThat(cache.getInt("k")).isNull();
    }

    @Test
    @DisplayName("getInt:非整数 → null(fail-open)")
    void getInt_notInteger_returnsNull() {
        when(cacheGateway.get("k")).thenReturn("oops");
        assertThat(cache.getInt("k")).isNull();
    }

    // ── put / fail-open ──

    @Test
    @DisplayName("put:按 TTL 写入缓存")
    void put_storesWithTtl() {
        cache.put("k", 20, Duration.ofSeconds(3600));
        verify(cacheGateway).set(eq("k"), eq(20), eq(3600L), eq(TimeUnit.SECONDS));
    }

    // ── 防穿透 claim ──

    @Test
    @DisplayName("tryClaim:占位成功 → true(本次承担真调)")
    void tryClaim_acquire_returnsTrue() {
        when(cacheGateway.setIfAbsent(eq("k:lock"), eq(true), eq(5000L), eq(TimeUnit.MILLISECONDS)))
                .thenReturn(Boolean.TRUE);
        assertThat(cache.tryClaim("k", Duration.ofSeconds(5))).isTrue();
    }

    @Test
    @DisplayName("tryClaim:已有其他请求在填 → false(本次复用等待)")
    void tryClaim_alreadyClaimed_returnsFalse() {
        when(cacheGateway.setIfAbsent(eq("k:lock"), eq(true), eq(5000L), eq(TimeUnit.MILLISECONDS)))
                .thenReturn(Boolean.FALSE);
        assertThat(cache.tryClaim("k", Duration.ofSeconds(5))).isFalse();
    }

    @Test
    @DisplayName("tryClaim:Redis 异常 → false(fail-open,视为未抢到锁,不阻断真调路径)")
    void tryClaim_redisException_returnsFalse() {
        when(cacheGateway.setIfAbsent(eq("k:lock"), eq(true), eq(5000L), eq(TimeUnit.MILLISECONDS)))
                .thenThrow(new RuntimeException("redis down"));
        assertThat(cache.tryClaim("k", Duration.ofSeconds(5))).isFalse();
    }

    @Test
    @DisplayName("releaseClaim:清楚占位锁")
    void releaseClaim_deletesLock() {
        cache.releaseClaim("k");
        verify(cacheGateway).delete("k:lock");
    }

    @Test
    @DisplayName("getList:底层 Redis 异常 → null 不抛(fail-open)")
    void getList_redisException_failOpen() {
        when(cacheGateway.get("k")).thenThrow(new RuntimeException("redis down"));
        assertThat(cache.getList("k", assetsType())).isNull();
    }
}