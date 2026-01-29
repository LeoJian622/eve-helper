package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.util.List;

/**
 * 市场组信息
 *
 * @author Leojan
 * date 2023-11-03 16:37
 */

@Data
@Tag(name = "市场组信息 200 ok")
public class GroupItemResponse {

    private String description;

    @JsonProperty("market_group_id")
    private Integer marketGroupId;

    private String name;

    @JsonProperty("parent_group_id")
    private Integer parentGroupId;

    private List<Integer> types;
}
