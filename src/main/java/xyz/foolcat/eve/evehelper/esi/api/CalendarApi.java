package xyz.foolcat.eve.evehelper.esi.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.foolcat.eve.evehelper.common.result.ResultCode;
import xyz.foolcat.eve.evehelper.esi.enums.CalendarEventEnum;
import xyz.foolcat.eve.evehelper.esi.model.CalendarEventAttendeesResponse;
import xyz.foolcat.eve.evehelper.esi.model.CalendarEventResponse;
import xyz.foolcat.eve.evehelper.esi.model.CalendarResponse;
import xyz.foolcat.eve.evehelper.esi.model.ErrorResponse;
import xyz.foolcat.eve.evehelper.exception.EsiException;

import java.util.HashMap;
import java.util.Map;

/**
 * ESI 日历事件相关接口
 *
 * @author Leojan
 * date 2023-09-28 16:17
 */

@Service
@RequiredArgsConstructor
@Tag(name = "ESI 日历事件相关接口")
public class CalendarApi {

    private final WebClient apiClient;

    /**
     * 人物日历事件
     *
     * @param characterId   人物ID
     * @param datasource    服务器
     * @param fromEventId   事件码
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "fromEventId", description = "事件码", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-日历事件列表")
    public Flux<CalendarResponse> queryCharactersCalendar(Integer characterId, String datasource, Integer fromEventId, String accessesToken) {
        return apiClient.get().uri("/characters/{character_id}/calendar/?datasource={datasource}&from_event={fromEventId}", characterId, datasource, fromEventId)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(CalendarResponse.class);
    }

    /**
     * 日程事件详情
     *
     * @param characterId   人物ID
     * @param datasource    服务器
     * @param eventId       事件码
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "eventId", description = "事件ID", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-事件详情")
    public Mono<CalendarEventResponse> queryCharactersCalendarEventId(Integer characterId, String datasource, Integer eventId, String accessesToken) {
        return apiClient.get().uri("/characters/{character_id}/calendar/{event_id}/?datasource={datasource}", characterId, eventId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(CalendarEventResponse.class);
    }


    /**
     * 回复事件确认
     *
     * @param characterId       人物ID
     * @param datasource        服务器
     * @param eventId           事件码
     * @param calendarEventEnum 回复值
     * @param accessesToken     授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "eventId", description = "事件ID", required = true),
            @Parameter(name = "calendarEvetEnum", description = "回复值", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-回复事件")
    public Mono<Object> updateCharactersCalendarEventId(Integer characterId, String datasource, Integer eventId, CalendarEventEnum calendarEventEnum, String accessesToken) {
        Map<String, String> responseReply = new HashMap<>();
        responseReply.put("response", calendarEventEnum.getValue());
        return apiClient.put().uri("/characters/{character_id}/calendar/{event_id}/?datasource={datasource}", characterId, eventId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(responseReply), Map.class)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(Object.class)
                ;
    }

    /**
     * 查询受邀请人的回复情况
     *
     * @param characterId   人物ID
     * @param datasource    服务器
     * @param eventId       事件码
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "eventId", description = "事件ID", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-事件参与列表")
    public Flux<CalendarEventAttendeesResponse> queryCharactersCalendarEventIdAttendees(Integer characterId, String datasource, Integer eventId, String accessesToken) {
        return apiClient.get().uri("/characters/{character_id}/calendar/{event_id}/attendees/?datasource={datasource}", characterId, eventId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(CalendarEventAttendeesResponse.class);
    }

}
