package xyz.foolcat.eve.evehelper.strategy.esi.impl;

import cn.hutool.json.JSONArray;
import com.dtflys.forest.annotation.Body;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.client.EsiCorporationClient;
import xyz.foolcat.eve.evehelper.domain.system.Assets;
import xyz.foolcat.eve.evehelper.domain.system.Blueprints;
import xyz.foolcat.eve.evehelper.dto.esi.IndustryJobDTO;
import xyz.foolcat.eve.evehelper.service.esi.EsiApiService;
import xyz.foolcat.eve.evehelper.strategy.esi.EsiClientStrategy;

import java.util.List;

/**
 * 国服ESI公司相关接口封装
 *
 * @author Leojan
 * @date 2021-12-07 16:54
 */

@Slf4j
@Service("crop")
@RequiredArgsConstructor
public class EsiCorporationApiService implements EsiClientStrategy {


    private final EsiCorporationClient esiCorporationClient;

    @Override
    public List<IndustryJobDTO> getJobList(String id, String accessToken) {
        return esiCorporationClient.getCorporationJobs(id, accessToken);
    }

    @Override
    public List<Assets> getAssetsList(String id,int page, String accessToken) {
        List<Assets> assetsList = esiCorporationClient.getCorporationAssets(id,page, accessToken);
        return assetsList;
    }

    @Override
    public JSONArray getAssetsNamesList(String id, List itemIds, String accessToken) {
        return esiCorporationClient.getCorporationAssetsNames(id, itemIds, accessToken);
    }

    @Override
    public List<Blueprints> getBlueprintsList(String id, int page, String accessToken) {
        List<Blueprints> blueprintsList = esiCorporationClient.getCharactorBlueprints(id,page, accessToken);
        return blueprintsList;
    }

}
