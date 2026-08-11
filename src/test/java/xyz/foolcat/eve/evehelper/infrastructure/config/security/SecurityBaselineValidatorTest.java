package xyz.foolcat.eve.evehelper.infrastructure.config.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code SecurityBaselineValidator} fail-closed 启动校验契约测试
 * (007 FR-008 + 006 L-10,T021;round3 §3.1 指定用例)。
 *
 * <p>校验 4 项生产安全基线,任一违规 → 拒绝启动(抛异常):</p>
 * <ol>
 *   <li>{@code mybatis-plus.configuration.log-impl} 不得为 StdOutImpl
 *       (无条件打印 SQL 参数 → refresh_token 明文入 stdout,006 FR-014)</li>
 *   <li>{@code logging.level.web} 不得为 debug/trace(请求/响应体落盘,accessToken 泄露)</li>
 *   <li>{@code eve.helper.debug.access-token-endpoint.enabled} 不得为 true
 *       (凭证暴露面,006 FR-017)</li>
 *   <li>keystore location:非 test profile 必须是文件系统路径,
 *       <b>classpath 或裸文件名一律拒绝</b>(SC-005 具体用例:
 *       生产 profile + {@code classpath:test-only.jks} → 拒启)</li>
 * </ol>
 *
 * <p><b>profile 规则(fail-closed 正向白名单)</b>:仅当 active profile 明确属于
 * {@code {test}} 才按测试环境放行 classpath 并豁免其余三项;
 * <b>缺失或未知 profile 一律按生产校验</b>(防「忘配 profile 就裸奔」。
 * 006 L-10 遗留项在本组件合并实现)。</p>
 *
 * <p>TDD 状态:T021 = RED(Validator 尚不存在,编译失败);T025 实现后转 GREEN。</p>
 *
 * @author Leojan
 * date 2026-08-12
 */
@DisplayName("启动安全基线校验契约测试(007 FR-008 fail-closed)")
class SecurityBaselineValidatorTest {

    private static final String LOG_IMPL = "mybatis-plus.configuration.log-impl";
    private static final String WEB_LOG_LEVEL = "logging.level.web";
    private static final String ENDPOINT_ENABLED = "eve.helper.debug.access-token-endpoint.enabled";
    private static final String KEYSTORE_LOCATION = "security.keystore.location";

    private static final String SLF4J_IMPL = "org.apache.ibatis.logging.slf4j.Slf4jImpl";
    private static final String STDOUT_IMPL = "org.apache.ibatis.logging.stdout.StdOutImpl";

    /** 全合规的生产配置(文件系统绝对路径 keystore) */
    private static MockEnvironment validProdEnv() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        env.setProperty(LOG_IMPL, SLF4J_IMPL);
        env.setProperty(WEB_LOG_LEVEL, "info");
        env.setProperty(ENDPOINT_ENABLED, "false");
        env.setProperty(KEYSTORE_LOCATION, "/etc/eve-helper/eve-jwt.jks");
        return env;
    }

    private static void assertRejects(MockEnvironment env, String expectedKeyword) {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new SecurityBaselineValidator(env).validate(),
                "违规配置必须拒绝启动");
        assertTrue(ex.getMessage().contains(expectedKeyword),
                "拒绝信息应含「" + expectedKeyword + "」,实际: " + ex.getMessage());
    }

    // ------------------------------------------------------------------
    // 4 项基线各自违规 → 拒绝启动
    // ------------------------------------------------------------------

    @Test
    @DisplayName("基线①:生产 + StdOutImpl → 拒绝启动")
    void prodProfile_stdOutLogImpl_rejectsStartup() {
        MockEnvironment env = validProdEnv();
        env.setProperty(LOG_IMPL, STDOUT_IMPL);
        assertRejects(env, "log-impl");
    }

    @Test
    @DisplayName("基线②:生产 + logging.level.web=debug → 拒绝启动")
    void prodProfile_webDebugLog_rejectsStartup() {
        MockEnvironment env = validProdEnv();
        env.setProperty(WEB_LOG_LEVEL, "debug");
        assertRejects(env, "web");
    }

    @Test
    @DisplayName("基线③:生产 + access-token-endpoint.enabled=true → 拒绝启动")
    void prodProfile_accessTokenEndpointEnabled_rejectsStartup() {
        MockEnvironment env = validProdEnv();
        env.setProperty(ENDPOINT_ENABLED, "true");
        assertRejects(env, "access-token-endpoint");
    }

    @Test
    @DisplayName("基线④:生产 + classpath:test-only.jks → 拒绝启动(round3 §3.1 指定用例)")
    void prodProfile_classpathKeystore_rejectsStartup() {
        MockEnvironment env = validProdEnv();
        env.setProperty(KEYSTORE_LOCATION, "classpath:test-only.jks");
        assertRejects(env, "keystore");
    }

    @Test
    @DisplayName("基线④:生产 + 裸文件名(classpath 语义)→ 拒绝启动")
    void prodProfile_bareFilenameKeystore_rejectsStartup() {
        MockEnvironment env = validProdEnv();
        env.setProperty(KEYSTORE_LOCATION, "test-only.jks");
        assertRejects(env, "keystore");
    }

    @Test
    @DisplayName("基线④:生产 + keystore location 缺失 → 拒绝启动(fail-fast)")
    void prodProfile_keystoreLocationMissing_rejectsStartup() {
        MockEnvironment env = validProdEnv();
        env.setProperty(KEYSTORE_LOCATION, "");
        assertRejects(env, "keystore");
    }

    // ------------------------------------------------------------------
    // fail-closed:缺失/未知 profile 按生产校验
    // ------------------------------------------------------------------

    @Test
    @DisplayName("无任何 active profile → 按生产校验(违规即拒,fail-closed)")
    void noProfile_failClosedAsProduction() {
        MockEnvironment env = validProdEnv();
        env.setActiveProfiles(); // 清空
        env.setProperty(LOG_IMPL, STDOUT_IMPL);
        assertRejects(env, "log-impl");
    }

    @Test
    @DisplayName("未知 profile(如 staging)→ 按生产校验(违规即拒,fail-closed)")
    void unknownProfile_failClosedAsProduction() {
        MockEnvironment env = validProdEnv();
        env.setActiveProfiles("staging");
        env.setProperty(ENDPOINT_ENABLED, "true");
        assertRejects(env, "access-token-endpoint");
    }

    // ------------------------------------------------------------------
    // test profile:classpath 允许,生产基线豁免
    // ------------------------------------------------------------------

    @Test
    @DisplayName("test profile → 允许 classpath keystore(合法测试配置放行)")
    void testProfile_classpathKeystore_allowed() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("test");
        env.setProperty(LOG_IMPL, SLF4J_IMPL);
        env.setProperty(WEB_LOG_LEVEL, "info");
        env.setProperty(ENDPOINT_ENABLED, "false");
        env.setProperty(KEYSTORE_LOCATION, "classpath:test-only.jks");

        assertDoesNotThrow(() -> new SecurityBaselineValidator(env).validate(),
                "test profile 允许 classpath 加载");
    }

    @Test
    @DisplayName("test profile + StdOutImpl → 放行(测试库无真实凭证,豁免生产基线)")
    void testProfile_stdOutLogImpl_allowed() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("test");
        env.setProperty(LOG_IMPL, STDOUT_IMPL); // application-test.yml 的合法现状
        env.setProperty(WEB_LOG_LEVEL, "info");
        env.setProperty(ENDPOINT_ENABLED, "false");
        env.setProperty(KEYSTORE_LOCATION, "test-only.jks");

        assertDoesNotThrow(() -> new SecurityBaselineValidator(env).validate(),
                "test profile 豁免 log-impl 生产基线(现状 application-test.yml 即 StdOutImpl)");
    }

    // ------------------------------------------------------------------
    // 全合规 → 放行
    // ------------------------------------------------------------------

    @Test
    @DisplayName("生产全合规(Slf4jImpl + info + false + 文件系统路径)→ 放行")
    void prodProfile_allValid_passes() {
        assertDoesNotThrow(() -> new SecurityBaselineValidator(validProdEnv()).validate(),
                "全合规生产配置不应被拒绝");
    }
}
