package xyz.foolcat.eve.evehelper.infrastructure.external.esi.cache;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EsiCacheTtlExtractor 单元测试(TDD 红-绿)。
 *
 * <p>覆盖 spec FR-002/FR-003 的 TTL 解析语义:</p>
 * <ul>
 *     <li>max-age(容忍空白/引号/单位)优先,≤0 → null(不缓存)</li>
 *     <li>退回 Expires(RFC1123);已过期/不可解析 → null</li>
 *     <li>两者皆无 → null(不缓存,每次真调)</li>
 * </ul>
 */
@DisplayName("EsiCacheTtlExtractor ESI 响应头 TTL 提取")
class EsiCacheTtlExtractorTest {

    private final EsiCacheTtlExtractor extractor = new EsiCacheTtlExtractor();

    private HttpHeaders headers(String cacheControl, String expires) {
        HttpHeaders h = new HttpHeaders();
        if (cacheControl != null) h.add(HttpHeaders.CACHE_CONTROL, cacheControl);
        if (expires != null) h.add(HttpHeaders.EXPIRES, expires);
        return h;
    }

    private String futureExpires() {
        return DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC).plusHours(1));
    }

    private String pastExpires() {
        return DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC).minusHours(1));
    }

    @Test
    @DisplayName("max-age=3600 → PT3600S")
    void maxAge_returnsDuration() {
        assertThat(extractor.extract(headers("max-age=3600", null)))
                .isEqualTo(Duration.ofSeconds(3600));
    }

    @Test
    @DisplayName("max-age 带空白与引号 → 容忍解析")
    void maxAge_withSpacesAndQuotes_parses() {
        assertThat(extractor.extract(headers("max-age = 300", null)))
                .isEqualTo(Duration.ofSeconds(300));
        assertThat(extractor.extract(headers("max-age=\"120\"", null)))
                .isEqualTo(Duration.ofSeconds(120));
        assertThat(extractor.extract(headers("no-cache, max-age=600", null)))
                .isEqualTo(Duration.ofSeconds(600));
    }

    @Test
    @DisplayName("max-age=0 → null(不缓存)")
    void maxAge_zero_returnsNull() {
        assertThat(extractor.extract(headers("max-age=0", null))).isNull();
        assertThat(extractor.extract(headers("max-age=-5", null))).isNull();
    }

    @Test
    @DisplayName("仅 Expires 未来时间 → 对应 Duration")
    void expires_future_returnsDuration() {
        Duration d = extractor.extract(headers(null, futureExpires()));
        assertThat(d).isNotNull();
        assertThat(d).isPositive();
    }

    @Test
    @DisplayName("Expires 已过期 → null")
    void expires_past_returnsNull() {
        assertThat(extractor.extract(headers(null, pastExpires()))).isNull();
    }

    @Test
    @DisplayName("Expires 不可解析 → null")
    void expires_unparseable_returnsNull() {
        assertThat(extractor.extract(headers(null, "not-a-date"))).isNull();
    }

    @Test
    @DisplayName("空头(null) → null(不缓存)")
    void noHeaders_returnsNull() {
        assertThat(extractor.extract(null)).isNull();
        assertThat(extractor.extract(new HttpHeaders())).isNull();
    }

    @Test
    @DisplayName("max-age 与 Expires 并存 → max-age 优先")
    void maxAge_wins_overExpires() {
        Duration d = extractor.extract(headers("max-age=300", futureExpires()));
        assertThat(d).isEqualTo(Duration.ofSeconds(300));
    }

    @Test
    @DisplayName("高位 'max-age' 大小写不敏感")
    void maxAge_caseInsensitive() {
        assertThat(extractor.extract(headers("MAX-AGE=60", null)))
                .isEqualTo(Duration.ofSeconds(60));
    }
}