package xyz.foolcat.eve.evehelper.esi.model.sub;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * ID和名称
 *
 * @author Leojan
 * date 2023-11-19 15:40
 */

@Data
@Tag(name = "ID和名称")
public class IdName {

    private Integer id;

    private String name;
}
