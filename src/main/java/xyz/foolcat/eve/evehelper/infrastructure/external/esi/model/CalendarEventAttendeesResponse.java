package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * @author Leojan
 * date 2023-10-18 16:32
 */
@Data
@Tag(name = "日历事件参与者响应体 200 ok")
public class CalendarEventAttendeesResponse {

    @JsonProperty("character_id")
    private Integer characterId;

    @JsonProperty("event_response")
    private String eventResponse;
}
