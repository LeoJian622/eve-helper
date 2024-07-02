package xyz.foolcat.eve.evehelper.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import xyz.foolcat.eve.evehelper.common.result.Result;
import xyz.foolcat.eve.evehelper.service.system.BlueprintsService;
import xyz.foolcat.eve.evehelper.vo.BlueprintsVO;

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

    private final BlueprintsService blueprintsService;

    @Parameters({
            @Parameter(name = "type", description = "枚举值，人物：char; 公司：crop" ,required = true),
            @Parameter(name = "cid", description = "人物或军团的ID" ,required = true)
    })
    @Operation(summary = "蓝图数据-蓝图读取")
    @GetMapping("/{type}/{cid}")
    public Result addBlueprintsList(@PathVariable String type, @PathVariable String cid) throws ParseException {
        blueprintsService.saveAndUpdateBlueprints(type, cid);
        return Result.success();
    }

    @Parameters({
            @Parameter(name = "id", description = "人物或军团的ID", required = true),
            @Parameter(name = "current", description = "页码"),
            @Parameter(name = "size", description = "每页行数")
    })
    @Operation(summary = "蓝图数据-蓝图清单")
    @GetMapping("/{id}")
    public Result getBlueprintsList(@PathVariable String id, @RequestParam(defaultValue = "0") Integer current, @RequestParam(defaultValue = "30") Integer size)  {
        IPage<BlueprintsVO> page = new Page<>();
        page.setCurrent(current);
        page.setSize(size);
        page = blueprintsService.getBlueprintsListById(page,id);
        return Result.success(page);
    }
}
