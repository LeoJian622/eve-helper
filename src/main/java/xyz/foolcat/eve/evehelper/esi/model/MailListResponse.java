package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 人物邮件列表
 *
 * @author Leojan
 * date 2023-11-03 13:55
 */

@Data
@Tag(name = "人物邮件列表 200 ok")
public class MailListResponse {

    @JsonProperty("mailing_list_id")
    private Integer mailingListId;

    private String name;
}
