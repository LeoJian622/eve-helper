package xyz.foolcat.eve.evehelper.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.foolcat.eve.evehelper.common.result.Result;
import xyz.foolcat.eve.evehelper.dto.esi.IndustryJobDTO;
import xyz.foolcat.eve.evehelper.service.esi.EsiApiService;

import java.text.ParseException;
import java.util.List;

/**
 * 工业生产线
 *
 * @author Leojan
 * @date 2022-04-02 16:59
 */

@Tag(name ="工业制造")
@RestController
@Slf4j
@RequestMapping("/*/job")
@RequiredArgsConstructor
public class JobController {

    private final EsiApiService esiApiService;

    @Parameters({
            @Parameter(name = "type", description = "枚举值，人物：char; 军团：crop" ,required = true),
            @Parameter(name = "id", description = "人物或军团的ID" ,required = true)
    })
    @Operation(summary = "工业制造-制造线数据读取")
    @GetMapping("/{type}/{id}")
    public Result jobs(@PathVariable String type, @PathVariable String id) throws ParseException {
        List<IndustryJobDTO> jsonArray = esiApiService.getJobList(type,id);
        return Result.success(jsonArray);
    }
}
