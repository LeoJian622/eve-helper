package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 *
 * 角色的代理研究信息列表响应体
 *
 * @author Leojan
 * date 2023-10-19 11:03
 */
@Data
@Tag(name = "角色的代理研究信息列表响应体 200 ok")
public class AgentsResearchResponse {

    @JsonProperty("agent_id")
    private Integer agentId;

    @JsonProperty("points_per_day")
    private Float pointsPerDay;

    @JsonProperty("remainder_points")
    private Float remainderPoints;

    @JsonProperty("skill_type_id")
    private Integer skillTypeId;

    @JsonProperty("started_at")
    private OffsetDateTime startedAt;
}
