package xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system;
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
 * 游戏角色表
 * @author Leojan
 */
@EqualsAndHashCode(callSuper = true)
@Schema(description = "游戏角色表")
@Data
@TableName(value = "eve_account")
public class EveAccountPO extends BaseEntity implements Serializable {
    /**
     * 角色ID
     */
    @TableId(value = "character_id")
    @Schema(description = "角色ID")
    private Long characterId;
    /**
     * 角色名
     */
    @TableField(value = "character_name")
    @Schema(description = "角色名")
    private String characterName;
    /**
     * 军团（公司）ID
     */
    @TableField(value = "corp_id")
    @Schema(description = "军团（公司）ID")
    private Long corpId;
    /**
     * 军团（公司）名称
     */
    @TableField(value = "corp_name")
    @Schema(description = "军团（公司）名称")
    private String corpName;
    /**
     * 联盟ID
     */
    @TableField(value = "alliance_id")
    @Schema(description = "联盟ID")
    private Long allianceId;
    /**
     * 联盟名称
     */
    @TableField(value = "alliance_name")
    @Schema(description = "联盟名称")
    private String allianceName;
    /**
     * 角色授权
     */
    @TableField(value = "refresh_token")
    @Schema(description = "角色授权")
    private String refreshToken;
    /**
     * CH:0 EU:1
     */
    @TableField(value = "`type`")
    @Schema(description = "CH:0 EU:1")
    private Integer type;
    /**
     * QQ号
     */
    @TableField(value = "qq")
    @Schema(description = "QQ号")
    private String qq;
    /**
     * 用户ID
     */
    @TableField(value = "user_id")
    @Schema(description = "用户ID")
    private Long userId;
    @Serial
    private static final long serialVersionUID = 1L;
} 