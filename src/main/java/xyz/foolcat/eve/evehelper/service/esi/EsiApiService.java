package xyz.foolcat.eve.evehelper.service.esi;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.client.EsiNormalClient;
import xyz.foolcat.eve.evehelper.client.constant.EsiConstant;
import xyz.foolcat.eve.evehelper.common.constant.GlobalConstants;
import xyz.foolcat.eve.evehelper.common.result.ResultCode;
import xyz.foolcat.eve.evehelper.domain.system.Assets;
import xyz.foolcat.eve.evehelper.domain.system.Blueprints;
import xyz.foolcat.eve.evehelper.domain.system.EveCharacter;
import xyz.foolcat.eve.evehelper.domain.system.MarketOrder;
import xyz.foolcat.eve.evehelper.dto.esi.AuthTokenResponseDTO;
import xyz.foolcat.eve.evehelper.dto.esi.CharactorInfoResponseDTO;
import xyz.foolcat.eve.evehelper.dto.esi.IndustryJobDTO;
import xyz.foolcat.eve.evehelper.dto.esi.UniverseNameResponeDTO;
import xyz.foolcat.eve.evehelper.exception.EsiException;
import xyz.foolcat.eve.evehelper.service.system.EveCharacterService;
import xyz.foolcat.eve.evehelper.strategy.esi.EsiClientStrategyContext;

import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 国服ESI接口封装
 *
 * @author Leojan
 * @date 2021-12-07 16:54
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class EsiApiService {

    private final EsiNormalClient esiNormalClient;

    private final EsiClientStrategyContext strategyContext;

    private final RedisTemplate redisTemplate;

    private final EveCharacterService eveCharacterService;

    /**
     * 获取ESI接口授权
     * <p>
     * 优先从redis缓存中获取accesstoken，如果不存在 则授权已过期。
     * <p>
     * 如果code为认证code，则调用认证接口获取授权。
     * 如果code为角色ID，则调用refreshtoken获取授权
     *
     * @param type
     * @param code
     * @return
     * @throws ParseException
     */
    public String getAccessToken(String type, String code) throws ParseException {

        String redisKey = GlobalConstants.ESI_ACCESS_TOKEN_KEY + code + ":" + type;

        String accessToken = (String) redisTemplate.opsForValue().get(redisKey);

        if (StrUtil.isNotBlank(accessToken)) {
            return accessToken;
        }
        EveCharacter character = eveCharacterService.lambdaQuery().eq(EveCharacter::getCharacterId, code).one();
        AuthTokenResponseDTO authTokenResponseDTO;
        if (ObjectUtil.isNull(character)) {
            authTokenResponseDTO = esiNormalClient.addCharactorAuth(EsiConstant.GRANT_TYPE_AUTHORIZATION_CODE, code, EsiConstant.CLIENT_ID);
        } else {
            String refreshToken = null;
            refreshToken = getAccesstokenByType(type, character);
            authTokenResponseDTO = esiNormalClient.getAccessToken(EsiConstant.GRANT_TYPE_REFRESH_TOKEN, refreshToken, EsiConstant.CLIENT_ID, (ex, request, response) -> {
                String content = response.getContent();
                JSONObject result = JSONUtil.parseObj(content);
                if (EsiConstant.INVALID_GRANT.equals(result.get(EsiConstant.ERROR))) {
                    throw new EsiException(ResultCode.ACCESS_UNAUTHORIZED, "授权过期，请重新授权");
                } else {
                    throw new EsiException(ResultCode.ACCESS_UNAUTHORIZED, result.get("error_description", String.class));
                }
            });
        }


        accessToken = authTokenResponseDTO.getAccess_token();

        updateRefreshTocken(type, authTokenResponseDTO, accessToken);

        return GlobalConstants.TOKEN_PERN + accessToken;

    }

    /**
     * 更新保存 refreshtoken  用来获取accesstoke
     *
     * @param type
     * @param authTokenResponseDTO
     * @param accessToken
     * @throws ParseException
     */
    private void updateRefreshTocken(String type, AuthTokenResponseDTO authTokenResponseDTO, String accessToken) throws ParseException {
        //获取refresh_token
        String charactorRefreshToken = authTokenResponseDTO.getRefresh_token();

        //解析token
        SignedJWT signedJwt = SignedJWT.parse(accessToken);
        JWTClaimsSet jwtClaimsSet = signedJwt.getJWTClaimsSet();
        String charactorId = jwtClaimsSet.getStringClaim("sub").split(":")[2];
        String charactorName = jwtClaimsSet.getStringClaim("name");
        //获取角色信息
        CharactorInfoResponseDTO charactorInfoResponseDTO = esiNormalClient.getCharactorInfo(charactorId);
        //联盟军团信息
        List<UniverseNameResponeDTO> universeNames = esiNormalClient.getUniverseName(Arrays.asList(charactorInfoResponseDTO.getAlliance_id(), charactorInfoResponseDTO.getCorporation_id()));
        Map<Integer, String> universeNameMap = universeNames.stream().collect(Collectors.toMap(UniverseNameResponeDTO::getId, UniverseNameResponeDTO::getName, (k1, k2) -> k1));

        //redis缓存accesstoken
        String redisKey = GlobalConstants.ESI_ACCESS_TOKEN_KEY + charactorId + ":" + type;
        redisTemplate.opsForValue().set(redisKey, GlobalConstants.TOKEN_PERN + accessToken, 19 * 60, TimeUnit.SECONDS);

//        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = 1L;
        EveCharacter eveCharacter = new EveCharacter();
        eveCharacter.setUserId(userId.intValue());
        eveCharacter.setCharacterId(Integer.valueOf(charactorId));
        eveCharacter.setCharacterName(charactorName);
        eveCharacter.setCorpId(charactorInfoResponseDTO.getCorporation_id());
        eveCharacter.setCorpName(universeNameMap.get(charactorInfoResponseDTO.getCorporation_id()));
        eveCharacter.setAllianceId(charactorInfoResponseDTO.getAlliance_id());
        eveCharacter.setAllianceName(universeNameMap.get(charactorInfoResponseDTO.getAlliance_id()));

        setAccessTokenByType(type, charactorRefreshToken, eveCharacter);

        LambdaUpdateWrapper<EveCharacter> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        lambdaUpdateWrapper.eq(EveCharacter::getCharacterId, eveCharacter.getCharacterId());

        eveCharacterService.saveOrUpdate(eveCharacter, lambdaUpdateWrapper);
    }

    /**
     * 读取生产作业线列表
     *
     * @param type
     * @param id
     * @return
     * @throws ParseException
     */
    public List<IndustryJobDTO> getJobList(String type, String id) throws ParseException {
        String accessToken = getAccessToken(type, id);
        return strategyContext.getResource(type).getJobList(id, accessToken);
    }

    /**
     * 读取资产列表
     *
     * @param type
     * @param id
     * @return
     * @throws ParseException
     */
    public List<Assets> getAssetsList(String type,int page, String id) throws ParseException {
        String accessToken = getAccessToken(type, id);
        return strategyContext.getResource(type).getAssetsList(id, page, accessToken);
    }

    /**
     * 物品自定义名称获取
     *
     * @param type
     * @param id
     * @return
     * @throws ParseException
     */
    public JSONArray getAssetsNamesList(String type, List itemIds, String id) throws ParseException {
        String accessToken = getAccessToken(type, id);
        return strategyContext.getResource(type).getAssetsNamesList(id, itemIds, accessToken);
    }

    /**
     * 读取蓝图列表
     *
     * @param type
     * @param id
     * @return
     * @throws ParseException
     */
    public List<Blueprints> getBlueprintsList(String type, int page, String id) throws ParseException {
        String accessToken = getAccessToken(type, id);
        return strategyContext.getResource(type).getBlueprintsList(id, page, accessToken);
    }

    /**
     * 获取市场订单
     *
     * @param regionId
     * @param page
     * @return
     */
    public List<MarketOrder> readMarketOrder(String regionId , Integer page, Integer typeId) {
        return esiNormalClient.getMarketOrder(regionId, page, typeId);
    }

    /**
     * 获取Universe信息
     * @param items
     * @return
     */
    public List getUniverseName(List items){
        return esiNormalClient.getUniverseName(items);
    }

    /**
     * 根据请求类型，进行token保存
     *
     * @param type
     * @param charactorRefreshToken
     * @param eveCharacter
     */
    private void setAccessTokenByType(String type, String charactorRefreshToken, EveCharacter eveCharacter) {
        switch (type) {
            case GlobalConstants.CHAR: {
                eveCharacter.setRefreshTokenChar(charactorRefreshToken);
                break;
            }
            case GlobalConstants.CROP: {
                eveCharacter.setRefreshTokenCrop(charactorRefreshToken);
                break;
            }
            case GlobalConstants.SKILL: {
                eveCharacter.setRefreshTokenSkill(charactorRefreshToken);
                break;
            }
            case GlobalConstants.NORMAL: {
                eveCharacter.setRefreshTokenNormal(charactorRefreshToken);
                break;
            }
            default:
                throw new EsiException(ResultCode.COMMON_FAIL, "添加游戏角色失败");
        }
    }

    /**
     * 根据请求类型type 获取对应的refreshtoken
     *
     * @param type
     * @param character
     * @return
     */
    private String getAccesstokenByType(String type, EveCharacter character) {
        String refreshToken;
        switch (type) {
            case GlobalConstants.CHAR: {
                refreshToken = character.getRefreshTokenChar();
                break;
            }
            case GlobalConstants.CROP: {
                refreshToken = character.getRefreshTokenCrop();
                break;
            }
            case GlobalConstants.SKILL: {
                refreshToken = character.getRefreshTokenSkill();
                break;
            }
            case GlobalConstants.NORMAL: {
                refreshToken = character.getRefreshTokenNormal();
                break;
            }
            default:
                refreshToken = "";
        }
        return refreshToken;
    }


}
