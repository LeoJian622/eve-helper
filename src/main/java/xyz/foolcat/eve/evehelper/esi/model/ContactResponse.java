package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.util.List;

/**
 * 联系人信息响应体
 *
 * @author Leojan
 * @date 2023-10-24 14:04
 */

@Data
@Tag(name = "联系人信息响应体 200 ok")
public class ContactResponse {

    @JsonProperty("contact_id")
    private Integer contactId;

    @JsonProperty("contact_type")
    private String contactType;

    @JsonProperty("label_ids")
    private List<Long> labelIds;

    private Float standing;
}
