package xyz.foolcat.eve.evehelper.interfaces.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import xyz.foolcat.eve.evehelper.application.assembler.system.EveAccountAssembler;
import xyz.foolcat.eve.evehelper.application.assembler.system.SysUserAssembler;
import xyz.foolcat.eve.evehelper.application.dto.UserAccountDTO;
import xyz.foolcat.eve.evehelper.application.dto.response.UserDTO;
import xyz.foolcat.eve.evehelper.application.service.UserApplicationService;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysUser;
import xyz.foolcat.eve.evehelper.domain.service.system.SysUserService;
import xyz.foolcat.eve.evehelper.shared.result.Result;

import java.util.List;

/**
 * @author Leojan
 * date 2022-03-01 14:36
 */

@Tag(name ="用户")
@RestController
@Slf4j
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final SysUserService sysUserService;

    private final SysUserAssembler userAssembler;

    private final EveAccountAssembler eveAccountAssembler;

    private final PasswordEncoder passwordEncoder;

    private final UserApplicationService userApplicationService;

    @Parameter(name = "user", description = "用户对象",required = true)
    @Operation(summary = "用户服务-用户注册")
    @PostMapping
    public Result<String> addUser(@RequestBody UserDTO user) {

        SysUser sysUser = userAssembler.userDto2SysUser(user);

        if (sysUser == null) {
            return Result.failed("参数错误");
        }
        sysUser.setPassword(passwordEncoder.encode(sysUser.getPassword()));
        sysUserService.insert(sysUser);
        return Result.success("注册成功");
    }

    @Parameter(name = "userId", description = "用户ID",required = true)
    @Operation(summary = "用户服务-用户绑定的角色列表")
    @GetMapping("/{userId}")
    public Result<List<UserAccountDTO>> addUser(@PathVariable Integer userId) {
        return Result.success(eveAccountAssembler.domain2UserAccountTO(userApplicationService.queryAccountList(userId)));
    }
}
