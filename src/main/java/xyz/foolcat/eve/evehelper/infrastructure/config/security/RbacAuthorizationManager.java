package xyz.foolcat.eve.evehelper.infrastructure.config.security;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.convert.Convert;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import xyz.foolcat.eve.evehelper.shared.kernel.constants.GlobalConstants;
import xyz.foolcat.eve.evehelper.shared.kernel.constants.SecurityConstant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 权限验证器
 *
 * @author Leojan
 * date 2023-07-20 16:23
 */

@Component
@RequiredArgsConstructor
public class RbacAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    final RedisTemplate<String, Object> redisTemplate;

    /**
     * 白名单判定单一事实来源(007 T006,CRITICAL-1):
     * JwtAuthorizationTokenFilter 与本管理器共同消费,防语义漂移
     */
    final WhiteUrlMatcher whiteUrlMatcher;

    final PathMatcher pathMatcher = new AntPathMatcher();

    @Nullable
    @Override
    public AuthorizationResult authorize(Supplier<Authentication> authenticationSupplier, RequestAuthorizationContext requestAuthorizationContext) {
        return check(authenticationSupplier, requestAuthorizationContext);
    }

    @Override
    @Deprecated
    public AuthorizationDecision check(Supplier<Authentication> authenticationSupplier, RequestAuthorizationContext requestAuthorizationContext) {

        HttpServletRequest request = requestAuthorizationContext.getRequest();
        String method = request.getMethod();
        // OPTIONS 预检短路。注意:该短路【有意】不并入 WhiteUrlMatcher ——
        // CORS 预检不带 Authorization 头,走 JwtAuthorizationTokenFilter 的
        // 「非 JWT 不处理」分支,过滤器侧无需对应短路(plan v3 §3.2、round3 §1.3)。
        if (SecurityConstant.OPTIONS.equalsIgnoreCase(method)) {
            return new AuthorizationDecision(true);
        }

        String path = request.getRequestURI();
        // RESTFul接口权限设计
        String restfulPath = method + ":" + path;

        /**
         * 白名单路径(007 T006:委托 WhiteUrlMatcher 单一事实来源,
         * 与 JwtAuthorizationTokenFilter 的白名单放行语义保持完全一致)
         */
        if (whiteUrlMatcher.isWhiteListed(request)) {
            return new AuthorizationDecision(true);
        }

        Authentication authentication = authenticationSupplier.get();

        Collection<? extends GrantedAuthority> authentications = authentication.getAuthorities();

        /**
         * 具有超级管理员权限用户直接放过
         */

        /**
         * 鉴权
         * 缓存取 [URL权限-角色集合] 规则数据
         * urlPermRolesRules = [{'key':'GET:/api/v1/users/*','value':['ADMIN','TEST']},...]
         */
        Map<Object, Object> urlPermRolesRules = redisTemplate.opsForHash().entries(GlobalConstants.URL_PERM_ROLES_KEY);

        //根据请求路径判断有访问权限的角色列表
        List<String> authorizedRoles = new ArrayList<>();

        boolean personSourceVery = true;

        for (Map.Entry<Object, Object> permRoles : urlPermRolesRules.entrySet()) {
            String perm = String.valueOf(permRoles.getKey());
            if (pathMatcher.match(perm, restfulPath)) {
                List<String> roles = Convert.toList(String.class, permRoles.getValue());
                authorizedRoles.addAll(roles);

                /**
                 * 私人资源鉴权
                 * 非ADMIN角色要判断访问的用户和访问的用户资源是否一致
                 */

                if (pathMatcher.isPattern(perm)) {
                    String uid = pathMatcher.extractUriTemplateVariables(perm, restfulPath).get("uid");
                    if (uid != null) {
                        personSourceVery = authentication.getPrincipal().toString().equals(uid);
                    }
                }

            }
        }
        boolean hasPermission = false;

        if (authentication.isAuthenticated()) {

            boolean finalPersonSourceVery = personSourceVery;
            hasPermission = authentications.stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(authority ->
                            // 如果是 ROOT 角色，直接放行
                            GlobalConstants.ROOT_ROLE_CODE.equals(authority)
                                    ||
                                    // 否则，必须同时满足：有授权角色 且 人员来源校验通过
                                    (CollectionUtil.isNotEmpty(authorizedRoles) && authorizedRoles.contains(authority) && finalPersonSourceVery)
                    );
        }
        return new AuthorizationDecision(hasPermission);
//        return new AuthorizationDecision(true);
    }

    @Override
    public void verify(Supplier<Authentication> authentication, RequestAuthorizationContext object) {
        AuthorizationManager.super.verify(authentication, object);
    }
}
