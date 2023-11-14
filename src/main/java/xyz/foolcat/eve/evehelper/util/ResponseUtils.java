package xyz.foolcat.eve.evehelper.util;

import cn.hutool.json.JSONUtil;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import xyz.foolcat.eve.evehelper.common.result.Result;
import xyz.foolcat.eve.evehelper.common.result.ResultCode;

import javax.servlet.http.HttpServletResponse;
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
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                break;
            case TOKEN_ACCESS_FORBIDDEN:
                response.setStatus(HttpStatus.FORBIDDEN.value());
                break;
            default:
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                break;
        }
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
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
