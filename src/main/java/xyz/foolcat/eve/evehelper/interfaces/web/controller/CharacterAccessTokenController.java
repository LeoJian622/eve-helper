package xyz.foolcat.eve.evehelper.interfaces.web.controller;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.foolcat.eve.evehelper.application.service.CharacterApplicationService;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterAccessTokenResult;
import xyz.foolcat.eve.evehelper.domain.util.UserUtil;
import xyz.foolcat.eve.evehelper.shared.result.Result;

/**
 * 角色 ESI AccessToken 查询端点(内部调试/运维排查用)。
 *
 * <p><b>为何独立成类</b>:该端点由配置开关控制注册,若与 {@link CharacterController} 的
 * 角色授权端点同处一类,开关关闭会连带禁用授权功能。</p>
 *
 * <p><b>安全设计</b>:</p>
 * <ul>
 *     <li>{@code @Hidden} —— 不出现在 /v3/api-docs 与 Swagger UI。该文档端点为 permitAll,
 *         未认证者本可读到全部端点结构;凭证接口不应被这样发现</li>
 *     <li>{@code @ConditionalOnProperty} —— 生产默认不注册(返回 404),
 *         「仅内部用途」由机制保证而非文档约定</li>
 *     <li>所有权校验在应用/领域层执行:仅本人名下角色精确匹配,
 *         不接受军团维度放行,ROOT/ADMIN 亦不豁免</li>
 * </ul>
 *
 * @author Leojan
 * date 2026-08-11
 */
@Hidden
@Validated
@RestController
@RequestMapping("/character")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "eve.helper.debug.access-token-endpoint.enabled", havingValue = "true")
public class CharacterAccessTokenController {

    private final CharacterApplicationService characterApplicationService;

    /**
     * 查询指定角色访问 ESI 所需的 accessToken。
     *
     * <p>返回体不含 refreshToken。响应携带 {@code Cache-Control: no-store}
     * (由 Spring Security 默认 CacheControlHeadersWriter 提供)。</p>
     *
     * @param characterId 角色 ID,须为正整数且属于当前登录用户
     * @return accessToken、角色 ID 与剩余有效秒数
     */
    @GetMapping("/{characterId}/access-token")
    public Result<CharacterAccessTokenResult> getAccessToken(
            @PathVariable @Positive Integer characterId) {
        return Result.success(characterApplicationService.queryAccessToken(characterId, UserUtil.getUserId()));
    }
}
