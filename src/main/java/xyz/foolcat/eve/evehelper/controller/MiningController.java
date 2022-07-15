package xyz.foolcat.eve.evehelper.controller;

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

@RestController
@Slf4j
@RequestMapping("/mining")
@RequiredArgsConstructor
public class MiningController {

    private final MiningDetailService miningDetailService;

    @PutMapping("/{corporationId}")
    public R readMinigDetail(@PathVariable Long corporationId) {
        miningDetailService.saveAllObserverMining(corporationId);
        return R.success();
    }


    @PutMapping("/{corporationId}/{observerId}")
    public R readMinigDetail(@PathVariable Long corporationId, @PathVariable Long observerId) throws ParseException {
        miningDetailService.saveObserverMining(corporationId, observerId);
        return R.success();
    }

}
