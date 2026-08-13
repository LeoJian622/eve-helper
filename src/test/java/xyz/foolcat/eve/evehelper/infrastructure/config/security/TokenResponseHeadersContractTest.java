package xyz.foolcat.eve.evehelper.infrastructure.config.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

/**
 * Token 响应头契约测试(008 T011 / research R6,MEDIUM-1)。
 *
 * <p>钉死活路径响应头:登录成功({@code AuthenticationSuccessServletHandler} 直写)与 refresh 成功
 * (MVC 序列化)均经同一 {@code SecurityFilterChain},由 Spring Security 默认
 * {@code CacheControlHeadersWriter} 提供 {@code Cache-Control: no-store},且无
 * {@code Access-Control-Allow-Origin}。此处以真实过滤链的 permitAll 响应验证框架默认头,
 * 防未来 {@code .headers(...)} 定制静默回退(FR-007)。具体端点头行为由 quickstart §4 手工 curl 复核。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TokenResponseHeadersContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void chainResponseHasNoStoreAndNoAccessControlAllowOrigin() throws Exception {
        // permitAll /v3/api-docs:仍经 SecurityFilterChain,HeaderWriterFilter 写默认头
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(header().string("Cache-Control",
                        org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}