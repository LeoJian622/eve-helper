package xyz.foolcat.eve.evehelper.domain.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 建筑 增强/解锚时间提醒领域读模型(跨层共享查询结果载体)。
 * 返回处于增强窗口(state 含 vulnerable)或即将解锚(unanchorsAt 近期)的建筑。
 *
 * @author Leojan
 */
@Data
public class StructureTimerDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long structureId;
    private String name;
    private String typeName;
    private String systemName;
    private String state;
    private Integer reinforceHour;
    private Integer nextReinforceHour;
    private OffsetDateTime nextReinforceApply;
    private OffsetDateTime stateTimerStart;
    private OffsetDateTime stateTimerEnd;
    private OffsetDateTime unanchorsAt;
}
