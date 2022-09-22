package xyz.foolcat.eve.evehelper.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.foolcat.eve.evehelper.common.result.R;
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

@Api(tags = "工业制造")
@RestController
@Slf4j
@RequestMapping("/job")
@RequiredArgsConstructor
public class JobController {

    private final EsiApiService esiApiService;

    @ApiImplicitParams({
            @ApiImplicitParam(name = "type",value = "枚举值，角色：char; 军团：crop" ,required = true),
            @ApiImplicitParam(name = "id",value = "角色或军团的ID" ,required = true)
    })
    @ApiOperation(value = "工业制造-制造线数据读取")
    @GetMapping("/{type}/{id}")
    public R<List<IndustryJobDTO>> jobs(@PathVariable String type, @PathVariable String id) throws ParseException {
        List<IndustryJobDTO> jsonArray = esiApiService.getJobList(type,id);
        return R.success(jsonArray);
    }
}
