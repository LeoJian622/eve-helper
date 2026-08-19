package xyz.foolcat.eve.evehelper.interfaces.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import xyz.foolcat.eve.evehelper.application.service.WalletOverviewApplicationService;
import xyz.foolcat.eve.evehelper.domain.model.vo.WalletOverviewVO;
import xyz.foolcat.eve.evehelper.shared.result.Result;

import java.util.Date;

/**
 * 人物/军团钱包总览控制器。
 * <p>人物 cid 为 path 变量,归属校验在应用服务内(accessGuard.requireOwnership)执行。
 * 人物总览 divisions 恒为 null(US2 军团总览才返回 divisions)。</p>
 *
 * @author Leojan
 */
@Tag(name = "人物/军团钱包总览")
@RestController
@RequestMapping("/wallet/overview")
@RequiredArgsConstructor
public class WalletOverviewController {

    private final WalletOverviewApplicationService walletOverviewApplicationService;

    @Parameters({
            @Parameter(name = "cid", description = "人物ID", required = true),
            @Parameter(name = "range", description = "预设时间范围: today/last7d/last30d/last90d/year;与 start/end 二选一,都空为全量"),
            @Parameter(name = "start", description = "起始日期(ISO, 如 2026-08-01),可空"),
            @Parameter(name = "end", description = "结束日期(ISO, 如 2026-08-03),可空")
    })
    @Operation(summary = "钱包总览-人物", description = "按人物返回当前余额/收支合计/净流/条数/统计截点及类目与时间趋势(时间过滤作用于收支/类目/趋势,不影响余额与截点)")
    @GetMapping("/{cid}")
    public Result<WalletOverviewVO> characterOverview(@PathVariable Integer cid,
                                                      @RequestParam(required = false) String range,
                                                      @RequestParam(required = false)
                                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date start,
                                                      @RequestParam(required = false)
                                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date end) {
        return Result.success(walletOverviewApplicationService.getCharacterOverview(cid, range, start, end));
    }
}