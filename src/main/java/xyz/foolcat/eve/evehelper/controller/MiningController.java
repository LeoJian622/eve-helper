package xyz.foolcat.eve.evehelper.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.foolcat.eve.evehelper.common.result.R;
import xyz.foolcat.eve.evehelper.service.system.MiningDetailService;

import java.text.ParseException;

/**
 * @author Leojan
 * @date 2022-07-06 21:58
 */

@Api(tags = "月矿采掘")
@RestController
@Slf4j
@RequestMapping("/mining")
@RequiredArgsConstructor
public class MiningController {

    private final MiningDetailService miningDetailService;

    @ApiImplicitParams({
            @ApiImplicitParam(name = "corporationId",value = "军团ID" ,required = true),
            @ApiImplicitParam(name = "observerId",value = "月矿堡ID" ,required = true)
    })
    @ApiOperation(value = "月矿采掘-开采明细读取")
    @PutMapping("/{corporationId}/{observerId}")
    public R readMinigDetail(@PathVariable Long corporationId, @PathVariable Long observerId) throws ParseException {
        miningDetailService.saveObserverMining(corporationId, observerId);
        return R.success();
    }

}
