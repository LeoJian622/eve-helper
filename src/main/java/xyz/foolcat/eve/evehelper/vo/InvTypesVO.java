package xyz.foolcat.eve.evehelper.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
    * 物品表
    */
@Schema(title="物品表")
@Data
public class InvTypesVO implements Serializable {

    @Schema(name="物品typeID")
    private Integer typeId;

    @Schema(name="物品名")
    private String typeName;

}