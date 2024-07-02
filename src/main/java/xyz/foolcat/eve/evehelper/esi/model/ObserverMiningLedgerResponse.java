package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 单个建筑的采矿明细
 *
 * @author Leojan
 * date 2023-10-31 10:13
 */

@Data
@Tag(name = "单个建筑的采矿明细 200 ok")
public class ObserverMiningLedgerResponse {

    @JsonProperty("character_id")
    private Integer characterId;

    @JsonProperty("last_updated")
    private OffsetDateTime lastUpdated;

    private Long quantity;

    @JsonProperty("recorded_corporation_id")
    private Integer recordedCorporationId;

    @JsonProperty("type_id")
    private Integer typeId;
}
