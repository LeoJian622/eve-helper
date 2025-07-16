package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * ID解析结果
 *
 * @author Leojan
 * date 2023-11-19 16:21
 */

@Data
@Tag(name = "ID解析结果 200 ok")
public class Id2NameResponse {

    private String category;

    private Integer id;

    private String name;
}
