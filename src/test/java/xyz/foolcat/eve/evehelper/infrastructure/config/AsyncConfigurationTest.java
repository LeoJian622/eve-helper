package xyz.foolcat.eve.evehelper.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Field;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * AsyncConfiguration 线程池装饰器装配测试(008 T004 / research R1 实现注记)。
 *
 * <p>验证两个线程池均装配 {@link SecurityContextTaskDecorator}。装配在 {@code initialize()} 之前
 * (若顺序错误,{@code setTaskDecorator} 在已初始化池上会抛 {@link IllegalStateException})。
 */
class AsyncConfigurationTest {

    private final AsyncConfiguration config = new AsyncConfiguration();

    @Test
    void esiMarketOrderExecutorAssemblesSecurityContextDecorator() throws Exception {
        Executor executor = config.doSomethingExecutor();

        assertInstanceOf(ThreadPoolTaskExecutor.class, executor);
        assertInstanceOf(SecurityContextTaskDecorator.class, taskDecoratorOf((ThreadPoolTaskExecutor) executor));
    }

    @Test
    void esiAuthStatusExecutorAssemblesSecurityContextDecorator() throws Exception {
        Executor executor = config.esiAuthStatusExecutor();

        assertInstanceOf(ThreadPoolTaskExecutor.class, executor);
        assertInstanceOf(SecurityContextTaskDecorator.class, taskDecoratorOf((ThreadPoolTaskExecutor) executor));
    }

    /**
     * {@link ThreadPoolTaskExecutor#taskDecorator} 字段无公共 getter,经反射读取。
     */
    private static Object taskDecoratorOf(ThreadPoolTaskExecutor executor) throws Exception {
        Field f = ThreadPoolTaskExecutor.class.getDeclaredField("taskDecorator");
        f.setAccessible(true);
        return f.get(executor);
    }
}