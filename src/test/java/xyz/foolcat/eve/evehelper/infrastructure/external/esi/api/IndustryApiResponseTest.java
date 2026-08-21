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
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.IndustryJobPlacedResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.ObserverMiningLedgerResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * IndustryApi "含响应头"入口单元测试(015/T009,P4 模式复制 T008)。
 *
 * <p>纯离线:不依赖真实 ESI/Redis/MySQL,用 {@link WebClient.Builder#exchangeFunction} 注入
 * 假 {@link ExchangeFunction} 返回预构造 {@link ClientResponse}(含模拟响应头 + JSON body),
 * 构造注入 {@code new IndustryApi(fakeWebClient, new PageTotalApi())}。</p>
 *
 * <p>验证工业类两张列表与 maxPage 含头入口:能取到 {@code Cache-Control: max-age} 响应头
 * (T009 缓存 TTL 依赖)、body 正确反序列化为 {@code List<IndustryJobPlacedResponse>},
 * 4xx/5xx 分支与 T008 一致。</p>
 */
@DisplayName("IndustryApi 含响应头入口(离线)")
class IndustryApiResponseTest {

    private static final String JOB_BODY = "[{\"job_id\":1,\"activity_id\":2}]";

    private static WebClient clientReturning(ClientResponse response) {
        ExchangeFunction fake = request -> Mono.just(response);
        return WebClient.builder().baseUrl("http://localhost:1").exchangeFunction(fake).build();
    }

    private static ClientResponse okWithJobs() {
        return ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(JOB_BODY)
                .build();
    }

    private static ClientResponse okWithMaxPage() {
        return ClientResponse.create(HttpStatus.OK)
                .header("X-Pages", "4")
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=300")
                .build();
    }

    @Test
    @DisplayName("人物工业任务:取到 max-age 响应头且 body 反序列化为 List<IndustryJobPlacedResponse>")
    void characterIndustryJobsResponse_headerAndTypedBody() {
        IndustryApi api = new IndustryApi(clientReturning(okWithJobs()), new PageTotalApi());

        ResponseEntity<List<IndustryJobPlacedResponse>> resp = api
                .queryCharacterIndustryJobsResponse(1, "tranquility", true, "Bearer x")
                .block();

        assertThat(resp).isNotNull();
        assertThat(resp.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL)).contains("max-age=3600");
        assertThat(resp.getBody()).isInstanceOf(List.class).hasSize(1);
        IndustryJobPlacedResponse item = resp.getBody().get(0);
        assertThat(item).isInstanceOf(IndustryJobPlacedResponse.class);
        assertThat(item.getJobId()).isEqualTo(1);
        assertThat(item.getActivityId()).isEqualTo(2);
    }

    @Test
    @DisplayName("军团工业任务:取到 max-age 响应头且 body 反序列化为 List<IndustryJobPlacedResponse>")
    void corporationIndustryJobsResponse_headerAndTypedBody() {
        IndustryApi api = new IndustryApi(clientReturning(okWithJobs()), new PageTotalApi());

        ResponseEntity<List<IndustryJobPlacedResponse>> resp = api
                .queryCorporationIndustryJobsResponse(5, "tranquility", false, "Bearer x")
                .block();

        assertThat(resp).isNotNull();
        assertThat(resp.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL)).contains("max-age=3600");
        assertThat(resp.getBody()).hasSize(1);
        assertThat(resp.getBody().get(0).getJobId()).isEqualTo(1);
    }

    @Test
    @DisplayName("人物工业任务:401 → 抛 ESI_AUTHORIZATION_FAILURE")
    void characterIndustryJobsResponse_401_mapsAuthFailure() {
        ClientResponse unauthorized = ClientResponse.create(HttpStatus.UNAUTHORIZED).build();
        IndustryApi api = new IndustryApi(clientReturning(unauthorized), new PageTotalApi());

        assertThatThrownBy(() -> api
                .queryCharacterIndustryJobsResponse(1, "tranquility", true, "Bearer x")
                .block())
                .isInstanceOf(EsiException.class)
                .isInstanceOfSatisfying(EsiException.class, e ->
                        assertThat(e.getResultCode()).isEqualTo(ResultCode.ESI_AUTHORIZATION_FAILURE));
    }

    @Test
    @DisplayName("军团工业任务 maxPage 含头:返回 maxPage + 响应头(X-Pages 与 Cache-Control)")
    void corporationIndustryJobsMaxPageResponse_maxPageAndHeaders() {
        IndustryApi api = new IndustryApi(clientReturning(okWithMaxPage()), new PageTotalApi());

        PageTotalApi.PageCountResult r = api
                .queryCorporationIndustryJobsMaxPageResponse(5, "tranquility", true, "Bearer x");

        assertThat(r.maxPage()).isEqualTo(4);
        assertThat(r.headers().getFirst("X-Pages")).isEqualTo("4");
        assertThat(r.headers().getFirst(HttpHeaders.CACHE_CONTROL)).contains("max-age=300");
    }

    // ── 采矿观察(observerId 入 uri) ──

    @Test
    @DisplayName("军团单建筑采矿:取到 max-age 响应头且 body 反序列化为 List<ObserverMiningLedgerResponse>")
    void miningObserverResponse_headerAndTypedBody() {
        ClientResponse ok = ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body("[{\"character_id\":1,\"type_id\":2,\"quantity\":3}]")
                .build();
        IndustryApi api = new IndustryApi(clientReturning(ok), new PageTotalApi());

        ResponseEntity<List<ObserverMiningLedgerResponse>> resp = api
                .queryCorporationMiningObserverResponse(5, "tranquility", 9L, 1, "Bearer x")
                .block();

        assertThat(resp).isNotNull();
        assertThat(resp.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL)).contains("max-age=3600");
        assertThat(resp.getBody()).isInstanceOf(List.class).hasSize(1);
        ObserverMiningLedgerResponse item = resp.getBody().get(0);
        assertThat(item).isInstanceOf(ObserverMiningLedgerResponse.class);
        assertThat(item.getCharacterId()).isEqualTo(1);
        assertThat(item.getQuantity()).isEqualTo(3L);
    }

    @Test
    @DisplayName("军团单建筑采矿:401 → 抛 ESI_AUTHORIZATION_FAILURE")
    void miningObserverResponse_401_mapsAuthFailure() {
        ClientResponse unauthorized = ClientResponse.create(HttpStatus.UNAUTHORIZED).build();
        IndustryApi api = new IndustryApi(clientReturning(unauthorized), new PageTotalApi());

        assertThatThrownBy(() -> api
                .queryCorporationMiningObserverResponse(5, "tranquility", 9L, 1, "Bearer x")
                .block())
                .isInstanceOf(EsiException.class)
                .isInstanceOfSatisfying(EsiException.class, e ->
                        assertThat(e.getResultCode()).isEqualTo(ResultCode.ESI_AUTHORIZATION_FAILURE));
    }

    @Test
    @DisplayName("军团单建筑采矿 maxPage 含头:返回 maxPage + 响应头(X-Pages 与 Cache-Control)")
    void miningObserverMaxPageResponse_maxPageAndHeaders() {
        ClientResponse ok = ClientResponse.create(HttpStatus.OK)
                .header("X-Pages", "8")
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=300")
                .build();
        IndustryApi api = new IndustryApi(clientReturning(ok), new PageTotalApi());

        PageTotalApi.PageCountResult r = api
                .queryCorporationMiningObserverMaxPageResponse(5, "tranquility", 9L, "Bearer x");

        assertThat(r.maxPage()).isEqualTo(8);
        assertThat(r.headers().getFirst("X-Pages")).isEqualTo("8");
        assertThat(r.headers().getFirst(HttpHeaders.CACHE_CONTROL)).contains("max-age=300");
    }
}