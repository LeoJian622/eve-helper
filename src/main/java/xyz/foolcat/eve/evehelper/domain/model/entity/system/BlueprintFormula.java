package xyz.foolcat.eve.evehelper.domain.model.entity.system;

import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.foolcat.eve.evehelper.shared.kernel.base.BaseEntity;

import java.io.Serializable;

/**
 * 蓝图材料配方基础数据
 * @author Leojan
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class BlueprintFormula extends BaseEntity implements Serializable {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 蓝图ID
     */
    private Long blueprintId;

    /**
     * 原材料typeid
     */
    private Long materialTypeId;

    /**
     * 名称
     */
    private String typeName;

    /**
     * 活动类型
     */
    private Integer activityId;

    /**
     * 需求数量
     */
    private Integer quantity;

    private static final long serialVersionUID = 1L;
} 