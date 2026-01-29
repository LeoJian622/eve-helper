package xyz.foolcat.eve.evehelper.interfaces.web.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Assets;
import xyz.foolcat.eve.evehelper.domain.service.system.AssetsService;
import xyz.foolcat.eve.evehelper.shared.result.Result;

import java.text.ParseException;

/**
 * @author Leojan
 * date 2022-04-20 10:24
 */
@Tag(name = "游戏资产")
@RestController
@Slf4j
@RequestMapping("/assert")
@RequiredArgsConstructor
public class AssetsController {

    private final AssetsService assetsService;

    @Parameters({
            @Parameter(name = "cid",description = "人物或军团的ID" ,required = true)
    })
    @Operation(summary = "游戏资产-资产读取")
    @PutMapping("/{cid}")
    public Result addAssertsList( @PathVariable Integer cid) throws ParseException {
        assetsService.saveAndUpdateAsserts(cid);
        return Result.success();
    }

    @Parameters({
            @Parameter(name = "id", description = "人物或军团的ID", required = true),
            @Parameter(name = "current", description = "页码"),
            @Parameter(name = "size", description = "每页行数")
    })
    @Operation(summary = "游戏资产-资产清单")
    @GetMapping("/{cid}")
    public Result getAssetsList(@PathVariable String cid, @RequestParam(defaultValue = "0") Integer current, @RequestParam(defaultValue = "30") Integer size){
        IPage<Assets> page = new Page<>();
        page.setCurrent(current);
        page.setSize(size);
        page.setRecords( assetsService.getAssertsListById(cid,current,size));
        return Result.success(page);
    }


}
