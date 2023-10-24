package xyz.foolcat.eve.evehelper.esi.model.sub;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.util.List;

/**
 * 克隆体信息
 *
 * @author Leojan
 * @date 2023-10-24 11:25
 */

@Data
@Tag(name = "克隆体信息")
public class JumpClones {

    private List<Integer> implants;

    @JsonProperty("jump_clone_id")
    private Integer jumpCloneId;

    @JsonProperty("location_id")
    private Long locationId;

    @JsonProperty("location_type")
    private String locationType;

    private String name;
}
