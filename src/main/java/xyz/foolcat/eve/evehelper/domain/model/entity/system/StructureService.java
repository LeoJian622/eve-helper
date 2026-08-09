package xyz.foolcat.eve.evehelper.domain.model.entity.system;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 建筑服务领域值对象(只读查询用)。
 * 与 infrastructure 层 {@code StructuresService} 字段对齐,
 * 独立定义以避免领域层反向依赖 infrastructure 的 ESI 模型(DDD 依赖规则)。
 *
 * @author Leojan
 */
@Data
public class StructureService implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 服务名称
     */
    private String name;

    /**
     * 服务状态
     */
    private String state;
}
