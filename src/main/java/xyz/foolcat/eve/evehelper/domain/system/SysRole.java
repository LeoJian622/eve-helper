package xyz.foolcat.eve.evehelper.domain.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.foolcat.eve.evehelper.common.base.BaseEntity;

import java.io.Serializable;

/**
 * 角色表
 * @author Leojan
 */
@EqualsAndHashCode(callSuper = true)
@Schema(description = "角色表")
@Data
@TableName(value = "sys_role")
public class SysRole extends BaseEntity implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "")
    private Long id;

    /**
     * 角色名称
     */
    @TableField(value = "`name`")
    @Schema(description = "角色名称")
    private String name;

    /**
     * 角色编码
     */
    @TableField(value = "code")
    @Schema(description = "角色编码")
    private String code;

    /**
     * 显示顺序
     */
    @TableField(value = "sort")
    @Schema(description = "显示顺序")
    private Integer sort;

    /**
     * 角色状态：0-正常；1-停用
     */
    @TableField(value = "`status`")
    @Schema(description = "角色状态：0-正常；1-停用")
    private Boolean status;

    /**
     * 逻辑删除标识：0-未删除；1-已删除
     */
    @TableField(value = "deleted")
    @Schema(description = "逻辑删除标识：0-未删除；1-已删除")
    private Boolean deleted;

    private static final long serialVersionUID = 1L;

    public static final String COL_ID = "id";

    public static final String COL_NAME = "name";

    public static final String COL_CODE = "code";

    public static final String COL_SORT = "sort";

    public static final String COL_STATUS = "status";

    public static final String COL_DELETED = "deleted";

    public static final String COL_GMT_CREATE = "gmt_create";

    public static final String COL_GMT_MODIFIED = "gmt_modified";
}