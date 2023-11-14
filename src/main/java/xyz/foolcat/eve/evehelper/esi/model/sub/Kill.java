package xyz.foolcat.eve.evehelper.esi.model.sub;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 击杀信息
 *
 * @author Leojan
 * date 2023-10-26 16:19
 */

@Data
@Tag(name = "击杀信息")
public class Kill {

    @JsonProperty("last_week")
    private Integer lastWeek;

    private Integer total;

    private Integer yesterday;

}