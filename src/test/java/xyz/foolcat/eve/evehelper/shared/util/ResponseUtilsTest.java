package xyz.foolcat.eve.evehelper.shared.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import xyz.foolcat.eve.evehelper.shared.result.ResultCode;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ResponseUtils#writeErrorInfo} 现状诊断(007 FR-016 前置调研)。
 *
 * <p>FR-016 将让 {@code JwtAuthorizationTokenFilter} 复用该方法直接写 401 响应。
 * 在此之前须查明两件事,它们决定 FR-016 的设计是否需要额外处理:</p>
 * <ol>
 *   <li><b>状态码映射</b>:{@code TOKEN_ACCESS_EXPIRED}(AUT00210)当前落 switch 的
 *       {@code default} 分支 → 400。FR-016 须新增 401 分支 —— 本测试固化现状,
 *       实现后该断言需同步改为 401(届时它就是 RED→GREEN 的判据)</li>
 *   <li><b>字符编码</b>:该方法只 {@code setContentType},<b>不</b>设
 *       {@code setCharacterEncoding}(对比 {@code AuthenticationFailureServletHandler:79}
 *       设了)。而全部 {@code ResultCode} 的 msg 为中文。本测试查明响应体是否可正确解码,
 *       以及 Content-Type 是否声明 charset</li>
 * </ol>
 *
 * <p>本测试不断言"应该"如何,只固化当前行为 —— 属调研性质,实现 FR-016 时改写。</p>
 *
 * @author Leojan
 * date 2026-08-11
 */
@DisplayName("ResponseUtils 现状诊断(007 FR-016)")
class ResponseUtilsTest {

    @Test
    @DisplayName("AUT00210(TOKEN过期)当前映射的状态码 —— FR-016 须改为 401")
    void writeErrorInfo_tokenAccessExpired_currentStatus() throws Exception {
        // Arrange
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Act
        ResponseUtils.writeErrorInfo(response, ResultCode.TOKEN_ACCESS_EXPIRED);

        // Assert:固化现状 —— 落 default 分支得 400。
        // FR-016 实现后此处应为 401,该断言即 RED→GREEN 判据。
        assertEquals(400, response.getStatus(),
                "现状应为 400(落 switch default);若已是 401 说明 FR-016 已实现,请更新本断言");
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
    @DisplayName("Content-Type 是否声明 charset —— 决定客户端能否正确解码")
    void writeErrorInfo_contentTypeCharsetDeclaration() throws Exception {
        // Arrange
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Act
        ResponseUtils.writeErrorInfo(response, ResultCode.TOKEN_ACCESS_EXPIRED);

        // Assert:固化现状。方法只 setContentType(APPLICATION_JSON_VALUE),不设 charset。
        String contentType = response.getContentType();
        System.out.println("[FR-016] Content-Type      = " + contentType);
        System.out.println("[FR-016] characterEncoding = " + response.getCharacterEncoding());
        System.out.println("[FR-016] 说明: 若 Content-Type 无 charset 且 encoding 非 UTF-8,"
                + "客户端可能按默认编码解码中文 msg 而乱码");

        assertEquals("application/json", contentType,
                "现状应为不含 charset 的 application/json");
    }
}
