package xyz.foolcat.eve.evehelper.esi.model;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import xyz.foolcat.eve.evehelper.esi.model.sub.Division;

/**
 * 军团部门名称
 *
 * @author Leojan
 * date 2023-10-25 16:04
 */

@Data
@Tag(name = "军团部门名称响应体 200 ok")
public class DivisionNamesResponse {

    private Division[] hangar;

    private Division[] wallet;
}
