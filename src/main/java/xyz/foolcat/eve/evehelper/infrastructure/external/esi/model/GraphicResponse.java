package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 图标详情
 *
 * @author Leojan
 * date 2023-11-19 15:17
 */

@Data
@Tag(name = "图标详情 200 ok")
public class GraphicResponse {

    @JsonProperty("collision_file")
    private String collisionFile;

    @JsonProperty("graphic_file")
    private String graphicFile;

    @JsonProperty("graphic_id")
    private Integer graphicId;

    @JsonProperty("icon_folder")
    private String iconFolder;

    @JsonProperty("sof_dna")
    private String sofDna;

    @JsonProperty("sof_fation_name")
    private String sofFationName;

    @JsonProperty("sof_hull_name")
    private String sofHullName;

    @JsonProperty("sof_race_name")
    private String sofRaceName;
}
