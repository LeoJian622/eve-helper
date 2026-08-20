package xyz.foolcat.eve.evehelper.infrastructure.external.esi.cache;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * ESI 响应缓存配置载体。
 * <p>
 * 强刷固定时间窗与窗内真调额度、默认数据源。走 {@code eve.esi.cache.*} 前缀，可被配置文件覆盖。
 *
 * @author Leojan
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "eve.esi.cache")
public class EsiResponseCacheProperties {

    /**
     * 强制刷新固定时间窗，分钟。
     */
    private int forceRefreshWindowMinutes = 60;

    /**
     * 固定时间窗内单键允许的真实刷新次数。
     */
    private int forceRefreshLimit = 5;

    /**
     * 默认数据源。
     */
    private String datasource = "serenity";
}