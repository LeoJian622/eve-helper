package xyz.foolcat.eve.evehelper.esi.model.sub;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 保险等级及信息
 *
 * @author Leojan
 * date 2023-10-31 11:41
 */

@Data
@Tag(name = "保险等级及信息")
public class Level {

    private Float cost;

    private String name;

    private Float payout;
}
