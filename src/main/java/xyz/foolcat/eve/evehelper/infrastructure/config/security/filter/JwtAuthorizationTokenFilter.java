package xyz.foolcat.eve.evehelper.infrastructure.config.security.filter;

import cn.hutool.core.util.StrUtil;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.rememberme.InvalidCookieException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import xyz.foolcat.eve.evehelper.domain.service.security.TokenBlacklistService;
import xyz.foolcat.eve.evehelper.shared.kernel.constants.SecurityConstant;
import xyz.foolcat.eve.evehelper.shared.result.ResultCode;
import xyz.foolcat.eve.evehelper.shared.util.SensitiveDataMasker;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT认证校验
 *
 * @author Leojan
 * date 2022-01-14 13:47
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthorizationTokenFilter extends OncePerRequestFilter {

    private final KeyPair keyPair;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = request.getHeader(SecurityConstant.AUTHORIZATION_KEY);

        if (StrUtil.isEmpty(token) || !token.startsWith(SecurityConstant.JWT_PREFIX)) {
            //非JWT或者JWT为空不处理
            logger.debug("非JWT验证，进入下一个步");
            filterChain.doFilter(request, response);
            return;
        }

        token = token.replace(SecurityConstant.JWT_PREFIX, Strings.EMPTY);

        try {
            // 解析JWT
            SignedJWT signedJWT = SignedJWT.parse(token);
            PublicKey publicKey = keyPair.getPublic();
            JWSVerifier verifier = new RSASSAVerifier((RSAPublicKey) publicKey);

            // 验证签名
            if (!signedJWT.verify(verifier)) {
                throw new InvalidCookieException(ResultCode.TOKEN_ACCESS_EXPIRED.getMsg());
            }

            JWTClaimsSet jwtClaimsSet = signedJWT.getJWTClaimsSet();

            // 检查过期时间
            if (jwtClaimsSet.getExpirationTime().getTime() < System.currentTimeMillis()) {
                throw new InvalidCookieException(ResultCode.TOKEN_ACCESS_EXPIRED.getMsg());
            }

            // 检查黑名单
            String jti = jwtClaimsSet.getJWTID();
            if (tokenBlacklistService.isBlacklisted(jti)) {
                log.warn("Token已被撤销: jti={}", SensitiveDataMasker.maskToken(jti));
                throw new InvalidCookieException("Token已被撤销");
            }

            // 提取权限信息
            List<GrantedAuthority> authorities = jwtClaimsSet.getStringListClaim(SecurityConstant.JWT_AUTHORITIES_KEY)
                    .stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            Authentication authResult = new UsernamePasswordAuthenticationToken
                    (jwtClaimsSet.getClaim(SecurityConstant.USER_ID_KEY), null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authResult);
            log.debug("JWT验证完成: userId={}", jwtClaimsSet.getClaim(SecurityConstant.USER_ID_KEY));

        } catch (ParseException e) {
            log.error("JWT解析失败", e);
            throw new InvalidCookieException("无效的Token格式");
        } catch (JOSEException e) {
            log.error("JWT验证失败", e);
            throw new InvalidCookieException("Token验证失败");
        }

        filterChain.doFilter(request, response);
    }
}
