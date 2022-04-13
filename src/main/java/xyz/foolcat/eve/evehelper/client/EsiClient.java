package xyz.foolcat.eve.evehelper.client;

import cn.hutool.json.JSONArray;
import com.dtflys.forest.annotation.*;
import com.dtflys.forest.callback.OnError;
import xyz.foolcat.eve.evehelper.dto.esi.AuthTokenResponseDTO;
import xyz.foolcat.eve.evehelper.dto.esi.CharactorInfoResponseDTO;
import xyz.foolcat.eve.evehelper.dto.esi.UniverseNameResponeDTO;
import xyz.foolcat.eve.evehelper.interceptor.EsiCharactorInterceptor;

import java.util.List;

/**
 * 国服ESI调用
 *
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
public interface EsiClient {

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
        AuthTokenResponseDTO getAccessToken(@Body("grant_type") String grantType, @Body("refresh_token")String refreshToken, @Body("client_id")String clientId, OnError onError);

        /**
         *  获取角色信息
         * @param charactorId
         * @return
         */
        @Get("/characters/{charactor_id}")
        CharactorInfoResponseDTO getCharactorInfo(@Var("charactor_id") String charactorId);

        /**
         *  获取角色工业生产信息
         * @param charactorId
         * @param accessToken
         * @return
         */
        @Get("/characters/{character_id}/industry/jobs/")
        JSONArray getCharactorJob(@Var("character_id") String charactorId, @Header("Authorization") String accessToken);

        /**
         * 获取item名称
         * @param itemIds
         * @return
         */
        @Post("/universe/names")
        List<UniverseNameResponeDTO> getUniverseName(@JSONBody List itemIds);

}
