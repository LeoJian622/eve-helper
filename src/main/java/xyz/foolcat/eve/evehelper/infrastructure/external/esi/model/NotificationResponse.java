package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 *
 * 角色通知消息
 *
 * @author Leojan
 * date 2023-10-20 15:03
 */

@Data
@Tag(name = "角色通知消息响应体 200 ok")
public class NotificationResponse {

    @JsonProperty("is_read")
    private Boolean isRead;

    @JsonProperty("notification_id")
    private Long notificationId;

    @JsonProperty("sender_id")
    private Integer senderId;

    @JsonProperty("sender_type")
    private String senderType;

    private String text;

    private OffsetDateTime timestamp;

    private String type;
}
