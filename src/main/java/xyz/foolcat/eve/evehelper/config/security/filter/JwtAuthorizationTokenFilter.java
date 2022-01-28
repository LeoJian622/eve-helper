package xyz.foolcat.eve.evehelper.config.security.filter;

import cn.hutool.core.util.StrUtil;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.rememberme.InvalidCookieException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import xyz.foolcat.eve.evehelper.common.constant.SecurityConstant;
import xyz.foolcat.eve.evehelper.common.result.ResultCode;

import javax.annotation.Resource;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.hibernate.validator.internal.util.Contracts.assertTrue;

/**
 * JWT认证校验
 *
 * @author Leojan
 * @date 2022-01-14 13:47
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthorizationTokenFilter extends OncePerRequestFilter {

    private final KeyPair keyPair;

    @SneakyThrows
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        String token = request.getHeader(SecurityConstant.AUTHORIZATION_KEY);

        if (StrUtil.isBlank(token) || !token.startsWith(SecurityConstant.JWT_PREFIX)) {
            //非JWT或者JWT为空不处理
            chain.doFilter(request, response);
            return;
        }

        // 解析JWT获取JTI，以JTI为KEY判断redis的黑名单列表是否存在，存在则拦截访问
        token = token.replace(SecurityConstant.JWT_PREFIX, Strings.EMPTY);
        SignedJWT signedJWT = SignedJWT.parse(token);
        PublicKey publicKey = keyPair.getPublic();
        JWSVerifier verifier = new RSASSAVerifier((RSAPublicKey) publicKey);

        if (!signedJWT.verify(verifier)) {
            throw new InvalidCookieException(ResultCode.TOKEN_ACCESS_EXPIRED.getMsg());
        }

        JWTClaimsSet jwtClaimsSet = signedJWT.getJWTClaimsSet();
        if (jwtClaimsSet.getExpirationTime().getTime() < System.currentTimeMillis()) {
            throw new InvalidCookieException(ResultCode.TOKEN_ACCESS_EXPIRED.getMsg());
        }
        List<GrantedAuthority> authorities = jwtClaimsSet.getStringListClaim(SecurityConstant.JWT_AUTHORITIES_KEY).stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());

        Authentication authResult = new UsernamePasswordAuthenticationToken
                (jwtClaimsSet.getClaim(SecurityConstant.USER_ID_KEY), null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authResult);
        chain.doFilter(request, response);
    }
}
