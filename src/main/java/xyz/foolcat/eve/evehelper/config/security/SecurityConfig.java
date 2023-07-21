package xyz.foolcat.eve.evehelper.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import xyz.foolcat.eve.evehelper.config.security.filter.JwtAuthorizationTokenFilter;
import xyz.foolcat.eve.evehelper.config.security.handler.AccessDeniedServletHandler;
import xyz.foolcat.eve.evehelper.config.security.handler.AuthenticationSuccessServletHandler;
import xyz.foolcat.eve.evehelper.service.system.SysUserService;

import java.security.KeyPair;

/**
 * @author Leojan
 * @date 2023-07-20 15:05
 */

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    final RbacAuthorizationManager authorizationManager;

    final AccessDeniedServletHandler accessDeniedServletHandler;

    final AuthenticationServletEntryPoint authenticationServletEntryPoint;

    final SysUserService sysUserService;

    @Bean
    WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring().antMatchers("/swagger-ui.html"
                , "/swagger-ui/**"
                , "/v3/api-docs/**");
    }

    /**
     * 密钥库中获取密钥对(公钥+私钥)
     */
    @Bean
    public KeyPair keyPair() {
        KeyStoreKeyFactory factory = new KeyStoreKeyFactory(new ClassPathResource("eve-jwt.jks"), "eve000".toCharArray());
        return factory.getKeyPair("eve-jwt", "eve000".toCharArray());
    }

    @Bean
    JwtAuthorizationTokenFilter jwtAuthorizationTokenFilter(KeyPair keyPair) {
        JwtAuthorizationTokenFilter jwtAuthorizationTokenFilter = new JwtAuthorizationTokenFilter(keyPair);
        jwtAuthorizationTokenFilter.setAuthenticationManager(authenticationManager());
        return jwtAuthorizationTokenFilter;
    }

    @Bean
    @ConfigurationProperties(prefix = "jwt-token")
    JwtTokenConfig jwtTokenConfig() {
        return new JwtTokenConfig();
    }

    @Bean
    AuthenticationSuccessServletHandler authenticationSuccessServletHandler(KeyPair keyPair, JwtTokenConfig jwtTokenConfig) {
        return new AuthenticationSuccessServletHandler(keyPair, jwtTokenConfig);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().access(authorizationManager))
                .formLogin(form -> form.permitAll().successHandler(authenticationSuccessServletHandler(keyPair(),jwtTokenConfig())))
                .exceptionHandling(except -> except.accessDeniedHandler(accessDeniedServletHandler).authenticationEntryPoint(authenticationServletEntryPoint))
                .logout(LogoutConfigurer::permitAll)
                .csrf(CsrfConfigurer::disable);
        httpSecurity.authenticationManager(authenticationManager());
        httpSecurity.addFilterBefore(jwtAuthorizationTokenFilter(keyPair()), UsernamePasswordAuthenticationFilter.class);

        return httpSecurity.build();
    }

    @Bean
    AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
        // 是否隐藏用户不存在异常，默认:true-隐藏；false-抛出异常；
        daoAuthenticationProvider.setHideUserNotFoundExceptions(false);
        daoAuthenticationProvider.setUserDetailsService(sysUserService);
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(daoAuthenticationProvider);
    }

    /**
     * 密码加密算法
     *
     * @return BCryptPasswordEncoder
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();

    }

}