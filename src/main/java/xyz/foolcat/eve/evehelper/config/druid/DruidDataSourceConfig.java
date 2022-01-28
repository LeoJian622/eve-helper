package xyz.foolcat.eve.evehelper.config.druid;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Druid配置类
 *
 * @author Leojan
 * @date 2021-12-06 17:07
 */

@Configuration
public class DruidDataSourceConfig {

    @Primary
    @Bean
    @ConfigurationProperties("spring.datasource.druid.system")
    public DataSource systemDataSource(){
        return DruidDataSourceBuilder.create().build();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.druid.eve")
    public DataSource eveDataSource(){
        return DruidDataSourceBuilder.create().build();
    }
}
