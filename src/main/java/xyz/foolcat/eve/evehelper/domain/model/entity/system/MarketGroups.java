package xyz.foolcat.eve.evehelper.domain.model.entity.system;

import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.foolcat.eve.evehelper.shared.kernel.base.BaseEntity;

import java.io.Serializable;

/**
 * 市场组表
 * @author Leojan
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class MarketGroups extends BaseEntity implements Serializable {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 市场组ID
     */
    private Long marketGroupId;

    /**
     * 描述ID
     */
    private String descriptionId;

    /**
     * 是否有类型
     */
    private Boolean hasTypes;

    /**
     * 图标ID
     */
    private Long iconId;

    /**
     * 名称ID
     */
    private String nameId;

    /**
     * 父组ID
     */
    private Long parentGroupId;

    private static final long serialVersionUID = 1L;
} 