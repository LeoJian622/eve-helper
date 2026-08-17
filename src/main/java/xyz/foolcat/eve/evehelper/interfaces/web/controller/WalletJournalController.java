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
import xyz.foolcat.eve.evehelper.application.dto.response.WalletJournalVO;
import xyz.foolcat.eve.evehelper.application.service.WalletJournalApplicationService;
import xyz.foolcat.eve.evehelper.shared.kernel.base.PageResult;
import xyz.foolcat.eve.evehelper.shared.result.Result;

/**
 * 人物钱包流水控制器。
 * <p>cid 为 path 变量,归属校验在应用服务内(accessGuard.requireOwnership)执行。</p>
 *
 * @author Leojan
 */
@Tag(name = "人物钱包流水")
@RestController
@RequestMapping("/wallet/journal")
@RequiredArgsConstructor
public class WalletJournalController {

    private final WalletJournalApplicationService walletJournalApplicationService;

    @Parameters({
            @Parameter(name = "cid", description = "人物ID", required = true)
    })
    @Operation(summary = "钱包流水-同步", description = "调用服务器去获取ESI的钱包流水数据")
    @PostMapping("/{cid}/sync")
    public Result<Void> syncCharacterJournal(@PathVariable Integer cid) {
        walletJournalApplicationService.syncCharacterJournal(cid);
        return Result.success();
    }

    @Parameters({
            @Parameter(name = "cid", description = "人物ID", required = true),
            @Parameter(name = "current", description = "页码"),
            @Parameter(name = "size", description = "每页行数")
    })
    @Operation(summary = "钱包流水-分页查询", description = "按时间倒序分页返回人物钱包流水")
    @GetMapping("/{cid}")
    public Result<PageResult<WalletJournalVO>> queryPage(@PathVariable Integer cid,
                                                         @RequestParam(defaultValue = "1") Integer current,
                                                         @RequestParam(defaultValue = "20") Integer size) {
        return Result.success(walletJournalApplicationService.queryPage(cid, current, size));
    }
}