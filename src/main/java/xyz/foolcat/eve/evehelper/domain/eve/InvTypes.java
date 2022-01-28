package xyz.foolcat.eve.evehelper.domain.eve;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

@ApiModel(value="invTypes")
@Data
@Accessors(chain = true)
public class InvTypes implements Serializable {

    @TableId
    @ApiModelProperty(value="")
    private Integer typeid;

    @ApiModelProperty(value="")
    private Integer groupid;

    @ApiModelProperty(value="")
    private String typename;

    @ApiModelProperty(value="")
    private String description;

    @ApiModelProperty(value="")
    private Double mass;

    @ApiModelProperty(value="")
    private Double volume;

    @ApiModelProperty(value="")
    private Double packagedvolume;

    @ApiModelProperty(value="")
    private Double capacity;

    @ApiModelProperty(value="")
    private Integer portionsize;

    @ApiModelProperty(value="")
    private Integer factionid;

    @ApiModelProperty(value="")
    private Byte raceid;

    @ApiModelProperty(value="")
    private Double baseprice;

    @ApiModelProperty(value="")
    private Byte published;

    @ApiModelProperty(value="")
    private Integer marketgroupid;

    @ApiModelProperty(value="")
    private Integer graphicid;

    @ApiModelProperty(value="")
    private Double radius;

    @ApiModelProperty(value="")
    private Integer iconid;

    @ApiModelProperty(value="")
    private Integer soundid;

    @ApiModelProperty(value="")
    private String soffactionname;

    @ApiModelProperty(value="")
    private Integer sofmaterialsetid;

    @ApiModelProperty(value="")
    private Integer metagroupid;

    @ApiModelProperty(value="")
    private Integer variationparenttypeid;

    private static final long serialVersionUID = 1L;
}