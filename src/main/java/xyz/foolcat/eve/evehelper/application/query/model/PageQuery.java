package xyz.foolcat.eve.evehelper.application.query.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PageQuery {

    /**
     * 当前页码，从1开始
     */
    @Builder.Default
    @Min(value = 1, message = "页码不能小于1")
    private Integer current = 1;

    /**
     * 每页大小
     */
    @Builder.Default
    @Min(value = 1, message = "每页大小不能小于1")
    @Max(value = 1000, message = "每页大小不能超过1000")
    private Integer size = 20;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序方向：asc, desc
     */
    @Builder.Default
    private String sortOrder = "desc";
}