package xyz.foolcat.eve.evehelper.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.foolcat.eve.evehelper.common.result.R;
import xyz.foolcat.eve.evehelper.converter.UserConverter;
import xyz.foolcat.eve.evehelper.domain.system.SysUser;
import xyz.foolcat.eve.evehelper.dto.system.UserDTO;
import xyz.foolcat.eve.evehelper.service.system.SysUserService;

import javax.annotation.Resource;

/**
 * @author Leojan
 * @date 2022-03-01 14:36
 */

@RestController
@Slf4j
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    @Resource
    private SysUserService sysUserService;

    @Resource
    private UserConverter userConverter;

    @Resource
    private PasswordEncoder passwordEncoder;

    @PostMapping
    public R addUser(@RequestBody UserDTO user) {

        SysUser sysUser = userConverter.userDto2SysUser(user);

        if (sysUser == null) {
            return R.failed("参数错误");
        }
        sysUser.setPassword(passwordEncoder.encode(sysUser.getPassword()));
        sysUserService.save(sysUser);
        return R.success("注册成功");
    }
}
