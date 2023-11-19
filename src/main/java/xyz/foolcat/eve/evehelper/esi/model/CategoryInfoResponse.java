package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.util.List;

/**
 * 类别详情
 *
 * @author Leojan
 * date 2023-11-19 14:49
 */

@Data
@Tag(name = "类别详情 200 ok")
public class CategoryInfoResponse {

    @JsonProperty("category_id")
    private Integer categoryId;

    private List<Integer> groups;

    private String name;

    private Boolean published;
}
