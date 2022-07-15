package xyz.foolcat.eve.evehelper.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import xyz.foolcat.eve.evehelper.common.result.R;
import xyz.foolcat.eve.evehelper.service.system.BlueprintsService;
import xyz.foolcat.eve.evehelper.vo.AssetsVO;
import xyz.foolcat.eve.evehelper.vo.BlueprintsVO;

import java.text.ParseException;

/**
 * @author Leojan
 * @date 2022-05-19 11:02
 */
@RestController
@Slf4j
@RequestMapping("/blueprints")
@RequiredArgsConstructor
public class BlueprintsController {

    private final BlueprintsService blueprintsService;

    @GetMapping("/{type}/{id}")
    public R addBlueprintsList(@PathVariable String type, @PathVariable String id) throws ParseException {
        blueprintsService.saveAndUpdateBlueprints(type, id);
        return R.success();
    }

    @GetMapping("/{id}")
    public R getBlueprintsList(@PathVariable String id, @RequestParam(defaultValue = "0") Integer current, @RequestParam(defaultValue = "30") Integer size)  {
        IPage<BlueprintsVO> page = new Page<>();
        page.setCurrent(current);
        page.setSize(size);
        page = blueprintsService.getBlueprintsListById(page,id);
        return R.success(page);
    }
}
