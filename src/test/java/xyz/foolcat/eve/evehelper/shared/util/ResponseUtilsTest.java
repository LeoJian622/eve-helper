package xyz.foolcat.eve.evehelper.shared.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import xyz.foolcat.eve.evehelper.shared.result.ResultCode;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ResponseUtils#writeErrorInfo} 目标行为测试(007 T011;T012 实现的 RED 判据)。
 *
 * <p>FR-016 将让 {@code JwtAuthorizationTokenFilter} 复用该方法直接写 401 响应。
 * 诊断阶段查明的两处现状(结论已归档于评审记录)在此固化为目标断言:</p>
 * <ol>
 *   <li><b>状态码映射</b>:{@code TOKEN_ACCESS_EXPIRED}(AUT00210)当前落 switch 的
 *       {@code default} 分支 → 400。T012 新增 401 分支 —— 本测试断言 401(当前 RED)</li>
 *   <li><b>字符编码</b>:该方法当前只 {@code setContentType},不声明 charset,
 *       而 msg 均为中文,客户端可能按默认编码解码乱码。
 *       T012 补 {@code charset=UTF-8} —— 本测试断言 Content-Type 含 charset(当前 RED)</li>
 * </ol>
 *
 * <p>UTF-8 可解码用例固化「方法手工写 UTF-8 字节」的事实,不参与 RED→GREEN。</p>
 *
 * @author Leojan
 * date 2026-08-11
 */
@DisplayName("ResponseUtils 现状诊断(007 FR-016)")
class ResponseUtilsTest {

    @Test
    @DisplayName("AUT00210(TOKEN过期)应映射 401(007 FR-016)")
    void writeErrorInfo_tokenAccessExpired_maps401() throws Exception {
        // Arrange
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Act
        ResponseUtils.writeErrorInfo(response, ResultCode.TOKEN_ACCESS_EXPIRED);

        // Assert:RED 判据 —— 现状落 switch default 得 400;T012 增 401 分支后转 GREEN
        assertEquals(401, response.getStatus(),
                "TOKEN_ACCESS_EXPIRED 必须映射 401(现状落 switch default → 400)");
    }

    @Test
    @DisplayName("对照:AUT00201 与 AUT00301 已正确映射 401")
    void writeErrorInfo_unauthorizedCodes_map401() throws Exception {
        MockHttpServletResponse r1 = new MockHttpServletResponse();
        ResponseUtils.writeErrorInfo(r1, ResultCode.TOKEN_INVALID_OR_EXPIRED);
        assertEquals(401, r1.getStatus(), "AUT00201 应映射 401");

        MockHttpServletResponse r2 = new MockHttpServletResponse();
        ResponseUtils.writeErrorInfo(r2, ResultCode.ACCESS_UNAUTHORIZED);
        assertEquals(401, r2.getStatus(), "AUT00301 应映射 401");
    }

    @Test
    @DisplayName("中文 msg 的字节可按 UTF-8 正确解码(方法手工写 UTF-8 字节)")
    void writeErrorInfo_chineseMessage_decodableAsUtf8() throws Exception {
        // Arrange
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Act
        ResponseUtils.writeErrorInfo(response, ResultCode.TOKEN_ACCESS_EXPIRED);

        // Assert:该方法用 body.getBytes(UTF_8) 直写字节,故按 UTF-8 解码应得到原文
        String decoded = new String(response.getContentAsByteArray(), StandardCharsets.UTF_8);
        assertTrue(decoded.contains(ResultCode.TOKEN_ACCESS_EXPIRED.getMsg()),
                "按 UTF-8 解码后应含原始中文 msg,实际: " + decoded);
        assertTrue(decoded.contains("AUT00210"), "响应体应含错误码,实际: " + decoded);
    }

    @Test
    @DisplayName("Content-Type 应声明 charset=UTF-8(007 FR-016,中文 msg 防乱码)")
    void writeErrorInfo_contentTypeDeclaresUtf8Charset() throws Exception {
        // Arrange
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Act
        ResponseUtils.writeErrorInfo(response, ResultCode.TOKEN_ACCESS_EXPIRED);

        // Assert:RED 判据 —— 现状只 setContentType(APPLICATION_JSON_VALUE) 不含 charset;
        // T012 补 charset 后转 GREEN。
        String contentType = response.getContentType();
        assertNotNull(contentType, "Content-Type 不应为 null");
        assertTrue(contentType.contains("application/json"),
                "应为 application/json,实际: " + contentType);
        assertTrue(contentType.toLowerCase().contains("charset=utf-8"),
                "中文 msg 须声明 charset,否则客户端按默认编码解码乱码;实际: " + contentType);
    }
}
