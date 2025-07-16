package xyz.foolcat.eve.evehelper.infrastructure.config.security.handler;

import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.rememberme.InvalidCookieException;
import org.springframework.stereotype.Component;
import xyz.foolcat.eve.evehelper.shared.result.ResultCode;
import xyz.foolcat.eve.evehelper.shared.util.ResponseUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Leojan
 * date 2022-01-12 15:39
 */

@Component
public class AuthticationFailureServletHandller implements AuthenticationFailureHandler {
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        if (exception instanceof AccountExpiredException) {
            //账号过期
            ResponseUtils.writeErrorInfo(response, ResultCode.USER_ACCOUNT_EXPIRED);
        } else if (exception instanceof BadCredentialsException) {
            //密码错误
            ResponseUtils.writeErrorInfo(response, ResultCode.USER_CREDENTIALS_ERROR);
        } else if (exception instanceof CredentialsExpiredException) {
            //密码过期
            ResponseUtils.writeErrorInfo(response, ResultCode.USER_USER_CREDENTIALS_EXPIRED);
        } else if (exception instanceof DisabledException) {
            //账号不可用
            ResponseUtils.writeErrorInfo(response, ResultCode.USER_ACCOUNT_DISABLE);
        } else if (exception instanceof LockedException) {
            //账号锁定
            ResponseUtils.writeErrorInfo(response, ResultCode.USER_ACCOUNT_LOCKED);
        } else if (exception instanceof InternalAuthenticationServiceException) {
            //用户不存在
            ResponseUtils.writeErrorInfo(response, ResultCode.USER_ACCOUNT_NOT_EXIST);
        }else if (exception instanceof InvalidCookieException) {
            //登录过期
            ResponseUtils.writeErrorInfo(response, ResultCode.TOKEN_ACCESS_EXPIRED);
        } else {
            //其他错误
            ResponseUtils.writeErrorInfo(response, ResultCode.COMMON_FAIL);
        }
    }
}
