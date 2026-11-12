package xyz.foolcat.eve.evehelper.infrastructure.external.esi.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.EsiClientConstants;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.EsiStatusUtil;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.CurrentShipResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.LocationResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.OnlineStatusResponse;

/**
 * ESI 地点位置
 *
 * @author Leojan
 * date 2023-10-31 15:21
 */

@Service
@RequiredArgsConstructor
@Tag(name = "ESI 地点位置")
public class LocationApi {

    private final WebClient esiClient;

    /**
     * 人物当前位置的相关信息
     *
     * @param characterId 人物ID
     * @param datasource  服务器数据源
     * @param accessesToken  授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-人物当前位置的相关信息")
    public Mono<LocationResponse> queryCharacterLocation(Integer characterId, String datasource, String accessesToken) {
        return esiClient.get().uri("/characters/{character_id}/location/?datasource={datasource}", characterId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, EsiStatusUtil.dataError())
                .onStatus(HttpStatusCode::is5xxServerError, EsiStatusUtil.dataError())
                .bodyToMono(LocationResponse.class);
    }

    /**
     * 人物在线状态
     *
     * @param characterId 人物ID
     * @param datasource  服务器数据源
     * @param accessesToken  授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-人物在线状态")
    public Mono<OnlineStatusResponse> queryCharacterOnline(Integer characterId, String datasource, String accessesToken) {
        return esiClient.get().uri("/characters/{character_id}/online/?datasource={datasource}", characterId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, EsiStatusUtil.dataError())
                .onStatus(HttpStatusCode::is5xxServerError, EsiStatusUtil.dataError())
                .bodyToMono(OnlineStatusResponse.class);
    }

    /**
     * 人物在线状态(含响应头,供缓存 TTL 提取)。
     * <p>022/T009 新增:与 {@link #queryCharacterOnline} 同一 URI,改用 exchangeToMono 保留响应头。
     *
     * @param characterId   人物ID
     * @param datasource    服务器数据源
     * @param accessesToken 授权Token
     * @return 含响应头的 ResponseEntity
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-人物在线状态(含响应头)")
    public Mono<ResponseEntity<OnlineStatusResponse>> queryCharacterOnlineResponse(Integer characterId, String datasource, String accessesToken) {
        return esiClient.get().uri("/characters/{character_id}/online/?datasource={datasource}", characterId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .exchangeToMono(response -> EsiStatusUtil.handleResponseWithHeaders(
                        response, new ParameterizedTypeReference<OnlineStatusResponse>() {}, "ESI 人物在线状态"))
                .timeout(EsiClientConstants.ESI_DATA_TIMEOUT);
    }

    /**
     * 人物当前所在舰船信息
     *
     * @param characterId 人物ID
     * @param datasource  服务器数据源
     * @param accessesToken  授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-人物当前所在舰船信息")
    public Mono<CurrentShipResponse> queryCharacterShip(Integer characterId, String datasource, String accessesToken) {
        return esiClient.get().uri("/characters/{character_id}/ship/?datasource={datasource}", characterId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, EsiStatusUtil.dataError())
                .onStatus(HttpStatusCode::is5xxServerError, EsiStatusUtil.dataError())
                .bodyToMono(CurrentShipResponse.class);
    }
}
