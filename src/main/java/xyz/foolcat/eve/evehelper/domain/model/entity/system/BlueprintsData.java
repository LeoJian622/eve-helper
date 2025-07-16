package xyz.foolcat.eve.evehelper.domain.model.entity.system;

import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.foolcat.eve.evehelper.shared.kernel.base.BaseEntity;

import java.io.Serializable;

/**
 * 蓝图生产相关数据表
 * @author Leojan
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class BlueprintsData extends BaseEntity implements Serializable {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 产物typeId
     */
    private Long typeId;

    /**
     * 产物名称
     */
    private String typeName;

    /**
     * 蓝图typeId
     */
    private Long blueprintTypeId;

    /**
     * 蓝图名称
     */
    private String blueprintName;

    /**
     * 生产类型
     */
    private Integer activityId;

    /**
     * 单流程产量
     */
    private Integer quantity;

    /**
     * 基础成功率
     */
    private Double probability;

    /**
     * 生产基础时间
     */
    private Integer time;

    /**
     * 最大单次产量
     */
    private Integer maxProductionLimit;

    private static final long serialVersionUID = 1L;
} 