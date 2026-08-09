package xyz.foolcat.eve.evehelper.domain.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 建筑燃料预警领域读模型(跨层共享查询结果载体)
 *
 * @author Leojan
 */
@Data
public class StructureFuelDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long structureId;
    private String name;
    private String typeName;
    private String systemName;
    private String state;
    private OffsetDateTime fuelExpires;

    /**
     * 剩余时长(小时),燃料到期时间与当前国服时间的整小时差;无燃料为 null
     */
    private Long remainingHours;
}
