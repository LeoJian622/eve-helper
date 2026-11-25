package xyz.foolcat.eve.evehelper.application.dto.response;

/**
 * 授权 URL 响应(T043)。
 *
 * @param authorizeUrl ESI OAuth 授权 URL
 * @param state        防 CSRF state 参数(固定 "eve-helper")
 * @author Leojian
 */
public record AuthorizeUrlResponse(String authorizeUrl, String state) {
}
