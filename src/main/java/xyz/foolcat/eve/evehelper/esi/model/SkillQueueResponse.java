package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 训练中的技能信息
 *
 * @author Leojan
 * date 2023-11-14 11:24
 */

@Data
@Tag(name = "训练中的技能信息 200 ok")
public class SkillQueueResponse {

    @JsonProperty("finish_date")
    private OffsetDateTime finishDate;

    @JsonProperty("finished_level")
    private Integer finishedLevel;

    @JsonProperty("level_end_sp")
    private Integer levelEndSp;

    @JsonProperty("level_start_sp")
    private Integer levelStartSp;

    @JsonProperty("queue_position")
    private Integer queuePosition;

    @JsonProperty("skill_id")
    private Integer skillId;

    @JsonProperty("start_date")
    private OffsetDateTime startDate;

    @JsonProperty("training_start_sp")
    private Integer trainingStartSp;
}
