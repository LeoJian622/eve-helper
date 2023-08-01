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
import xyz.foolcat.eve.evehelper.domain.system.*;
import xyz.foolcat.eve.evehelper.dto.esi.AuthTokenResponseDTO;
import xyz.foolcat.eve.evehelper.dto.esi.CharacterInfoResponseDTO;
import xyz.foolcat.eve.evehelper.dto.esi.IndustryJobDTO;
import xyz.foolcat.eve.evehelper.dto.esi.UniverseNameResponeDTO;
import xyz.foolcat.eve.evehelper.exception.EsiException;
import xyz.foolcat.eve.evehelper.service.system.EveCharacterService;
import xyz.foolcat.eve.evehelper.strategy.esi.EsiClientStrategyContext;
import xyz.foolcat.eve.evehelper.util.UserUtil;

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
        EveCharacter character = eveCharacterService.lambdaQuery().eq(EveCharacter::getCharacterId, code).eq(EveCharacter::getUserId, UserUtil.getUserId()).one();
        AuthTokenResponseDTO authTokenResponseDTO;
        if (ObjectUtil.isNull(character)) {
            authTokenResponseDTO = esiNormalClient.addCharacterAuth(EsiConstant.GRANT_TYPE_AUTHORIZATION_CODE, code, EsiConstant.CLIENT_ID);
        } else {
            String refreshToken = null;
            refreshToken = character.getRefreshToken();
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

        updateRefreshToken(type, authTokenResponseDTO, accessToken);

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
    @SuppressWarnings("unchecked")
    private void updateRefreshToken(String type, AuthTokenResponseDTO authTokenResponseDTO, String accessToken) throws ParseException {
        //获取refresh_token
        String characterRefreshToken = authTokenResponseDTO.getRefresh_token();

        //解析token
        SignedJWT signedJwt = SignedJWT.parse(accessToken);
        JWTClaimsSet jwtClaimsSet = signedJwt.getJWTClaimsSet();
        String characterId = jwtClaimsSet.getStringClaim("sub").split(":")[2];
        String characterName = jwtClaimsSet.getStringClaim("name");
        //获取角色信息
        CharacterInfoResponseDTO characterInfoResponseDTO = esiNormalClient.getCharacterInfo(characterId);
        //联盟军团信息
        List<UniverseNameResponeDTO> universeNames = esiNormalClient.getUniverseName(Arrays.asList(characterInfoResponseDTO.getAlliance_id(), characterInfoResponseDTO.getCorporation_id()));
        Map<Integer, String> universeNameMap = universeNames.stream().collect(Collectors.toMap(UniverseNameResponeDTO::getId, UniverseNameResponeDTO::getName, (k1, k2) -> k1));

        //redis缓存access_token
        String redisKey = GlobalConstants.ESI_ACCESS_TOKEN_KEY + characterId + ":" + type;
        redisTemplate.opsForValue().set(redisKey, GlobalConstants.TOKEN_PERN + accessToken, 19 * 60, TimeUnit.SECONDS);

        EveCharacter eveCharacter = new EveCharacter();
        eveCharacter.setUserId(UserUtil.getUserId());
        eveCharacter.setCharacterId(Integer.valueOf(characterId));
        eveCharacter.setCharacterName(characterName);
        eveCharacter.setCorpId(characterInfoResponseDTO.getCorporation_id());
        eveCharacter.setCorpName(universeNameMap.get(characterInfoResponseDTO.getCorporation_id()));
        eveCharacter.setAllianceId(characterInfoResponseDTO.getAlliance_id());
        eveCharacter.setAllianceName(universeNameMap.get(characterInfoResponseDTO.getAlliance_id()));
        eveCharacter.setRefreshToken(characterRefreshToken);

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
    public List<Asserts> getAssetsList(String type, int page, String id) throws ParseException {
        String accessToken = getAccessToken(type, id);
        return strategyContext.getResource(type).getAssetsList(id, page, accessToken);
    }

    /**
     * 获取钱包记录
     *
     * @param type
     * @param page
     * @param cid
     * @return
     */
    public List<WalletJournal> getWalletJournalList(String type, Integer page, String cid) throws ParseException {
        String accessToken = getAccessToken(type, cid);
        return strategyContext.getResource(type).getWalletJournalList(cid, page, accessToken);
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
    public List<MarketOrder> readMarketOrder(String regionId, Integer page, Integer typeId) {
        return esiNormalClient.getMarketOrder(regionId, page, typeId);
    }

    /**
     * 获取Universe信息
     *
     * @param items
     * @return
     */
    public List getUniverseName(List items) {
        return esiNormalClient.getUniverseName(items);
    }

}
