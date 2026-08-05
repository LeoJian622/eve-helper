package xyz.foolcat.eve.evehelper.domain.model.entity.eve;

import xyz.foolcat.eve.evehelper.shared.kernel.base.BaseEntity;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 工业蓝图表
 *
 * @author Leojan
 */
@Data
public class IndustryBlueprints extends BaseEntity implements Serializable {

    private Integer blueprinttypeid;

    private Integer maxproductionlimit;

    @Serial
    private static final long serialVersionUID = 1L;
} 