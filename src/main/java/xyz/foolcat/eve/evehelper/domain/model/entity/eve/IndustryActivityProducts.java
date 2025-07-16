package xyz.foolcat.eve.evehelper.domain.model.entity.eve;

import lombok.Data;

import java.io.Serializable;

/**
 * 工业活动产品表
 * @author Leojan
 */
@Data
public class IndustryActivityProducts implements Serializable {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 蓝图类型ID
     */
    private Long blueprintTypeId;

    /**
     * 活动ID
     */
    private Integer activityId;

    /**
     * 产品类型ID
     */
    private Long productTypeId;

    /**
     * 产品类型名称
     */
    private String productTypeName;

    /**
     * 数量
     */
    private Integer quantity;

    /**
     * 概率
     */
    private Double probability;

    private static final long serialVersionUID = 1L;
} 