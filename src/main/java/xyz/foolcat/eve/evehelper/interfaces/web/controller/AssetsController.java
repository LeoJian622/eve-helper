package xyz.foolcat.eve.evehelper.interfaces.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import xyz.foolcat.eve.evehelper.application.service.AssetsApplicationService;

import java.util.List;
import xyz.foolcat.eve.evehelper.domain.model.vo.AssetsAggregateVO;
import xyz.foolcat.eve.evehelper.domain.model.vo.AssetsVO;
import xyz.foolcat.eve.evehelper.shared.kernel.base.PageResult;
import xyz.foolcat.eve.evehelper.shared.result.Result;

/**
 * @author Leojan
 * date 2022-04-20 10:24
 */
@Tag(name = "游戏资产")
@RestController
@RequestMapping("/assets")
@RequiredArgsConstructor
public class AssetsController {

    private final AssetsApplicationService assetsApplicationService;

    @Parameters({
            @Parameter(name = "cid",description = "人物或军团的ID" ,required = true)
    })
    @Operation(summary = "游戏资产-资产同步",description = "调用服务器去获取ESI的数据")
    @PostMapping("/{cid}/sync")
    public Result syncAssets(@PathVariable Integer cid) {
        assetsApplicationService.syncAssets(cid);
        return Result.success();
    }

    @Parameters({
            @Parameter(name = "cid", description = "人物或军团的ID", required = true),
            @Parameter(name = "current", description = "页码"),
            @Parameter(name = "size", description = "每页行数")
    })
    @Operation(summary = "游戏资产-资产清单")
    @GetMapping("/{cid}")
    public Result<PageResult<AssetsVO>> getAssetsList(@PathVariable String cid,
                                                      @RequestParam(defaultValue = "0") Integer current,
                                                      @RequestParam(defaultValue = "30") Integer size) {
        return Result.success(assetsApplicationService.queryAssetsList(cid, current, size));
    }

    @Operation(summary = "游戏资产-多角色聚合", description = "按角色聚合当前登录用户全部角色的资产件数/价值/类目数")
    @GetMapping("/aggregate")
    public Result<List<AssetsAggregateVO>> getAssetsAggregate() {
        return Result.success(assetsApplicationService.aggregateAssetsByUser());
    }
}