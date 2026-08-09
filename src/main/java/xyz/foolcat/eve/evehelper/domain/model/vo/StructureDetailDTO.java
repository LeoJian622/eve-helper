package xyz.foolcat.eve.evehelper.domain.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 建筑详情领域读模型(跨层共享查询结果载体)。
 * {@code servicesJson} 为数据库原始 JSON 字符串,由应用层容错解析为
 * {@code List<StructureService>},避免领域层引入序列化逻辑。
 *
 * @author Leojan
 */
@Data
public class StructureDetailDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long structureId;
    private Long corporationId;
    private OffsetDateTime fuelExpires;
    private String name;
    private OffsetDateTime nextReinforceApply;
    private Integer nextReinforceHour;
    private Long profileId;
    private Integer reinforceHour;
    private String state;
    private OffsetDateTime stateTimerEnd;
    private OffsetDateTime stateTimerStart;
    private Long systemId;
    private Long typeId;
    private OffsetDateTime unanchorsAt;
    /**
     * 建筑服务原始 JSON(应用层解析为 List<StructureService>)
     */
    private String servicesJson;
    private String typeName;
    private String systemName;
}
