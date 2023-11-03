package xyz.foolcat.eve.evehelper.esi.model.sub;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 人物排行榜排行信息
 *
 * @author Leojan
 * @date 2023-10-26 16:50
 */

@Data
@Tag(name = "人物排行榜排行信息")
public class LeaderboardCharacterInfo {

    private Integer amount;

    @JsonProperty("character_id")
    private Integer characterId;
}
