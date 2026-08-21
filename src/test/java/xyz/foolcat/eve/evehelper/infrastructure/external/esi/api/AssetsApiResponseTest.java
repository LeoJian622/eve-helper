package xyz.foolcat.eve.evehelper.infrastructure.external.esi.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.EsiException;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.ResultCode;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.AssetResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AssetsApi "含响应头"入口单元测试(015/T006,P4 试点)。
 *
 * <p>纯离线:不依赖真实 ESI/Redis/MySQL,用 {@link WebClient.Builder#exchangeFunction} 注入
 * 假的 {@link ExchangeFunction} 返回预构造 {@link ClientResponse}(含模拟响应头 + JSON body),
 * 构造注入 {@code new AssetsApi(fakeWebClient, new PageTotalApi())}。</p>
 *
 * <p>验证:</p>
 * <ul>
 *     <li>{@code queryCharactersAssetsResponse / queryCorporationsAssetsResponse} 返回
 *         {@link ResponseEntity<List<AssetResponse>>},能取到 {@code Cache-Control: max-age} 响应头
 *         (T008 缓存 TTL 依赖),body 正确反序列化为 {@code List<AssetResponse>}(非 LinkedHashMap)。</li>
 *     <li>403 → {@link ResultCode#ESI_AUTH_PERMISSION_LOW};5xx → {@link ResultCode#ESI_SERVER_FAILURE}
 *         (复刻 {@link PageTotalApi#queryMaxPage} 分支)。</li>
 *     <li>原 {@code Flux} 方法不回归:仍返回 {@link Flux}<AssetResponse>。</li>
 * </ul>
 */
@DisplayName("AssetsApi 含响应头列表入口(离线)")
class AssetsApiResponseTest {

    private static final String ASSET_BODY = "[{\"item_id\":1,\"type_id\":2,\"quantity\":3}]";

    /**
     * 构造一个对任意请求都返回给定 ClientResponse 的假 WebClient。
     */
    private static WebClient clientReturning(ClientResponse response) {
        ExchangeFunction fake = request -> Mono.just(response);
        return WebClient.builder().baseUrl("http://localhost:1").exchangeFunction(fake).build();
    }

    private static ClientResponse okWithAssets() {
        return ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(ASSET_BODY)
                .build();
    }

    @Test
    @DisplayName("人物资产:取到 max-age 响应头且 body 反序列化为 List<AssetResponse>")
    void charactersResponse_headerAndBodyRoundTrip() {
        AssetsApi api = new AssetsApi(clientReturning(okWithAssets()), new PageTotalApi());

        ResponseEntity<List<AssetResponse>> resp = api
                .queryCharactersAssetsResponse(1, "tranquility", 1, "Bearer x")
                .block();

        assertThat(resp).isNotNull();
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        // 响应头 TTL 可取到(T008 缓存依赖)
        assertThat(resp.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL)).contains("max-age=3600");
        // body 反序列化为领域类型,而非 LinkedHashMap
        assertThat(resp.getBody()).isInstanceOf(List.class).hasSize(1);
        AssetResponse item = resp.getBody().get(0);
        assertThat(item).isInstanceOf(AssetResponse.class);
        assertThat(item.getItemId()).isEqualTo(1L);
        assertThat(item.getTypeId()).isEqualTo(2);
        assertThat(item.getQuantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("军团资产:取到 max-age 响应头且 body 反序列化为 List<AssetResponse>")
    void corporationsResponse_headerAndBodyRoundTrip() {
        AssetsApi api = new AssetsApi(clientReturning(okWithAssets()), new PageTotalApi());

        ResponseEntity<List<AssetResponse>> resp = api
                .queryCorporationsAssetsResponse(5, "tranquility", 1, "Bearer x")
                .block();

        assertThat(resp).isNotNull();
        assertThat(resp.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL)).contains("max-age=3600");
        assertThat(resp.getBody()).hasSize(1);
        assertThat(resp.getBody().get(0).getItemId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("403 → 抛 ESI_AUTH_PERMISSION_LOW(复刻 PageTotalApi 分支)")
    void charactersResponse_403_mapsPermissionLow() {
        ClientResponse forbidden = ClientResponse.create(HttpStatus.FORBIDDEN).build();
        AssetsApi api = new AssetsApi(clientReturning(forbidden), new PageTotalApi());

        assertThatThrownBy(() -> api
                .queryCharactersAssetsResponse(1, "tranquility", 1, "Bearer x")
                .block())
                .isInstanceOf(EsiException.class)
                .isInstanceOfSatisfying(EsiException.class, e ->
                        assertThat(e.getResultCode()).isEqualTo(ResultCode.ESI_AUTH_PERMISSION_LOW));
    }

    @Test
    @DisplayName("5xx(500) → 抛 ESI_SERVER_FAILURE(复刻 PageTotalApi 分支)")
    void charactersResponse_500_mapsServerFailure() {
        ClientResponse serverError = ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).build();
        AssetsApi api = new AssetsApi(clientReturning(serverError), new PageTotalApi());

        assertThatThrownBy(() -> api
                .queryCharactersAssetsResponse(1, "tranquility", 1, "Bearer x")
                .block())
                .isInstanceOf(EsiException.class)
                .isInstanceOfSatisfying(EsiException.class, e ->
                        assertThat(e.getResultCode()).isEqualTo(ResultCode.ESI_SERVER_FAILURE));
    }

    @Test
    @DisplayName("非 403 的其它 4xx(401) → 抛 ESI_AUTHORIZATION_FAILURE(区分 403-vs-其它4xx)")
    void charactersResponse_other4xx_mapsAuthorizationFailure() {
        ClientResponse unauthorized = ClientResponse.create(HttpStatus.UNAUTHORIZED).build();
        AssetsApi api = new AssetsApi(clientReturning(unauthorized), new PageTotalApi());

        assertThatThrownBy(() -> api
                .queryCharactersAssetsResponse(1, "tranquility", 1, "Bearer x")
                .block())
                .isInstanceOf(EsiException.class)
                .isInstanceOfSatisfying(EsiException.class, e ->
                        assertThat(e.getResultCode()).isEqualTo(ResultCode.ESI_AUTHORIZATION_FAILURE));
    }

    @Test
    @DisplayName("人物资产 maxPage 含头:返回 maxPage + 响应头(X-Pages 与 Cache-Control)")
    void charactersMaxPageResponse_headerAndMaxPage() {
        ClientResponse ok = ClientResponse.create(HttpStatus.OK)
                .header("X-Pages", "5")
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=300")
                .build();
        AssetsApi api = new AssetsApi(clientReturning(ok), new PageTotalApi());

        PageTotalApi.PageCountResult r = api.queryCharactersAssetsMaxPageResponse(1, "tranquility", "Bearer x");

        assertThat(r.maxPage()).isEqualTo(5);
        assertThat(r.headers().getFirst("X-Pages")).isEqualTo("5");
        // 缓存 TTL 来源:Cache-Control 头必须透出给包裹层
        assertThat(r.headers().getFirst(HttpHeaders.CACHE_CONTROL)).contains("max-age=300");
    }

    @Test
    @DisplayName("人物资产 maxPage 含头:缺少 X-Pages 头 → 抛 ESI_SERVER_FAILURE")
    void charactersMaxPageResponse_missingXPages_throwsServerFailure() {
        ClientResponse ok = ClientResponse.create(HttpStatus.OK).build();
        AssetsApi api = new AssetsApi(clientReturning(ok), new PageTotalApi());

        assertThatThrownBy(() -> api.queryCharactersAssetsMaxPageResponse(1, "tranquility", "Bearer x"))
                .isInstanceOf(EsiException.class)
                .isInstanceOfSatisfying(EsiException.class, e ->
                        assertThat(e.getResultCode()).isEqualTo(ResultCode.ESI_SERVER_FAILURE));
    }

    @Test
    @DisplayName("原 Flux 方法不回归:仍解析为 Flux<AssetResponse>")
    void originalFluxMethods_unaffected() {
        AssetsApi api = new AssetsApi(clientReturning(okWithAssets()), new PageTotalApi());

        List<AssetResponse> chars = api
                .queryCharactersAssets(1, "tranquility", 1, "Bearer x")
                .collectList()
                .block();
        assertThat(chars).hasSize(1);
        assertThat(chars.get(0).getItemId()).isEqualTo(1L);

        List<AssetResponse> corps = api
                .queryCorporationsAssets(5, "tranquility", 1, "Bearer x")
                .collectList()
                .block();
        assertThat(corps).hasSize(1);
        assertThat(corps.get(0).getItemId()).isEqualTo(1L);
    }
}
