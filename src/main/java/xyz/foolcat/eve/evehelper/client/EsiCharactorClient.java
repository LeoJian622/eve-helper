package xyz.foolcat.eve.evehelper.client;

import cn.hutool.json.JSONArray;
import com.dtflys.forest.annotation.*;
import xyz.foolcat.eve.evehelper.dto.esi.AuthTokenResponseDTO;
import xyz.foolcat.eve.evehelper.dto.esi.CharactorInfoResponseDTO;
import xyz.foolcat.eve.evehelper.dto.esi.UniverseNameResponeDTO;
import xyz.foolcat.eve.evehelper.interceptor.EsiCharactorInterceptor;

import java.util.List;

/**
 * 国服ESI接口调用
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
         * code认证
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
        AuthTokenResponseDTO addCharactorAuth(@Body("grant_type") String grantType, @Body("code")String code, @Body("client_id")String clientId);

        /**
         * refresh_token认证
         * @param grantType
         * @param refresh_token
         * @param clientId
         */
        @PostRequest(
                url = "https://login.evepc.163.com/v2/oauth/token",
                headers = {
                        "Content-Type: application/x-www-form-urlencoded",
                        "Host: login.evepc.163.com"
                }
        )
        String getAccessToken(@Body("grant_type") String grantType, @Body("refresh_token")String refresh_token, @Body("client_id")String clientId);

        /**
         *  获取角色信息
         * @param charactorId
         * @param accessToken
         * @return
         */
        @Get("/characters/{charactorId}")
        CharactorInfoResponseDTO getCharactorInfo(@Var("charactorId") String charactorId, @Header("authorization") String accessToken);

        /**
         * 获取item名称
         * @param itemIds
         * @return
         */
        @Post("/universe/names")
        List<UniverseNameResponeDTO> getUniverseName(@JSONBody List itemIds);

}
