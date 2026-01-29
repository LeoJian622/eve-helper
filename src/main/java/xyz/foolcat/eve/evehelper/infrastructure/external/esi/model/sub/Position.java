package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.sub;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 *
 * 位置信息
 *
 * @author Leojan
 * date 2023-09-28 9:04
 */

@Data
@Tag(name = "位标信息")
public class Position {

    private Double x;

    private Double y;

    private Double z;
}
