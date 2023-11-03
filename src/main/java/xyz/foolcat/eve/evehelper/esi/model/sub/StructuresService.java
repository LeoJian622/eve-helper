package xyz.foolcat.eve.evehelper.esi.model.sub;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 建筑服务信息
 *
 * @author Leojan
 * @date 2023-10-26 14:09
 */

@Data
@Tag(name = "建筑服务")
public class StructuresService {

    private String name;

    private String state;
}
