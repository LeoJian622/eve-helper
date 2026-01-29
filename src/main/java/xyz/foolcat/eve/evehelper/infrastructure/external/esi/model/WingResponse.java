package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.sub.Squad;

import java.util.List;

/**
 * 联队信息
 *
 * @author Leojan
 * date 2023-10-30 11:13
 */

@Data
@Tag(name = "联队信息响应体 200 is ok")
public class WingResponse {

    private Long id;

    private String name;

    private List<Squad> squads;
}
