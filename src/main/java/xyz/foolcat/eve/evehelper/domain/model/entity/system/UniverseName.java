package xyz.foolcat.eve.evehelper.domain.model.entity.system;

import xyz.foolcat.eve.evehelper.shared.kernel.base.BaseEntity;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 宇宙名称表
 * @author Leojan
 */
@Data
public class UniverseName extends BaseEntity implements Serializable {
    /**
     * 主键ID
     */
    private Integer id;

    /**
     * 名称
     */
    private String name;

    /**
     * alliance, character, constellation, corporation, inventory_type, region, solar_system, station, faction
     */
    private String category;

    @Serial
    private static final long serialVersionUID = 1L;
} 