package xyz.foolcat.eve.evehelper.domain.eve;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Schema(title="industryActivityProducts",hidden = true)
@Data
@Accessors(chain = true)
public class IndustryActivityProducts implements Serializable {
    @Schema(name="")
    private Integer blueprinttypeid;

    @Schema(name="")
    private Byte activityid;

    @Schema(name="")
    private Integer producttypeid;

    @Schema(name="")
    private Integer quantity;

    @Schema(name="")
    private Double probability;

    private static final long serialVersionUID = 1L;
}