package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 *
 * 人物职位列表
 *
 * @author Leojan
 * @date 2023-10-20 16:46
 */

@Data
@Tag(name = "人物职位列表响应体 200 is ok")
public class TitleResponse {

    private String name;

    @JsonProperty("title_id")
    private Integer titleId;
}
