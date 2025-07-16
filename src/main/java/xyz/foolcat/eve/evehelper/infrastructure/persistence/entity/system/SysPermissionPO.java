package xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.foolcat.eve.evehelper.shared.kernel.base.BaseEntity;

import java.io.Serializable;

/**
 * 权限表
 * @author Leojan
 */
@EqualsAndHashCode(callSuper = true)
@Schema(description = "权限表")
@Data
@TableName(value = "sys_permission")
public class SysPermissionPO extends BaseEntity implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "")
    private Long id;

    /**
     * 权限名称
     */
    @TableField(value = "`name`")
    @Schema(description = "权限名称")
    private String name;

    /**
     * 权限编码
     */
    @TableField(value = "code")
    @Schema(description = "权限编码")
    private String code;

    /**
     * 权限类型：1-菜单 2-按钮
     */
    @TableField(value = "type")
    @Schema(description = "权限类型：1-菜单 2-按钮")
    private Integer type;

    /**
     * 权限描述
     */
    @TableField(value = "description")
    @Schema(description = "权限描述")
    private String description;

    /**
     * 状态：0-禁用 1-开启
     */
    @TableField(value = "status")
    @Schema(description = "状态：0-禁用 1-开启")
    private Boolean status;

    private static final long serialVersionUID = 1L;

    public static final String COL_ID = "id";

    public static final String COL_NAME = "name";

    public static final String COL_CODE = "code";

    public static final String COL_TYPE = "type";

    public static final String COL_DESCRIPTION = "description";

    public static final String COL_STATUS = "status";

    public static final String COL_GMT_CREATE = "gmt_create";

    public static final String COL_GMT_MODIFIED = "gmt_modified";
} 