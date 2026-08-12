package xyz.foolcat.eve.evehelper.infrastructure.config.security;

import jakarta.annotation.PostConstruct;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T040 前提实测:{@code ApplicationRunner} 执行时,web 端口是否已在监听?
 *
 * <p><b>为何需要这个诊断</b>:评审 MEDIUM-2/LOW-6 断言「Runner 在容器已监听端口<b>之后</b>
 * 才执行,拒启前存在可服务请求的窗口」。007 此前已有<b>三次</b>「评审前提实测不成立」的记录
 * (见 {@code docs/reviews/} 与 tasks.md),故先实测再改造 —— 若窗口不存在,T040 应改为
 * 「记录前提不成立并关闭」而非动手改时机。</p>
 *
 * <p><b>实验设计</b>:构造一个最小 servlet 应用(仅 Tomcat 工厂 + 一个探针 Runner,
 * <b>不引入</b> 自动配置/数据源/Redis,避免环境依赖 —— 本机 MySQL 不通)。探针 Runner 在
 * 自己的 {@code run()} 内向本进程监听端口发起 TCP 连接:</p>
 * <ul>
 *   <li>连接<b>成功</b> → 端口已 bound 且 accept,窗口<b>真实存在</b> → T040 应改造时机</li>
 *   <li>连接<b>失败</b> → Runner 早于端口监听 → 评审前提不成立</li>
 * </ul>
 *
 * <p><b>本测试证明范围的诚实声明</b>:TCP 连接成功证明「socket 已 bound 并 accept」,
 * 这是「可服务请求」的<b>必要前提</b>;是否真正走完 DispatcherServlet 分派本测试不验证
 * (最小应用未注册 DispatcherServlet)。对 T040 的决策而言这已足够 ——
 * 端口一旦 accept,攻击者的连接就不会被拒,拒启前的暴露窗口即成立。</p>
 *
 * <p>本测试<b>不测</b> {@code SecurityBaselineValidator} 的校验逻辑(那是
 * {@code SecurityBaselineValidatorTest} 的职责),只测 Spring Boot 的<b>生命周期时序</b>。</p>
 *
 * @author Leojan
 * date 2026-08-12
 */
@DisplayName("T040 前提实测:ApplicationRunner 执行时端口是否已监听")
class BaselineTimingDiagnosticTest {

    /** 探针观测结果,由 {@link PortProbeRunner} 在启动期写入 */
    private static volatile Integer observedPort;
    private static volatile Boolean portAcceptedConnection;
    private static volatile String probeFailure;

    /** {@code @PostConstruct} 阶段(候选新时机)的观测结果 */
    private static volatile Boolean postConstructSawWebServerReady;
    /** 由 {@code WebServerInitializedEvent} 被动置位 */
    private static volatile boolean webServerInitialized;
    private static volatile Integer actualPort;

    @Test
    @DisplayName("最小 servlet 应用:Runner 内向本进程端口发起 TCP 连接")
    void applicationRunner_runsAfterPortIsListening() {
        observedPort = null;
        portAcceptedConnection = null;
        probeFailure = null;
        postConstructSawWebServerReady = null;
        webServerInitialized = false;
        actualPort = null;

        SpringApplication app = new SpringApplication(MinimalWebConfig.class);
        app.setWebApplicationType(WebApplicationType.SERVLET);
        // 端口 0 = 由 OS 分配空闲端口,避免与本机 9999 或并行测试冲突
        app.setDefaultProperties(Map.of("server.port", "0"));

        try (ConfigurableApplicationContext ctx = app.run()) {
            assertNotNull(ctx, "最小应用应能启动");
        }

        assertNotNull(observedPort, "探针未取到端口 —— 实验本身失败,结论无效");
        assertTrue(observedPort > 0,
                "端口应为 OS 分配的正值,实际=" + observedPort);

        // 防污染断言:Runner 观测到的端口必须与容器事件报告的端口一致。
        // 若不等 → 说明存在第二个连接器(观测行为触发了额外 WebServer 创建),
        // 该次测量无效 —— 第一版 @PostConstruct 探针正是踩了这个坑
        assertEquals(actualPort, observedPort,
                "Runner 观测端口与 WebServerInitializedEvent 报告端口不一致 → "
                        + "存在多连接器,测量被污染,结论无效");

        System.out.println("[T040-DIAG] Runner 执行时端口=" + observedPort
                + ", TCP 连接=" + portAcceptedConnection
                + (probeFailure == null ? "" : ", 失败原因=" + probeFailure));
        System.out.println("[T040-DIAG] @PostConstruct 时 web 容器已就绪="
                + postConstructSawWebServerReady);

        assertNotNull(portAcceptedConnection, "探针未完成连接尝试");
        assertTrue(portAcceptedConnection,
                "结论:Runner 执行时端口已 accept 连接 → T040 描述的暴露窗口真实存在。"
                        + "若此断言失败,说明 Runner 早于端口监听,评审前提不成立,"
                        + "T040 应改为『记录前提不成立并关闭』");

        // 候选新时机必须严格更早:@PostConstruct 执行时 web 容器尚未就绪
        assertNotNull(postConstructSawWebServerReady, "@PostConstruct 探针未执行");
        assertFalse(postConstructSawWebServerReady,
                "@PostConstruct 阶段 web 容器不应已就绪 —— 若为 true,"
                        + "则改到该阶段并不能消除窗口,T040 需另选时机");
    }

    @Test
    @DisplayName("T040 回归锁:校验器必须在 @PostConstruct 执行,不得是 ApplicationRunner")
    void validator_mustValidateAtPostConstruct_notApplicationRunner() {
        // ① 不得再实现 ApplicationRunner/CommandLineRunner —— 那会把校验推迟到端口监听之后
        assertFalse(ApplicationRunner.class.isAssignableFrom(SecurityBaselineValidator.class),
                "SecurityBaselineValidator 不得实现 ApplicationRunner —— Runner 回调发生在"
                        + "端口已 accept 之后,基线违规实例会有一段可服务请求的暴露窗口(T040)");
        assertFalse(CommandLineRunner.class.isAssignableFrom(SecurityBaselineValidator.class),
                "同上,CommandLineRunner 亦是容器就绪后回调");

        // ② 必须有一个带 @PostConstruct 的方法,且它确实会触发校验
        boolean hasPostConstruct = java.util.Arrays.stream(
                        SecurityBaselineValidator.class.getDeclaredMethods())
                .anyMatch(m -> m.isAnnotationPresent(PostConstruct.class));
        assertTrue(hasPostConstruct,
                "必须存在 @PostConstruct 方法,使校验在 Bean 初始化期(容器就绪前)完成");
    }

    @Test
    @DisplayName("T040 行为锁:@PostConstruct 阶段基线违规 → 上下文刷新即失败")
    void baselineViolation_failsBeforeWebServerStarts() {
        webServerInitialized = false;

        SpringApplication app = new SpringApplication(ViolatingBaselineConfig.class);
        app.setWebApplicationType(WebApplicationType.SERVLET);
        app.setDefaultProperties(Map.of("server.port", "0"));

        // 刻意构造违规环境:profile=prod 且 log-impl 缺失 → 必须拒启
        assertThrows(Exception.class, app::run,
                "基线违规必须导致启动失败");

        // 核心断言:失败发生时 web 容器从未就绪 —— 窗口不存在
        assertFalse(webServerInitialized,
                "基线违规拒启时 web 容器不得曾经就绪 —— 若为 true,说明端口已对外 accept 过,"
                        + "T040 的暴露窗口仍然存在");
    }

    /**
     * 违规基线的最小应用:profile 未设(fail-closed → 按生产校验)且基线键全缺失 → 必拒启。
     * 只装 Tomcat 工厂 + 就绪监听器 + 校验器本身。
     */
    @Configuration(proxyBeanMethods = false)
    static class ViolatingBaselineConfig {

        @Bean
        ServletWebServerFactory servletWebServerFactory() {
            return new TomcatServletWebServerFactory(0);
        }

        @Bean
        WebServerReadyListener webServerReadyListener() {
            return new WebServerReadyListener();
        }

        @Bean
        SecurityBaselineValidator violatingValidator(Environment env) {
            return new SecurityBaselineValidator(env);
        }
    }

    /** 最小 servlet 应用:只要 Tomcat 工厂与探针,刻意不启用自动配置 */
    @Configuration(proxyBeanMethods = false)
    static class MinimalWebConfig {

        @Bean
        ServletWebServerFactory servletWebServerFactory() {
            return new TomcatServletWebServerFactory(0);
        }

        @Bean
        PortProbeRunner portProbeRunner(ConfigurableApplicationContext ctx) {
            return new PortProbeRunner(ctx);
        }

        @Bean
        PostConstructProbe postConstructProbe() {
            return new PostConstructProbe();
        }

        @Bean
        WebServerReadyListener webServerReadyListener() {
            return new WebServerReadyListener();
        }
    }

    /**
     * 在 {@code @PostConstruct} 阶段记录时序 —— T040 的候选新时机
     * (与 {@code KeyPairConfig} fail-fast 同阶段)。
     *
     * <p><b>为何不在此主动探测端口</b>:第一版实现调用了
     * {@code ctx.getWebServer()},结果<b>触发 WebServer 提前创建</b>,
     * Tomcat 日志变成 {@code started on ports 8080 (http), 60249 (http)} ——
     * 多出一个默认 8080 连接器,{@code getPort()} 返回的是那个错误的端口,
     * 而 8080 恰好可连(本机常见占用)导致假阳性。<b>观测行为改变了被观测系统</b>。
     * 现改为纯被动记录:只看 {@code WebServerInitializedEvent} 是否已发生。</p>
     */
    static class PostConstructProbe {

        @PostConstruct
        void probe() {
            // 纯被动:不触碰 WebServer,只读事件标志位
            postConstructSawWebServerReady = webServerInitialized;
        }
    }

    /** 被动记录 web 容器就绪事件的发生时刻 */
    static class WebServerReadyListener implements ApplicationListener<WebServerInitializedEvent> {

        @Override
        public void onApplicationEvent(WebServerInitializedEvent event) {
            webServerInitialized = true;
            actualPort = event.getWebServer().getPort();
        }
    }

    /**
     * 在 {@code ApplicationRunner} 回调内探测本进程端口 —— 与
     * {@code SecurityBaselineValidator} 当前所处的生命周期阶段完全相同。
     */
    static class PortProbeRunner implements ApplicationRunner {

        private final ConfigurableApplicationContext ctx;

        PortProbeRunner(ConfigurableApplicationContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public void run(ApplicationArguments args) {
            int port = ((WebServerApplicationContext) ctx).getWebServer().getPort();
            observedPort = port;
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress("127.0.0.1", port), 2000);
                portAcceptedConnection = socket.isConnected();
            } catch (Exception e) {
                portAcceptedConnection = false;
                probeFailure = e.getClass().getSimpleName() + ": " + e.getMessage();
            }
        }
    }
}
