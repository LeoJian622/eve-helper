package xyz.foolcat.eve.evehelper.interfaces.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import xyz.foolcat.eve.evehelper.application.dto.request.RefreshTokenRequest;
import xyz.foolcat.eve.evehelper.application.dto.response.TokenPair;
import xyz.foolcat.eve.evehelper.application.service.AuthApplicationService;
import xyz.foolcat.eve.evehelper.shared.result.Result;

/**
 * 认证资源控制器 (RESTful)
 * 处理登出、token刷新等认证相关操作
 *
 * @author Leojan
 * date 2026-01-30
 */
@Tag(name = "认证")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@Validated
public class AuthController {

    private final AuthApplicationService authApplicationService;

    /**
     * 用户登出 (撤销token)
     * RESTful: DELETE /auth/sessions (删除当前会话)
     */
    @Operation(summary = "用户登出")
    @DeleteMapping("/sessions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Result logout(HttpServletRequest request) {
        authApplicationService.logout(request);
        return Result.success("登出成功");
    }

    /**
     * 刷新Access Token
     * RESTful: POST /auth/tokens (创建新的token)
     */
    @Operation(summary = "刷新Access Token")
    @PostMapping("/tokens")
    public Result<TokenPair> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return Result.success(authApplicationService.refreshToken(request));
    }
}