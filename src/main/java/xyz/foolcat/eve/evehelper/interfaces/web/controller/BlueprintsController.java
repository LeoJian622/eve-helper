package xyz.foolcat.eve.evehelper.interfaces.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import xyz.foolcat.eve.evehelper.application.query.model.BlueprintsQuery;
import xyz.foolcat.eve.evehelper.application.service.BlueprintsApplicationService;
import xyz.foolcat.eve.evehelper.domain.model.vo.BlueprintsVO;
import xyz.foolcat.eve.evehelper.shared.kernel.base.PageResult;
import xyz.foolcat.eve.evehelper.shared.result.Result;

/**
 * @author Leojan
 * date 2022-05-19 11:02
 */
@Tag(name ="蓝图数据")
@RestController
@Slf4j
@RequestMapping("/blueprints")
@RequiredArgsConstructor
public class BlueprintsController {

    private final BlueprintsApplicationService blueprintsApplicationService;

    @Parameters({
            @Parameter(name = "id", description = "人物或军团的ID", required = true),
            @Parameter(name = "current", description = "页码"),
            @Parameter(name = "size", description = "每页行数"),
            @Parameter(name = "sortField", description = "排序字段"),
            @Parameter(name = "sortOrder", description = "排序方向：asc, desc"),
            @Parameter(name = "blueprintName", description = "蓝图名称（模糊查询）"),
            @Parameter(name = "blueprintType", description = "蓝图类型")
    })
    @Operation(summary = "蓝图数据-蓝图清单")
    @GetMapping("/{id}")
    public Result<PageResult<BlueprintsVO>> getBlueprintsList(
            @PathVariable String id,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size,
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
