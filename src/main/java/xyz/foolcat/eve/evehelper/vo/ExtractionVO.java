package xyz.foolcat.eve.evehelper.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 卫星矿开采信息
 *
 * @author Leojan
 * date 2024-06-28 15:06
 */

@Schema(title = "卫星矿开采信息")
@Data
public class ExtractionVO {

    @Schema(name="结束开采")
    private OffsetDateTime chunkArrivalTime;

    @Schema(name="开始开采")
    private OffsetDateTime extractionStartTime;

    @Schema(name="卫星ID")
    private Integer moonId;

    @Schema(name="卫星名")
    private String moonName;

    @Schema(name="碎裂时间")
    private OffsetDateTime naturalDecayTime;

    @Schema(name="建筑名")
    private String structureName;
}
