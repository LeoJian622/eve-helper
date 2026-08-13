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
 * 角色管理
 * @author Leojan
 */
@EqualsAndHashCode(callSuper = true)
@Schema(description = "角色管理")
@Data
@TableName(value = "sys_role")
public class SysRolePO extends BaseEntity implements Serializable {
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
     * 角色描述
     */
    @TableField(value = "description")
    @Schema(description = "角色描述")
    private String description;
    /**
     * 状态：0-禁用 1-开启
     */
    @TableField(value = "status")
    @Schema(description = "状态：0-禁用 1-开启")
    private Boolean status;
    /**
     * 显示顺序
     */
    @TableField(value = "sort")
    @Schema(description = "显示顺序")
    private Integer sort;
    /**
     * 逻辑删除标识：0-未删除；1-已删除
     */
    @TableField(value = "deleted")
    @Schema(description = "逻辑删除标识：0-未删除；1-已删除")
    private Boolean deleted;
    @Serial
    private static final long serialVersionUID = 1L;
} 