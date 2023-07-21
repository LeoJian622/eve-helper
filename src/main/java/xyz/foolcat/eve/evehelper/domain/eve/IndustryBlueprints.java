package xyz.foolcat.eve.evehelper.domain.eve;

import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Schema(title="industryBlueprints",hidden = true)
@Data
@Accessors(chain = true)
public class IndustryBlueprints implements Serializable {

    @TableId
    @Schema(name="")
    private Integer blueprinttypeid;

    @Schema(name="")
    private Integer maxproductionlimit;

    private static final long serialVersionUID = 1L;
}