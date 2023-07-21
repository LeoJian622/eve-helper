package xyz.foolcat.eve.evehelper.domain.system;

import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Schema(title="市场组")
@Data
@Accessors(chain = true)
public class MarketGroups implements Serializable {

    @TableId
    @Schema(name="")
    private Integer marketgroupid;

    @Schema(name="")
    private String descriptionid;

    @Schema(name="")
    private Byte hastypes;

    @Schema(name="")
    private Integer iconid;

    @Schema(name="")
    private String nameid;

    @Schema(name="")
    private Integer parentgroupid;

    private static final long serialVersionUID = 1L;
}