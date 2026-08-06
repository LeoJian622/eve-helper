package xyz.foolcat.eve.evehelper.domain.model.query;

import lombok.Builder;
import lombok.Getter;

/**
 * 蓝图分页查询的领域条件
 * 由应用层从请求 DTO 转换而来，使领域仓储契约不依赖上层类型
 *
 * @author Leojan
 */
@Builder
@Getter
public class BlueprintsPageCriteria {

    /**
     * 蓝图原图/拷贝筛选
     */
    public enum CopyFilter {
        /**
         * 不限
         */
        ANY,
        /**
         * 仅原图（BPO）
         */
        ORIGINAL,
        /**
         * 仅拷贝（BPC）
         */
        COPY
    }

    /**
     * 允许的排序字段白名单：领域字段名 -> 数据库列
     */
    @Getter
    public enum SortField {
        TYPE_NAME("it.name"),
        MATERIAL_EFFICIENCY("bt.material_efficiency"),
        TIME_EFFICIENCY("bt.time_efficiency"),
        RUNS("bt.runs"),
        QUANTITY("bt.quantity"),
        ITEM_ID("bt.item_id");

        /**
         * 对应的数据库列名，硬编码，不接受外部输入
         */
        private final String column;

        SortField(String column) {
            this.column = column;
        }
    }

    /**
     * 人物或军团的ID
     */
    private final String ownerId;

    /**
     * 蓝图名称（模糊查询），null 表示不筛选
     */
    private final String blueprintName;

    /**
     * 原图/拷贝筛选
     */
    @Builder.Default
    private final CopyFilter copyFilter = CopyFilter.ANY;

    /**
     * 排序字段，null 表示使用默认排序
     */
    private final SortField sortField;

    /**
     * 是否升序
     */
    @Builder.Default
    private final boolean ascending = false;

    /**
     * 当前页码，从 1 开始
     */
    @Builder.Default
    private final long current = 1L;

    /**
     * 每页大小
     */
    @Builder.Default
    private final long size = 20L;
}
