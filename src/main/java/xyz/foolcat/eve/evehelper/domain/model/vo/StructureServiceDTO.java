package xyz.foolcat.eve.evehelper.domain.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 建筑服务状态领域读模型(跨层共享查询结果载体)。
 * {@code servicesJson} 为数据库原始 JSON 字符串,由应用层容错解析为
 * {@code List<StructureService>},避免领域层引入序列化逻辑(沿用 US2 模式, R-01)。
 *
 * @author Leojan
 */
@Data
public class StructureServiceDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long structureId;

    private Long corporationId;

    private String name;

    /**
     * 建筑服务原始 JSON(应用层解析为 List<StructureService>)
     */
    private String servicesJson;
}
