package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 *
 * 图标响应体
 *
 * @author Leojan
 * date 2023-09-22 17:26
 */

@Data
@Tag(name = "图标响应体 200 ok")
public class IconResponse {

    private String px512x512;

    private String px256x256;

    private String px128x128;

    private String px64x64;

}
