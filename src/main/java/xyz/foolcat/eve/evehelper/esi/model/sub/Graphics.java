package xyz.foolcat.eve.evehelper.esi.model.sub;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * @author Leojan
 * @date 2023-10-20 14:48
 */

@Data
@Tag(name = "图标数据")
public class Graphics {

    private Integer color;

    private String graphic;

    private Integer layer;

    private Integer part;
}
