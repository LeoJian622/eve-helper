package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 工业产线信息
 *
 * @author Leojan
 * @date 2023-10-30 16:08
 */

@Data
@Tag(name = "工业产线信息 200 ok")
public class IndustryJobPlacedResponse {

    @JsonProperty("activity_id")
    private Integer activityId;

    @JsonProperty("blueprint_id")
    private Long blueprintId;

    @JsonProperty("blueprint_location_id")
    private Long blueprintLocationId;

    @JsonProperty("blueprint_type_id")
    private Integer blueprintTypeId;

    @JsonProperty("completed_character_id")
    private Integer completedCharacterId;

    @JsonProperty("completed_date")
    private OffsetDateTime completedDate;

    private Double cost;

    private Integer duration;

    @JsonProperty("end_date")
    private OffsetDateTime endDate;

    @JsonProperty("facility_id")
    private Long facilityId;

    @JsonProperty("installer_id")
    private Integer installerId;

    @JsonProperty("job_id")
    private Integer jobId;

    @JsonProperty("licensed_runs")
    private Integer licensedRuns;

    @JsonProperty("output_location_id")
    private Long outputLocationId;

    @JsonProperty("pause_date")
    private OffsetDateTime pauseDate;

    private Float probability;

    @JsonProperty("product_type_id")
    private Integer productTypeId;

    private Integer runs;

    @JsonProperty("start_date")
    private OffsetDateTime startDate;

    @JsonProperty("station_id")
    private Long stationId;

    private String status;

    @JsonProperty("successful_runs")
    private Integer successfulRuns;
}
