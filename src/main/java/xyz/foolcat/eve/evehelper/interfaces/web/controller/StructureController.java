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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import xyz.foolcat.eve.evehelper.application.dto.request.StructureFuelQuery;
import xyz.foolcat.eve.evehelper.application.dto.request.StructureQuery;
import xyz.foolcat.eve.evehelper.application.dto.response.StructureDetailVO;
import xyz.foolcat.eve.evehelper.application.dto.response.StructureFuelVO;
import xyz.foolcat.eve.evehelper.application.dto.response.StructureListItemVO;
import xyz.foolcat.eve.evehelper.application.dto.response.StructureServiceVO;
import xyz.foolcat.eve.evehelper.application.dto.response.StructureSummaryVO;
import xyz.foolcat.eve.evehelper.application.dto.response.StructureTimerVO;
import xyz.foolcat.eve.evehelper.application.service.StructureQueryApplicationService;
import xyz.foolcat.eve.evehelper.shared.kernel.base.PageResult;
import xyz.foolcat.eve.evehelper.shared.result.Result;

import java.util.List;

/**
 * 建筑表只读查询接口
 *
 * @author Leojan
 */
@Tag(name = "建筑数据")
@RestController
@Validated
@RequestMapping("/structures")
@RequiredArgsConstructor
public class StructureController {

    private final StructureQueryApplicationService structureQueryApplicationService;

    @Parameters({
            @Parameter(name = "corpId", description = "军团ID", required = true),
            @Parameter(name = "current", description = "页码"),
            @Parameter(name = "size", description = "每页行数"),
            @Parameter(name = "sortField", description = "排序字段:structureId, name, state, fuelExpires"),
            @Parameter(name = "sortOrder", description = "排序方向:asc, desc"),
            @Parameter(name = "name", description = "建筑名称(模糊查询)"),
            @Parameter(name = "state", description = "状态筛选"),
            @Parameter(name = "lowFuelOnly", description = "仅缺油(默认 false)")
    })
    @Operation(summary = "建筑数据-建筑清单")
    @GetMapping("/{corpId}")
    public Result<PageResult<StructureListItemVO>> getStructuresList(
            @PathVariable String corpId,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码不能小于1") Integer current,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "每页大小不能小于1")
            @Max(value = 1000, message = "每页大小不能超过1000") Integer size,
            @RequestParam(required = false) String sortField,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String state,
            @RequestParam(required = false, defaultValue = "false") Boolean lowFuelOnly) {

        StructureQuery queryDTO = StructureQuery.builder()
                .corporationId(corpId)
                .current(current)
                .size(size)
                .sortField(sortField)
                .sortOrder(sortOrder)
                .name(name)
                .state(state)
                .lowFuelOnly(lowFuelOnly)
                .build();

        return Result.success(structureQueryApplicationService.queryStructuresByPage(queryDTO));
    }

    @Operation(summary = "建筑数据-建筑详情")
    @GetMapping("/{corpId}/detail/{structureId}")
    public Result<StructureDetailVO> getStructureDetail(
            @PathVariable String corpId,
            @PathVariable Long structureId) {
        return Result.success(structureQueryApplicationService.queryDetailById(corpId, structureId));
    }

    @Operation(summary = "建筑数据-燃料预警")
    @Parameters({
            @Parameter(name = "corpId", description = "军团ID", required = true),
            @Parameter(name = "hours", description = "预警时长(小时,1~720,默认72)")
    })
    @GetMapping("/{corpId}/fuel")
    public Result<List<StructureFuelVO>> getStructureFuel(
            @PathVariable String corpId,
            @RequestParam(defaultValue = "72")
            @Min(value = 1, message = "预警时长不能小于1小时")
            @Max(value = 720, message = "预警时长不能超过720小时") Integer hours) {
        StructureFuelQuery query = StructureFuelQuery.builder()
                .corporationId(corpId)
                .hours(hours)
                .build();
        return Result.success(structureQueryApplicationService.queryFuelExpiring(query));
    }

    @Operation(summary = "建筑数据-服务状态")
    @GetMapping("/{corpId}/detail/{structureId}/services")
    public Result<StructureServiceVO> getStructureServices(
            @PathVariable String corpId,
            @PathVariable Long structureId) {
        return Result.success(structureQueryApplicationService.queryServices(corpId, structureId));
    }

    @Operation(summary = "建筑数据-统计概览")
    @GetMapping("/{corpId}/stats")
    public Result<StructureSummaryVO> getStructureStats(@PathVariable String corpId) {
        return Result.success(structureQueryApplicationService.querySummary(corpId));
    }

    @Operation(summary = "建筑数据-增强/解锚提醒")
    @GetMapping("/{corpId}/timers")
    public Result<List<StructureTimerVO>> getStructureTimers(@PathVariable String corpId) {
        return Result.success(structureQueryApplicationService.queryTimers(corpId));
    }
}
