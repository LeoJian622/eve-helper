package xyz.foolcat.eve.evehelper.domain.model.query;

import lombok.Builder;
import lombok.Getter;

/**
 * 建筑分页查询的领域条件
 * 由应用层从请求 DTO 转换而来,使领域仓储契约不依赖上层类型
 *
 * @author Leojan
 */
@Builder
@Getter
public class StructurePageCriteria {

    /**
     * 允许的排序字段白名单:枚举 -> 数据库列(硬编码,不接受外部输入,防注入 FR-015)
     */
    @Getter
    public enum SortField {
        STRUCTURE_ID("s.structure_id"),
        NAME("s.`name`"),
        STATE("s.`state`"),
        FUEL_EXPIRES("s.fuel_expires");

        /**
         * 对应的数据库列名,硬编码
         */
        private final String column;

        SortField(String column) {
            this.column = column;
        }
    }

    /**
     * 军团ID(纯数字字符串,应用层已校验)
     */
    private final String corporationId;

    /**
     * 建筑名称(模糊查询),null 表示不筛选
     */
    private final String name;

    /**
     * 状态筛选,null 表示不筛选
     */
    private final String state;

    /**
     * 仅缺油(fuel_expires 在预警窗内或 null)
     */
    @Builder.Default
    private final boolean lowFuelOnly = false;

    /**
     * 排序字段,null 表示使用默认排序
     */
    private final SortField sortField;

    /**
     * 是否升序
     */
    @Builder.Default
    private final boolean ascending = false;

    /**
     * 当前页码,从 1 开始
     */
    @Builder.Default
    private final long current = 1L;

    /**
     * 每页大小
     */
    @Builder.Default
    private final long size = 20L;
}
