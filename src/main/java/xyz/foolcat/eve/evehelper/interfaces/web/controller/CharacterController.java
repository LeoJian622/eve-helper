package xyz.foolcat.eve.evehelper.interfaces.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import xyz.foolcat.eve.evehelper.application.dto.response.AuthorizeUrlResponse;
import xyz.foolcat.eve.evehelper.application.service.CharacterApplicationService;
import xyz.foolcat.eve.evehelper.shared.result.Result;
import xyz.foolcat.eve.evehelper.domain.util.UserUtil;

/**
 * @author Leojan
 * date 2021-12-10 17:02
 */

@Tag(name ="人物")
@RestController
@RequestMapping("/character")
@RequiredArgsConstructor
public class CharacterController {

    private final CharacterApplicationService characterApplicationService;

    @Operation(summary = "角色服务-获取 ESI OAuth 授权 URL")
    @GetMapping("/auth-url")
    public Result<AuthorizeUrlResponse> authUrl(@RequestParam(required = false) String server) {
        return Result.success(characterApplicationService.getAuthorizeUrl(server));
    }

    @Parameters({
            @Parameter(name = "code", description = "授权code" ,required = true)
    })
    @Operation(summary = "角色服务- 角色授权绑定")
    @PostMapping("/{code}")
    public Result addCharacterAuth( @PathVariable String code) {
        characterApplicationService.authorizeCharacter( code, UserUtil.getUserId());
        return Result.success();
    }
}