package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 联盟公开信息响应体
 * @author Leojan
 * date 2023-09-22 16:27
 */

@Data
@Tag(name = "联盟公开信息响应体 200 ok")
public class AlliancesResponse {

    @Schema(description = "创建军团ID")
    @JsonProperty("creator_corporation_id")
    private Integer creatorCorporationId;

    @Schema(description = "创建者ID")
    @JsonProperty("creator_id")
    private Integer creatorId;

    @Schema(description = "创建时间")
    @JsonProperty("date_founded")
    private OffsetDateTime dateFounded;

    @Schema(description = "执行军团ID")
    @JsonProperty("executor_corporation_id")
    private Integer executorCorporationId;

    @Schema(description = "势力ID")
    @JsonProperty("faction_id")
    private Integer factionId;

    @Schema(description = "联盟名称")
    @JsonProperty("name")
    private String name;

    @Schema(description = "缩写")
    @JsonProperty("ticker")
    private String ticker;
}
