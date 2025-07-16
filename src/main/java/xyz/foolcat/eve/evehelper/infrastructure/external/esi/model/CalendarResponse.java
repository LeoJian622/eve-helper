package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 日历事件响应体
 *
 * @author Leojan
 * date 2023-09-28 16:04
 */

@Data
@Tag(name = "日历事件响应体 200 ok")
public class CalendarResponse {

    @JsonProperty("event_date")
    private OffsetDateTime eventDate;

    @JsonProperty("event_id")
    private Integer eventId;

    /**
     * declined, not_responded, accepted, tentative
     */
    @JsonProperty("event_response")
    private String eventResponse;

    @JsonProperty("importance")
    private Integer importance;

    @JsonProperty("title")
    private String title;
}
