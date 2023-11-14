package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 创建联队成功返回体
 *
 * @author Leojan
 * date 2023-10-30 20:11
 */

@Data
@Tag(name = "创建联队成功返回体 200 is ok")
public class NewWingResponse {

    @JsonProperty("wing_id")
    private Long wingId;
}
