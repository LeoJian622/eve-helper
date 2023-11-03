package xyz.foolcat.eve.evehelper.esi.model.sub;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 中队信息
 *
 * @author Leojan
 * @date 2023-10-30 11:11
 */

@Data
@Tag(name = "中队信息")
public class Squad {

    private Long id;

    private String name;
}
