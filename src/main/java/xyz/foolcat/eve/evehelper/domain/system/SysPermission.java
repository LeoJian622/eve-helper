package xyz.foolcat.eve.evehelper.domain.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.foolcat.eve.evehelper.common.base.BaseEntity;

import java.util.List;

/**
    * 权限表
    */
@Schema(title="权限表")
@Data
@EqualsAndHashCode(callSuper=true)
@TableName(value = "sys_permission")
public class SysPermission extends BaseEntity {

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(name="主键")
    private Long id;

    /**
     * 权限名称
     */
    @TableField(value = "`name`")
    @Schema(name="权限名称")
    private String name;

    /**
     * 菜单模块ID

     */
    @TableField(value = "menu_id")
    @Schema(name="菜单模块ID,")
    private Long menuId;

    /**
     * URL权限标识
     */
    @TableField(value = "url_perm")
    @Schema(name="URL权限标识")
    private String urlPerm;

    /**
     * 按钮权限标识
     */
    @TableField(value = "btn_perm")
    @Schema(name="按钮权限标识")
    private String btnPerm;

    @TableField(exist = false)
    @Schema(name="角色列表")
    private List<String> roles;

    public static final String COL_ID = "id";

    public static final String COL_NAME = "name";

    public static final String COL_MENU_ID = "menu_id";

    public static final String COL_URL_PERM = "url_perm";

    public static final String COL_BTN_PERM = "btn_perm";

    public static final String COL_GMT_CREATE = "gmt_create";

    public static final String COL_GMT_MODIFIED = "gmt_modified";
}