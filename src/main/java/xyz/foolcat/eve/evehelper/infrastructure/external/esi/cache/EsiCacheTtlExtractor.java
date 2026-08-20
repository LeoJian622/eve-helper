package xyz.foolcat.eve.evehelper.infrastructure.external.esi.cache;

import cn.hutool.core.util.StrUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从 ESI 响应头解析缓存失效时长。
 * <p>
 * 覆盖 spec FR-002/FR-003 语义：优先 {@code Cache-Control: max-age}，退回 {@code Expires}（RFC1123）；
 * 两者皆缺失、max-age ≤ 0、Expires 已过期或不可解析 → 返回 {@code null}（不缓存、每次真调）。
 * <p>
 * null 即"不缓存"语义，由调用方据此跳过缓存写、每次真实请求 ESI（避免为未知 TTL 猜默认时长导致陈旧滞留）。
 *
 * @author Leojan
 */
@Component
public class EsiCacheTtlExtractor {

    /**
     * max-age 正则：容忍空白与可选引号，形如 max-age=3600 / max-age = 300 / max-age="120"
     */
    private static final Pattern MAX_AGE = Pattern.compile("(?i)max-age\\s*=\\s*\"?(\\d+)\"?");

    /**
     * 提取缓存失效时长。
     *
     * @param headers ESI 响应头；null 或空头 → 返回 null（不缓存）
     * @return 缓存时长；无法确定（无头/max-age≤0/Expires 过期或不可解析）→ null
     */
    public Duration extract(HttpHeaders headers) {
        if (headers == null || headers.isEmpty()) {
            return null;
        }
        Long maxAge = parseMaxAge(headers);
        if (maxAge != null) {
            return maxAge > 0 ? Duration.ofSeconds(maxAge) : null;
        }
        return parseExpires(headers);
    }

    /**
     * 解析 Cache-Control 中的 max-age；不存在返回 null（交由 Expires 分支）。
     */
    private Long parseMaxAge(HttpHeaders headers) {
        String cacheControl = headers.getFirst(HttpHeaders.CACHE_CONTROL);
        if (StrUtil.isBlank(cacheControl)) {
            return null;
        }
        Matcher m = MAX_AGE.matcher(cacheControl);
        if (m.find()) {
            try {
                return Long.parseLong(m.group(1));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    /**
     * 解析 Expires（HTTP-date，RFC1123）。
     * 已过期或不可解析 → null（不缓存）。
     */
    private Duration parseExpires(HttpHeaders headers) {
        String expires = headers.getFirst(HttpHeaders.EXPIRES);
        if (StrUtil.isBlank(expires)) {
            return null;
        }
        try {
            ZonedDateTime expiresAt = ZonedDateTime.parse(expires, DateTimeFormatter.RFC_1123_DATE_TIME);
            Instant now = Instant.now();
            Duration remaining = Duration.between(now, expiresAt);
            return remaining.isNegative() || remaining.isZero() ? null : remaining;
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}