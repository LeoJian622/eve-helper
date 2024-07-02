package xyz.foolcat.eve.evehelper.strategy.esi;

import cn.hutool.json.JSONArray;
import xyz.foolcat.eve.evehelper.domain.system.*;
import xyz.foolcat.eve.evehelper.dto.esi.IndustryJobDTO;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Leojan
 * date 2022-04-21 17:12
 */
@Deprecated
public interface EsiClientStrategy {

    /**
     * 获取工业生产作业线信息
     * @param accessToken
     * @param id
     * @return
     */
    List<IndustryJobDTO> getJobList(String id,String accessToken);

    /**
     * 获取资产信息
     * @param accessToken
     * @param id
     * @return
     */
    List<Assets> getAssetsList(String id, int page, String accessToken);

    /**
     * 获取资产信息
     * @param accessToken
     * @param id
     * @return
     */
    List<WalletJournal> getWalletJournalList(String id, int page, String accessToken);

    /**
     * 获取物品自定义名称
     * @param accessToken
     * @param id
     * @return
     */
    JSONArray getAssetsNamesList(String id,  List itemIds, String accessToken);

    /**
     * 获取蓝图信息
     * @param accessToken
     * @param id
     * @return
     */
    List<Blueprints> getBlueprintsList(String id, int page, String accessToken);

    /**
     *
     * 返回开采观察者列表
     * 只对公司有效
     *
     * @param id
     * @param page
     * @param accessToken
     * @return
     */
    default List<Observer> getCropObserverList(Integer id, Integer page, String accessToken){
        return new ArrayList<>();
    }

    /**
     *
     * 返回对应公司的观察者所记录的采矿明细
     *
     * @param id
     * @param observerId
     * @param page
     * @param accessToken
     * @return
     */
    default List<MiningDetail> getMiningDetailListByObserver(Integer id, Long observerId, Integer page, String accessToken) {
        return new ArrayList<>();
    }


}
