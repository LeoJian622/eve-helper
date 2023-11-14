package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import xyz.foolcat.eve.evehelper.esi.model.sub.Allies;
import xyz.foolcat.eve.evehelper.esi.model.sub.Combatants;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 战争详情
 *
 * @author Leojan
 * date 2023-11-14 20:33
 */

@Data
@Tag(name = "战争详情 200 ok")
public class WarDetailsResponse {

    private Combatants aggressor;

    private List<Allies> allies;

    private OffsetDateTime declared;

    private Combatants defender;

    private OffsetDateTime finished;

    private Integer id;

    private Boolean mutual;

    @JsonProperty("open_for_allies")
    private Boolean openForAllies;

    private OffsetDateTime retracted;

    private OffsetDateTime started;
}
