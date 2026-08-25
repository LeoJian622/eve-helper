package xyz.foolcat.eve.evehelper.shared.kernel.enums;

/**
 * 余额查询结果状态（018 IMPORTANT-2）。
 *
 * <p>用于把「余额确实为 0」与「余额没查到」在返回结构上区分开。此前两者同形——
 * 查询失败被 fail-open 的 catch 降级为 {@code 0.0}，调用方（尤其展示 ISK 资产的前端）
 * 无从判断该数值是否可信，可能据此做出错误的交易决策。</p>
 *
 * <ul>
 *     <li>OK:查询成功,balance 为可信数值</li>
 *     <li>UNAUTHORIZED:授权失效(refreshToken 被撤销/过期、归属校验不通过),
 *         需重新授权;balance 为 null</li>
 *     <li>UPSTREAM_ERROR:上游故障(ESI 5xx、超时、响应解析失败),
 *         与授权无关,重试可能恢复;balance 为 null</li>
 * </ul>
 *
 * <p>与 {@link EsiAuthStatus} 的区别:后者描述角色的<b>授权有效性</b>,本枚举描述
 * 单次<b>余额查询的结果</b>。二者会分叉——授权正常(AUTHORIZED)但 ESI 返回 5xx 时,
 * 余额查询状态是 UPSTREAM_ERROR。故不复用。</p>
 *
 * @author Leojian
 * date 2026-08-25
 */
public enum BalanceQueryStatus {

    OK("查询成功"),
    UNAUTHORIZED("需重新授权"),
    UPSTREAM_ERROR("上游故障");

    private final String description;

    BalanceQueryStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
