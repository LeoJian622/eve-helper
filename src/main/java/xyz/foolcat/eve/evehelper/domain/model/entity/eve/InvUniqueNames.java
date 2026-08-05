package xyz.foolcat.eve.evehelper.domain.model.entity.eve;

import xyz.foolcat.eve.evehelper.shared.kernel.base.BaseEntity;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 物品唯一名称表
 * @author Leojan
 */
@Data
public class InvUniqueNames extends BaseEntity implements Serializable {

    private Integer itemId;

    private String itemName;

    private Integer groupId;

    @Serial
    private static final long serialVersionUID = 1L;
} 