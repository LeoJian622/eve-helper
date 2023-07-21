package xyz.foolcat.eve.evehelper.config.security.handler;

import cn.hutool.core.lang.UUID;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import xyz.foolcat.eve.evehelper.common.constant.SecurityConstant;
import xyz.foolcat.eve.evehelper.config.security.JwtTokenConfig;
import xyz.foolcat.eve.evehelper.config.security.KeyStoreKeyFactory;
import xyz.foolcat.eve.evehelper.domain.system.SysUser;
import xyz.foolcat.eve.evehelper.util.ResponseUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * @author Leojan
 * @date 2022-01-12 14:57
 */

@Slf4j
@RequiredArgsConstructor
public class AuthenticationSuccessServletHandler implements AuthenticationSuccessHandler {

    private final KeyPair keyPair;

    private final JwtTokenConfig jwtTokenConfig;

    @SneakyThrows
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        SysUser sysUser = (SysUser) authentication.getPrincipal();

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(jwtTokenConfig.getSubject())
                .issuer(jwtTokenConfig.getIssuer())
                .jwtID(UUID.randomUUID().toString())
                .claim(SecurityConstant.USER_ID_KEY, sysUser.getId())
                .claim(SecurityConstant.USER_NAME_KEY, sysUser.getUsername())
                .claim(SecurityConstant.JWT_AUTHORITIES_KEY, sysUser.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()))
//                .claim("esi-authorise", Arrays.asList("qwe", "asd", "zxc"))
                .expirationTime(new Date(System.currentTimeMillis() + jwtTokenConfig.getExpirationTime() * 1000))
                .build();

        System.out.println(claimsSet);
        SignedJWT signedJwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).build(),
                claimsSet);
        JWSSigner jwsSigner = new RSASSASigner(keyPair.getPrivate());
        signedJwt.sign(jwsSigner);

        ResponseUtils.writeTokenInfo(response, signedJwt);
    }

    /**
     * JWT 创建与校验测试
     * @param args
     * @throws JOSEException
     * @throws ParseException
     */
    public static void main(String[] args) throws JOSEException, ParseException {
        KeyStoreKeyFactory factory = new KeyStoreKeyFactory(new ClassPathResource("jwt.jks"), "123456".toCharArray());
        KeyPair keyPair1 = factory.getKeyPair("jwt", "123456".toCharArray());

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("alice")
                .jwtID(UUID.randomUUID().toString())
                .issuer("https://c2id.com")
                .claim(SecurityConstant.USER_ID_KEY, "1")
                .claim(SecurityConstant.USER_NAME_KEY, "admin")
                .claim(SecurityConstant.JWT_AUTHORITIES_KEY, Arrays.asList("qwe", "asd", "zxc"))
                .claim("esi-authorise", Arrays.asList("qwe", "asd", "zxc"))
                .expirationTime(new Date(System.currentTimeMillis() + 60 * 1000))
                .build();

        SignedJWT signedJwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).build(),
                claimsSet);
        JWSSigner jwsSigner = new RSASSASigner(keyPair1.getPrivate());

        signedJwt.sign(jwsSigner);

        System.out.println(signedJwt.serialize());

        SignedJWT signedJwtParse = SignedJWT.parse(signedJwt.serialize());

        PublicKey publicKey = keyPair1.getPublic();
        JWSVerifier verifier = new RSASSAVerifier((RSAPublicKey) publicKey);

        System.out.println(signedJwtParse.verify(verifier));
    }
}
