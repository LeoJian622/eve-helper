package xyz.foolcat.eve.evehelper.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 基础蓝图制造清单
 *
 * @author Leojan
 * date 2022-09-21 16:36
 */

@Schema(title="蓝图材料清单")
@Data
public class BlueprintFormulaDTO implements Serializable {

    private static final long serialVersionUID = 8409925024967880300L;

    @Schema(name="物品ID")
    Integer typeId;

    @Schema(name="物品名称")
    String typeName;

    @Schema(name="生产时间")
    Integer time;

    @Schema(name="材料清单")
    List<BlueprintFormulaDTO> materials;
}
