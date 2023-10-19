package xyz.foolcat.eve.evehelper.esi.model;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 *
 * 联盟图标响应体
 *
 * @author Leojan
 * @date 2023-09-22 17:26
 */

@Data
@Tag(name = "联盟图标响应体 200 ok")
public class AlliancesIconResponse {

    private String px128x128;

    private String px64x64;
}
