package xyz.foolcat.eve.evehelper.interfaces.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.foolcat.eve.evehelper.application.service.WalletBalanceApplicationService;
import xyz.foolcat.eve.evehelper.domain.model.vo.UserWalletBalanceVO;
import xyz.foolcat.eve.evehelper.shared.result.Result;

/**
 * 用户钱包余额聚合控制器（018:按用户聚合余额查询）。
 *
 * <p>提供按用户维度聚合的钱包余额查询端点。从 JWT token 解析当前用户ID，
 * 返回该用户绑定的所有人物余额和关联军团余额（多角色权限合并）。</p>
 *
 * <p>端点无需 path 参数，不存在 IDOR 风险。</p>
 *
 * @author Leojian
 */
@Tag(name = "用户钱包余额聚合")
@RestController
@RequestMapping("/wallet/balance")
@RequiredArgsConstructor
public class WalletBalanceController {

    private final WalletBalanceApplicationService walletBalanceApplicationService;

    @Operation(summary = "获取当前用户所有人物和军团的余额",
            description = "从 JWT 解析当前用户ID，返回该用户绑定的所有人物余额和关联军团余额。军团余额按多角色权限合并（union by division）。")
    @GetMapping
    public Result<UserWalletBalanceVO> getUserWalletBalance() {
        // 018 IMPORTANT-4:userId 由应用服务自行从安全上下文取得,控制器不传递身份,
        // 避免信任边界外移(详见 WalletBalanceApplicationService.getUserWalletBalance 注释)
        return Result.success(walletBalanceApplicationService.getUserWalletBalance());
    }
}
