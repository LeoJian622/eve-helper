package xyz.foolcat.eve.evehelper.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import xyz.foolcat.eve.evehelper.domain.system.EveCharacter;

import java.io.Serializable;

@ApiModel(value = "资产视图")
@Data
public class AssetsVO implements Serializable {
    /**
     * 物品ID
     */
    @ApiModelProperty(value = "物品ID")
    private Long itemId;

    /**
     * invTypeID
     */
    @ApiModelProperty(value = "invTypeID")
    private Integer typeId;

    /**
     * TypeName
     */
    @ApiModelProperty(value = "TypeName")
    private String typeName;

    /**
     * 建筑ID
     */
    @ApiModelProperty(value = "建筑ID")
    private Long locationId;

    /**
     * 建筑ID
     */
    @ApiModelProperty(value = "建筑名")
    private Long name;

    /**
     * 类型station, solar_system, item, other
     */
    @ApiModelProperty(value = "类型station, solar_system, item, other")
    private String locationType;

    @ApiModelProperty(value = "")
    private String locationFlag;

    @ApiModelProperty(value = "")
    private Boolean isSingleton;

    /**
     * 蓝图拷贝
     */
    @ApiModelProperty(value = "蓝图拷贝")
    private Boolean isBlueprintCopy;

    /**
     * 数量
     */
    @ApiModelProperty(value = "数量")
    private Long quantity;

    /**
     * 所有者
     */
    @ApiModelProperty(value = "所有者")
    private String owner;

    /**
     * 所有者ID
     */
    @ApiModelProperty(value = "所有者ID")
    private String ownerId;

}