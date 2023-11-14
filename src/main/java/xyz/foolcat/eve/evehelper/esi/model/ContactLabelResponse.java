package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 联系人自定义标签响应体
 *
 * @author Leojan
 * date 2023-10-24 14:24
 */

@Data
@Tag(name = "联系人自定义标签响应体 200 ok")
public class ContactLabelResponse {

    @JsonProperty("label_id")
    private Long labelId;

    @JsonProperty("label_name")
    private String labelName;
}
