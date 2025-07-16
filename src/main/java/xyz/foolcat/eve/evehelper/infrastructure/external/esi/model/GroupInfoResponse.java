package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.util.List;

/**
 * 物品分组详情
 *
 * @author Leojan
 * date 2023-11-19 15:23
 */

@Data
@Tag(name = "物品分组详情 200 ok")
public class GroupInfoResponse {

    @JsonProperty("category_id")
    private Integer categoryId;

    @JsonProperty("group_id")
    private Integer groupId;

    private String name;

    private Boolean published;

    private List<Integer> types;
}
