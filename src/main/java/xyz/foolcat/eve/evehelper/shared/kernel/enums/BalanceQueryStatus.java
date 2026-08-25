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
 *     <li>PERMISSION_DENIED:token 有效但该人物缺少目标军团的 Accountant/Director 等角色,
 *         或已离开该军团。<b>重新授权无用</b>,需军团管理者调整角色权限;balance 为 null</li>
 *     <li>UPSTREAM_ERROR:上游故障(ESI 5xx、超时、响应解析失败),
 *         与授权无关,重试可能恢复;balance 为 null</li>
 * </ul>
 *
 * <p><b>调用方须据此决定是否重试</b>：仅 UPSTREAM_ERROR 值得重试。
 * UNAUTHORIZED 与 PERMISSION_DENIED 都是持久性状态，重试只会反复撞 ESI 4xx 并消耗
 * 全应用共享的 ESI error limit（100 errors/60s/IP），超限后所有用户的所有 ESI 功能
 * 一起不可用。把 PERMISSION_DENIED 混入 UPSTREAM_ERROR 正是该放大链的一环
 * （2026-08-25 安全审计 MEDIUM-1）。</p>
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
    PERMISSION_DENIED("人物缺少军团钱包权限"),
    UPSTREAM_ERROR("上游故障");

    private final String description;

    BalanceQueryStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
