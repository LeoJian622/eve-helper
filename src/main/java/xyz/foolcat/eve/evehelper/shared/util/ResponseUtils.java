package xyz.foolcat.eve.evehelper.shared.util;

import cn.hutool.json.JSONUtil;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import xyz.foolcat.eve.evehelper.shared.result.Result;
import xyz.foolcat.eve.evehelper.shared.result.ResultCode;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Leojan
 * date 2021-08-13 15:19
 */

public class ResponseUtils {

    public static HttpServletResponse writeErrorInfo(HttpServletResponse response, ResultCode resultCode) throws IOException {
        switch (resultCode) {
            case ACCESS_UNAUTHORIZED:
            case TOKEN_INVALID_OR_EXPIRED:
            // 007 T012(FR-016):TOKEN过期/验签失败/被撤销统一 401,由
            // JwtAuthorizationTokenFilter 直写。统一返回 AUT00210 是防信息泄露的
            // 有意决策(LOW-2),勿当 bug「修复」为按原因区分的状态码
            case TOKEN_ACCESS_EXPIRED:
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                break;
            case TOKEN_ACCESS_FORBIDDEN:
                response.setStatus(HttpStatus.FORBIDDEN.value());
                break;
            default:
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                break;
        }
        // 007 T012:声明 charset —— msg 为中文,不声明则客户端可能按默认编码解码乱码
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Cache-Control", "no-cache");
        String body = JSONUtil.toJsonStr(Result.failed(resultCode));
        response.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
        return response;
    }

    public static HttpServletResponse writeTokenInfo(HttpServletResponse response, SignedJWT signedJwt) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
        Map<String,String> tokenObejct = new HashMap<>(2);
        tokenObejct.put("access_token",signedJwt.serialize());
        String body = JSONUtil.toJsonStr(Result.success(tokenObejct));
        response.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
        return response;
    }
}
