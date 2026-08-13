package xyz.foolcat.eve.evehelper.interfaces.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;
import xyz.foolcat.eve.evehelper.application.dto.UserAccountDTO;
import xyz.foolcat.eve.evehelper.application.dto.response.UserDTO;
import xyz.foolcat.eve.evehelper.application.service.UserApplicationService;
import xyz.foolcat.eve.evehelper.shared.result.Result;

import java.util.List;

/**
 * @author Leojan
 * date 2022-03-01 14:36
 */

@Tag(name ="用户")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserApplicationService userApplicationService;

    @Operation(summary = "用户服务-用户注册")
    @Parameter(name = "user", description = "用户对象",required = true)
    @PostMapping
    public Result<String> addUser(@RequestBody UserDTO user) {
        userApplicationService.register(user);
        return Result.success("注册成功");
    }

    @Operation(summary = "用户服务-用户绑定的角色列表(含 ESI 授权状态)")
    @Parameter(name = "userId", description = "用户ID",required = true)
    @GetMapping("/{userId}")
    public Result<List<UserAccountDTO>> queryUserAccounts(@PathVariable Integer userId) {
        return Result.success(userApplicationService.queryAccountListWithAuthStatus(userId));
    }
}