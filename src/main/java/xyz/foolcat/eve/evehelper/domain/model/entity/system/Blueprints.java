package xyz.foolcat.eve.evehelper.domain.model.entity.system;

import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.foolcat.eve.evehelper.shared.kernel.base.BaseEntity;

import java.io.Serializable;

/**
 * 蓝图属性表
 * @author Leojan
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class Blueprints extends BaseEntity implements Serializable {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 唯一item id
     */
    private Long itemId;

    /**
     * invType id
     */
    private Long typeId;

    /**
     * 材料效率
     */
    private Integer materialEfficiency;

    /**
     * 时间效率
     */
    private Integer timeEfficiency;

    /**
     * 流程数
     */
    private Integer runs;

    /**
     * 数量
     */
    private Long quantity;

    /**
     * 建筑ID
     */
    private Long locationId;

    /**
     * 位置
     */
    private String locationFlag;

    /**
     * 所有者ID
     */
    private Long ownerId;

    private static final long serialVersionUID = 1L;
} 