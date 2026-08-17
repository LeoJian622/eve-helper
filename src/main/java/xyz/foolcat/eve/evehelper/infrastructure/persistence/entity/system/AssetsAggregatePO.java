package xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system;

import lombok.Data;

/**
 * 资产多角色聚合查询投影(PO)。
 *
 * <p>由 {@code AssetsMapper} 聚合 SQL(resultType)直接映射,仅承载聚合结果,
 * 非独立表实体,不参与 BaseMapper CRUD。</p>
 */
@Data
public class AssetsAggregatePO {

    /**
     * 角色 ID(资产归因 owner_id)
     */
    private Integer ownerId;

    /**
     * 物品总件数(quantity 求和)
     */
    private Long assetCount;

    /**
     * 资产总价值(quantity * COALESCE(base_price,0) 求和)
     */
    private Double assetValue;

    /**
     * 类目数(去重 type_id 数)
     */
    private Long categoryCount;
}