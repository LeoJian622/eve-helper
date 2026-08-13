package xyz.foolcat.eve.evehelper.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author Leojan
 * date 2021-12-10 16:57
 */

@Configuration
@EnableAsync
public class AsyncConfiguration {

    @Bean("EsiMarketOrderRequestExecutor")
    public Executor doSomethingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数：线程池创建时候初始化的线程数
        executor.setCorePoolSize(5);
        // 最大线程数：线程池最大的线程数，只有在缓冲队列满了之后才会申请超过核心线程数的线程
        executor.setMaxPoolSize(15);
        // 缓冲队列：用来缓冲执行任务的队列
        executor.setQueueCapacity(20);
        // 允许线程的空闲时间60秒：当超过了核心线程之外的线程在空闲时间到达之后会被销毁
        executor.setKeepAliveSeconds(30);
        // 线程池名的前缀：设置好了之后可以方便我们定位处理任务所在的线程池
        executor.setThreadNamePrefix("esi-order-");
        // 缓冲队列满了之后的拒绝策略：由调用线程处理（一般是主线程）
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        // 任务身份快照传播:须在 initialize() 之前装配(008 US1 / research R1)
        executor.setTaskDecorator(new SecurityContextTaskDecorator());
        executor.initialize();
        return executor;
    }

    /**
     * ESI 授权状态判定专用线程池
     * <p>
     * 用于并行判定多角色的 ESI 授权状态(冷路径,缓存未命中时触发)。
     * 常态命中状态缓存,不触发并行;冷路径把多角色刷新限制在约 5s。
     *
     * @author Leojan
     * date 2026-08-04
     */
    @Bean("esiAuthStatusExecutor")
    public Executor esiAuthStatusExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(50);
        executor.setKeepAliveSeconds(30);
        executor.setThreadNamePrefix("esi-auth-status-");
        // 队列满时由调用线程执行,避免任务丢弃
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 任务身份快照传播(CallerRunsPolicy 由装饰器同线程直通保障,008 US1 / research R1)
        executor.setTaskDecorator(new SecurityContextTaskDecorator());
        executor.initialize();
        return executor;
    }

}
