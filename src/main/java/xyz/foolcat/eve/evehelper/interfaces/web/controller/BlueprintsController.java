package xyz.foolcat.eve.evehelper.interfaces.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import xyz.foolcat.eve.evehelper.application.dto.query.BlueprintsQuery;
import xyz.foolcat.eve.evehelper.application.dto.response.PageResultDTO;
import xyz.foolcat.eve.evehelper.application.service.BlueprintsApplicationService;
import xyz.foolcat.eve.evehelper.interfaces.web.vo.BlueprintsVO;
import xyz.foolcat.eve.evehelper.shared.result.Result;

import java.text.ParseException;

/**
 * @author Leojan
 * date 2022-05-19 11:02
 */
@Tag(name ="蓝图数据")
@RestController
@Slf4j
@RequestMapping("/*/blueprints")
@RequiredArgsConstructor
public class BlueprintsController {

    private final BlueprintsApplicationService blueprintsApplicationService;

    @Parameters({
            @Parameter(name = "type", description = "枚举值，人物：char; 公司：crop" ,required = true),
            @Parameter(name = "cid", description = "人物或军团的ID" ,required = true)
    })
    @Operation(summary = "蓝图数据-蓝图读取")
    @GetMapping("/{type}/{cid}")
    public Result addBlueprintsList(@PathVariable String type, @PathVariable String cid) throws ParseException {
//        blueprintsService.saveAndUpdateBlueprints(type, cid);
        return Result.success();
    }

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
    public Result getBlueprintsList(
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
        PageResultDTO<BlueprintsVO> result = blueprintsApplicationService.queryBlueprintsByPage(queryDTO);
        return Result.success(result);
    }
}
