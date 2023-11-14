package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 机遇任务详情
 *
 * @author Leojan
 * date 2023-11-07 10:57
 */

@Data
@Tag(name = "机遇任务详情 200 ok")
public class OpportunitiesTaskResponse {

    private String description;

    private String name;

    private String notification;

    @JsonProperty("task_id")
    private Integer taskId;
}
