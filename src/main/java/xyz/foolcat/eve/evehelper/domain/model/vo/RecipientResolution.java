package xyz.foolcat.eve.evehelper.domain.model.vo;

/**
 * 名称→ID 解析结果(领域读模型)。
 * <p>
 * T018 创建。{@code resolved=false} 表示请求中的名称未在 ESI 中匹配到。
 *
 * @param name     请求的名称
 * @param id       解析到的 ID(未解析时为 null)
 * @param type     类型(character/corporation/alliance;未解析时为 null)
 * @param resolved 是否成功解析
 * @author Leojan
 * @since 022
 */
public record RecipientResolution(String name, Integer id, String type, boolean resolved) {
}
