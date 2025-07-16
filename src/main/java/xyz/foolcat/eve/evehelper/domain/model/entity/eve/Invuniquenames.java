package xyz.foolcat.eve.evehelper.domain.model.entity.eve;

import lombok.Data;

import java.io.Serializable;

/**
 * 物品唯一名称表
 * @author Leojan
 */
@Data
public class Invuniquenames implements Serializable {

    /**
     * 物品ID
     */
    private Long itemId;

    /**
     * 物品名称
     */
    private String itemName;

    /**
     * 物品组ID
     */
    private Long groupId;

    /**
     * 物品组名称
     */
    private String groupName;

    /**
     * 物品类别ID
     */
    private Long categoryId;

    /**
     * 物品类别名称
     */
    private String categoryName;

    private static final long serialVersionUID = 1L;
} 