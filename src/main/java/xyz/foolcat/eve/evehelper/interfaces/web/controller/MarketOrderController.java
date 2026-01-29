package xyz.foolcat.eve.evehelper.interfaces.web.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Leojan
 * date 2022-04-20 10:24
 */

@Tag(name ="市场订单")
@RestController
@Slf4j
@RequestMapping("/*/market/order")
@RequiredArgsConstructor
public class MarketOrderController {

}
