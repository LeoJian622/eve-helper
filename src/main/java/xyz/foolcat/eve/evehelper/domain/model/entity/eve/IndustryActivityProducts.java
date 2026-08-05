package xyz.foolcat.eve.evehelper.domain.model.entity.eve;

import xyz.foolcat.eve.evehelper.shared.kernel.base.BaseEntity;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 工业活动产品表
 *
 * @author Leojan
 */
@Data
public class IndustryActivityProducts extends BaseEntity implements Serializable {

    private Integer blueprinttypeid;

    private Byte activityid;

    private Integer producttypeid;

    private Integer quantity;

    private Double probability;

    @Serial
    private static final long serialVersionUID = 1L;
} 