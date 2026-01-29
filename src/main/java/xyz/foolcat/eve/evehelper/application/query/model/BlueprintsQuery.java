package xyz.foolcat.eve.evehelper.application.query.model;

import lombok.*;
import lombok.experimental.SuperBuilder;
import xyz.foolcat.eve.evehelper.application.dto.BlueprintsDTO;
import xyz.foolcat.eve.evehelper.shared.kernel.base.PageResult;

import javax.validation.constraints.NotBlank;

/**
 * 蓝图查询DTO
 * 包含蓝图查询的特定条件和分页参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BlueprintsQuery extends PageQuery implements Query<PageResult<BlueprintsDTO>> {
    
    /**
     * 人物或军团的ID
     */
    @NotBlank(message = "ID不能为空")
    private String ownerId;
    
    /**
     * 蓝图名称（模糊查询）
     */
    private String blueprintName;
    
    /**
     * 蓝图类型
     */
    private String blueprintType;
    
    /**
     * 是否包含材料信息
     */
    @Builder.Default
    private Boolean includeMaterials = false;
    
    /**
     * 是否包含产品信息
     */
    @Builder.Default
    private Boolean includeProducts = false;
} 