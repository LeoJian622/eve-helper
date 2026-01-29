package xyz.foolcat.eve.evehelper.domain.model.entity.system;

import lombok.Data;

import java.io.Serializable;

/**
 * 宇宙名称表
 * @author Leojan
 */
@Data
public class UniverseName  implements Serializable {
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

    private static final long serialVersionUID = 1L;
} 