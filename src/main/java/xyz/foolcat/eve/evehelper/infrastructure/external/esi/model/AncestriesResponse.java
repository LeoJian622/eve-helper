package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 民族信息
 *
 * @author Leojan
 * date 2023-11-18 22:52
 */

@Data
@Tag(name = "民族信息 200 ok")
public class AncestriesResponse {

    @JsonProperty("bloodline_id")
    private Integer bloodlineId;

    private String description;

    @JsonProperty("icon_id")
    private Integer iconId;

    private Integer id;

    private String name;

    @JsonProperty("short_description")
    private String shortDescription;

}
