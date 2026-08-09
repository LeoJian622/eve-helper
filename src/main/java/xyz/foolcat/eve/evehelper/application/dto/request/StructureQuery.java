package xyz.foolcat.eve.evehelper.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import xyz.foolcat.eve.evehelper.shared.kernel.base.PageQuery;

/**
 * 建筑列表查询条件
 *
 * @author Leojan
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StructureQuery extends PageQuery {

    /**
     * 军团ID
     */
    @NotBlank(message = "军团ID不能为空")
    private String corporationId;

    /**
     * 建筑名称(模糊查询)
     */
    private String name;

    /**
     * 状态筛选
     */
    private String state;

    /**
     * 仅缺油
     */
    private Boolean lowFuelOnly;
}
