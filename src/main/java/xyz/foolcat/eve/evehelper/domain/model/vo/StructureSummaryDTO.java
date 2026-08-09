package xyz.foolcat.eve.evehelper.domain.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 建筑统计概览行级领域读模型(按状态分组,跨层共享查询结果载体)。
 * 每行表示一个状态分组的计数与小计,由应用层遍历汇总为 {@link StructureSummaryVO}。
 * 采用单 SQL GROUP BY state 聚合以满足 SC-003(避免 N+1)。
 *
 * @author Leojan
 */
@Data
public class StructureSummaryDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 建筑状态(分组键)
     */
    private String state;

    /**
     * 该状态建筑数
     */
    private Long stateCount;

    /**
     * 该状态中已缺油(fuel_expires IS NULL)的建筑数
     */
    private Long fuelExpiredCount;

    /**
     * 该状态中即将缺油(72 小时内)的建筑数
     */
    private Long lowFuelCount;
}
