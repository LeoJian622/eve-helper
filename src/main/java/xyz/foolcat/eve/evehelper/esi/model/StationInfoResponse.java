package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import xyz.foolcat.eve.evehelper.esi.model.sub.Position;

import java.util.List;

/**
 * 空间站详情
 *
 * @author Leojan
 * date 2023-11-19 17:12
 */

@Data
@Tag(name = "空间站详情 200 ok")
public class StationInfoResponse {

    @JsonProperty("max_dockable_ship_volume")
    private Float maxDockableShipVolume;

    private String name;

    @JsonProperty("office_rental_cost")
    private Float officeRentalCost;

    private Integer owner;

    private Position position;

    @JsonProperty("race_id")
    private Integer raceId;

    @JsonProperty("reprocessing_efficiency")
    private Float reprocessingEfficiency;

    @JsonProperty("reprocessing_stations_take")
    private Float reprocessingStationsTake;

    private List<String> services;

    @JsonProperty("station_id")
    private Integer stationId;

    @JsonProperty("system_id")
    private Integer systemId;

    @JsonProperty("type_id")
    private Integer typeId;
}
