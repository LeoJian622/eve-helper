package xyz.foolcat.eve.evehelper.esi.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.foolcat.eve.evehelper.common.result.ResultCode;
import xyz.foolcat.eve.evehelper.esi.model.ContractBidsResponse;
import xyz.foolcat.eve.evehelper.esi.model.ContractItemResponse;
import xyz.foolcat.eve.evehelper.esi.model.ContractResponse;
import xyz.foolcat.eve.evehelper.esi.model.ErrorResponse;
import xyz.foolcat.eve.evehelper.exception.EsiException;

/**
 * ESI 合同接口
 *
 * @author Leojan
 * @date 2023-10-24 16:38
 */

@Service
@RequiredArgsConstructor
@Tag(name = "ESI 合同接口")
public class ContractsApi {

    private final WebClient apiClient;

    /**
     * 人物合同记录
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
    @Operation(summary = "ESI-人物合同记录")
    public Flux<ContractResponse> queryCharactersContracts(Long characterId, String datasource, Integer page, String accessesToken) {
        return apiClient.get().uri("/characters/{character_id}/contracts/?datasource={datasource}&page={page}", characterId, datasource, page)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(ContractResponse.class);
    }

    /**
     * 人物合同（拍卖）出价信息
     *
     * @param characterId   人物ID
     * @param datasource    服务器数据源
     * @param contractId    合同ID
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "contractId", description = "合同ID", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-人物合同（拍卖）出价信息")
    public Flux<ContractBidsResponse> queryCharactersContractsBids(Long characterId, String datasource, Integer contractId, String accessesToken) {
        return apiClient.get().uri("/characters/{character_id}/contracts/{contract_id}/bids/?datasource={datasource}", characterId, contractId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(ContractBidsResponse.class);
    }

    /**
     * 人物合同物品清单
     *
     * @param characterId   人物ID
     * @param datasource    服务器数据源
     * @param contractId    合同ID
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "contractId", description = "合同ID", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-人物合同物品清单")
    public Flux<ContractItemResponse> queryCharactersContractsItems(Long characterId, String datasource, Integer contractId, String accessesToken) {
        return apiClient.get().uri("/characters/{character_id}/contracts/{contract_id}/items/?datasource={datasource}", characterId, contractId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(ContractItemResponse.class);
    }

    /**
     * 星域公开合同记录
     *
     * @param regionId      星域ID
     * @param datasource    服务器数据源
     * @param page          页码
     * @return
     */
    @Parameters({
            @Parameter(name = "regionId", description = "星域ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "page", description = "页码", required = true),
    })
    @Operation(summary = "ESI-星域公开合同记录")
    public Flux<ContractResponse> queryPublicContractsRegion(Integer regionId, String datasource, Integer page) {
        return apiClient.get().uri("/contracts/public/{region_id}/?datasource={datasource}&page={page}", regionId, datasource, page)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(ContractResponse.class);
    }

    /**
     * 星域公开合同（拍卖）出价信息
     *
     * @param datasource    服务器数据源
     * @param contractId    合同ID
     * @return
     */
    @Parameters({
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "contractId", description = "合同ID", required = true),
    })
    @Operation(summary = "ESI-星域公开合同（拍卖）出价信息")
    public Flux<ContractBidsResponse> queryPublicContractsBids(String datasource, Integer contractId) {
        return apiClient.get().uri("/contracts/public/bids/{contract_id}/?datasource={datasource}",contractId, datasource)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(ContractBidsResponse.class);
    }

    /**
     * 星域公开合同物品清单
     *
     * @param datasource    服务器数据源
     * @param contractId    合同ID
     * @return
     */
    @Parameters({
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "contractId", description = "合同ID", required = true),
    })
    @Operation(summary = "ESI-星域公开合同物品清单")
    public Flux<ContractItemResponse> queryPublicContractsItems( String datasource, Integer contractId) {
        return apiClient.get().uri("/contracts/public/items/{contract_id}/?datasource={datasource}",  contractId, datasource)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(ContractItemResponse.class);
    }


    /**
     * 军团合同记录R
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
    @Operation(summary = "ESI-军团合同记录")
    public Flux<ContractResponse> queryCorporationsContracts(Long corporationId, String datasource, Integer page, String accessesToken) {
        return apiClient.get().uri("/corporations/{corporation_id}/contracts/?datasource={datasource}&page={page}", corporationId, datasource, page)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(ContractResponse.class);
    }

    /**
     * 军团合同（拍卖）出价信息
     *
     * @param corporationId   军团ID
     * @param datasource    服务器数据源
     * @param contractId    合同ID
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "corporationId", description = "军团ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "contractId", description = "合同ID", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-军团合同（拍卖）出价信息")
    public Flux<ContractBidsResponse> queryCorporationsContractsBids(Long corporationId, String datasource, Integer contractId, String accessesToken) {
        return apiClient.get().uri("/corporations/{corporation_id}/contracts/{contract_id}/bids/?datasource={datasource}", corporationId, contractId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(ContractBidsResponse.class);
    }

    /**
     * 军团合同物品清单
     *
     * @param corporationId   军团ID
     * @param datasource    服务器数据源
     * @param contractId    合同ID
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "corporationId", description = "军团ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "contractId", description = "合同ID", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-军团合同物品清单")
    public Flux<ContractItemResponse> queryCorporationsContractsItems(Long corporationId, String datasource, Integer contractId, String accessesToken) {
        return apiClient.get().uri("/corporations/{corporation_id}/contracts/{contract_id}/items/?datasource={datasource}", corporationId, contractId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(ContractItemResponse.class);
    }
}
