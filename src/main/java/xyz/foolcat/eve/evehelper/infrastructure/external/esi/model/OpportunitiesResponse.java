package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 已完成任务信息
 *
 * @author Leojan
 * date 2023-11-07 9:54
 */

@Data
@Tag(name = "已完成任务信息 200 ok")
public class OpportunitiesResponse {

    @JsonProperty("completed_at")
    private OffsetDateTime completedAt;

    @JsonProperty("task_id")
    private Integer taskId;
}
