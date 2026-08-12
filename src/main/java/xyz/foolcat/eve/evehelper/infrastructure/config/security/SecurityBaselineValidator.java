package xyz.foolcat.eve.evehelper.infrastructure.config.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Map;

/**
 * 启动安全基线校验器(007 FR-008 + 006 L-10,T025;fail-closed 正向白名单)。
 *
 * <p><b>背景</b>:{@code application-*.yml} 均不入库,.gitignore 使「安全基线是否
 * 被应用」无法由版本控制保证 —— 手写 profile 文件漏掉任何一项都不会有报警。
 * 本组件在启动期把 4 项基线变成<b>运行时硬门禁</b>(006 C1 纵深防御的最后一环):</p>
 * <ol>
 *   <li>{@code mybatis-plus.configuration.log-impl} 必须显式为 Slf4jImpl
 *       (StdOutImpl 的 {@code isDebugEnabled} 硬编码 true,会把 refresh_token 明文打进 stdout,006 FR-014)</li>
 *   <li><b>全部</b> {@code logging.level.*} 均不得为 debug/trace,且 {@code root} 必须显式声明
 *       (请求/响应体、ESI 请求头、SQL 绑定参数落盘 → 凭证泄露)</li>
 *   <li>{@code eve.helper.debug.access-token-endpoint.enabled} 必须显式为 false
 *       (该端点交出可冒用的 bearer 凭证,006 FR-017)</li>
 *   <li>keystore location 必须是文件系统路径 —— classpath 前缀或裸文件名一律拒绝,
 *       杜绝生产误用入库的 {@code test-only.jks}(SC-005)</li>
 * </ol>
 *
 * <p><b>正向白名单语义(T038 修正 HIGH-3)</b>:初版实现是<b>黑名单</b> ——
 * 「不等于危险值就放行」。这使三项基线在<b>键缺失</b>时恒放行,却照打
 * 「基线校验通过(4/4)」,是虚假保证:</p>
 * <ul>
 *   <li>{@code Boolean.parseBoolean(null)} = false → debug 端点键缺失 → 恒放行</li>
 *   <li>{@code null != StdOutImpl} → log-impl 键缺失 → 恒放行</li>
 *   <li>只查 {@code logging.level.web} → {@code root: debug} 完全绕过</li>
 * </ul>
 * <p>现改为:<b>键必须显式存在且取值属于安全集合,缺失即拒启</b>。
 * 校验器的保证强度必须等于它宣称的强度 —— 否则不如没有。</p>
 *
 * <p><b>profile 规则(fail-closed)</b>:仅当 active profile <b>明确且仅为</b>
 * {@code test} 才豁免(测试库无真实凭证,classpath 与 StdOutImpl 合法);
 * <b>缺失或未知 profile 一律按生产校验</b> —— 宁可拒启,不可裸奔。
 * 006 遗留项 L-10 由本组件合并实现。</p>
 *
 * <p>任一违规 → 抛 {@link IllegalStateException} 终止启动。契约测试见
 * {@code SecurityBaselineValidatorTest}。</p>
 *
 * <p><b>⚠️ 能力边界:只校验路径语法,不校验密钥身份</b>(T039 已由用户否决,2026-08-12)。
 * 基线④ 确认 {@code location} 是文件系统绝对路径,<b>但不检查该文件里的密钥是哪一把</b> ——
 * 把 {@code location} 指向已泄露的旧 {@code eve-jwt.jks} 可通过全部 4 项校验。
 * 这正是 007 CRITICAL-1 的原始成因,该路径<b>依然敞开</b>。故「生产是否用了正确密钥」
 * 只能靠部署时人工比对指纹(见 {@code docs/DEPLOYMENT.md} 轮换章节),
 * <b>本类不提供该保证</b>。修改本类时勿把「基线校验通过」误读为「密钥正确」。</p>
 *
 * @author Leojan
 * date 2026-08-12
 */
@Slf4j
@Component
public class SecurityBaselineValidator implements ApplicationRunner {

    static final String LOG_IMPL_KEY = "mybatis-plus.configuration.log-impl";
    /** T038:取代原 {@code logging.level.web} 单点检查 —— 现全扫该前缀下所有 logger */
    static final String LOG_LEVEL_PREFIX = "logging.level";
    static final String ROOT_LOGGER = "root";
    static final String ENDPOINT_ENABLED_KEY = "eve.helper.debug.access-token-endpoint.enabled";
    static final String KEYSTORE_LOCATION_KEY = "security.keystore.location";

    private static final String SLF4J_IMPL = "org.apache.ibatis.logging.slf4j.Slf4jImpl";

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
        checkLogLevels();
        checkAccessTokenEndpoint();
        checkKeystoreLocation();
        log.info("启动安全基线校验通过(4/4:log-impl、logging.level.* 全扫、debug 端点、keystore 路径)");
    }

    /** 仅当 active profile 明确且仅为 "test" 才视为测试环境(正向白名单) */
    private boolean isTestProfile() {
        String[] active = environment.getActiveProfiles();
        return active.length == 1 && "test".equals(active[0]);
    }

    private void checkLogImpl() {
        String logImpl = environment.getProperty(LOG_IMPL_KEY);
        // 正向白名单:必须显式等于 Slf4jImpl。null 与任意其他值同样被拒 ——
        // 「没配」不等于安全,MyBatis 默认实现随版本变化(T038)
        if (!SLF4J_IMPL.equalsIgnoreCase(logImpl)) {
            throw new IllegalStateException(
                    "启动基线校验失败: " + LOG_IMPL_KEY + " 必须显式为 " + SLF4J_IMPL
                            + ",当前为 " + (logImpl == null ? "<未声明>" : logImpl)
                            + " —— StdOutImpl 会无条件打印 SQL 绑定参数,"
                            + "refresh_token(ESI 长期凭证)将明文进入 stdout");
        }
    }

    /**
     * 校验全部 {@code logging.level.*} 键(T038 取代原「只查 logging.level.web」)。
     *
     * <p><b>为何必须全扫而非枚举固定 logger 名</b>:原实现只查 {@code logging.level.web},
     * 存在三条真实绕过路径,任一条都能让凭证进日志:</p>
     * <ol>
     *   <li>{@code root: debug} —— 所有未显式配置的 logger 一并降级</li>
     *   <li>{@code reactor.netty: debug} —— ESI 客户端请求头落盘,
     *       其中含 {@code Authorization: Bearer <ESI accessToken>}</li>
     *   <li>{@code xyz.foolcat...persistence.mapper: debug} —— 配合 Slf4jImpl 打 SQL 绑定参数,
     *       {@code eve_account.refresh_token} 明文入日志。<b>这条与 log-impl 基线等效</b> ——
     *       从另一侧达成同一泄露,而原实现完全看不见</li>
     * </ol>
     *
     * <p>枚举法无法穷尽(第三方库随时新增包名),故改为全扫:任一 logger 为 debug/trace 即拒。
     * 同时要求 {@code root} 必须显式声明 —— 未声明的根级别不能视为安全。</p>
     */
    private void checkLogLevels() {
        Map<String, String> levels = Binder.get(environment)
                .bind(LOG_LEVEL_PREFIX, Bindable.mapOf(String.class, String.class))
                .orElse(Collections.emptyMap());

        String rootLevel = levels.get(ROOT_LOGGER);
        if (!StringUtils.hasText(rootLevel)) {
            throw new IllegalStateException(
                    "启动基线校验失败: " + LOG_LEVEL_PREFIX + "." + ROOT_LOGGER
                            + " 未显式声明 —— 生产必须明确根日志级别(info 或更高)。"
                            + "缺失即拒:未声明的根级别取决于框架默认与 logback 配置,不可假定安全");
        }

        levels.forEach((logger, level) -> {
            if (isVerboseLevel(level)) {
                throw new IllegalStateException(
                        "启动基线校验失败: " + LOG_LEVEL_PREFIX + "." + logger + " = " + level
                                + " —— debug/trace 会使请求/响应体、ESI 请求头(Bearer token)或 "
                                + "SQL 绑定参数(refresh_token)落盘。生产一律设为 info 或更高");
            }
        });
    }

    private static boolean isVerboseLevel(String level) {
        return "debug".equalsIgnoreCase(level) || "trace".equalsIgnoreCase(level);
    }

    private void checkAccessTokenEndpoint() {
        String enabled = environment.getProperty(ENDPOINT_ENABLED_KEY);
        // 正向白名单:必须显式为 "false"。原实现用 Boolean.parseBoolean(null)=false
        // 判定,导致键缺失恒放行,却照打「基线校验通过」—— 虚假保证(T038 修正 HIGH-3)
        if (!"false".equalsIgnoreCase(enabled)) {
            throw new IllegalStateException(
                    "启动基线校验失败: " + ENDPOINT_ENABLED_KEY + " 在生产必须显式为 false,当前为 "
                            + (enabled == null ? "<未声明>" : enabled)
                            + " —— 该端点按角色返回可冒用的 ESI bearer 凭证,禁止常驻开启。"
                            + "本校验只接受字面 false:缺失或任意其他值(yes/1/on 等)一律拒启");
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
