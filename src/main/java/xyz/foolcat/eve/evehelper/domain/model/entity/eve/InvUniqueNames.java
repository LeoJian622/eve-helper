package xyz.foolcat.eve.evehelper.domain.model.entity.eve;

import lombok.Data;

import java.io.Serializable;

/**
 * 物品唯一名称表
 * @author Leojan
 */
@Data
public class InvUniqueNames implements Serializable {

    private Integer itemId;

    private String itemName;

    private Integer groupId;

    private static final long serialVersionUID = 1L;
} 