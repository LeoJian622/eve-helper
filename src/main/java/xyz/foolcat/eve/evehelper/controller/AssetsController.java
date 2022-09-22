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
import xyz.foolcat.eve.evehelper.service.system.AssetsService;
import xyz.foolcat.eve.evehelper.vo.AssetsVO;

import java.text.ParseException;

/**
 * @author Leojan
 * @date 2022-04-20 10:24
 */
@Api(tags = "游戏资产")
@RestController
@Slf4j
@RequestMapping("/assets")
@RequiredArgsConstructor
public class AssetsController {

    private final AssetsService assetsService;

    @ApiImplicitParams({
            @ApiImplicitParam(name = "type",value = "枚举值，角色：char; 军团：crop" ,required = true),
            @ApiImplicitParam(name = "id",value = "角色或军团的ID" ,required = true)
    })
    @ApiOperation(value = "游戏资产-资产读取")
    @GetMapping("/{type}/{id}")
    public R addAssetsList(@PathVariable String type, @PathVariable String id) throws ParseException {
        assetsService.saveAndUpdateAssets(type, id);
        return R.success();
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "角色或军团的ID", required = true),
            @ApiImplicitParam(name = "current", value = "页码"),
            @ApiImplicitParam(name = "size", value = "每页行数")
    })
    @ApiOperation(value = "游戏资产-资产清单")
    @GetMapping("/{id}")
    public R<IPage<AssetsVO>> getAssetsList(@PathVariable String id, @RequestParam(defaultValue = "0") Integer current, @RequestParam(defaultValue = "30") Integer size){
        IPage<AssetsVO> page = new Page<>();
        page.setCurrent(current);
        page.setSize(size);
        page = assetsService.getAssertsListById(page,id);
        return R.success(page);
    }


}
