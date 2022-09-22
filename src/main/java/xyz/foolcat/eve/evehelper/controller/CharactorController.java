package xyz.foolcat.eve.evehelper.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
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

@Api(tags = "角色")
@RestController
@Slf4j
@RequestMapping("/charactor")
@RequiredArgsConstructor
public class CharactorController {

    private final EsiApiService esiApiService;

    @ApiImplicitParams({
            @ApiImplicitParam(name = "type",value = "枚举值，角色：char; 军团：crop; 技能：skill; 基础：normal" ,required = true),
            @ApiImplicitParam(name = "code",value = "授权code" ,required = true)
    })
    @PostMapping("/{type}/{code}")
    public R addCharactorAuth(@PathVariable String type, @PathVariable String code) throws ParseException {
        esiApiService.getAccessToken(type, code);
        return R.success();
    }
}
