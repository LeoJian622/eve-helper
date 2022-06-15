package xyz.foolcat.eve.evehelper.domain.system;

import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

@ApiModel(value = "marketGroups")
@Data
@Accessors(chain = true)
public class MarketGroups implements Serializable {

    @TableId
    @ApiModelProperty(value = "")
    private Integer marketgroupid;

    @ApiModelProperty(value = "")
    private String descriptionid;

    @ApiModelProperty(value = "")
    private Byte hastypes;

    @ApiModelProperty(value = "")
    private Integer iconid;

    @ApiModelProperty(value = "")
    private String nameid;

    @ApiModelProperty(value = "")
    private Integer parentgroupid;

    private static final long serialVersionUID = 1L;
}