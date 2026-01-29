package xyz.foolcat.eve.evehelper.domain.model.entity.system;

import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.foolcat.eve.evehelper.shared.kernel.base.BaseEntity;

import java.io.Serializable;
import java.util.Date;

/**
 * 观察者表
 * @author Leojan
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class Observer extends BaseEntity implements Serializable {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 观察者ID
     */
    private Long observerId;

    /**
     * 观察者类型  枚举：structure
     */
    private String observerType;

    /**
     * 上次更新时间
     */
    private Date lastUpdated;

    /**
     * 公司ID
     */
    private Long corporationId;

    private static final long serialVersionUID = 1L;
} 