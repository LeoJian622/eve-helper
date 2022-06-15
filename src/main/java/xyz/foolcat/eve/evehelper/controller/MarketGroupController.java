package xyz.foolcat.eve.evehelper.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.foolcat.eve.evehelper.common.result.R;
import xyz.foolcat.eve.evehelper.service.system.MarketGroupsService;

/**
 * @author Leojan
 * @date 2022-06-10 11:26
 */

@RestController
@Slf4j
@RequestMapping("/market/group")
@RequiredArgsConstructor
public class MarketGroupController {

    private final MarketGroupsService marketGroupsService;


    @GetMapping
    public R getMarketGrouptTree(){
        return R.success(marketGroupsService.selectMarketGroupTree());
    }
}
