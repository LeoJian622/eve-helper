package xyz.foolcat.eve.evehelper.domain.eve;

import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

@ApiModel(value="industryBlueprints")
@Data
@Accessors(chain = true)
public class IndustryBlueprints implements Serializable {

    @TableId
    @ApiModelProperty(value="")
    private Integer blueprinttypeid;

    @ApiModelProperty(value="")
    private Integer maxproductionlimit;

    private static final long serialVersionUID = 1L;
}