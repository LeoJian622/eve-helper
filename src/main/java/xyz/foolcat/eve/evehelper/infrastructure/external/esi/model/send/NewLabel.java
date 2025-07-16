package xyz.foolcat.eve.evehelper.esi.model.send;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 邮件标签
 *
 * @author Leojan
 * date 2023-11-01 15:49
 */

@Data
@Tag(name = "邮件标签")
public class NewLabel {

    private String color = "#ffffff";

    private String name;

}
