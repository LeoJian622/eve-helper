package xyz.foolcat.eve.evehelper.config.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import xyz.foolcat.eve.evehelper.config.security.filter.JwtAuthorizationTokenFilter;
import xyz.foolcat.eve.evehelper.config.security.handler.AccessDeniedServletHandler;
import xyz.foolcat.eve.evehelper.config.security.handler.AuthenticationSuccessServletHandler;
import xyz.foolcat.eve.evehelper.service.system.SysUserService;

/**
 * @author Leojan
 * @date 2021-12-14 15:05
 */

@Configuration
@EnableWebSecurity
@Slf4j
@RequiredArgsConstructor
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    private final SysUserService sysUserService;

    private final JwtAuthorizationTokenFilter jwtAuthorizationTokenFilter;

    private final AccessDeniedServletHandler accessDeniedServletHandler;

    private final AuthenticationServletEntryPoint authenticationServletEntryPoint;

    private final AuthenticationSuccessServletHandler authenticationSuccessServletHandler;

    /**
     * http请求设置
     */
    @Override
    public void configure(HttpSecurity http) throws Exception {
        http
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                .antMatchers(
                        "/oauth/**"
                        , "/webjars/**"
                        , "/swagger-ui/**"
                        , "/swagger-resources/**"
                        , "/*/v3/api-docs"
                )
                .permitAll()
                .anyRequest()
                .access("@rbacPermission.hasPermission(request, authentication)")
                .and()
                .addFilterBefore(jwtAuthorizationTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .formLogin()
                .permitAll()
                .successHandler(authenticationSuccessServletHandler)
                .and()
                .exceptionHandling()
                .accessDeniedHandler(accessDeniedServletHandler)
                .authenticationEntryPoint(authenticationServletEntryPoint)
                .and()
                .logout()
                .permitAll()
                .and()
                .csrf()
                .disable();
            }

    /**
     * 自定义获取用户信息接口
     */
    @Override
    public void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(daoAuthenticationProvider());
    }

    /**
     * 用户名密码认证授权提供者
     *
     * @return
     */
    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        // 是否隐藏用户不存在异常，默认:true-隐藏；false-抛出异常；
        provider.setHideUserNotFoundExceptions(false);
        provider.setUserDetailsService(sysUserService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * 密码加密算法
     *
     * @return
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();

    }


}
