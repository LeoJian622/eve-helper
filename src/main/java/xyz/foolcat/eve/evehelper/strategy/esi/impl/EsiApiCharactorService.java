package xyz.foolcat.eve.evehelper.strategy.esi.impl;

import cn.hutool.json.JSONArray;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.client.EsiCharactorClient;
import xyz.foolcat.eve.evehelper.domain.system.Assets;
import xyz.foolcat.eve.evehelper.domain.system.Blueprints;
import xyz.foolcat.eve.evehelper.dto.esi.IndustryJobDTO;
import xyz.foolcat.eve.evehelper.service.esi.EsiApiService;
import xyz.foolcat.eve.evehelper.strategy.esi.EsiClientStrategy;

import java.util.List;

/**
 * 国服ESI角色相关接口封装
 *
 * @author Leojan
 * @date 2021-12-07 16:54
 */

@Slf4j
@Service("char")
@RequiredArgsConstructor
public class EsiApiCharactorService implements EsiClientStrategy {


    private final EsiCharactorClient esiCharactorClient;

    @Override
    public List<IndustryJobDTO> getJobList( String id,String accessToken) {
        return esiCharactorClient.getCharactorJobs(id, accessToken);
    }

    @Override
    public List<Assets> getAssetsList(String id, int page,String accessToken) {
        List<Assets> assetsList  = esiCharactorClient.getCharactorAssets(id,page, accessToken);
        return assetsList;
    }

    @Override
    public JSONArray getAssetsNamesList(String id, List item_ids, String accessToken) {
        return esiCharactorClient.getCharactorAssetsNames(id, item_ids, accessToken);
    }

    @Override
    public List<Blueprints> getBlueprintsList(String id, int page, String accessToken) {
        List<Blueprints> blueprintsList  = esiCharactorClient.getCharactorBlueprints(id,page, accessToken);
        return blueprintsList;
    }
}
