package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 添加到联系人通知
 *
 * @author Leojan
 * @date 2023-10-20 15:34
 */

@Data
@Tag(name="添加到联系人通知响应体 200 is ok")
public class NotificationContactResponse {

    private String message;

    @JsonProperty("notification_id")
    private Integer notificationId;

    @JsonProperty("send_date")
    private OffsetDateTime sendDate;

    @JsonProperty("sender_character_id")
    private Integer senderCharacterId;

    @JsonProperty("standing_level")
    private Float standingLevel;
}
