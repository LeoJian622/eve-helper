package xyz.foolcat.eve.evehelper.interfaces.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.foolcat.eve.evehelper.application.service.MiningApplicationService;
import xyz.foolcat.eve.evehelper.shared.result.Result;

/**
 * @author Leojan
 * date 2022-07-06 21:58
 */

@Tag(name ="月矿采掘")
@RestController
@Slf4j
@RequestMapping("/mining")
@RequiredArgsConstructor
public class MiningController {

    private final MiningApplicationService miningApplicationService;

    @Parameters({
            @Parameter(name = "characterId", description = "人物ID" ,required = true),
            @Parameter(name = "observerId", description = "月矿堡ID" ,required = true)
    })
    @Operation(summary = "月矿采掘-开采明细同步",description = "调用服务器去获取ESI的数据")
    @PostMapping("/{characterId}/{observerId}")
    public Result syncMiningByObserver(@PathVariable Integer characterId, @PathVariable Long observerId) {
        miningApplicationService.syncMiningByObserver(characterId, observerId);
        return Result.success();
    }
}