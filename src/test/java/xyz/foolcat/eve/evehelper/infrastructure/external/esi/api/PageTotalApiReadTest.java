package xyz.foolcat.eve.evehelper.infrastructure.external.esi.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.EsiException;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.ResultCode;

import java.io.IOException;
import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * PageTotalApi.queryMaxPage 403 语义单元测试(013/T008 修复后的验证)。
 *
 * <p>用 JDK 内置 {@link HttpServer} 起本地 HTTP mock(`com.sun.net.httpserver`,无新依赖),
 * 验证修复后的状态分支:</p>
 * <ul>
 *     <li>HTTP 403 → 抛 {@link EsiException}({@link ResultCode#ESI_AUTH_PERMISSION_LOW})</li>
 *     <li>HTTP 200 + X-Pages 头 → 正常返回页数</li>
 * </ul>
 *
 * <p>此前的 bug(4xx 分支未 return、异常被丢弃)→ 403 被当 200 解析致 NPE/500;
 * 本测试锁定回归:403 必须抛 ESI_AUTH_PERMISSION_LOW。</p>
 */
@DisplayName("PageTotalApi.queryMaxPage 403 语义(修复后)")
class PageTotalApiReadTest {

    private static HttpServer server;
    private static String baseUrl;
    private static final PageTotalApi pageTotalApi = new PageTotalApi();

    @BeforeAll
    static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        // /ok → 200 + X-Pages:3
        server.createContext("/ok", PageTotalApiReadTest::handleOk);
        // /forbidden → 403
        server.createContext("/forbidden", PageTotalApiReadTest::handleForbidden);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterAll
    static void stopServer() {
        server.stop(0);
    }

    private static void handleOk(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("X-Pages", "3");
        byte[] body = "[]".getBytes();
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private static void handleForbidden(HttpExchange exchange) throws IOException {
        byte[] body = "{\"error\":\"Forbidden\",\"error_description\":\"character lacks role\"}"
                .getBytes();
        exchange.sendResponseHeaders(403, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    @Test
    @DisplayName("403 → 抛 ESI_AUTH_PERMISSION_LOW(原 bug 会被当 200 解析)")
    void forbidden_throwsPermissionLow() {
        WebClient client = WebClient.create(baseUrl);
        assertThatThrownBy(() -> pageTotalApi.queryMaxPage("Bearer x", "/forbidden", client))
                .isInstanceOf(EsiException.class)
                .satisfies(e -> {
                    EsiException esi = (EsiException) e;
                    assertThat(esi.getResultCode()).isEqualTo(ResultCode.ESI_AUTH_PERMISSION_LOW);
                    // H2:message 用友好文案,不含 ESI 原始 error 串
                    assertThat(esi.getMessage()).doesNotContain("Forbidden");
                });
    }

    @Test
    @DisplayName("200 + X-Pages → 返回页数")
    void ok_returnsPageCount() {
        WebClient client = WebClient.create(baseUrl);
        int pages = pageTotalApi.queryMaxPage("Bearer x", "/ok", client);
        assertThat(pages).isEqualTo(3);
    }
}