package xyz.foolcat.eve.evehelper.domain.model.entity.eve;

import lombok.Data;

import java.io.Serializable;

/**
 * 工业蓝图表
 * @author Leojan
 */
@Data
public class IndustryBlueprints implements Serializable {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 蓝图ID
     */
    private Long blueprintId;

    /**
     * 蓝图类型ID
     */
    private Long blueprintTypeId;

    /**
     * 蓝图类型名称
     */
    private String blueprintTypeName;

    /**
     * 材料效率
     */
    private Integer materialEfficiency;

    /**
     * 时间效率
     */
    private Integer timeEfficiency;

    /**
     * 运行次数
     */
    private Integer runs;

    /**
     * 最大运行次数
     */
    private Integer maxRuns;

    /**
     * 所有者ID
     */
    private Long ownerId;

    /**
     * 所有者名称
     */
    private String ownerName;

    /**
     * 位置ID
     */
    private Long locationId;

    /**
     * 位置名称
     */
    private String locationName;

    /**
     * 位置类型
     */
    private String locationType;

    private static final long serialVersionUID = 1L;
} 