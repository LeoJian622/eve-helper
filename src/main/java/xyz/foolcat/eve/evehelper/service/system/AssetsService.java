package xyz.foolcat.eve.evehelper.service.system;

import cn.hutool.json.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.injector.methods.SelectPage;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dtflys.forest.exceptions.ForestNetworkException;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.util.List;
import java.util.stream.Collectors;

import xyz.foolcat.eve.evehelper.domain.system.Assets;
import xyz.foolcat.eve.evehelper.mapper.system.AssetsMapper;
import xyz.foolcat.eve.evehelper.service.esi.EsiApiService;
import xyz.foolcat.eve.evehelper.vo.AssetsVO;

@Service
public class AssetsService extends ServiceImpl<AssetsMapper, Assets> {

    @Resource
    private EsiApiService esiApiService;

    public int batchInsert(List<Assets> list) {
        return baseMapper.batchInsert(list);
    }

    /**
     * 与ESI资产数据进行同步并返回
     *
     * @param type
     * @param id
     * @return
     * @throws ParseException
     */
    public void saveAndUpdateAssets(String type, String id) throws ParseException {
        List<Assets> assetsList;
        int page = 1;
        do {
            try {

                assetsList = esiApiService.getAssetsList(type, page, id);
            }catch (ForestNetworkException e){
                if (e.getMessage().indexOf("Requested page does not exist") > 0) {
                    break;
                } else {
                    log.error("ESI请求失败", e);
                    break;
                }
            }
            Long ownId = Long.valueOf(id);
            assetsList.forEach(assets -> {
                assets.setOwnerId(ownId);
            });
            this.saveOrUpdateBatch(assetsList);
            page++;
        } while (assetsList.size() != 0);
    }

    /**
     * 获取资产列表
     *
     * @param id
     * @return
     */
    public IPage<AssetsVO> getAssertsListById(IPage<AssetsVO> page, String id) {
        return baseMapper.selectAssetsInvtypeUniverse(page, id);
    }
}

