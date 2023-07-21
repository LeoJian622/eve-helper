package xyz.foolcat.eve.evehelper.dto.system;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 物品买卖单对象
 *
 * @author Leojan
 * @date 2022-06-09 11:29
 */

@Schema(title="买卖单最高或最低售价")
@Data
public class MarketOrderDTO implements Serializable {

    private static final long serialVersionUID = 4187181620195980282L;

    /**
     * 物品类型ID
     */
    @Schema(name="物品类型ID")
    private Long typeId;

    /**
     * 物品名
     */
    @Schema(name="物品类型ID")
    private String typeName;


    /**
     * 销售价
     */
    @Schema(name="销售价")
    private Long salePrice;

    /**
     * 收购价
     */
    @Schema(name="收购价")
    private Long buyPrice;

}
