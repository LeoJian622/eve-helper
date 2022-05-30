package xyz.foolcat.eve.evehelper.client;

import cn.hutool.json.JSONArray;
import com.dtflys.forest.annotation.*;
import xyz.foolcat.eve.evehelper.domain.system.Assets;
import xyz.foolcat.eve.evehelper.domain.system.Blueprints;
import xyz.foolcat.eve.evehelper.dto.esi.CharactorInfoResponseDTO;
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
public interface EsiCharactorClient {

    /**
     * 获取角色信息
     *
     * @param charactorId
     * @return
     */
    @Get("/characters/{charactor_id}")
    CharactorInfoResponseDTO getCharactorInfo(@Var("charactor_id") String charactorId);

    /**
     * 获取角色工业生产信息
     *
     * @param charactorId
     * @param accessToken
     * @return
     */
    @Get(url = "/characters/{character_id}/industry/jobs/", dataType = "json")
    List<IndustryJobDTO> getCharactorJobs(@Var("character_id") String charactorId, @Header("Authorization") String accessToken);

    /**
     * 获取角色资产信息
     *
     * @param charactorId
     * @param page
     * @param accessToken
     * @return
     */
    @Get(url = "/characters/{character_id}/assets/", dataType = "json")
    List<Assets> getCharactorAssets(@Var("character_id") String charactorId, @Query("page") int page, @Header("Authorization") String accessToken);

    /**
     * 获取角色资产信息名称
     *
     * @param charactorId
     * @param itemIds
     * @param accessToken
     * @return
     */
    @Post(url = "/characters/{character_id}/assets/names/", dataType = "json")
    JSONArray getCharactorAssetsNames(@Var("character_id") String charactorId, @JSONBody List itemIds, @Header("Authorization") String accessToken);

    /**
     * 获取角色蓝图信息
     *
     * @param charactorId
     * @param page
     * @param accessToken
     * @return
     */
    @Get(url = "/characters/{character_id}/blueprints/", dataType = "json")
    List<Blueprints> getCharactorBlueprints(@Var("character_id") String charactorId, @Query("page") int page, @Header("Authorization") String accessToken);

}
