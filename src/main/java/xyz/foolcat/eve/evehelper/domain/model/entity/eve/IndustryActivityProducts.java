package xyz.foolcat.eve.evehelper.domain.model.entity.eve;

import lombok.Data;

import java.io.Serializable;

/**
 * 工业活动产品表
 *
 * @author Leojan
 */
@Data
public class IndustryActivityProducts implements Serializable {

    private Integer blueprinttypeid;

    private Byte activityid;

    private Integer producttypeid;

    private Integer quantity;

    private Double probability;

    private static final long serialVersionUID = 1L;
} 