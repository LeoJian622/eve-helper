package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.util.List;

/**
 * 机遇任务组信息
 *
 * @author Leojan
 * @date 2023-11-07 10:13
 */

@Data
@Tag(name = "机遇任务组信息 200 ok")
public class OpportunitiesGroupResponse {

    @JsonProperty("connected_groups")
    private List<Integer> connectedGroups;

    private String description;

    @JsonProperty("group_id")
    private Integer groupId;

    private String name;

    private String notification;

    @JsonProperty("required_tasks")
    private List<Integer> requiredTasks;

}
