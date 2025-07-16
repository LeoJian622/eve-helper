package xyz.foolcat.eve.evehelper.domain.model.entity.system;

import lombok.Data;

import java.io.Serializable;

/**
 * 物品表
 * @author Leojan
 */
@Data
public class InvTypes implements Serializable {

    /**
     * 物品ID
     */
    private Long typeId;

    /**
     * 物品组ID
     */
    private Long groupId;

    /**
     * 名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 质量
     */
    private Double mass;

    /**
     * 体积
     */
    private Double volume;

    /**
     * 打包体积
     */
    private Double packagedVolume;

    /**
     * 类别
     */
    private Double capacity;

    /**
     * 部分大小
     */
    private Integer portionSize;

    /**
     * 派系ID
     */
    private Long factionId;

    /**
     * 种族ID
     */
    private Long raceId;

    /**
     * 基础价格
     */
    private Double basePrice;

    /**
     * 是否发布
     */
    private Boolean published;

    /**
     * 市场组ID
     */
    private Long marketGroupId;

    /**
     * 图片ID
     */
    private Long graphicId;

    /**
     * 半径
     */
    private Double radius;

    /**
     * 图标ID
     */
    private Long iconId;

    /**
     * 音乐ID
     */
    private Long soundId;

    /**
     * SOF派系名称
     */
    private String sofFactionName;

    /**
     * SOF材料集ID
     */
    private Long sofMaterialSetId;

    /**
     * 元组ID
     */
    private Long metaGroupId;

    /**
     * 变体父类型ID
     */
    private Long variationparentTypeId;

    private static final long serialVersionUID = 1L;
} 