package xyz.foolcat.eve.evehelper.interfaces.web.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import xyz.foolcat.eve.evehelper.application.service.CharacterApplicationService;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * AccessToken 端点的注册开关与暴露面测试(纯上下文测试,不依赖 DB/Redis/Web 容器)。
 *
 * <p>该端点交出可直接冒用的 ESI bearer 凭证,{@code @ConditionalOnProperty} 是其
 * <b>主要生产安全控制</b> —— 一个未被测试覆盖的 kill switch 与不存在无异。
 * 项目既有 controller 测试用 {@code @SpringBootTest},依赖测试库中角色 2112818290
 * 的有效 ESI 授权行,该数据缺失时全部 error(与 DB 可达性无关);
 * 故此处改用 {@link ApplicationContextRunner} 只加载该 Bean 与其条件注解,不依赖任何外部数据。</p>
 *
 * <p>对应 006 feature T012(FR-017,设计评审 M4)。</p>
 *
 * @author Leojan
 * date 2026-08-11
 */
@DisplayName("AccessToken 端点开关与暴露面测试")
class CharacterAccessTokenControllerTest {

    private static final String ENABLE_PROPERTY = "eve.helper.debug.access-token-endpoint.enabled";

    /**
     * 以 @Import 目标类的方式加载,使其 @ConditionalOnProperty 真正参与判定
     * (withBean 会绕过条件注解,测不到开关行为)。
     */
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(MockDependencies.class, CharacterAccessTokenController.class);

    @Configuration
    static class MockDependencies {
        @Bean
        CharacterApplicationService characterApplicationService() {
            return mock(CharacterApplicationService.class);
        }
    }

    @Test
    @DisplayName("开关未配置(默认) -> Controller 不注册,端点不存在")
    void propertyAbsent_controllerNotRegistered() {
        runner.run(context ->
                assertTrue(context.getBeansOfType(CharacterAccessTokenController.class).isEmpty(),
                        "开关未配置时该凭证端点必须不注册 —— 默认关闭是本 feature 的核心安全约定"));
    }

    @Test
    @DisplayName("开关显式为 false -> Controller 不注册")
    void propertyFalse_controllerNotRegistered() {
        runner.withPropertyValues(ENABLE_PROPERTY + "=false").run(context ->
                assertTrue(context.getBeansOfType(CharacterAccessTokenController.class).isEmpty(),
                        "开关为 false 时该端点必须不注册"));
    }

    @Test
    @DisplayName("开关为 true -> Controller 注册")
    void propertyTrue_controllerRegistered() {
        runner.withPropertyValues(ENABLE_PROPERTY + "=true").run(context ->
                assertEquals(1, context.getBeansOfType(CharacterAccessTokenController.class).size(),
                        "开关为 true 时端点应可用,否则运维无法排查"));
    }

    @Test
    @DisplayName("Controller 标注 @Hidden -> 不出现在 permitAll 的 /v3/api-docs 中")
    void controllerIsHiddenFromApiDocs() {
        // /v3/api-docs 与 /swagger-ui.html 在 SecurityConfig 中为 permitAll,
        // 未认证者可读到全部端点结构;凭证端点不应被这样发现。
        assertNotNull(CharacterAccessTokenController.class.getAnnotation(Hidden.class),
                "凭证端点必须标注 @Hidden,否则其路径与响应结构会暴露给未认证访问者");
    }

    @Test
    @DisplayName("端点路径与 HTTP 方法符合契约:GET /character/{characterId}/access-token")
    void endpointPathMatchesContract() throws Exception {
        RequestMapping classMapping = CharacterAccessTokenController.class.getAnnotation(RequestMapping.class);
        assertNotNull(classMapping);
        assertEquals("/character", classMapping.value()[0]);

        Method handler = CharacterAccessTokenController.class
                .getDeclaredMethod("getAccessToken", Integer.class);
        GetMapping methodMapping = handler.getAnnotation(GetMapping.class);
        assertNotNull(methodMapping, "必须是 GET(只读查询),不可用写语义方法");
        assertEquals("/{characterId}/access-token", methodMapping.value()[0]);
    }

    @Test
    @DisplayName("characterId 参数标注 @Positive 且类标注 @Validated -> 非正整数在进入业务逻辑前被拒")
    void characterIdParameterIsValidated() throws Exception {
        Method handler = CharacterAccessTokenController.class
                .getDeclaredMethod("getAccessToken", Integer.class);

        boolean hasPositive = Arrays.stream(handler.getParameterAnnotations()[0])
                .anyMatch(a -> a instanceof jakarta.validation.constraints.Positive);

        assertTrue(hasPositive, "characterId 须标注 @Positive(FR-011),否则 0/负数会直达 SQL");
        assertNotNull(CharacterAccessTokenController.class.getAnnotation(Validated.class),
                "类须标注 @Validated,否则方法参数上的 @Positive 不生效");
    }
}
