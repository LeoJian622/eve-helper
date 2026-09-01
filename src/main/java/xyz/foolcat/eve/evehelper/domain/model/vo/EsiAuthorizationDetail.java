package xyz.foolcat.eve.evehelper.domain.model.vo;

import xyz.foolcat.eve.evehelper.shared.kernel.enums.EsiAuthStatus;

import java.util.List;

/**
 * ESI 授权状态 + 已授权权限 跨层读模型。
 *
 * <p>承载 {@code getAuthorizationDetail} 组合判定的结果:授权状态与
 * 该角色当前有效 accessToken 中声明的已授权权限(原始 scope 名),
 * 由 application 层直接消费并透出到响应 DTO。</p>
 *
 * <p><b>scopes 语义</b>:
 * <ul>
 *     <li>{@code null} = 无法确定(非 AUTHORIZED / 无可用 accessToken / 解析失败)</li>
 *     <li>空列表 = token 有效但未声明任何受限权限</li>
 * </ul></p>
 *
 * @param status  ESI 授权状态
 * @param scopes  已授权权限列表(原始 scope 名),语义见上
 * @author Leojan
 * date 2026-09-01
 */
public record EsiAuthorizationDetail(EsiAuthStatus status, List<String> scopes) {
}
