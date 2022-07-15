package xyz.foolcat.eve.evehelper.domain.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import lombok.Data;

/**
 * universe_name  ids_names
 */
@ApiModel(value = "universe_name  ids_names")
@Data
@TableName(value = "universe_name")
public class UniverseName implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    @ApiModelProperty(value = "")
    private Integer id;

    @TableField(value = "`name`")
    @ApiModelProperty(value = "")
    private String name;

    /**
     * alliance, character, constellation, corporation, inventory_type, region, solar_system, station, faction
     */
    @TableField(value = "category")
    @ApiModelProperty(value = "alliance, character, constellation, corporation, inventory_type, region, solar_system, station, faction")
    private String category;

    private static final long serialVersionUID = 1L;

    public static final String COL_ID = "id";

    public static final String COL_NAME = "name";

    public static final String COL_CATEGORY = "category";
}