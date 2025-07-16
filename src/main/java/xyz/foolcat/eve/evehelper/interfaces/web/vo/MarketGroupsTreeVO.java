package xyz.foolcat.eve.evehelper.interfaces.web.vo;

import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.InvTypes;

import java.io.Serializable;
import java.util.List;

@Schema(title="市场菜单树")
@Data
@Accessors(chain = true)
public class MarketGroupsTreeVO implements Serializable {

    @TableId
    @Schema(name="组ID")
    private Integer marketGroupId;

//    @Schema(name="描述")
//    private String descriptionid;

    @Schema(name="")
    private Byte hasTypes;

    @Schema(name="图标ID")
    private Integer iconId;

    @Schema(name="名字")
    private String nameId;

    @Schema(name="上级组ID")
    private Integer parentGroupId;

    @Schema(name="子菜单")
    private List<MarketGroupsTreeVO> childrens;

    @Schema(name="物品清单")
    private List<InvTypes> goods;

    private static final long serialVersionUID = 1L;
}