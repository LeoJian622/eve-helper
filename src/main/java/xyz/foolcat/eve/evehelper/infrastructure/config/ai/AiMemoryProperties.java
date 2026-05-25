package xyz.foolcat.eve.evehelper.infrastructure.config.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 会话记忆配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "spring.ai.memory")
public class AiMemoryProperties {

    /**
     * 记忆存储类型: inmemory / redis
     */
    private String type = "inmemory";

    /**
     * Redis 过期时间（分钟），默认 24 小时
     */
    private long redisTtlMinutes = 1440;

    /**
     * 是否启用 TTL 刷新
     */
    private boolean refreshTtl = true;
}
