package xyz.foolcat.eve.evehelper.esi.model.sub;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 技能训练信息
 *
 * @author Leojan
 * date 2023-11-14 11:49
 */

@Data
@Tag(name = "技能训练信息")
public class Skill {

    @JsonProperty("active_skill_level")
    private Integer activeSkillLevel;

    @JsonProperty("skill_id")
    private Integer skillId;

    @JsonProperty("skillpoints_in_skill")
    private Long skillpointsInSkill;

    @JsonProperty("trained_skill_level")
    private Integer trainedSkillLevel;

}
