package xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.eve;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Schema(title="industryActivityMaterials",hidden = true)
@Data
@Accessors(chain = true)
public class IndustryActivityMaterialsPO implements Serializable {

    @Schema(name="蓝图类型IO")
    private Integer blueprinttypeid;

    @Schema(name="")
    private Byte activityid;

    @Schema(name="")
    private Integer materialtypeid;

    @Schema(name="")
    private Integer quantity;

    private static final long serialVersionUID = 1L;
} 