package xyz.foolcat.eve.evehelper.interfaces.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.foolcat.eve.evehelper.application.service.JobApplicationService;
import xyz.foolcat.eve.evehelper.shared.result.Result;

/**
 * 工业生产线
 *
 * @author Leojan
 * date 2022-04-02 16:59
 */

@Tag(name = "工业制造")
@RestController
@Slf4j
@RequestMapping("/job")
@RequiredArgsConstructor
public class JobController {

    private final JobApplicationService jobApplicationService;

    @Parameters({
            @Parameter(name = "type", description = "枚举值，人物：char; 公司：crop", required = true),
            @Parameter(name = "id", description = "人物或军团的ID", required = true),
            @Parameter(name = "complete", description = "是否包含已完成任务", required = true)
    })
    @Operation(summary = "工业制造-制造线数据同步",description = "调用服务器去获取ESI的数据")
    @PostMapping("/{type}/{id}/{complete}")
    public Result syncJobs(@PathVariable String type, @PathVariable String complete, @PathVariable Integer id) {
        jobApplicationService.syncJobs(type, id, complete);
        return Result.success();
    }
}