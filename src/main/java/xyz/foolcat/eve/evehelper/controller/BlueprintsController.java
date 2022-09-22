package xyz.foolcat.eve.evehelper.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import xyz.foolcat.eve.evehelper.common.result.R;
import xyz.foolcat.eve.evehelper.service.system.BlueprintsService;
import xyz.foolcat.eve.evehelper.vo.BlueprintsVO;

import java.text.ParseException;

/**
 * @author Leojan
 * @date 2022-05-19 11:02
 */
@Api(tags = "蓝图数据")
@RestController
@Slf4j
@RequestMapping("/blueprints")
@RequiredArgsConstructor
public class BlueprintsController {

    private final BlueprintsService blueprintsService;

    @ApiImplicitParams({
            @ApiImplicitParam(name = "type",value = "枚举值，角色：char; 军团：crop" ,required = true),
            @ApiImplicitParam(name = "id",value = "角色或军团的ID" ,required = true)
    })
    @ApiOperation(value = "蓝图数据-蓝图读取")
    @GetMapping("/{type}/{id}")
    public R addBlueprintsList(@PathVariable String type, @PathVariable String id) throws ParseException {
        blueprintsService.saveAndUpdateBlueprints(type, id);
        return R.success();
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "角色或军团的ID", required = true),
            @ApiImplicitParam(name = "current", value = "页码"),
            @ApiImplicitParam(name = "size", value = "每页行数")
    })
    @ApiOperation(value = "蓝图数据-蓝图清单")
    @GetMapping("/{id}")
    public R<IPage<BlueprintsVO>> getBlueprintsList(@PathVariable String id, @RequestParam(defaultValue = "0") Integer current, @RequestParam(defaultValue = "30") Integer size)  {
        IPage<BlueprintsVO> page = new Page<>();
        page.setCurrent(current);
        page.setSize(size);
        page = blueprintsService.getBlueprintsListById(page,id);
        return R.success(page);
    }
}
