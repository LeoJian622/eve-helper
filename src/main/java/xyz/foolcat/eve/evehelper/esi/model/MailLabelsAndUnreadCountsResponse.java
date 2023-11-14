package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import xyz.foolcat.eve.evehelper.esi.model.sub.Label;

import java.util.List;

/**
 * 标签列表和未读邮件数量
 *
 * @author Leojan
 * date 2023-11-01 15:48
 */

@Data
@Tag(name = "标签列表和未读邮件数量 200 ok")
public class MailLabelsAndUnreadCountsResponse {

    private List<Label> labels;

    @JsonProperty("total_unread_count")
    private Integer totalUnreadCount;
}
