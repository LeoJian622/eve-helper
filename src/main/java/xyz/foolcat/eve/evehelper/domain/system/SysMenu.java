package xyz.foolcat.eve.evehelper.domain.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.foolcat.eve.evehelper.common.base.BaseEntity;

/**
    * 菜单管理
    */
@ApiModel(value="菜单管理")
@Data
@EqualsAndHashCode(callSuper=true)
@TableName(value = "sys_menu")
public class SysMenu extends BaseEntity{
    @TableId(value = "id", type = IdType.AUTO)
    @ApiModelProperty(value="")
    private Long id;

    /**
     * 菜单名称
     */
    @TableField(value = "`name`")
    @ApiModelProperty(value="菜单名称")
    private String name;

    /**
     * 父菜单ID
     */
    @TableField(value = "parent_id")
    @ApiModelProperty(value="父菜单ID")
    private Long parentId;

    /**
     * 路由路径
     */
    @TableField(value = "`path`")
    @ApiModelProperty(value="路由路径")
    private String path;

    /**
     * 组件路径
     */
    @TableField(value = "component")
    @ApiModelProperty(value="组件路径")
    private String component;

    /**
     * 菜单图标
     */
    @TableField(value = "icon")
    @ApiModelProperty(value="菜单图标")
    private String icon;

    /**
     * 排序
     */
    @TableField(value = "sort")
    @ApiModelProperty(value="排序")
    private Integer sort;

    /**
     * 状态：0-禁用 1-开启
     */
    @TableField(value = "visible")
    @ApiModelProperty(value="状态：0-禁用 1-开启")
    private Boolean visible;

    /**
     * 跳转路径
     */
    @TableField(value = "redirect")
    @ApiModelProperty(value="跳转路径")
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