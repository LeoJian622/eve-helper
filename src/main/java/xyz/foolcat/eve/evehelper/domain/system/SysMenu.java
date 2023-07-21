package xyz.foolcat.eve.evehelper.domain.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.foolcat.eve.evehelper.common.base.BaseEntity;

/**
    * 菜单管理
    */
@Schema(title="菜单管理")
@Data
@EqualsAndHashCode(callSuper=true)
@TableName(value = "sys_menu")
public class SysMenu extends BaseEntity{
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(name="")
    private Long id;

    /**
     * 菜单名称
     */
    @TableField(value = "`name`")
    @Schema(name="菜单名称")
    private String name;

    /**
     * 父菜单ID
     */
    @TableField(value = "parent_id")
    @Schema(name="父菜单ID")
    private Long parentId;

    /**
     * 路由路径
     */
    @TableField(value = "`path`")
    @Schema(name="路由路径")
    private String path;

    /**
     * 组件路径
     */
    @TableField(value = "component")
    @Schema(name="组件路径")
    private String component;

    /**
     * 菜单图标
     */
    @TableField(value = "icon")
    @Schema(name="菜单图标")
    private String icon;

    /**
     * 排序
     */
    @TableField(value = "sort")
    @Schema(name="排序")
    private Integer sort;

    /**
     * 状态：0-禁用 1-开启
     */
    @TableField(value = "visible")
    @Schema(name="状态：0-禁用 1-开启")
    private Boolean visible;

    /**
     * 跳转路径
     */
    @TableField(value = "redirect")
    @Schema(name="跳转路径")
    private String redirect;

    public static final String COL_ID = "id";

    public static final String COL_NAME = "name";

    public static final String COL_PARENT_ID = "parent_id";

    public static final String COL_PATH = "path";

    public static final String COL_COMPONENT = "component";

    public static final String COL_ICON = "icon";

    public static final String COL_SORT = "sort";

    public static final String COL_VISIBLE = "visible";

    public static final String COL_REDIRECT = "redirect";

    public static final String COL_GMT_CREATE = "gmt_create";

    public static final String COL_GMT_MODIFIED = "gmt_modified";
}