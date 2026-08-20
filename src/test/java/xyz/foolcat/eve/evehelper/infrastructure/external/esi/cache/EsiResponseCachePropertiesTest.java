package xyz.foolcat.eve.evehelper.infrastructure.external.esi.cache;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EsiResponseCacheProperties 单元测试。
 *
 * <p>校验默认配置值(plan Phase A1):强刷固定窗 60 分钟、窗内真调额度 5、默认 datasource=serenity。</p>
 */
@DisplayName("EsiResponseCacheProperties 缓存配置默认值")
class EsiResponseCachePropertiesTest {

    @Test
    @DisplayName("默认值:window=60,limit=5,datasource=serenity")
    void defaults_matchPlan() {
        EsiResponseCacheProperties p = new EsiResponseCacheProperties();
        assertThat(p.getForceRefreshWindowMinutes()).isEqualTo(60);
        assertThat(p.getForceRefreshLimit()).isEqualTo(5);
        assertThat(p.getDatasource()).isEqualTo("serenity");
    }

    @Test
    @DisplayName("setter 生效(配置可覆盖默认值)")
    void setters_apply() {
        EsiResponseCacheProperties p = new EsiResponseCacheProperties();
        p.setForceRefreshWindowMinutes(30);
        p.setForceRefreshLimit(3);
        p.setDatasource("tranquility");
        assertThat(p.getForceRefreshWindowMinutes()).isEqualTo(30);
        assertThat(p.getForceRefreshLimit()).isEqualTo(3);
        assertThat(p.getDatasource()).isEqualTo("tranquility");
    }
}