package xyz.foolcat.eve.evehelper.infrastructure.config.mybatis;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import xyz.foolcat.eve.evehelper.infrastructure.config.mybatis.handler.MyMetaObjectHandler;

/**
 * @author Leojan
 * date 2021-06-16 16:00
 */

@Configuration
@Slf4j
@EnableTransactionManagement
public class MybatisPlusConfig {

    /**
     * 单页最大记录数，防止超大分页请求耗尽资源
     */
    private static final long MAX_PAGE_SIZE = 1000L;

    /**
     * 分页插件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        // 全局兜底：超过上限的 size 会被截断，入口层仍应显式校验以返回明确错误
        pagination.setMaxLimit(MAX_PAGE_SIZE);
        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }

    /**
     * 自动填充数据库创建时间、更新时间
     */
    @Bean
    public GlobalConfig globalConfig() {
        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setMetaObjectHandler(new MyMetaObjectHandler());
        return globalConfig;
    }

}
