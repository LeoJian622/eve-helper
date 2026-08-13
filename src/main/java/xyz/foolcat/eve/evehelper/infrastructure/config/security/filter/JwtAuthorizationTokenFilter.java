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
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import xyz.foolcat.eve.evehelper.domain.service.security.TokenBlacklistService;
import xyz.foolcat.eve.evehelper.infrastructure.config.security.WhiteUrlMatcher;
import xyz.foolcat.eve.evehelper.shared.kernel.constants.SecurityConstant;
import xyz.foolcat.eve.evehelper.shared.result.ResultCode;
import xyz.foolcat.eve.evehelper.shared.util.ResponseUtils;
import xyz.foolcat.eve.evehelper.shared.util.SensitiveDataMasker;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT认证校验(007 T013 重写:失败干净终止,白名单匿名放行)。
 *
 * <p><b>重写动机(FR-016)</b>:旧实现在验签/过期/黑名单/解析失败时抛
 * {@code InvalidCookieException},该异常在 {@code ExceptionTranslationFilter}
 * 上游抛出,冒泡为异常逃逸而非干净的 401。现改为全部 5 个失败出口统一走
 * {@link #rejectOrPass}:白名单路径匿名放行,非白名单直写 401。</p>
 *
 * <p><b>白名单单一事实来源(CRITICAL-1)</b>:白名单判定消费
 * {@link WhiteUrlMatcher},与 {@code RbacAuthorizationManager} 完全一致 ——
 * 防止「RBAC 放行、过滤器拦截」的 401 死循环。</p>
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
    /** 白名单判定单一事实来源(与 RbacAuthorizationManager 共同消费) */
    private final WhiteUrlMatcher whiteUrlMatcher;

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

            // 验证签名(失败出口①)
            if (!signedJWT.verify(verifier)) {
                rejectOrPass(request, response, filterChain, ResultCode.TOKEN_ACCESS_EXPIRED);
                return;
            }

            JWTClaimsSet jwtClaimsSet = signedJWT.getJWTClaimsSet();

            // 检查过期时间(失败出口②)
            if (jwtClaimsSet.getExpirationTime().getTime() < System.currentTimeMillis()) {
                rejectOrPass(request, response, filterChain, ResultCode.TOKEN_ACCESS_EXPIRED);
                return;
            }

            // 检查黑名单(失败出口③)
            String jti = jwtClaimsSet.getJWTID();
            if (tokenBlacklistService.isBlacklisted(jti)) {
                log.warn("Token已被撤销: jti={}", SensitiveDataMasker.maskToken(jti));
                rejectOrPass(request, response, filterChain, ResultCode.TOKEN_ACCESS_EXPIRED);
                return;
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
            // 失败出口④:只打异常类名,不打堆栈与 token 内容
            log.warn("JWT解析失败: {}", e.getClass().getSimpleName());
            rejectOrPass(request, response, filterChain, ResultCode.TOKEN_ACCESS_EXPIRED);
            return;
        } catch (JOSEException e) {
            // 失败出口⑤
            log.warn("JWT验证失败: {}", e.getClass().getSimpleName());
            rejectOrPass(request, response, filterChain, ResultCode.TOKEN_ACCESS_EXPIRED);
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 认证失败统一出口(007 T013,plan v3 §5):
     * <ul>
     *   <li><b>白名单路径</b>:匿名放行(继续过滤器链,<b>不</b>写 SecurityContext)——
     *       让 {@code POST /auth/tokens} 在持旧/非法 token 时仍能到达 controller 换新 token</li>
     *   <li><b>非白名单路径</b>:直写 401 + {@code AUT00210},不再继续过滤器链</li>
     * </ul>
     *
     * <p><b>统一返回 AUT00210 是防信息泄露的有意决策(LOW-2)</b>:不向客户端区分
     * 「验签失败/过期/被撤销」,避免泄露服务端密钥状态。勿当 bug「修复」。</p>
     */
    private void rejectOrPass(HttpServletRequest request, HttpServletResponse response,
                              FilterChain filterChain, ResultCode code) throws IOException, ServletException {
        if (whiteUrlMatcher.isWhiteListed(request)) {
            filterChain.doFilter(request, response);
            // 与下方 isCommitted 守卫构成双保险(纵深防御)。007 T032 变异测试实测:
            // 单删本 return、或连同 isCommitted 守卫一起删,黑盒测试均无法捕获 ——
            // 白名单请求经 controller 后响应已提交,Servlet 规范下 setStatus 无效。
            // 即本行无可观测行为差异,故无测试保护;勿因「无测试覆盖」当冗余删除
            return;
        }
        if (response.isCommitted()) {
            log.warn("响应已提交,跳过写入 401");
            return;
        }
        ResponseUtils.writeErrorInfo(response, code);
    }
}
