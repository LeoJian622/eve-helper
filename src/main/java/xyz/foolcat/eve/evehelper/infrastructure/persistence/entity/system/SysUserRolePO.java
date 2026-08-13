package xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.foolcat.eve.evehelper.shared.kernel.base.BaseEntity;
import java.io.Serial;
import java.io.Serializable;
/**
 * 用户角色关联表
 * @author Leojan
 */
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户角色关联表")
@Data
@TableName(value = "sys_user_role")
public class SysUserRolePO extends BaseEntity implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "")
    private Long id;
    /**
     * 用户ID
     */
    @TableField(value = "user_id")
    @Schema(description = "用户ID")
    private Long userId;
    /**
     * 角色ID
     */
    @TableField(value = "role_id")
    @Schema(description = "角色ID")
    private Long roleId;
    @Serial
    private static final long serialVersionUID = 1L;
} 