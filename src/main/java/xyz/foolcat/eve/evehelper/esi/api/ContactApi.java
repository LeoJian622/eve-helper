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
 * date 2023-10-24 11:53
 */

@Service
@RequiredArgsConstructor
@Tag(name = "ESI 联系人接口")
public class ContactApi {

    private final WebClient apiClient;

    private final PageTotalApi pageTotalApi;

    /**
     * 联盟联系人最大页数
     *
     * @param allianceId    联盟ID
     * @param datasource    服务器数据源
     * @param accessesToken 授权Token
     * @return 最大页数
     */
    @Parameters({
            @Parameter(name = "allianceId", description = "联盟ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-联盟联系人最大页数")
    public Integer queryAlliancesContactsMaxPage(Long allianceId, String datasource, String accessesToken) {
        String uri = "/alliances/" + allianceId + "/contacts/?datasource=" + datasource + "&page=1";
        return pageTotalApi.queryMaxPage(accessesToken, uri, apiClient);
    }

    /**
     * 联盟联系人
     *
     * @param allianceId    联盟ID
     * @param datasource    服务器数据源
     * @param accessesToken 授权Token
     * @return 联盟联系人列表
     */
    @Parameters({
            @Parameter(name = "allianceId", description = "联盟ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "page", description = "页码", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-联盟联系人")
    public Flux<ContactResponse> queryAlliancesContacts(Long allianceId, String datasource, Integer page, String accessesToken) {
        return apiClient.get().uri("/alliances/{alliance_id}/contacts/?datasource={datasource}&page={page}", allianceId, datasource, page)
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
     * @return 联盟自定义标签列表
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
     * @return 返回信息
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "contactIds", description = "联系人IDs", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-人物删除人物联系人")
    public Mono<Object> deleteCharactersContacts(Integer characterId, String datasource, List<Long> contactIds, String accessesToken) {
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
     * 人物联系人最大页数
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
    @Operation(summary = "ESI-人物联系人最大页数")
    public Integer queryCharactersContactsMaxPage(Integer characterId, String datasource, String accessesToken) {
        String uri = "/characters/" + characterId + "/contacts/?datasource=" + datasource + "&page=1";
        return pageTotalApi.queryMaxPage(accessesToken, uri,  apiClient);
    }

    /**
     * 人物联系人
     *
     * @param characterId   人物ID
     * @param datasource    服务器数据源
     * @param accessesToken 授权Token
     * @return 联系人信息响应体
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-人物获取联系人")
    public Flux<ContactResponse> queryCharactersContacts(Integer characterId, String datasource, String accessesToken) {
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
     * @return ID
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
    public Flux<Long> addCharactersContacts(Integer characterId, String datasource, List<Long> contactIds, List<Long> labelIds, Integer standing, boolean watched, String accessesToken) {
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
     * @return 返回信息
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
    public Mono<Object> updateCharactersContacts(Integer characterId, String datasource, List<Long> contactIds, List<Long> labelIds, Integer standing, boolean watched, String accessesToken) {
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
     * @param characterId   联盟ID
     * @param datasource    服务器数据源
     * @param accessesToken 授权Token
     * @return 自定义标签列表
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-人物联系人自定义标签")
    public Flux<ContactLabelResponse> queryCharactersContactsLabel(Integer characterId, String datasource, String accessesToken) {
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
     * 军团联系人最大页数
     *
     * @param corporationId 军团ID
     * @param datasource    服务器
     * @param accessesToken 授权Token
     * @return 最大页数
     */
    @Parameters({
            @Parameter(name = "corporationId", description = "军团ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-军团联系人最大页数")
    public Integer queryCorporationsContactsMaxPage(Integer corporationId, String datasource, String accessesToken) {
        String uri = "/corporations/" + corporationId + "/contacts/?datasource=" + datasource + "&page=1";
        return pageTotalApi.queryMaxPage(accessesToken, uri,  apiClient);
    }


    /**
     * 军团联系人
     *
     * @param corporationId 军团ID
     * @param datasource    服务器数据源
     * @param accessesToken 授权Token
     * @return 军团联系人列表
     */
    @Parameters({
            @Parameter(name = "corporationId", description = "军团ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-军团联系人")
    public Flux<ContactResponse> queryCorporationsContacts(Integer corporationId, String datasource, String accessesToken) {
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
     * @param corporationId 军团ID
     * @param datasource    服务器数据源
     * @param accessesToken 授权Token
     * @return 军团自定义标签列表
     */
    @Parameters({
            @Parameter(name = "corporationId", description = "军团ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-军团联系人自定义标签")
    public Flux<ContactLabelResponse> queryCorporationsContactsLabel(Integer corporationId, String datasource, String accessesToken) {
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
