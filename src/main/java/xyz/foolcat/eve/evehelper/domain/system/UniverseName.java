package xyz.foolcat.eve.evehelper.domain.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * universe_name  ids_names
 */
@Schema(title="ID名称对照")
@Data
@TableName(value = "universe_name")
public class UniverseName implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(name="")
    private Integer id;

    @TableField(value = "`name`")
    @Schema(name="")
    private String name;

    /**
     * alliance, character, constellation, corporation, inventory_type, region, solar_system, station, faction
     */
    @TableField(value = "category")
    @Schema(name="alliance, character, constellation, corporation, inventory_type, region, solar_system, station, faction")
    private String category;

    private static final long serialVersionUID = 1L;

    public static final String COL_ID = "id";

    public static final String COL_NAME = "name";

    public static final String COL_CATEGORY = "category";
}