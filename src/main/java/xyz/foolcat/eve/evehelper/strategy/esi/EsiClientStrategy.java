package xyz.foolcat.eve.evehelper.strategy.esi;

import cn.hutool.json.JSONArray;
import com.dtflys.forest.annotation.Body;
import xyz.foolcat.eve.evehelper.domain.system.Assets;
import xyz.foolcat.eve.evehelper.domain.system.Blueprints;
import xyz.foolcat.eve.evehelper.dto.esi.IndustryJobDTO;

import java.util.List;

/**
 * @author Leojan
 * @date 2022-04-21 17:12
 */

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
    List<Assets> getAssetsList(String id,int page, String accessToken);

    /**
     * 获取物品自定义名称
     * @param accessToken
     * @param id
     * @return
     */
    JSONArray getAssetsNamesList(String id,  List itemIds, String accessToken);

    /**
     * 获取资产信息
     * @param accessToken
     * @param id
     * @return
     */
    List<Blueprints> getBlueprintsList(String id, int page, String accessToken);


}
