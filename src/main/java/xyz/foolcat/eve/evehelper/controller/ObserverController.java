package xyz.foolcat.eve.evehelper.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.foolcat.eve.evehelper.common.result.R;
import xyz.foolcat.eve.evehelper.service.system.ObserverService;

import java.text.ParseException;

/**
 * 采矿观察者清单对象
 *
 * @author Leojan
 * @date 2022-07-06 22:26
 */

@Api(tags = "月矿采掘")
@RestController
@Slf4j
@RequestMapping("/observer")
@RequiredArgsConstructor
public class ObserverController {

        private final ObserverService observerService;

        @ApiImplicitParam(name = "corporationId", value = "军团ID", required = true)
        @ApiOperation(value = "月矿采掘-月矿堡读取")
        @PutMapping("/{corporationId}")
        public R saveObserverByCorporationId(@PathVariable Long corporationId) throws ParseException {
                observerService.saveObserverFromEsi(corporationId);
                return R.success();
        }

}
