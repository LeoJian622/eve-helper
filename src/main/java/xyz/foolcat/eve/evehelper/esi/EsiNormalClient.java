package xyz.foolcat.eve.evehelper.esi;

import com.dtflys.forest.annotation.*;
import com.dtflys.forest.callback.OnError;
import xyz.foolcat.eve.evehelper.domain.system.MarketOrder;
import xyz.foolcat.eve.evehelper.esi.model.AuthTokenResponse;
import xyz.foolcat.eve.evehelper.dto.esi.CharacterInfoResponseDTO;
import xyz.foolcat.eve.evehelper.dto.esi.UniverseNameResponeDTO;
import xyz.foolcat.eve.evehelper.interceptor.EsiClentInterceptor;

import java.util.List;

/**
 * 国服ESI调用
 * <p>
 * 用于调用网易提供的各种ESI接口
 *
 *
 * @author Leojan
 * @date 2021-12-07 17:19
 */
@BaseRequest(
        baseURL = "https://ali-esi.evepc.163.com/latest",     // 默认域名
        headers = {
                "Accept:application/json; charset=UTF-8",// 默认请求头
                "Cache-Control: no-cache",
        },
        interceptor = EsiClentInterceptor.class
)
@Deprecated
public interface EsiNormalClient {

    /**
     * code认证
     *
     * @param grantType
     * @param code
     * @param clientId
     */
    @PostRequest(
            url = "https://login.evepc.163.com/v2/oauth/token",
            headers = {
                    "Content-Type: application/x-www-form-urlencoded",
                    "Host: login.evepc.163.com",
            }
    )
    AuthTokenResponse addCharacterAuth(@Body("grant_type") String grantType, @Body("code") String code, @Body("client_id") String clientId);

    /**
     * refresh_token认证
     *
     * @param grantType
     * @param refreshToken
     * @param clientId
     */
    @PostRequest(
            url = "https://login.evepc.163.com/v2/oauth/token",
            headers = {
                    "Content-Type: application/x-www-form-urlencoded",
                    "Host: login.evepc.163.com"
            }
    )
    AuthTokenResponse getAccessToken(@Body("grant_type") String grantType, @Body("refresh_token") String refreshToken, @Body("client_id") String clientId, OnError onError);


    /**
     * 获取角色信息
     *
     * @param characterId
     * @return
     */
    @Get("/characters/{character_id}")
    CharacterInfoResponseDTO getCharacterInfo(@Var("character_id") String characterId);

    /**
     * 获取军团信息
     *
     * @param corporationId
     * @return
     */
    @Get("/corporations/{corporation_id}")
    CharacterInfoResponseDTO getCorporationInfo(@Var("corporation_id") String corporationId);

    /**
     * 获取item名称
     *
     * @param itemIds
     * @return
     */
    @Post("/universe/names")
    List<UniverseNameResponeDTO> getUniverseName(@JSONBody List itemIds);

    /**
     * 获取星域市场订单
     * @param regionId
     * @param page
     * @return
     */
    @Get("/markets/{region_id}/orders/")
    List<MarketOrder> getMarketOrder(@Var("region_id") String regionId,@Query("page") Integer page,@Query("type_id") Integer typeId);

}
