package xyz.foolcat.eve.evehelper.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.foolcat.eve.evehelper.common.result.R;
import xyz.foolcat.eve.evehelper.service.system.MarketGroupsService;
import xyz.foolcat.eve.evehelper.vo.MarketGroupsTreeVO;

import java.util.List;

/**
 * @author Leojan
 * @date 2022-06-10 11:26
 */

@Api(tags = "市场物品组")
@RestController
@Slf4j
@RequestMapping("/market/group")
@RequiredArgsConstructor
public class MarketGroupController {

    private final MarketGroupsService marketGroupsService;

    @ApiImplicitParam(name="parent", value = "分组的父节点ID",required = true)
    @ApiOperation(value = "市场物品分组-子分类查询")
    @GetMapping("/{parent}")
    public R<List<MarketGroupsTreeVO>> getMarketGrouptTree(@PathVariable Integer parent){

        return R.success(marketGroupsService.selectMarketGroupByParent(parent)
        );
    }
}
