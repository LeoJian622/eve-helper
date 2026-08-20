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
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.WalletJournalResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.WalletTransactionsResponse;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * WalletApi "含响应头"入口单元测试(015/T011,P4 复制 T008-T010 模式)。
 *
 * <p>纯离线:用 {@link WebClient.Builder#exchangeFunction} 注入假 {@link ExchangeFunction},
 * 构造注入 {@code new WalletApi(fakeWebClient, new PageTotalApi())}。</p>
 *
 * <p>验证流水两张列表 + 两 maxPage + 交易两列表的含头入口:能取到 {@code Cache-Control: max-age}
 * 响应头、body 正确反序列化为领域类型;交易入口特测 from_id null/非 null 双 uri 形态。</p>
 */
@DisplayName("WalletApi 含响应头入口(离线)")
class WalletApiResponseTest {

    private static final String JOURNAL_BODY = "[{\"ref_type\":\"bounty_prizes\",\"amount\":5}]";
    private static final String TXN_BODY = "[{\"transaction_id\":1,\"amount\":2}]";

    private static WebClient clientReturning(ClientResponse response) {
        ExchangeFunction fake = request -> Mono.just(response);
        return WebClient.builder().baseUrl("http://localhost:1").exchangeFunction(fake).build();
    }

    private static ClientResponse okWith(MediaType type, String body) {
        return ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .header(HttpHeaders.CONTENT_TYPE, type == null ? MediaType.APPLICATION_JSON_VALUE : type.toString())
                .body(body)
                .build();
    }

    @Test
    @DisplayName("人物钱包流水含头:取到 max-age 头且 body 反序列化为 List<WalletJournalResponse>")
    void characterJournalResponse_headerAndTypedBody() {
        WalletApi api = new WalletApi(clientReturning(okWith(MediaType.APPLICATION_JSON, JOURNAL_BODY)), new PageTotalApi());

        ResponseEntity<List<WalletJournalResponse>> resp = api
                .queryCharacterWalletJournalResponse(1, "tranquility", 1, "Bearer x")
                .block();

        assertThat(resp).isNotNull();
        assertThat(resp.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL)).contains("max-age=3600");
        assertThat(resp.getBody()).isInstanceOf(List.class).hasSize(1);
        assertThat(resp.getBody().get(0)).isInstanceOf(WalletJournalResponse.class);
    }

    @Test
    @DisplayName("军团钱包流水含头:取到 max-age 头且 body 反序列化为 List<WalletJournalResponse>")
    void corporationJournalResponse_headerAndTypedBody() {
        WalletApi api = new WalletApi(clientReturning(okWith(MediaType.APPLICATION_JSON, JOURNAL_BODY)), new PageTotalApi());

        ResponseEntity<List<WalletJournalResponse>> resp = api
                .queryCorporationWalletJournalResponse(5, 2, "tranquility", 1, "Bearer x")
                .block();

        assertThat(resp).isNotNull();
        assertThat(resp.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL)).contains("max-age=3600");
        assertThat(resp.getBody()).hasSize(1);
        assertThat(resp.getBody().get(0)).isInstanceOf(WalletJournalResponse.class);
    }

    @Test
    @DisplayName("人物钱包流水 maxPage 含头:返回 maxPage + 响应头")
    void characterJournalMaxPageResponse_maxPageAndHeaders() {
        ClientResponse ok = ClientResponse.create(HttpStatus.OK)
                .header("X-Pages", "9")
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=300")
                .build();
        WalletApi api = new WalletApi(clientReturning(ok), new PageTotalApi());

        PageTotalApi.PageCountResult r = api.queryCharacterWalletJournalMaxPageResponse(1, "tranquility", "Bearer x");

        assertThat(r.maxPage()).isEqualTo(9);
        assertThat(r.headers().getFirst(HttpHeaders.CACHE_CONTROL)).contains("max-age=300");
    }

    @Test
    @DisplayName("军团钱包流水 maxPage 含头:返回 maxPage + 响应头(division 进 uri)")
    void corporationJournalMaxPageResponse_maxPageAndHeaders() {
        ClientResponse ok = ClientResponse.create(HttpStatus.OK)
                .header("X-Pages", "6")
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=300")
                .build();
        WalletApi api = new WalletApi(clientReturning(ok), new PageTotalApi());

        PageTotalApi.PageCountResult r = api.queryCorporationWalletJournalMaxPageResponse(5, 2, "tranquility", "Bearer x");

        assertThat(r.maxPage()).isEqualTo(6);
        assertThat(r.headers().getFirst(HttpHeaders.CACHE_CONTROL)).contains("max-age=300");
    }

    @Test
    @DisplayName("人物交易含头:from_id 非 null 时 uri 携带 from_id")
    void characterTransactionsResponse_withFromId_hasFromIdSegment() {
        AtomicReference<String> capturedUri = new AtomicReference<>();
        ExchangeFunction fake = request -> {
            capturedUri.set(request.url().toString());
            return Mono.just(okWith(MediaType.APPLICATION_JSON, TXN_BODY));
        };
        WebClient wc = WebClient.builder().baseUrl("http://localhost:1").exchangeFunction(fake).build();
        WalletApi api = new WalletApi(wc, new PageTotalApi());

        ResponseEntity<List<WalletTransactionsResponse>> resp = api
                .queryCharacterWalletTransactionsResponse(1, "tranquility", 123L, "Bearer x")
                .block();

        assertThat(capturedUri.get()).contains("from_id=123");
        assertThat(resp.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL)).contains("max-age=3600");
        assertThat(resp.getBody()).hasSize(1);
        assertThat(resp.getBody().get(0)).isInstanceOf(WalletTransactionsResponse.class);
    }

    @Test
    @DisplayName("人物交易含头:from_id 为 null(首页)时 uri 不含 from_id")
    void characterTransactionsResponse_nullFromId_noFromIdSegment() {
        AtomicReference<String> capturedUri = new AtomicReference<>();
        ExchangeFunction fake = request -> {
            capturedUri.set(request.url().toString());
            return Mono.just(okWith(MediaType.APPLICATION_JSON, TXN_BODY));
        };
        WebClient wc = WebClient.builder().baseUrl("http://localhost:1").exchangeFunction(fake).build();
        WalletApi api = new WalletApi(wc, new PageTotalApi());

        ResponseEntity<List<WalletTransactionsResponse>> resp = api
                .queryCharacterWalletTransactionsResponse(1, "tranquility", null, "Bearer x")
                .block();

        assertThat(capturedUri.get()).doesNotContain("from_id");
        assertThat(resp.getBody()).hasSize(1);
    }

    @Test
    @DisplayName("军团交易含头:division 与 from_id 均进 uri")
    void corporationTransactionsResponse_divisionAndFromIdInUri() {
        AtomicReference<String> capturedUri = new AtomicReference<>();
        ExchangeFunction fake = request -> {
            capturedUri.set(request.url().toString());
            return Mono.just(okWith(MediaType.APPLICATION_JSON, TXN_BODY));
        };
        WebClient wc = WebClient.builder().baseUrl("http://localhost:1").exchangeFunction(fake).build();
        WalletApi api = new WalletApi(wc, new PageTotalApi());

        ResponseEntity<List<WalletTransactionsResponse>> resp = api
                .queryCorporationWalletTransactionsResponse(5, 2, "tranquility", 456L, "Bearer x")
                .block();

        assertThat(capturedUri.get()).contains("wallets/2/transactions/").contains("from_id=456");
        assertThat(resp.getBody()).hasSize(1);
        assertThat(resp.getBody().get(0)).isInstanceOf(WalletTransactionsResponse.class);
    }

    @Test
    @DisplayName("人物钱包流水含头:401 → 抛 ESI_AUTHORIZATION_FAILURE")
    void characterJournalResponse_401_mapsAuthFailure() {
        ClientResponse unauthorized = ClientResponse.create(HttpStatus.UNAUTHORIZED).build();
        WalletApi api = new WalletApi(clientReturning(unauthorized), new PageTotalApi());

        assertThatThrownBy(() -> api
                .queryCharacterWalletJournalResponse(1, "tranquility", 1, "Bearer x")
                .block())
                .isInstanceOf(EsiException.class)
                .isInstanceOfSatisfying(EsiException.class, e ->
                        assertThat(e.getResultCode()).isEqualTo(ResultCode.ESI_AUTHORIZATION_FAILURE));
    }
}