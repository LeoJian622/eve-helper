package xyz.foolcat.eve.evehelper.client;

import cn.hutool.json.JSONArray;
import com.dtflys.forest.annotation.*;
import xyz.foolcat.eve.evehelper.domain.system.Asserts;
import xyz.foolcat.eve.evehelper.domain.system.Blueprints;
import xyz.foolcat.eve.evehelper.domain.system.WalletJournal;
import xyz.foolcat.eve.evehelper.dto.esi.CharacterInfoResponseDTO;
import xyz.foolcat.eve.evehelper.dto.esi.IndustryJobDTO;
import xyz.foolcat.eve.evehelper.interceptor.EsiClentInterceptor;

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
        interceptor = EsiClentInterceptor.class
)
public interface EsiCharacterClient {

    /**
     * 获取角色信息
     *
     * @param characterId
     * @return
     */
    @Get("/characters/{character_id}")
    CharacterInfoResponseDTO getCharacterInfo(@Var("character_id") String characterId);

    /**
     * 获取角色工业生产信息
     *
     * @param characterId
     * @param accessToken
     * @return
     */
    @Get(url = "/characters/{character_id}/industry/jobs/", dataType = "json")
    List<IndustryJobDTO> getCharacterJobs(@Var("character_id") String characterId, @Header("Authorization") String accessToken);

    /**
     * 获取角色资产信息
     *
     * @param characterId
     * @param page
     * @param accessToken
     * @return
     */
    @Get(url = "/characters/{character_id}/assets/", dataType = "json")
    List<Asserts> getCharacterAssets(@Var("character_id") String characterId, @Query("page") int page, @Header("Authorization") String accessToken);

    /**
     * 获取角色钱包交易记录
     *
     * @param characterId
     * @param page
     * @param accessToken
     * @return
     */
    @Get(url = "/characters/{character_id}/wallet/journal/", dataType = "json")
    List<WalletJournal> getCharacterWalletJournal(@Var("character_id") String characterId, @Query("page") int page, @Header("Authorization") String accessToken);

    /**
     * 获取角色资产信息名称
     *
     * @param characterId
     * @param itemIds
     * @param accessToken
     * @return
     */
    @Post(url = "/characters/{character_id}/assets/names/", dataType = "json")
    JSONArray getCharacterAssetsNames(@Var("character_id") String characterId, @JSONBody List itemIds, @Header("Authorization") String accessToken);

    /**
     * 获取角色蓝图信息
     *
     * @param characterId
     * @param page
     * @param accessToken
     * @return
     */
    @Get(url = "/characters/{character_id}/blueprints/", dataType = "json")
    List<Blueprints> getCharacterBlueprints(@Var("character_id") String characterId, @Query("page") int page, @Header("Authorization") String accessToken);

}
