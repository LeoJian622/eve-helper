package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 合同物品信息
 *
 * @author Leojan
 * date 2023-10-25 11:34
 */

@Data
@Tag(name = "esi合同物品信息响应体 200 ok")
public class ContractItemResponse {

    @JsonProperty("is_included")
    private Boolean isIncluded;

    @JsonProperty("is_singleton")
    private Boolean isSingleton;

    private Integer quantity;

    @JsonProperty("raw_quantity")
    private Integer rawQuantity;

    @JsonProperty("record_id")
    private Long recordId;

    @JsonProperty("type_id")
    private Integer typeId;
}
