package xyz.foolcat.eve.evehelper.domain.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 建筑列表项领域读模型(跨层共享查询结果载体)
 *
 * @author Leojan
 */
@Data
public class StructureListItemDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long structureId;
    private String name;
    private Long typeId;
    private String typeName;
    private Long systemId;
    private String systemName;
    private String state;
    private OffsetDateTime fuelExpires;
    private Long corporationId;
}
