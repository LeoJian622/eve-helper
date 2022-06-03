package xyz.foolcat.eve.evehelper.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import xyz.foolcat.eve.evehelper.common.result.R;
import xyz.foolcat.eve.evehelper.domain.system.Assets;
import xyz.foolcat.eve.evehelper.service.system.AssetsService;
import xyz.foolcat.eve.evehelper.vo.AssetsVO;

import java.text.ParseException;
import java.util.List;

/**
 * @author Leojan
 * @date 2022-04-20 10:24
 */

@RestController
@Slf4j
@RequestMapping("/assets")
@RequiredArgsConstructor
public class AssetsController {

    private final AssetsService assetsService;

    @GetMapping("/{type}/{id}")
    public R addAssetsList(@PathVariable String type, @PathVariable String id) throws ParseException {
        assetsService.saveAndUpdateAssets(type, id);
        return R.success();
    }

    @GetMapping("/{id}")
    public R getAssetsList(@PathVariable String id, @RequestParam(defaultValue = "0") Integer current, @RequestParam(defaultValue = "30") Integer size){
        IPage<AssetsVO> page = new Page<>();
        page.setCurrent(current);
        page.setSize(size);
        page = assetsService.getAssertsListById(page,id);
        return R.success(page);
    }


}
