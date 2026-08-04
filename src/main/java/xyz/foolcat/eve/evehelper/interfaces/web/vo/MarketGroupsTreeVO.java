package xyz.foolcat.eve.evehelper.interfaces.web.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.InvTypes;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Schema(description="市场菜单树")
@Data
@Accessors(chain = true)
public class MarketGroupsTreeVO implements Serializable {

    @Schema(description="组ID")
    private Integer marketGroupId;

    @Schema(description="")
    private Byte hasTypes;

    @Schema(description="图标ID")
    private Integer iconId;

    @Schema(description="名字")
    private String nameId;

    @Schema(description="上级组ID")
    private Integer parentGroupId;

    @Schema(description="子菜单")
    private List<MarketGroupsTreeVO> childrens;

    @Schema(description="物品清单")
    private List<InvTypes> goods;

    @Serial
    private static final long serialVersionUID = 1L;
}