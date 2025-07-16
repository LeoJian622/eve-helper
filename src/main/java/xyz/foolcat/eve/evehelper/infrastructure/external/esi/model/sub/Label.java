package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.sub;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 邮件标签
 *
 * @author Leojan
 * date 2023-11-01 15:49
 */

@Data
@Tag(name = "邮件标签")
public class Label {

    private String color;

    @JsonProperty("label_id")
    private Integer labelId;

    private String name;

    @JsonProperty("unread_count")
    private Integer unreadCount;
}
