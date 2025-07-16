package xyz.foolcat.eve.evehelper.domain.model.entity.system;

import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.foolcat.eve.evehelper.shared.kernel.base.BaseEntity;

import java.io.Serializable;

/**
 * ESI配置表
 * @author Leojan
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class EsiConfig extends BaseEntity implements Serializable {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * ESI请求地址
     */
    private String baseUrl;

    /**
     * ESI版本
     */
    private String version;

    /**
     * CH:0 EU:1
     */
    private Integer type;

    private static final long serialVersionUID = 1L;
} 