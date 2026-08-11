package xyz.foolcat.eve.evehelper.infrastructure.config.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 白名单匹配单一事实来源(007 CRITICAL-1,T005)。
 *
 * <p>匹配语义与原 {@code RbacAuthorizationManager:67-73} 内联实现完全一致:
 * {@code method + ":" + requestURI} <b>精确字符串相等</b>(非 Ant 通配、
 * 大小写敏感、尾斜杠不匹配)。白名单数据源为
 * {@link EveHelperSecurityConfig#getWhiteUrlList()}(配置项
 * {@code eve.helper.whiteUrlList}),null 列表视为无白名单。</p>
 *
 * <p><b>为什么抽出本组件</b>:007 让 {@code JwtAuthorizationTokenFilter} 在验签失败时
 * 依据白名单决定「匿名放行」还是「直写 401」。改造前白名单判定仅存在于
 * {@code RbacAuthorizationManager} 内部,过滤器不可见 —— 若两处各写一份判定,
 * 语义漂移将导致「RBAC 放行、过滤器拦截」的 401 死循环。过滤器与授权管理器
 * 必须共同消费本组件(契约测试见 {@code WhiteUrlMatcherContractTest})。</p>
 *
 * <p><b>语义边界</b>:OPTIONS 预检短路属于 {@code RbacAuthorizationManager}
 * (先于白名单判定),<b>不</b>在本组件内 —— CORS 预检不带 Authorization 头,
 * 走过滤器「非 JWT 不处理」分支,两者无需在此对齐(plan v3 §3.2)。</p>
 *
 * @author Leojan
 * date 2026-08-12
 */
@Component
@RequiredArgsConstructor
public class WhiteUrlMatcher {

    private final EveHelperSecurityConfig eveHelperSecurityConfig;

    /**
     * 判断请求是否命中白名单。
     *
     * @param request HTTP 请求(method 与 requestURI 参与匹配,query 不参与)
     * @return 命中返回 true;白名单未配置(null)或为空时恒为 false
     */
    public boolean isWhiteListed(HttpServletRequest request) {
        List<String> whiteUrlList = eveHelperSecurityConfig.getWhiteUrlList();
        if (whiteUrlList == null || whiteUrlList.isEmpty()) {
            return false;
        }
        String restfulPath = request.getMethod() + ":" + request.getRequestURI();
        return whiteUrlList.stream().anyMatch(restfulPath::equals);
    }
}
