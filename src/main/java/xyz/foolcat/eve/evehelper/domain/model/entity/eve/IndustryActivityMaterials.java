package xyz.foolcat.eve.evehelper.domain.model.entity.eve;

import lombok.Data;

import java.io.Serializable;

/**
 * 工业活动材料表
 * @author Leojan
 */
@Data
public class IndustryActivityMaterials implements Serializable {

    private Integer blueprinttypeid;

    private Byte activityid;

    private Integer materialtypeid;

    private Integer quantity;


    private static final long serialVersionUID = 1L;
} 