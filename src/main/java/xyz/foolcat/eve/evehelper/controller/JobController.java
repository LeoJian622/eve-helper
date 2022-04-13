package xyz.foolcat.eve.evehelper.controller;

import cn.hutool.json.JSONArray;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import xyz.foolcat.eve.evehelper.common.result.R;
import xyz.foolcat.eve.evehelper.domain.system.SysUser;
import xyz.foolcat.eve.evehelper.dto.system.UserDTO;
import xyz.foolcat.eve.evehelper.service.esi.EsiApiService;

import javax.annotation.Resource;
import java.text.ParseException;

/**
 * 工业生产线
 *
 * @author Leojan
 * @date 2022-04-02 16:59
 */

@RestController
@Slf4j
@RequestMapping("/job")
@RequiredArgsConstructor
public class JobController {

    @Resource
    private EsiApiService esiApiService;

    @GetMapping("/{type}/{id}")
    public R jobs(@PathVariable String type, @PathVariable String id) throws ParseException {
        JSONArray jsonArray = esiApiService.getJobList(type,id);
        return R.success(jsonArray);
    }
}
