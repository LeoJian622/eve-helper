package xyz.foolcat.eve.evehelper.domain.model.entity.eve;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 工业活动材料表
 * @author Leojan
 */
@Data
public class IndustryActivityMaterials implements Serializable {
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
     * 材料类型ID
     */
    private Long materialTypeId;

    /**
     * 材料类型名称
     */
    private String materialTypeName;

    /**
     * 数量
     */
    private Integer quantity;

    private static final long serialVersionUID = 1L;
} 