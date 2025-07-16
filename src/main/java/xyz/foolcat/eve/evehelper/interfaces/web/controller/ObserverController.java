package xyz.foolcat.eve.evehelper.interfaces.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.foolcat.eve.evehelper.domain.service.system.ObserverService;
import xyz.foolcat.eve.evehelper.shared.result.Result;

import java.text.ParseException;

/**
 * 采矿观察者清单对象
 *
 * @author Leojan
 * date 2022-07-06 22:26
 */

@Tag(name ="月矿采掘")
@RestController
@Slf4j
@RequestMapping("/*/observer")
@RequiredArgsConstructor
public class ObserverController {

        private final ObserverService observerService;

        @Parameter(name = "corporationId", description = "军团ID", required = true)
        @Operation(summary = "月矿采掘-月矿堡读取")
        @GetMapping("/{corporationId}")
        public Result saveObserverByCorporationId(@PathVariable Integer corporationId) throws ParseException {
                return Result.success();
        }

}
