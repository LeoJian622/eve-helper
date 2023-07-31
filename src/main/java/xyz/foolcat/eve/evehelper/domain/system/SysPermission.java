package xyz.foolcat.eve.evehelper.domain.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import xyz.foolcat.eve.evehelper.common.base.BaseEntity;

import java.io.Serializable;
import java.util.List;

/**
 * 权限表
 */
@Schema(description = "权限表")
@Data
@TableName(value = "sys_permission")
public class SysPermission extends BaseEntity implements Serializable {
    public static final String COL_ID = "id";
    public static final String COL_NAME = "name";
    public static final String COL_MENU_ID = "menu_id";
    public static final String COL_URL_PERM = "url_perm";
    public static final String COL_BTN_PERM = "btn_perm";
    public static final String COL_GMT_CREATE = "gmt_create";
    public static final String COL_GMT_MODIFIED = "gmt_modified";
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键")
    private Long id;

    /**
     * 权限名称
     */
    @TableField(value = "`name`")
    @Schema(description = "权限名称")
    private String name;
    @TableField(exist = false)
    @Schema(name = "角色列表")
    private List<String> roles;

    /**
     * 菜单模块ID
     */
    @TableField(value = "menu_id")
    @Schema(description = "菜单模块ID")
    private Long menuId;

    /**
     * URL权限标识
     */
    @TableField(value = "url_perm")
    @Schema(description = "URL权限标识")
    private String urlPerm;

    /**
     * 按钮权限标识
     */
    @TableField(value = "btn_perm")
    @Schema(description = "按钮权限标识")
    private String btnPerm;

}