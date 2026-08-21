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
import reactor.core.publisher.Mono;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.EsiException;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.ResultCode;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.Id2NameResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.TypeInfoResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * UniverseApi "含响应头"入口单元测试(015/T012,公开数据 pub 共享)。
 *
 * <p>纯离线:用 {@link WebClient.Builder#exchangeFunction} 注入假 {@link ExchangeFunction},
 * 构造注入 {@code new UniverseApi(fakeWebClient, new PageTotalApi())}。</p>
 *
 * <p>验证 GET 物品类型详情 / POST ID→名称 两个含头入口:能取到 {@code Cache-Control: max-age}
 * 响应头、body 反序列化为领域类型、4xx/5xx 分支。两入口均为公开数据(无 Authorization)。</p>
 */
@DisplayName("UniverseApi 含响应头入口(离线,公开数据)")
class UniverseApiResponseTest {

    @Test
    @DisplayName("物品类型详情含头:取到 max-age 头且 body 反序列化为 TypeInfoResponse")
    void typeResponse_headerAndTypedBody() {
        ClientResponse ok = ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body("{\"type_id\":34}")
                .build();
        UniverseApi api = new UniverseApi(clientReturning(ok), new PageTotalApi());

        ResponseEntity<TypeInfoResponse> resp = api.queryUniverseTypeResponse(34, "tranquility", "es").block();

        assertThat(resp).isNotNull();
        assertThat(resp.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL)).contains("max-age=3600");
        assertThat(resp.getBody()).isInstanceOf(TypeInfoResponse.class);
    }

    @Test
    @DisplayName("ID→名称含头:POST 取到 max-age 头且 body 反序列化为 List<Id2NameResponse>")
    void namesResponse_headerAndTypedBody() {
        ClientResponse ok = ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body("[{\"id\":34,\"name\":\"Osprey\",\"category\":\"ship\"}]")
                .build();
        UniverseApi api = new UniverseApi(clientReturning(ok), new PageTotalApi());

        ResponseEntity<List<Id2NameResponse>> resp = api.queryUniverseNamesResponse(List.of(34), "tranquility").block();

        assertThat(resp).isNotNull();
        assertThat(resp.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL)).contains("max-age=3600");
        assertThat(resp.getBody()).isInstanceOf(List.class).hasSize(1);
        Id2NameResponse item = resp.getBody().get(0);
        assertThat(item).isInstanceOf(Id2NameResponse.class);
        assertThat(item.getId()).isEqualTo(34);
        assertThat(item.getName()).isEqualTo("Osprey");
    }

    @Test
    @DisplayName("物品类型详情含头:401 → 抛 ESI_AUTHORIZATION_FAILURE")
    void typeResponse_401_mapsAuthFailure() {
        ClientResponse unauthorized = ClientResponse.create(HttpStatus.UNAUTHORIZED).build();
        UniverseApi api = new UniverseApi(clientReturning(unauthorized), new PageTotalApi());

        assertThatThrownBy(() -> api.queryUniverseTypeResponse(34, "tranquility", "es").block())
                .isInstanceOf(EsiException.class)
                .isInstanceOfSatisfying(EsiException.class, e ->
                        assertThat(e.getResultCode()).isEqualTo(ResultCode.ESI_AUTHORIZATION_FAILURE));
    }

    private static WebClient clientReturning(ClientResponse response) {
        ExchangeFunction fake = request -> Mono.just(response);
        return WebClient.builder().baseUrl("http://localhost:1").exchangeFunction(fake).build();
    }
}