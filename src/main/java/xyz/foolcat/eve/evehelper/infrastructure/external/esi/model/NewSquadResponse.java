package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 创建中队成功返回体
 *
 * @author Leojan
 * date 2023-10-30 20:32
 */

@Data
@Tag(name = "创建中队成功返回体 200 is ok")
public class NewSquadResponse {

    @JsonProperty("squad_id")
    private Long squadId;
}
