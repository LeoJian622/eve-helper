package xyz.foolcat.eve.evehelper.domain.model.entity.system;

import lombok.Data;

import java.io.Serializable;

/**
 * 资产实体
 * @author Leojan
 */
@Data
public class Assets implements Serializable {
    /**
     * 物品ID
     */
    private Long itemId;

    /**
     * invTypeID
     */
    private Integer typeId;

    /**
     * 建筑ID
     */
    private Long locationId;

    /**
     * 类型station, solar_system, item, other
     */
    private String locationType;

    /**
     * 位置标识
     */
    private String locationFlag;

    /**
     * 是否单例
     */
    private Boolean isSingleton;

    /**
     * 蓝图拷贝
     */
    private Boolean isBlueprintCopy;

    /**
     * 数量
     */
    private Long quantity;

    /**
     * 所有者ID
     */
    private Long ownerId;

    private static final long serialVersionUID = 1L;
} 