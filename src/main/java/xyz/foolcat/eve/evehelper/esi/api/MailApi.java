package xyz.foolcat.eve.evehelper.esi.api;

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
import xyz.foolcat.eve.evehelper.common.result.ResultCode;
import xyz.foolcat.eve.evehelper.esi.model.*;
import xyz.foolcat.eve.evehelper.esi.model.send.NewLabel;
import xyz.foolcat.eve.evehelper.esi.model.send.NewMail;
import xyz.foolcat.eve.evehelper.exception.EsiException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ESI 邮件接口
 *
 * @author Leojan
 * date 2023-10-31 16:52
 */

@Service
@RequiredArgsConstructor
@Tag(name = "ESI 邮件接口")
public class MailApi {

    private final WebClient apiClient;

    /**
     * 返回属于符合查询条件的人物的50个最新邮件标题。查询可按标签过滤，last_mail_id 可用于向后分页
     *
     * @param characterId   人物ID
     * @param datasource    服务器数据源
     * @param labels        标签ID
     * @param lastMailId    最新的邮件ID
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "labels", description = "标签ID", required = true),
            @Parameter(name = "lastMailId", description = "最新的邮件ID", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-符合查询条件的50个最新邮件标题")
    public Flux<RequestedMailResponse> queryCharacterMails(Integer characterId, String datasource, List<Integer> labels, Integer lastMailId, String accessesToken) {
        return apiClient.get().uri("/characters/{character_id}/mail/?datasource={datasource}&labels={labels}&last_mail_id={last_mail_id}", characterId, datasource, labels, lastMailId)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(RequestedMailResponse.class);
    }

    /**
     * 发送新邮件
     *
     * @param characterId   人物ID
     * @param datasource    服务器数据源
     * @param newMail       邮件信息
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "newMail", description = "邮件信息", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-发送新邮件")
    public Mono<Integer> addCharacterMail(Integer characterId, String datasource, NewMail newMail, String accessesToken) {
        return apiClient.post().uri("/characters/{character_id}/mail/?datasource={datasource}", characterId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .body(Mono.just(newMail), NewMail.class)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(Integer.class);
    }


    /**
     * 删除邮件
     *
     * @param characterId   人物ID
     * @param datasource    服务器数据源
     * @param mailId        邮件ID
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "mailId", description = "邮件ID", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-删除邮件")
    public Mono<Object> deleteCharacterMail(Integer characterId, String datasource, Integer mailId, String accessesToken) {
        return apiClient.delete().uri("/characters/{character_id}/mail/{mail_id}/?datasource={datasource}", characterId, mailId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(Object.class);
    }


    /**
     * 邮件详情
     *
     * @param characterId   人物ID
     * @param datasource    服务器数据源
     * @param mailId        邮件ID
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "mailId", description = "邮件ID", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-邮件详情")
    public Mono<MailResponse> queryCharacterMail(Integer characterId, String datasource, Integer mailId, String accessesToken) {
        return apiClient.get().uri("/characters/{character_id}/mail/{mail_id}/?datasource={datasource}", characterId, mailId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(MailResponse.class);
    }

    /**
     * 修改邮件标签和阅读状态
     *
     * @param characterId   人物ID
     * @param datasource    服务器数据源
     * @param mailId        邮件ID
     * @param labelIds      标签ID列表
     * @param read          阅读状态
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "mailId", description = "邮件ID", required = true),
            @Parameter(name = "labelIds", description = "标签ID列表", required = true),
            @Parameter(name = "read", description = "阅读状态", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-邮件详情")
    public Mono<MailResponse> updateCharacterMail(Integer characterId, String datasource, Integer mailId, List<Integer> labelIds, Boolean read, String accessesToken) {
        Map<String, Object> contents = new HashMap<>(2);
        contents.put("labels", labelIds);
        contents.put("read", read);
        return apiClient.put().uri("/characters/{character_id}/mail/{mail_id}/?datasource={datasource}", characterId, mailId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .body(Mono.just(contents), Map.class)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(MailResponse.class);
    }

    /**
     * 邮件标签
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
    @Operation(summary = "ESI-邮件标签")
    public Mono<MailLabelsAndUnreadCountsResponse> queryCharacterMailLabels(Integer characterId, String datasource, String accessesToken) {
        return apiClient.get().uri("/characters/{character_id}/mail/labels/?datasource={datasource}", characterId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(MailLabelsAndUnreadCountsResponse.class);
    }

    /**
     * 新增邮件标签
     *
     * @param characterId   人物ID
     * @param datasource    服务器数据源
     * @param newLabel       标签信息
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "newLabel", description = "标签信息", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-新增邮件标签")
    public Mono<Integer> addCharacterMailLabels(Integer characterId, String datasource, NewLabel newLabel, String accessesToken) {
        return apiClient.post().uri("/characters/{character_id}/mail/labels/?datasource={datasource}", characterId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .body(Mono.just(newLabel), NewMail.class)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(Integer.class);
    }

    /**
     * 删除邮件标签
     *
     * @param characterId   人物ID
     * @param datasource    服务器数据源
     * @param labelId       标签ID
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "labelId", description = "标签ID", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-删除邮件标签")
    public Mono<Integer> deleteCharacterMailLabel(Integer characterId, String datasource, Integer labelId, String accessesToken) {
        return apiClient.delete().uri("/characters/{character_id}/mail/labels/{label_id}/?datasource={datasource}", characterId, labelId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(Integer.class);
    }

    /**
     * 查询人物邮件列表
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
    @Operation(summary = "ESI-查询人物邮件列表")
    public Flux<MailListResponse> queryCharacterMailList(Integer characterId, String datasource, String accessesToken) {
        return apiClient.get().uri("/characters/{character_id}/mail/lists/?datasource={datasource}", characterId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(MailListResponse.class);
    }

}
