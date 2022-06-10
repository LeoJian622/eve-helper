package xyz.foolcat.eve.evehelper.vo;

import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;
import xyz.foolcat.eve.evehelper.domain.system.InvTypes;

import java.io.Serializable;
import java.util.List;

@ApiModel(value = "市场菜单树")
@Data
@Accessors(chain = true)
public class MarketGroupsTreeVO implements Serializable {

    @TableId
    @ApiModelProperty(value = "组ID")
    private Integer marketGroupId;

//    @ApiModelProperty(value = "描述")
//    private String descriptionid;

    @ApiModelProperty(value = "")
    private Byte hasTypes;

    @ApiModelProperty(value = "图标ID")
    private Integer iconId;

    @ApiModelProperty(value = "名字")
    private String nameId;

    @ApiModelProperty(value = "上级组ID")
    private Integer parentGroupId;

    @ApiModelProperty(value = "子菜单")
    private List<MarketGroupsTreeVO> childrens;

    @ApiModelProperty(value = "物品清单")
    private List<InvTypes> goods;

    private static final long serialVersionUID = 1L;
}