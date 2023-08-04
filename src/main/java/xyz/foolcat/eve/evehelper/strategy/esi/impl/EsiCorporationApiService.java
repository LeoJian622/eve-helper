package xyz.foolcat.eve.evehelper.strategy.esi.impl;

import cn.hutool.json.JSONArray;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.esiclient.EsiCorporationClient;
import xyz.foolcat.eve.evehelper.domain.system.*;
import xyz.foolcat.eve.evehelper.dto.esi.IndustryJobDTO;
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
    public List<Asserts> getAssetsList(String id, int page, String accessToken) {
        return esiCorporationClient.getCorporationAssets(id,page, accessToken);
    }

    @Override
    public List<WalletJournal> getWalletJournalList(String id, int page, String accessToken) {
        return null;
    }

    @Override
    public JSONArray getAssetsNamesList(String id, List itemIds, String accessToken) {
        return esiCorporationClient.getCorporationAssetsNames(id, itemIds, accessToken);
    }

    @Override
    public List<Blueprints> getBlueprintsList(String id, int page, String accessToken) {
        return esiCorporationClient.getCorporationBlueprints(id,page, accessToken);
    }

    @Override
    public List<Observer> getCropObserverList(Long id, Integer page, String accessToken) {
        return esiCorporationClient.getObserverList(id, page, accessToken);
    }

    @Override
    public List<MiningDetail> getMiningDetailListByObserver(Long id, Long observerId, Integer page, String accessToken) {
        return esiCorporationClient.getMinigDetail(id, observerId, page, accessToken);
    }
}
