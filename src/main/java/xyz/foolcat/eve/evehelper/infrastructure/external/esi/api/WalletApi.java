package xyz.foolcat.eve.evehelper.infrastructure.external.esi.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.EsiClientConstants;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.EsiStatusUtil;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.CorporationWalletsResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.WalletJournalResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.WalletTransactionsResponse;

import java.util.List;

/**
 * ESI 钱包接口
 *
 * @author Leojan
 * date 2023-11-16 21:45
 */

@Slf4j
@Service
@RequiredArgsConstructor
@Tag(name = "ESI 主权相关接口")
public class WalletApi {

    private final WebClient esiClient;

    private final PageTotalApi pageTotalApi;



    /**
     * 人物钱包记录最大页数
     *
     * @param characterId   人物ID
     * @param datasource    服务器
     * @param accessesToken 授权Token
     * @return 最大页数
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-人物钱包记录最大页数")
    public Integer queryCharacterWalletJournalMaxPage(Integer characterId, String datasource, String accessesToken) {
        String uri = "/characters/" + characterId + "/wallet/journal/?datasource=" + datasource + "&page=1";
        return pageTotalApi.queryMaxPage(accessesToken, uri,  esiClient);
    }

    /**
     * 人物钱包记录最大页数(含响应头,015/T011 maxPage TTL 来源通路)。
     *
     * <p>调 {@link PageTotalApi#queryMaxPageWithHeaders} 同时取最大页数与响应头。
     * 原 {@link #queryCharacterWalletJournalMaxPage} 保留、零破坏。</p>
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-人物钱包记录最大页数(含响应头)")
    public PageTotalApi.PageCountResult queryCharacterWalletJournalMaxPageResponse(Integer characterId, String datasource, String accessesToken) {
        String uri = "/characters/" + characterId + "/wallet/journal/?datasource=" + datasource + "&page=1";
        return pageTotalApi.queryMaxPageWithHeaders(accessesToken, uri, esiClient);
    }

    /**
     * 人物钱包余额
     *
     * @param characterId   人物ID
     * @param datasource    服务器数据源
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-人物钱包余额")
    public Mono<Double> queryCharacterWallet(Integer characterId, String datasource, String accessesToken) {
        return esiClient.get().uri("/characters/{character_id}/wallet/?datasource={datasource}", characterId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, EsiStatusUtil.dataError())
                .onStatus(HttpStatusCode::is5xxServerError, EsiStatusUtil.dataError())
                .bodyToMono(Double.class);
    }


    /**
     * 人物钱包记录
     *
     * @param characterId   人物ID
     * @param datasource    服务器数据源
     * @param page          页码
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "page", description = "页码", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-人物钱包记录")
    public Flux<WalletJournalResponse> queryCharacterWalletJournal(Integer characterId, String datasource, Integer page, String accessesToken) {
        return esiClient.get().uri("/characters/{character_id}/wallet/journal/?datasource={datasource}&page={page}", characterId, datasource, page)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, EsiStatusUtil.dataError())
                .onStatus(HttpStatusCode::is5xxServerError, EsiStatusUtil.dataError())
                .bodyToFlux(WalletJournalResponse.class);
    }

    /**
     * 人物钱包记录(含响应头,015/T011 复制 T008 模式)。
     *
     * <p>用 {@code exchangeToMono} + {@code toEntity(ParameterizedTypeReference)} 一次取出
     * 含响应头(如 {@code Cache-Control: max-age})的 {@link ResponseEntity},
     * 供包裹层解析缓存 TTL。4xx/5xx 分支复刻 {@link PageTotalApi#queryMaxPage} 先例。</p>
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "page", description = "页码", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-人物钱包记录(含响应头)")
    public Mono<ResponseEntity<List<WalletJournalResponse>>> queryCharacterWalletJournalResponse(Integer characterId, String datasource, Integer page, String accessesToken) {
        return esiClient.get().uri("/characters/{character_id}/wallet/journal/?datasource={datasource}&page={page}", characterId, datasource, page)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .exchangeToMono(response -> EsiStatusUtil.handleResponseWithHeaders(
                        response, new ParameterizedTypeReference<List<WalletJournalResponse>>() {}, "ESI 人物钱包记录")).timeout(EsiClientConstants.ESI_DATA_TIMEOUT);
    }

    /**
     * 人物钱包市场交易记录最大页数
     *
     * @param characterId   人物ID
     * @param datasource    服务器
     * @param accessesToken 授权Token
     * @return 最大页数
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-人物钱包市场交易记录最大页数")
    public Integer queryCharacterWalletTransactionsMaxPage(Integer characterId, String datasource, String accessesToken) {
        String uri = "/characters/" + characterId + "/wallet/transactions/?datasource=" + datasource + "&page=1";
        return pageTotalApi.queryMaxPage(accessesToken, uri,  esiClient);
    }

    /**
     * 人物钱包市场交易记录
     *
     * @param characterId   人物ID
     * @param datasource    服务器数据源
     * @param fromId        在该ID之前的记录
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "fromId", description = "在该ID之前的记录"),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-人物钱包市场交易记录")
    public Flux<WalletTransactionsResponse> queryCharacterWalletTransactions(Integer characterId, String datasource, Long fromId, String accessesToken) {
        String uri = fromId != null
                ? "/characters/{character_id}/wallet/transactions/?datasource={datasource}&from_id={fromId}"
                : "/characters/{character_id}/wallet/transactions/?datasource={datasource}";
        Object[] vars = fromId != null
                ? new Object[]{characterId, datasource, fromId}
                : new Object[]{characterId, datasource};
        return esiClient.get().uri(uri, vars)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, EsiStatusUtil.dataError())
                .onStatus(HttpStatusCode::is5xxServerError, EsiStatusUtil.dataError())
                .bodyToFlux(WalletTransactionsResponse.class);
    }

    /**
     * 人物钱包市场交易记录(含响应头,015/T011 复制 T008 模式)。
     *
     * <p>uri 与 {@link #queryCharacterWalletTransactions} 一致:from_id 是否非 null 决定
     * 是否携带 {@code from_id} 查询段(游标分页)。4xx/5xx 分支复刻 {@link PageTotalApi#queryMaxPage} 先例。</p>
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "fromId", description = "在该ID之前的记录"),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-人物钱包市场交易记录(含响应头)")
    public Mono<ResponseEntity<List<WalletTransactionsResponse>>> queryCharacterWalletTransactionsResponse(Integer characterId, String datasource, Long fromId, String accessesToken) {
        String uri = fromId != null
                ? "/characters/{character_id}/wallet/transactions/?datasource={datasource}&from_id={fromId}"
                : "/characters/{character_id}/wallet/transactions/?datasource={datasource}";
        Object[] vars = fromId != null
                ? new Object[]{characterId, datasource, fromId}
                : new Object[]{characterId, datasource};
        return esiClient.get().uri(uri, vars)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .exchangeToMono(response -> EsiStatusUtil.handleResponseWithHeaders(
                        response, new ParameterizedTypeReference<List<WalletTransactionsResponse>>() {}, "ESI 人物钱包交易")).timeout(EsiClientConstants.ESI_DATA_TIMEOUT);
    }


    /**
     * 军团钱包余额
     *
     * @param corporationId   军团ID
     * @param datasource    服务器数据源
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "corporationId", description = "军团ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-军团钱包余额")
    public Flux<CorporationWalletsResponse> queryCorporationWallet(Integer corporationId, String datasource, String accessesToken) {
        return esiClient.get().uri("/corporations/{corporation_id}/wallets/?datasource={datasource}", corporationId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, EsiStatusUtil.dataError())
                .onStatus(HttpStatusCode::is5xxServerError, EsiStatusUtil.dataError())
                .bodyToFlux(CorporationWalletsResponse.class);
    }
    /**
     * 军团钱包记录最大页数
     *
     * @param corporationId 军团ID
     * @param division      部门ID
     * @param datasource    服务器
     * @param accessesToken 授权Token
     * @return 最大页数
     */
    @Parameters({
            @Parameter(name = "corporationId", description = "军团ID", required = true),
            @Parameter(name = "division", description = "部门ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-军团钱包记录最大页数")
    public Integer queryCorporationWalletJournalMaxPage(Integer corporationId, Integer division, String datasource, String accessesToken) {
        String uri = "/corporations/" + corporationId + "/wallets/" + division + "/journal/?datasource=" + datasource + "&page=1";
        return pageTotalApi.queryMaxPage(accessesToken, uri,  esiClient);
    }

    /**
     * 军团钱包记录最大页数(含响应头,015/T011 maxPage TTL 来源通路,division 进 uri)。
     *
     * <p>调 {@link PageTotalApi#queryMaxPageWithHeaders} 同时取最大页数与响应头。
     * 原 {@link #queryCorporationWalletJournalMaxPage} 保留、零破坏。</p>
     */
    @Parameters({
            @Parameter(name = "corporationId", description = "军团ID", required = true),
            @Parameter(name = "division", description = "部门ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-军团钱包记录最大页数(含响应头)")
    public PageTotalApi.PageCountResult queryCorporationWalletJournalMaxPageResponse(Integer corporationId, Integer division, String datasource, String accessesToken) {
        String uri = "/corporations/" + corporationId + "/wallets/" + division + "/journal/?datasource=" + datasource + "&page=1";
        return pageTotalApi.queryMaxPageWithHeaders(accessesToken, uri, esiClient);
    }
    /**
     * 军团钱包记录
     *
     * @param corporationId   军团ID
     * @param datasource    服务器数据源
     * @param page          页码
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "corporationId", description = "军团ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "page", description = "页码", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-军团钱包记录")
    public Flux<WalletJournalResponse> queryCorporationWalletJournal(Integer corporationId, Integer division, String datasource, Integer page, String accessesToken) {
        return esiClient.get().uri("/corporations/{corporation_id}/wallets/{division}/journal/?datasource={datasource}&page={page}", corporationId, division, datasource, page)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, EsiStatusUtil.dataError())
                .onStatus(HttpStatusCode::is5xxServerError, EsiStatusUtil.dataError())
                .bodyToFlux(WalletJournalResponse.class);
    }

    /**
     * 军团钱包记录(含响应头,015/T011 复制 T008 模式,division 进 uri)。
     *
     * <p>同 {@link #queryCharacterWalletJournalResponse}:一次取出含响应头 {@link ResponseEntity},
     * 供包裹层解析缓存 TTL。4xx/5xx 分支复刻 {@link PageTotalApi#queryMaxPage} 先例。</p>
     */
    @Parameters({
            @Parameter(name = "corporationId", description = "军团ID", required = true),
            @Parameter(name = "division", description = "部门ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "page", description = "页码", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-军团钱包记录(含响应头)")
    public Mono<ResponseEntity<List<WalletJournalResponse>>> queryCorporationWalletJournalResponse(Integer corporationId, Integer division, String datasource, Integer page, String accessesToken) {
        return esiClient.get().uri("/corporations/{corporation_id}/wallets/{division}/journal/?datasource={datasource}&page={page}", corporationId, division, datasource, page)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .exchangeToMono(response -> EsiStatusUtil.handleResponseWithHeaders(
                        response, new ParameterizedTypeReference<List<WalletJournalResponse>>() {}, "xxx")).timeout(EsiClientConstants.ESI_DATA_TIMEOUT);
    }

    /**
     * 军团钱包交易记录
     *
     * @param corporationId   军团ID
     * @param datasource    服务器数据源
     * @param fromId        在该ID之前的记录
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "corporationId", description = "军团ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "fromId", description = "在该ID之前的记录"),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-军团钱包交易记录")
    public Flux<WalletTransactionsResponse> queryCorporationWalletTransactions(Integer corporationId, Integer division, String datasource, Long fromId, String accessesToken) {
        String uri = fromId != null
                ? "/corporations/{corporation_id}/wallets/{division}/transactions/?datasource={datasource}&from_id={fromId}"
                : "/corporations/{corporation_id}/wallets/{division}/transactions/?datasource={datasource}";
        Object[] vars = fromId != null
                ? new Object[]{corporationId, division, datasource, fromId}
                : new Object[]{corporationId, division, datasource};
        return esiClient.get().uri(uri, vars)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, EsiStatusUtil.dataError())
                .onStatus(HttpStatusCode::is5xxServerError, EsiStatusUtil.dataError())
                .bodyToFlux(WalletTransactionsResponse.class);
    }

    /**
     * 军团钱包交易记录(含响应头,015/T011 复制 T008 模式,division 与 from_id 双形态)。
     *
     * <p>uri 与 {@link #queryCorporationWalletTransactions} 一致:含 division 段,
     * from_id 是否非 null 决定是否携带 {@code from_id} 查询段(游标分页)。</p>
     */
    @Parameters({
            @Parameter(name = "corporationId", description = "军团ID", required = true),
            @Parameter(name = "division", description = "部门ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "fromId", description = "在该ID之前的记录"),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-军团钱包交易记录(含响应头)")
    public Mono<ResponseEntity<List<WalletTransactionsResponse>>> queryCorporationWalletTransactionsResponse(Integer corporationId, Integer division, String datasource, Long fromId, String accessesToken) {
        String uri = fromId != null
                ? "/corporations/{corporation_id}/wallets/{division}/transactions/?datasource={datasource}&from_id={fromId}"
                : "/corporations/{corporation_id}/wallets/{division}/transactions/?datasource={datasource}";
        Object[] vars = fromId != null
                ? new Object[]{corporationId, division, datasource, fromId}
                : new Object[]{corporationId, division, datasource};
        return esiClient.get().uri(uri, vars)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .exchangeToMono(response -> EsiStatusUtil.handleResponseWithHeaders(
                        response, new ParameterizedTypeReference<List<WalletTransactionsResponse>>() {}, "ESI 军团钱包交易")).timeout(EsiClientConstants.ESI_DATA_TIMEOUT);
    }


}
