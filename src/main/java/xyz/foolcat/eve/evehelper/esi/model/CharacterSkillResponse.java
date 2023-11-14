package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import xyz.foolcat.eve.evehelper.esi.model.sub.Skill;

import java.util.List;

/**
 * 人物技能信息
 *
 * @author Leojan
 * date 2023-11-14 11:52
 */

@Data
@Tag(name = "人物技能信息 200 ok")
public class CharacterSkillResponse {

    private List<Skill> skills;

    @JsonProperty("total_sp")
    private Long totalSp;

    @JsonProperty("unallocated_sp")
    private Integer unallocatedSp;
}
