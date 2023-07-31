package xyz.foolcat.eve.evehelper.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.foolcat.eve.evehelper.common.result.Result;
import xyz.foolcat.eve.evehelper.service.esi.EsiApiService;

import java.text.ParseException;

/**
 * @author Leojan
 * @date 2021-12-10 17:02
 */

@Tag(name ="角色")
@RestController
@Slf4j
@RequestMapping("/character")
@RequiredArgsConstructor
public class CharacterController {

    private final EsiApiService esiApiService;

    @Parameters({
            @Parameter(name = "type", description = "枚举值，角色：char; 军团：crop; 技能：skill; 基础：normal" ,required = true),
            @Parameter(name = "code", description = "授权code" ,required = true)
    })
    @PostMapping("/{type}/{code}")
    public Result addCharacterAuth(@PathVariable String type, @PathVariable String code) throws ParseException {
        esiApiService.getAccessToken(type, code);
        return Result.success();
    }
}
