package xyz.foolcat.eve.evehelper.config.security;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.convert.Convert;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
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

/**
 * @author Leojan
 * @date 2021-12-28 11:32
 */

@Component
@RequiredArgsConstructor
public class RbacPermission {

    private final SysPermissionMapper sysPermissionMapper;

    private final RedisTemplate redisTemplate;

    private final EveHelperSecurityConfig eveHelperSecurityConfig;

    public boolean hasPermission(HttpServletRequest request, Authentication authentication) {


        String method = request.getMethod();
        if (SecurityConstant.OPTIONS.equalsIgnoreCase(method)) {
            return true;
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

        if (isWhiteList){
            return isWhiteList;
        }

        /**
         * 鉴权
         * 缓存取 [URL权限-角色集合] 规则数据
         * urlPermRolesRules = [{'key':'GET:/api/v1/users/*','value':['ADMIN','TEST']},...]
         */
        Map<String, Object> urlPermRolesRules = redisTemplate.opsForHash().entries(GlobalConstants.URL_PERM_ROLES_KEY);

        //根据请求路径判断有访问权限的角色列表
        List<String> authorizedRoles = new ArrayList<>();
        boolean requireCheck = Boolean.FALSE;

        PathMatcher pathMatcher = new AntPathMatcher();

        for (Map.Entry<String, Object> permRoles : urlPermRolesRules.entrySet()) {
            String perm = permRoles.getKey();
            if (pathMatcher.match(perm, restfulPath)) {
                List<String> roles = Convert.toList(String.class, permRoles.getValue());
                authorizedRoles.addAll(roles);
                if (!requireCheck) {
                    requireCheck = Boolean.TRUE;
                }
            }
        }

        boolean hasPermission = false;
        if (authentication.isAuthenticated() && requireCheck) {

            Collection<? extends GrantedAuthority> authentications = authentication.getAuthorities();
            hasPermission = authentications.stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(authority -> {
                        if (GlobalConstants.ROOT_ROLE_CODE.equals(authority)) {
                            return true;
                        }
                        return CollectionUtil.isNotEmpty(authorizedRoles) && authorizedRoles.contains(authority);
                    });
        }

        return hasPermission;
    }


}
