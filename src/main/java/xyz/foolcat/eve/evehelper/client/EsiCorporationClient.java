package xyz.foolcat.eve.evehelper.client;

import cn.hutool.json.JSONArray;
import com.dtflys.forest.annotation.*;
import xyz.foolcat.eve.evehelper.domain.system.Assets;
import xyz.foolcat.eve.evehelper.domain.system.Blueprints;
import xyz.foolcat.eve.evehelper.dto.esi.IndustryJobDTO;
import xyz.foolcat.eve.evehelper.interceptor.EsiCharactorInterceptor;

import java.util.List;

/**
 * 国服ESI调用
 * <p>
 * 用于调用网易提供的各种ESI接口
 *
 * @author Leojan
 * @date 2021-12-07 17:19
 */
@BaseRequest(
        baseURL = "https://esi.evepc.163.com/latest",     // 默认域名
        headers = {
                "Accept:application/json; charset=UTF-8",// 默认请求头
                "Cache-Control: no-cache",
        },
        interceptor = EsiCharactorInterceptor.class
)
public interface EsiCorporationClient {


    /**
     * 获取公司工业生产信息
     *
     * @param corporationId
     * @param accessToken
     * @return
     */
    @Get(url = "/characters/{corporation_id}/industry/jobs/", dataType = "json")
    List<IndustryJobDTO> getCorporationJobs(@Var("corporation_id") String corporationId, @Header("Authorization") String accessToken);

    /**
     * 获取公司资产信息
     *
     * @param corporationId
     * @param page
     * @param accessToken
     * @return
     */
    @Get(url = "/characters/{corporation_id}/assets/", dataType = "json")
    List<Assets> getCorporationAssets(@Var("corporation_id") String corporationId, @Query("page") int page, @Header("Authorization") String accessToken);

    /**
     * 获取公司资产信息名称
     *
     * @param corporationId
     * @param itemIds
     * @param accessToken
     * @return
     */
    @Post(url = "/characters/{corporation_id}/assets/names/", dataType = "json")
    JSONArray getCorporationAssetsNames(@Var("corporation_id") String corporationId, @JSONBody("item_ids") List itemIds, @Header("Authorization") String accessToken);

    /**
     * 获取角色蓝图信息
     *
     * @param corporationId
     * @param page
     * @param accessToken
     * @return
     */
    @Get(url = "/corporations/{corporation_id}/blueprints/", dataType = "json")
    List<Blueprints> getCharactorBlueprints(@Var("corporation_id") String corporationId, @Query("page") int page, @Header("Authorization") String accessToken);

}
