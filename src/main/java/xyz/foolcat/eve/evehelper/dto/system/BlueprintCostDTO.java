package xyz.foolcat.eve.evehelper.dto.system;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 蓝图成本
 *
 * @author Leojan
 * @date 2022-09-09 17:15
 */

@Schema(title="制造成本")
@Data
public class BlueprintCostDTO implements Serializable {

    private static final long serialVersionUID = -1321213174703409278L;
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
    private Long sale;

    /**
     * 收购价
     */
    @Schema(name="收购价")
    private Long buy;


    /**
     * 卖单成本
     */
    @Schema(name="卖单成本")
    private Long costSale;

    /**
     * 收单成本
     */
    @Schema(name="收单成本")
    private Long costBuy;

}
