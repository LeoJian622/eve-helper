package xyz.foolcat.eve.evehelper.esi.model.sub;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 行星开发采集器信息
 *
 * @author Leojan
 * @date 2023-11-07 14:45
 */

@Data
@Tag(name = "行星开发采集器信息")
public class Head {

    @JsonProperty("head_id")
    private Integer headId;

    private Float latitude;

    private Float longitude;
}
