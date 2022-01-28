package xyz.foolcat.eve.evehelper.domain.eve;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

@ApiModel(value="industryActivityProducts")
@Data
@Accessors(chain = true)
public class IndustryActivityProducts implements Serializable {
    @ApiModelProperty(value="")
    private Integer blueprinttypeid;

    @ApiModelProperty(value="")
    private Byte activityid;

    @ApiModelProperty(value="")
    private Integer producttypeid;

    @ApiModelProperty(value="")
    private Integer quantity;

    @ApiModelProperty(value="")
    private Double probability;

    private static final long serialVersionUID = 1L;
}