package xyz.foolcat.eve.evehelper.esi.model.sub;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 部门名称
 *
 * @author Leojan
 * @date 2023-10-25 16:01
 */

@Data
@Tag(name = "部门信息数据")
public class Division {

    private Integer division;

    private String name;
}
