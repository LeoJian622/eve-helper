package xyz.foolcat.eve.evehelper.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.foolcat.eve.evehelper.common.result.Result;
import xyz.foolcat.eve.evehelper.service.system.MarketGroupsService;

/**
 * @author Leojan
 * date 2022-06-10 11:26
 */

@Tag(name ="市场物品组")
@RestController
@Slf4j
@RequestMapping("/market/group")
@RequiredArgsConstructor
public class MarketGroupController {

    private final MarketGroupsService marketGroupsService;

    @Parameter(name="parent", description = "分组的父节点ID",required = true)
    @Operation(summary = "市场物品分组-子分类查询")
    @GetMapping("/{parent}")
    public Result getMarketGrouptTree(@PathVariable Integer parent){

        return Result.success(marketGroupsService.selectMarketGroupByParent(parent)
        );
    }
}
