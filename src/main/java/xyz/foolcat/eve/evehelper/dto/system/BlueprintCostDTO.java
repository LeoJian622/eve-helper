package xyz.foolcat.eve.evehelper.dto.system;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 蓝图成本
 *
 * @author Leojan
 * @date 2022-09-09 17:15
 */

@ApiModel(value = "制造成本")
@Data
public class BlueprintCostDTO implements Serializable {

    private static final long serialVersionUID = -1321213174703409278L;
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
    private Long sale;

    /**
     * 收购价
     */
    @ApiModelProperty(value="收购价")
    private Long buy;


    /**
     * 卖单成本
     */
    @ApiModelProperty(value = "卖单成本")
    private Long costSale;

    /**
     * 收单成本
     */
    @ApiModelProperty(value = "收单成本")
    private Long costBuy;

}
