package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.util.List;

/**
 * 搜索结果响应体
 *
 * @author Leojan
 * date 2023-11-09 16:17
 */

@Data
@Tag(name = "搜索结果响应体 200 ok")
public class SearchResponse {

    List<Integer> agent;

    List<Integer> alliance;

    List<Integer> character;

    List<Integer> constellation;

    List<Integer> corporation;

    List<Integer> faction;

    @JsonProperty("inventory_typ")
    List<Integer> inventoryTyp;

    List<Integer> region;

    @JsonProperty("solar_system")
    List<Integer> solarSystem;

    List<Integer> station;

    List<Integer> structure;
}
