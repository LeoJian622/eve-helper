package xyz.foolcat.eve.evehelper.application.dto.request;

import lombok.*;
import lombok.experimental.SuperBuilder;
import xyz.foolcat.eve.evehelper.shared.kernel.base.PageQuery;

import jakarta.validation.constraints.NotBlank;

/**
 * 蓝图查询条件
 * 包含蓝图查询的特定条件和分页参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BlueprintsQuery extends PageQuery {
    
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
     * 蓝图类型：original(原图) 或 copy(拷贝)，缺省不筛选
     */
    private String blueprintType;
} 