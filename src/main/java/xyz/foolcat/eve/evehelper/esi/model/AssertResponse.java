package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 资产信息响应体
 *
 * @author Leojan
 * @date 2023-09-25 16:05
 */

@Data
@Tag(name = "资产信息响应体 200 ok")
public class AssertResponse {

    @Schema(description = "蓝图拷贝")
    @JsonProperty("is_blueprint_copy")
    private Boolean isBlueprintCopy;

    @Schema(description = "容器")
    @JsonProperty("is_singleton")
    private Boolean isSingleton;

    @Schema(description = "物品ID")
    @JsonProperty("item_id")
    private Long itemId;

    @Schema(description = "位置")
    @JsonProperty("location_flag")
    private String locationFlag;

    @Schema(description = "位置ID")
    @JsonProperty("location_id")
    private Long locationId;

    @Schema(description = "位置类型")
    @JsonProperty("location_type")
    private String locationType;

    @Schema(description = "数量")
    @JsonProperty("quantity")
    private Integer quantity;

    @Schema(description = "物品类型ID")
    @JsonProperty("type_id")
    private Integer typeId;
}
