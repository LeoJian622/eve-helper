package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.sub.Attacker;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.sub.Victim;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 击毁报告
 *
 * @author Leojan
 * date 2023-10-31 14:28
 */

@Data
@Tag(name = "击毁报告 200 ok")
public class KillMailResponse {

    private List<Attacker> attackers;

    @JsonProperty("killmail_id")
    private Integer killMailId;

    @JsonProperty("killmail_time")
    private OffsetDateTime killMailTime;

    @JsonProperty("moon_id")
    private Integer moonId;

    @JsonProperty("solar_system_id")
    private Integer solarSystemId;

    private Victim victim;

    @JsonProperty("war_id")
    private Integer warId;

}
