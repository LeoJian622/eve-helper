package xyz.foolcat.eve.evehelper.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.StructureService;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 建筑服务状态响应 VO
 *
 * @author Leojan
 */
@Data
@Schema(description = "建筑服务状态")
public class StructureServiceVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "建筑ID")
    private Long structureId;

    @Schema(description = "建筑名称")
    private String name;

    @Schema(description = "建筑服务列表")
    private List<StructureService> services;
}
