package xyz.foolcat.eve.evehelper.dto.system;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 物品买卖单对象
 *
 * @author Leojan
 * @date 2022-06-09 11:29
 */

@ApiModel(value="买卖单最高或最低售价")
@Data
public class MarketOrderDTO {

    /**
     * 物品类型ID
     */
    @ApiModelProperty(value="物品类型ID")
    private Long typeId;

    /**
     * 物品名
     */
    @ApiModelProperty(value="物品类型ID")
    private String typeName;


    /**
     * 销售价
     */
    @ApiModelProperty(value="销售价")
    private Long salePrice;

    /**
     * 收购价
     */
    @ApiModelProperty(value="收购价")
    private Long buyPrice;
}
