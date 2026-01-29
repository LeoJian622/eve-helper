package xyz.foolcat.eve.evehelper.domain.model.entity.eve;

import lombok.Data;

import java.io.Serializable;

/**
 * 工业蓝图表
 *
 * @author Leojan
 */
@Data
public class IndustryBlueprints implements Serializable {

    private Integer blueprinttypeid;

    private Integer maxproductionlimit;

    private static final long serialVersionUID = 1L;
} 