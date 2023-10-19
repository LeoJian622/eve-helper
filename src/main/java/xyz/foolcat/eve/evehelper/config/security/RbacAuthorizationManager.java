package xyz.foolcat.eve.evehelper.config.security;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.convert.Convert;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import xyz.foolcat.eve.evehelper.common.constant.GlobalConstants;
import xyz.foolcat.eve.evehelper.common.constant.SecurityConstant;
import xyz.foolcat.eve.evehelper.mapper.system.SysPermissionMapper;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 权限验证器
 *
 * @author Leojan
 * @date 2023-07-20 16:23
 */

@Component
@RequiredArgsConstructor
public class RbacAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    final SysPermissionMapper sysPermissionMapper;

    final RedisTemplate redisTemplate;

    final EveHelperSecurityConfig eveHelperSecurityConfig;

    final PathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authenticationSupplier, RequestAuthorizationContext requestAuthorizationContext) {

        HttpServletRequest request = requestAuthorizationContext.getRequest();
        String method = request.getMethod();
        if (SecurityConstant.OPTIONS.equalsIgnoreCase(method)) {
            return new AuthorizationDecision(true);
        }

        String path = request.getRequestURI();
        // RESTFul接口权限设计
        String restfulPath = method + ":" + path;

        /**
         * 白名单路径
         */
        boolean isWhiteList = eveHelperSecurityConfig.getWhiteUrlList().stream()
                .anyMatch(whilte -> {
                    if (restfulPath.equals(whilte)) {
                        return true;
                    }
                    return false;
                });

        if (isWhiteList) {
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
        Map<String, Object> urlPermRolesRules = redisTemplate.opsForHash().entries(GlobalConstants.URL_PERM_ROLES_KEY);

        //根据请求路径判断有访问权限的角色列表
        List<String> authorizedRoles = new ArrayList<>();

        boolean personSourceVery = true;

        for (Map.Entry<String, Object> permRoles : urlPermRolesRules.entrySet()) {
            String perm = permRoles.getKey();
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
                    .anyMatch(authority -> {
                        if (GlobalConstants.ROOT_ROLE_CODE.equals(authority)) {
                            return true;
                        }
                        return CollectionUtil.isNotEmpty(authorizedRoles) && authorizedRoles.contains(authority) && finalPersonSourceVery;
                    });
        }
        return new AuthorizationDecision(hasPermission);
//        return new AuthorizationDecision(true);
    }

    @Override
    public void verify(Supplier<Authentication> authentication, RequestAuthorizationContext object) {
        AuthorizationManager.super.verify(authentication, object);
    }
}
