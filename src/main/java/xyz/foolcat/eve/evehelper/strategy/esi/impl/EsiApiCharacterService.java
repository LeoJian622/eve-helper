package xyz.foolcat.eve.evehelper.strategy.esi.impl;

import cn.hutool.json.JSONArray;
import com.dtflys.forest.http.ForestResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.client.EsiCharacterClient;
import xyz.foolcat.eve.evehelper.domain.system.Assets;
import xyz.foolcat.eve.evehelper.domain.system.Blueprints;
import xyz.foolcat.eve.evehelper.dto.esi.IndustryJobDTO;
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
public class EsiApiCharacterService implements EsiClientStrategy {


    private final EsiCharacterClient esiCharacterClient;

    @Override
    public List<IndustryJobDTO> getJobList( String id,String accessToken) {
        return esiCharacterClient.getCharacterJobs(id, accessToken);
    }

    @Override
    public ForestResponse<List<Assets>> getAssetsList(String id, int page, String accessToken) {
        return esiCharacterClient.getCharacterAssets(id,page, accessToken);
    }

    @Override
    public JSONArray getAssetsNamesList(String id, List itemIds, String accessToken) {
        return esiCharacterClient.getCharacterAssetsNames(id, itemIds, accessToken);
    }

    @Override
    public List<Blueprints> getBlueprintsList(String id, int page, String accessToken) {
        List<Blueprints> blueprintsList  = esiCharacterClient.getCharacterBlueprints(id,page, accessToken);
        return blueprintsList;
    }
}
