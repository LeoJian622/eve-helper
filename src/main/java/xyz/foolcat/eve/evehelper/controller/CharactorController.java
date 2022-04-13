package xyz.foolcat.eve.evehelper.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import xyz.foolcat.eve.evehelper.common.result.R;
import xyz.foolcat.eve.evehelper.service.esi.EsiApiService;

import java.text.ParseException;

/**
 * @author Leojan
 * @date 2021-12-10 17:02
 */

@RestController
@Slf4j
@RequestMapping("/charactor")
@RequiredArgsConstructor
public class CharactorController {

    private final EsiApiService esiApiService;

    @PostMapping("/{type}/{code}")
    public R addCharactorAuth(@PathVariable String type, @PathVariable String code) throws ParseException {
        esiApiService.getAccessToken(type, code);
        return R.success();
    }
}
