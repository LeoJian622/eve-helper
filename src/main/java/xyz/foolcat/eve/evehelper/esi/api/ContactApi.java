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
import xyz.foolcat.eve.evehelper.esi.model.ContactLabelResponse;
import xyz.foolcat.eve.evehelper.esi.model.ContactResponse;
import xyz.foolcat.eve.evehelper.esi.model.ErrorResponse;
import xyz.foolcat.eve.evehelper.exception.EsiException;

import java.util.List;

/**
 * ESI 联系人接口
 *
 * @author Leojan
 * @date 2023-10-24 11:53
 */

@Service
@RequiredArgsConstructor
@Tag(name = "ESI 联系人接口")
public class ContactApi {

    private final WebClient apiClient;

    /**
     * 联盟联系人
     *
     * @param allianceId    联盟ID
     * @param datasource    服务器数据源
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "allianceId", description = "联盟ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-联盟联系人")
    public Flux<ContactResponse> queryAlliancesContacts(Long allianceId, String datasource, String accessesToken) {
        return apiClient.get().uri("/alliances/{alliance_id}/contacts/?datasource={datasource}", allianceId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(ContactResponse.class);
    }

    /**
     * 联盟联系人自定义标签
     *
     * @param allianceId    联盟ID
     * @param datasource    服务器数据源
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "allianceId", description = "联盟ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-联盟联系人自定义标签")
    public Flux<ContactLabelResponse> queryAlliancesContactsLabel(Long allianceId, String datasource, String accessesToken) {
        return apiClient.get().uri("/alliances/{alliance_id}/contacts/labels/?datasource={datasource}", allianceId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(ContactLabelResponse.class);
    }

    /**
     * 人物删除联系人
     *
     * @param characterId   人物ID
     * @param datasource    服务器数据源
     * @param contactIds    联系人IDs
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "contactIds", description = "联系人IDs", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-人物删除人物联系人")
    public Mono<Object> deleteCharactersContacts(Long characterId, String datasource, List<Long> contactIds, String accessesToken) {
        return apiClient.delete().uri("/characters/{character_id}/contacts/?datasource={datasource}&contact_ids={contact_ids}", characterId, datasource, contactIds.toArray())
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(Object.class);
    }

    /**
     * 人物联系人
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
    @Operation(summary = "ESI-人物获取联系人")
    public Flux<ContactResponse> queryCharactersContacts(Long characterId, String datasource, String accessesToken) {
        return apiClient.get().uri("/characters/{character_id}/contacts/?datasource={datasource}", characterId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(ContactResponse.class);
    }

    /**
     * 人物添加联系人
     *
     * @param characterId   人物ID
     * @param datasource    服务器数据源
     * @param contactIds    联系人IDs
     * @param labelIds      标签IDs
     * @param standing      声望
     * @param watched       是否通知联系人
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "contactIds", description = "联系人IDs", required = true),
            @Parameter(name = "labelIds", description = "标签IDs", required = true),
            @Parameter(name = "standing", description = "声望", required = true),
            @Parameter(name = "watched", description = "是否通知联系人", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-人物添加联系人")
    public Flux<Long> addCharactersContacts(Long characterId, String datasource, List<Long> contactIds, List<Long> labelIds, Integer standing, boolean watched, String accessesToken) {
        return apiClient.post().uri("/characters/{character_id}/contacts/?datasource={datasource}&label_ids={label_ids}&standing={standing}&watched={watched}", characterId, datasource, labelIds.toArray(), standing, watched)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .body(Mono.just(contactIds), List.class)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(Long.class);
    }

    /**
     * 人物更新联系人
     *
     * @param characterId   人物ID
     * @param datasource    服务器数据源
     * @param contactIds    联系人IDs
     * @param labelIds      标签IDs
     * @param standing      声望
     * @param watched       是否通知联系人
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "contactIds", description = "联系人IDs", required = true),
            @Parameter(name = "labelIds", description = "标签IDs", required = true),
            @Parameter(name = "standing", description = "声望", required = true),
            @Parameter(name = "watched", description = "是否通知联系人", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-人物更新联系人")
    public Mono<Object> updateCharactersContacts(Long characterId, String datasource, List<Long> contactIds, List<Long> labelIds, Integer standing, boolean watched, String accessesToken) {
        return apiClient.put().uri("/characters/{character_id}/contacts/?datasource={datasource}&label_ids={label_ids}&standing={standing}&watched={watched}", characterId, datasource, labelIds.toArray(), standing, watched)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .body(Mono.just(contactIds), List.class)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(Object.class);
    }

    /**
     * 人物联系人自定义标签
     *
     * @param characterId    联盟ID
     * @param datasource    服务器数据源
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-人物联系人自定义标签")
    public Flux<ContactLabelResponse> queryCharactersContactsLabel(Long characterId, String datasource, String accessesToken) {
        return apiClient.get().uri("/characters/{character_id}/contacts/labels/?datasource={datasource}", characterId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(ContactLabelResponse.class);
    }

    /**
     * 军团联系人
     *
     * @param corporationId    军团ID
     * @param datasource    服务器数据源
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "corporationId", description = "军团ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-军团联系人")
    public Flux<ContactResponse> queryCorporationsContacts(Long corporationId, String datasource, String accessesToken) {
        return apiClient.get().uri("/corporations/{corporation_id}/contacts/?datasource={datasource}", corporationId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(ContactResponse.class);
    }

    /**
     * 军团联系人自定义标签
     *
     * @param corporationId    军团ID
     * @param datasource    服务器数据源
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "corporationId", description = "军团ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-军团联系人自定义标签")
    public Flux<ContactLabelResponse> queryCorporationsContactsLabel(Long corporationId, String datasource, String accessesToken) {
        return apiClient.get().uri("/corporations/{corporation_id}/contacts/labels/?datasource={datasource}", corporationId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(ContactLabelResponse.class);
    }
}
