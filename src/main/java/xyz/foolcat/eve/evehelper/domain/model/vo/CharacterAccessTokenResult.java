package xyz.foolcat.eve.evehelper.domain.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 角色 ESI AccessToken 领域读模型。
 *
 * <p>承载单个角色访问 ESI 所需的访问令牌及其剩余有效期,
 * 由 application/interfaces 层直接作为响应载荷返回。</p>
 *
 * <p><b>安全约定</b>:本读模型只承载 accessToken(短期令牌),
 * 绝不包含 refreshToken(可无限续期的长期凭证)。</p>
 *
 * @param accessToken 访问令牌(带 Bearer 前缀)
 * @param characterId 角色 ID
 * @param expiresIn   剩余有效时间(秒);无法确定时为 0,不透出缓存哨兵值
 * @author Leojan
 * date 2026-08-11
 */
@Schema(description = "角色ESI访问令牌")
public record CharacterAccessTokenResult(

        @Schema(description = "访问令牌(带Bearer前缀)", example = "Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...")
        String accessToken,

        @Schema(description = "角色ID", example = "95465499")
        Integer characterId,

        @Schema(description = "剩余有效时间(秒)", example = "1140")
        Long expiresIn) {

    /**
     * 脱敏渲染:绝不输出 accessToken。
     *
     * <p>record 自动生成的 toString() 会打印全部组件。若沿用默认实现,
     * 任何一句 log.debug("result={}", result) 都会把凭证写进日志。
     * 此处把脱敏固化在类型上,不依赖调用方自律。</p>
     *
     * <p>characterId 与 expiresIn 保留,便于排查缓存命中与过期问题。
     * 注意:Jackson 序列化走访问器而非 toString(),故响应体仍返回真实令牌。</p>
     *
     * @return 不含令牌的可读描述
     */
    @Override
    public String toString() {
        return "CharacterAccessTokenResult[accessToken=***, characterId=" + characterId
                + ", expiresIn=" + expiresIn + "]";
    }
}
