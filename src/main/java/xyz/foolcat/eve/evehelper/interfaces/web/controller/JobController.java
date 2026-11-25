package xyz.foolcat.eve.evehelper.interfaces.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import xyz.foolcat.eve.evehelper.application.dto.response.IndustryJobPageResponse;
import xyz.foolcat.eve.evehelper.application.service.IndustryJobQueryApplicationService;
import xyz.foolcat.eve.evehelper.application.service.JobApplicationService;
import xyz.foolcat.eve.evehelper.shared.kernel.base.PageQuery;
import xyz.foolcat.eve.evehelper.shared.result.Result;

import java.text.ParseException;

/**
 * 工业生产线
 *
 * @author Leojan
 * date 2022-04-02 16:59
 */

@Tag(name = "工业制造")
@RestController
@RequestMapping("/job")
@RequiredArgsConstructor
@Validated
public class JobController {

    private final JobApplicationService jobApplicationService;
    private final IndustryJobQueryApplicationService industryJobQueryApplicationService;

    @Parameters({
            @Parameter(name = "type", description = "枚举值，人物：char; 公司：crop", required = true),
            @Parameter(name = "id", description = "人物或军团的ID", required = true),
            @Parameter(name = "complete", description = "是否包含已完成任务", required = true),
            @Parameter(name = "forceRefresh", description = "是否强制刷新(忽略缓存,默认false)", example = "false")
    })
    @Operation(summary = "工业制造-制造线数据同步",description = "调用服务器去获取ESI的数据")
    @PostMapping("/{type}/{id}/{complete}")
    public Result syncJobs(@PathVariable String type, @PathVariable String complete, @PathVariable Integer id,
                           @RequestParam(defaultValue = "false") boolean forceRefresh) {
        jobApplicationService.syncJobs(type, id, complete, forceRefresh);
        return Result.success();
    }

    @Parameters({
            @Parameter(name = "type", description = "枚举值，人物：char; 军团：corp", required = true),
            @Parameter(name = "id", description = "角色ID(军团场景下 corpId 由 account 派生)", required = true),
            @Parameter(name = "status", description = "状态过滤(active/cancelled/delivered/paused/ready/reverted);不传则不过滤"),
            @Parameter(name = "current", description = "页码"),
            @Parameter(name = "size", description = "每页行数"),
            @Parameter(name = "forceRefresh", description = "是否强制刷新(忽略缓存,默认false)", example = "false")
    })
    @Operation(summary = "工业制造-直查工业订单", description = "直查 ESI 获取工业订单,不经 QQ 同步链路;corp 需 Director/Accountant 角色")
    @GetMapping("/{type}/{id}")
    public Result<IndustryJobPageResponse> getJobs(@PathVariable String type,
                                                   @PathVariable Integer id,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(defaultValue = "1") @Min(1) Integer current,
                                                   @RequestParam(defaultValue = "20") @Min(1) @Max(1000) Integer size,
                                                   @RequestParam(defaultValue = "false") boolean forceRefresh) throws ParseException {
        PageQuery pageQuery = PageQuery.builder().current(current).size(size).build();
        return Result.success(industryJobQueryApplicationService.queryJobs(type, id, status, pageQuery, forceRefresh));
    }
}