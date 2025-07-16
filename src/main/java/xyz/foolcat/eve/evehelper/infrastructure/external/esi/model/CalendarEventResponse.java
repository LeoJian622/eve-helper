package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * @author Leojan
 * date 2023-10-18 11:52
 */

@Data
@Tag(name = "日历事件详情响应体 200 ok")
public class CalendarEventResponse {

    private OffsetDateTime date;

    private Integer duration;

    @JsonProperty("event_id")
    private Integer eventId;

    private Integer importance;

    @JsonProperty("owner_id")
    private Integer ownerId;

    @JsonProperty("owner_name")
    private String ownerName;

    /**
     * eve_server, corporation, faction, character, alliance
     */
    @JsonProperty("owner_type")
    private String ownerType;

    private String response;

    private String text;

    private String title;
}
