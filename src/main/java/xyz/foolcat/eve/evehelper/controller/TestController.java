package xyz.foolcat.eve.evehelper.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Leojan
 * @date 2021-12-23 9:23
 */

@RestController
@Slf4j
@RequiredArgsConstructor
public class TestController {

    @GetMapping("/hello")
    public String test() {
        return "hello word";
    }
}
