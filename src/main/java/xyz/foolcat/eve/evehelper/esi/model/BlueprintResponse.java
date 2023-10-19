package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 蓝图信息
 *
 * @author Leojan
 * @date 2023-10-19 11:43
 */

@Data
@Tag(name = "蓝图信息响应体 200 ok")
public class BlueprintResponse {

    @JsonProperty("item_id")
    private Long itemId;

    @JsonProperty("location_flag")
    private String locationFlag;

    /**
     * AutoFit, Cargo, CorpseBay, DroneBay, FleetHangar, Deliveries, HiddenModifiers, Hangar, HangarAll, LoSlot0, LoSlot1, LoSlot2, LoSlot3, LoSlot4, LoSlot5, LoSlot6, LoSlot7, MedSlot0, MedSlot1, MedSlot2, MedSlot3, MedSlot4, MedSlot5, MedSlot6, MedSlot7, HiSlot0, HiSlot1, HiSlot2, HiSlot3, HiSlot4, HiSlot5, HiSlot6, HiSlot7, AssetSafety, Locked, Unlocked, Implant, QuafeBay, RigSlot0, RigSlot1, RigSlot2, RigSlot3, RigSlot4, RigSlot5, RigSlot6, RigSlot7, ShipHangar, SpecializedFuelBay, SpecializedOreHold, SpecializedGasHold, SpecializedMineralHold, SpecializedSalvageHold, SpecializedShipHold, SpecializedSmallShipHold, SpecializedMediumShipHold, SpecializedLargeShipHold, SpecializedIndustrialShipHold, SpecializedAmmoHold, SpecializedCommandCenterHold, SpecializedPlanetaryCommoditiesHold, SpecializedMaterialBay, SubSystemSlot0, SubSystemSlot1, SubSystemSlot2, SubSystemSlot3, SubSystemSlot4, SubSystemSlot5, SubSystemSlot6, SubSystemSlot7, FighterBay, FighterTube0, FighterTube1, FighterTube2, FighterTube3, FighterTube4, Module
     */
    @JsonProperty("location_id")
    private Long locationId;

    @JsonProperty("material_efficiency")
    private Integer materialEfficiency;

    private Integer quantity;

    private Integer runs;

    @JsonProperty("time_efficiency")
    private Integer timeEfficiency;

    @JsonProperty("type_id")
    private Integer typeId;
}
