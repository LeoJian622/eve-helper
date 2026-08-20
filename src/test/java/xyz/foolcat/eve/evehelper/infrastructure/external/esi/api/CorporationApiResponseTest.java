package xyz.foolcat.eve.evehelper.infrastructure.external.esi.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.EsiException;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.ResultCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * CorporationApi "含响应头"maxPage 入口单元测试(015/T009,P4 模式复制 T008)。
 *
 * <p>纯离线:用 {@link WebClient.Builder#exchangeFunction} 注入假 {@link ExchangeFunction},
 * 构造注入 {@code new CorporationApi(fakeWebClient, new PageTotalApi())}。</p>
 *
 * <p>验证军团蓝图 maxPage 含头入口返回 {@code maxPage + HttpHeaders}(TTL 来源),
 * 缺 X-Pages 头 → {@link ResultCode#ESI_SERVER_FAILURE}。</p>
 */
@DisplayName("CorporationApi 蓝图 maxPage 含头入口(离线)")
class CorporationApiResponseTest {

    private static WebClient clientReturning(ClientResponse response) {
        ExchangeFunction fake = request -> Mono.just(response);
        return WebClient.builder().baseUrl("http://localhost:1").exchangeFunction(fake).build();
    }

    @Test
    @DisplayName("军团蓝图 maxPage 含头:返回 maxPage + 响应头(X-Pages 与 Cache-Control)")
    void blueprintsMaxPageResponse_maxPageAndHeaders() {
        ClientResponse ok = ClientResponse.create(HttpStatus.OK)
                .header("X-Pages", "6")
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=300")
                .build();
        CorporationApi api = new CorporationApi(clientReturning(ok), new PageTotalApi());

        PageTotalApi.PageCountResult r = api.queryCorporationBlueprintsMaxPageResponse(5, "tranquility", "Bearer x");

        assertThat(r.maxPage()).isEqualTo(6);
        assertThat(r.headers().getFirst("X-Pages")).isEqualTo("6");
        assertThat(r.headers().getFirst(HttpHeaders.CACHE_CONTROL)).contains("max-age=300");
    }

    @Test
    @DisplayName("军团蓝图 maxPage 含头:缺少 X-Pages 头 → 抛 ESI_SERVER_FAILURE")
    void blueprintsMaxPageResponse_missingXPages_throwsServerFailure() {
        ClientResponse ok = ClientResponse.create(HttpStatus.OK).build();
        CorporationApi api = new CorporationApi(clientReturning(ok), new PageTotalApi());

        assertThatThrownBy(() -> api.queryCorporationBlueprintsMaxPageResponse(5, "tranquility", "Bearer x"))
                .isInstanceOf(EsiException.class)
                .isInstanceOfSatisfying(EsiException.class, e ->
                        assertThat(e.getResultCode()).isEqualTo(ResultCode.ESI_SERVER_FAILURE));
    }
}