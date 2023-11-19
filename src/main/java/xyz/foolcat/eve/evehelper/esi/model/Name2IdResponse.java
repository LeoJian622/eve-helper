package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import xyz.foolcat.eve.evehelper.esi.model.sub.IdName;

import java.util.List;


/**
 * 名称解析结果
 *
 * @author Leojan
 * date 2023-11-19 15:42
 */

@Data
@Tag(name = "名称解析结果 200 ok")
public class Name2IdResponse {

    private List<IdName> agents;

    private List<IdName> alliances;

    private List<IdName> characters;

    private List<IdName> constellations;

    private List<IdName> corporations;

    private List<IdName> factions;

    @JsonProperty("inventory_types")
    private List<IdName> inventoryTypes;

    private List<IdName> regions;

    private List<IdName> stations;

    private List<IdName> systems;

}
