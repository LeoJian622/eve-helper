package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.util.Date;

/**
 * 人物采矿明细记录
 *
 * @author Leojan
 * date 2023-10-30 16:46
 */

@Data
@Tag(name = "人物采矿明细记录 200 ok")
public class MiningLedgerResponse {

    private Date date;

    private Long quantity;

    @JsonProperty("solar_system_id")
    private Integer solarSystemId;

    @JsonProperty("type_id")
    private Integer typeId;
}
