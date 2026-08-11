package xyz.foolcat.eve.evehelper.infrastructure.config.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 启动安全基线校验器(007 FR-008 + 006 L-10,T025;fail-closed 正向白名单)。
 *
 * <p><b>背景</b>:{@code application-*.yml} 均不入库,.gitignore 使「安全基线是否
 * 被应用」无法由版本控制保证 —— 手写 profile 文件漏掉任何一项都不会有报警。
 * 本组件在启动期把 4 项基线变成<b>运行时硬门禁</b>(006 C1 纵深防御的最后一环):</p>
 * <ol>
 *   <li>{@code mybatis-plus.configuration.log-impl} 不得为 StdOutImpl
 *       (其 {@code isDebugEnabled} 硬编码 true,会把 refresh_token 明文打进 stdout,006 FR-014)</li>
 *   <li>{@code logging.level.web} 不得为 debug/trace(请求/响应体落盘,accessToken 泄露)</li>
 *   <li>{@code eve.helper.debug.access-token-endpoint.enabled} 不得为 true
 *       (该端点交出可冒用的 bearer 凭证,006 FR-017)</li>
 *   <li>keystore location 必须是文件系统路径 —— classpath 前缀或裸文件名一律拒绝,
 *       杜绝生产误用入库的 {@code test-only.jks}(SC-005)</li>
 * </ol>
 *
 * <p><b>profile 规则(fail-closed)</b>:仅当 active profile <b>明确且仅为</b>
 * {@code test} 才豁免(测试库无真实凭证,classpath 与 StdOutImpl 合法);
 * <b>缺失或未知 profile 一律按生产校验</b> —— 宁可拒启,不可裸奔。
 * 006 遗留项 L-10 由本组件合并实现。</p>
 *
 * <p>任一违规 → 抛 {@link IllegalStateException} 终止启动。契约测试见
 * {@code SecurityBaselineValidatorTest}。</p>
 *
 * @author Leojan
 * date 2026-08-12
 */
@Slf4j
@Component
public class SecurityBaselineValidator implements ApplicationRunner {

    static final String LOG_IMPL_KEY = "mybatis-plus.configuration.log-impl";
    static final String WEB_LOG_LEVEL_KEY = "logging.level.web";
    static final String ENDPOINT_ENABLED_KEY = "eve.helper.debug.access-token-endpoint.enabled";
    static final String KEYSTORE_LOCATION_KEY = "security.keystore.location";

    private static final String STDOUT_IMPL = "org.apache.ibatis.logging.stdout.StdOutImpl";

    private final Environment environment;

    public SecurityBaselineValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        validate();
    }

    /**
     * 执行 4 项基线校验;任一违规抛出 IllegalStateException 拒绝启动。
     * 包级可见以便契约测试直接驱动(不经 Spring 上下文)。
     */
    void validate() {
        if (isTestProfile()) {
            log.info("启动安全基线校验:test profile,豁免生产基线(classpath keystore 与 StdOutImpl 合法)");
            return;
        }

        // 缺失/未知 profile 与已知生产 profile 同等校验(fail-closed)
        checkLogImpl();
        checkWebLogLevel();
        checkAccessTokenEndpoint();
        checkKeystoreLocation();
        log.info("启动安全基线校验通过(4/4)");
    }

    /** 仅当 active profile 明确且仅为 "test" 才视为测试环境(正向白名单) */
    private boolean isTestProfile() {
        String[] active = environment.getActiveProfiles();
        return active.length == 1 && "test".equals(active[0]);
    }

    private void checkLogImpl() {
        String logImpl = environment.getProperty(LOG_IMPL_KEY);
        if (STDOUT_IMPL.equalsIgnoreCase(logImpl)) {
            throw new IllegalStateException(
                    "启动基线校验失败: log-impl 不得为 StdOutImpl —— 它会无条件打印 SQL 绑定参数,"
                            + "refresh_token(ESI 长期凭证)将明文进入 stdout。请改用 Slf4jImpl 并核对日志级别");
        }
    }

    private void checkWebLogLevel() {
        String level = environment.getProperty(WEB_LOG_LEVEL_KEY);
        if ("debug".equalsIgnoreCase(level) || "trace".equalsIgnoreCase(level)) {
            throw new IllegalStateException(
                    "启动基线校验失败: logging.level.web 不得为 debug/trace —— 请求/响应体会落盘,"
                            + "accessToken 随之泄露。请设为 info 或更高");
        }
    }

    private void checkAccessTokenEndpoint() {
        String enabled = environment.getProperty(ENDPOINT_ENABLED_KEY);
        if (Boolean.parseBoolean(enabled)) {
            throw new IllegalStateException(
                    "启动基线校验失败: access-token-endpoint.enabled 在生产必须为 false —— "
                            + "该端点按角色返回可冒用的 ESI bearer 凭证,禁止常驻开启");
        }
    }

    private void checkKeystoreLocation() {
        String location = environment.getProperty(KEYSTORE_LOCATION_KEY);
        if (!StringUtils.hasText(location)) {
            throw new IllegalStateException(
                    "启动基线校验失败: security.keystore.location 缺失 —— "
                            + "请通过环境变量 KEYSTORE_LOCATION 指定文件系统绝对路径");
        }
        if (location.startsWith("classpath:")) {
            throw new IllegalStateException(
                    "启动基线校验失败: keystore 禁止从 classpath 加载 —— 生产必须使用文件系统绝对路径,"
                            + "防止误用入库的 test-only.jks;当前值为 classpath 资源");
        }
        if (!looksLikeFilesystemPath(location)) {
            throw new IllegalStateException(
                    "启动基线校验失败: keystore location 为裸文件名,按 classpath 语义处理 —— "
                            + "生产必须使用文件系统绝对路径(如 /etc/eve-helper/eve-jwt.jks)");
        }
    }

    /** 含目录分隔符或 Windows 盘符前缀视为文件系统路径 */
    private static boolean looksLikeFilesystemPath(String location) {
        return location.contains("/")
                || location.contains("\\")
                || location.matches("^[A-Za-z]:.*");
    }
}
