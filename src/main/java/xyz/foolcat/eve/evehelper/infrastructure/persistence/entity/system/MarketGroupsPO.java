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
 * 市场组表
 * @author Leojan
 */
@EqualsAndHashCode(callSuper = true)
@Schema(description = "市场组表")
@Data
@TableName(value = "market_groups")
public class MarketGroupsPO extends BaseEntity implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;
    @TableField(value = "market_group_id")
    @Schema(description = "市场组ID")
    private Long marketGroupId;
    @TableField(value = "description_id")
    @Schema(description = "描述ID")
    private String descriptionId;
    @TableField(value = "has_types")
    @Schema(description = "是否有类型")
    private Boolean hasTypes;
    @TableField(value = "icon_id")
    @Schema(description = "图标ID")
    private Long iconId;
    @TableField(value = "name_id")
    @Schema(description = "名称ID")
    private String nameId;
    @TableField(value = "parent_group_id")
    @Schema(description = "父组ID")
    private Long parentGroupId;
    @Serial
    private static final long serialVersionUID = 1L;
} 