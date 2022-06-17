package xyz.foolcat.eve.evehelper.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
    * 物品表
    */
@ApiModel(value="物品表")
@Data
public class InvTypesVO implements Serializable {

    @ApiModelProperty(value="物品typeID")
    private Integer typeId;

    @ApiModelProperty(value="物品名")
    private String typeName;

}