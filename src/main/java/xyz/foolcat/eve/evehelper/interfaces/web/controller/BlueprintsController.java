package xyz.foolcat.eve.evehelper.interfaces.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import xyz.foolcat.eve.evehelper.application.dto.request.BlueprintsQuery;
import xyz.foolcat.eve.evehelper.application.dto.response.BlueprintsVO;
import xyz.foolcat.eve.evehelper.application.service.BlueprintsApplicationService;
import xyz.foolcat.eve.evehelper.shared.kernel.base.PageResult;
import xyz.foolcat.eve.evehelper.shared.result.Result;

/**
 * @author Leojan
 * date 2022-05-19 11:02
 */
@Tag(name ="蓝图数据")
@RestController
@Slf4j
@Validated
@RequestMapping("/blueprints")
@RequiredArgsConstructor
public class BlueprintsController {

    private final BlueprintsApplicationService blueprintsApplicationService;

    @Parameters({
            @Parameter(name = "id", description = "人物或军团的ID", required = true),
            @Parameter(name = "current", description = "页码"),
            @Parameter(name = "size", description = "每页行数"),
            @Parameter(name = "sortField", description = "排序字段：typeName, materialEfficiency, timeEfficiency, runs, quantity, itemId"),
            @Parameter(name = "sortOrder", description = "排序方向：asc, desc"),
            @Parameter(name = "blueprintName", description = "蓝图名称（模糊查询）"),
            @Parameter(name = "blueprintType", description = "蓝图类型：original(原图) 或 copy(拷贝)，缺省不筛选")
    })
    @Operation(summary = "蓝图数据-蓝图清单")
    @GetMapping("/{id}")
    public Result<PageResult<BlueprintsVO>> getBlueprintsList(
            @PathVariable String id,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码不能小于1") Integer current,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "每页大小不能小于1")
            @Max(value = 1000, message = "每页大小不能超过1000") Integer size,
            @RequestParam(required = false) String sortField,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestParam(required = false) String blueprintName,
            @RequestParam(required = false) String blueprintType) {
        
        // 构建查询DTO
        BlueprintsQuery queryDTO = BlueprintsQuery.builder()
                .ownerId(id)
                .current(current)
                .size(size)
                .sortField(sortField)
                .sortOrder(sortOrder)
                .blueprintName(blueprintName)
                .blueprintType(blueprintType)
                .build();
        
        // 调用应用服务
        return Result.success(blueprintsApplicationService.queryBlueprintsByPage(queryDTO));
    }
}
