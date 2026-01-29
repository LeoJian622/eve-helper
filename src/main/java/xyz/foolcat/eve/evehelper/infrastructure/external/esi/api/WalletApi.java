package xyz.foolcat.eve.evehelper.infrastructure.external.esi.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.CorporationWalletsResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.ErrorResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.WalletJournalResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.WalletTransactionsResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.EsiException;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.ResultCode;

/**
 * ESI 钱包接口
 *
 * @author Leojan
 * date 2023-11-16 21:45
 */

@Service
@RequiredArgsConstructor
@Tag(name = "ESI 主权相关接口")
public class WalletApi {

    private final WebClient apiClient;

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
        return pageTotalApi.queryMaxPage(accessesToken, uri,  apiClient);
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
        return apiClient.get().uri("/characters/{character_id}/wallet/?datasource={datasource}", characterId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
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
        return apiClient.get().uri("/characters/{character_id}/wallet/journal/?datasource={datasource}&page={page}", characterId, datasource, page)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(WalletJournalResponse.class);
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
        return pageTotalApi.queryMaxPage(accessesToken, uri,  apiClient);
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
        return apiClient.get().uri("/characters/{character_id}/wallet/transactions/?datasource={datasource}&from_id={fromId}", characterId, datasource, fromId)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(WalletTransactionsResponse.class);
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
        return apiClient.get().uri("/corporations/{corporation_id}/wallets/?datasource={datasource}", corporationId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
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
        return pageTotalApi.queryMaxPage(accessesToken, uri,  apiClient);
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
        return apiClient.get().uri("/corporations/{corporation_id}/wallets/{division}/journal/?datasource={datasource}&page={page}", corporationId, division, datasource, page)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(WalletJournalResponse.class);
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
        return apiClient.get().uri("/corporations/{corporation_id}/wallets/{division}/transactions/?datasource={datasource}&from_id={fromId}", corporationId, division, datasource, fromId)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(WalletTransactionsResponse.class);
    }


}
