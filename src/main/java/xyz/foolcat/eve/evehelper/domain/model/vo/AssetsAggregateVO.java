package xyz.foolcat.eve.evehelper.domain.model.vo;

/**
 * 资产多角色聚合读模型。
 *
 * <p>按角色(ownerId)汇总的物品总件数、资产总价值与类目数。跨层共享的查询结果载体。</p>
 *
 * @param ownerId      角色 ID(资产归因 owner_id)
 * @param assetCount   物品总件数(quantity 求和)
 * @param assetValue   资产总价值(quantity * COALESCE(base_price,0) 求和)
 * @param categoryCount 类目数(去重 type_id 数)
 */
public record AssetsAggregateVO(Integer ownerId, Long assetCount, Double assetValue, Long categoryCount) {

    /**
     * 无资产角色的零值视图。
     *
     * @param ownerId 角色 ID
     * @return 零值聚合视图
     */
    public static AssetsAggregateVO zero(Integer ownerId) {
        return new AssetsAggregateVO(ownerId, 0L, 0.0, 0L);
    }
}