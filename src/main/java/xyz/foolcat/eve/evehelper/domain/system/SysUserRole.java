package xyz.foolcat.eve.evehelper.domain.system;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import xyz.foolcat.eve.evehelper.common.base.BaseEntity;

import java.io.Serializable;

/**
 * 用户和角色关联表
 */
@Schema(description = "用户和角色关联表")
@Data
@TableName(value = "sys_user_role")
public class SysUserRole extends BaseEntity implements Serializable {
    /**
     * 用户ID
     */
    @TableField(value = "user_id")
    @Schema(description = "用户ID")
    private Integer userId;

    /**
     * 角色ID
     */
    @TableField(value = "role_id")
    @Schema(description = "角色ID")
    private Integer roleId;

    private static final long serialVersionUID = 1L;

    public static final String COL_USER_ID = "user_id";

    public static final String COL_ROLE_ID = "role_id";

    public static final String COL_GMT_CREATE = "gmt_create";

    public static final String COL_GMT_MODIFIED = "gmt_modified";
}