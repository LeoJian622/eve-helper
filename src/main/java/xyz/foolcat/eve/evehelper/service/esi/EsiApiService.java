package xyz.foolcat.eve.evehelper.service.esi;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.common.constant.GlobalConstants;
import xyz.foolcat.eve.evehelper.common.result.ResultCode;
import xyz.foolcat.eve.evehelper.domain.system.*;
import xyz.foolcat.eve.evehelper.dto.esi.IndustryJobDTO;
import xyz.foolcat.eve.evehelper.esi.EsiClient;
import xyz.foolcat.eve.evehelper.esi.EsiNormalClient;
import xyz.foolcat.eve.evehelper.esi.api.CharacterApi;
import xyz.foolcat.eve.evehelper.esi.api.UniverseApi;
import xyz.foolcat.eve.evehelper.esi.auth.AuthorizeOAuth;
import xyz.foolcat.eve.evehelper.esi.auth.GrantType;
import xyz.foolcat.eve.evehelper.esi.model.AuthTokenResponse;
import xyz.foolcat.eve.evehelper.esi.model.CharacterPublicInfoResponse;
import xyz.foolcat.eve.evehelper.esi.model.Id2NameResponse;
import xyz.foolcat.eve.evehelper.exception.EsiException;
import xyz.foolcat.eve.evehelper.service.system.EveAccountService;
import xyz.foolcat.eve.evehelper.strategy.esi.EsiClientStrategyContext;
import xyz.foolcat.eve.evehelper.util.UserUtil;

import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 国服ESI接口封装
 *
 * @author Leojan
 * date 2021-12-07 16:54
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class EsiApiService {

    private final EsiNormalClient esiNormalClient;

    private final EsiClientStrategyContext strategyContext;

    private final RedisTemplate redisTemplate;

    private final EveAccountService eveAccountService;

    private final AuthorizeOAuth authorizeOAuth;

    private final CharacterApi characterApi;

    private final UniverseApi universeApi;

    /**
     * 获取ESI接口授权
     * <p>
     * 优先从redis缓存中获取accesstoken，如果不存在 则授权已过期。
     * <p>
     * 如果code为认证code，则调用认证接口获取授权。
     * 如果code为角色ID，则调用refreshtoken获取授权
     *
     * @param code 人物或者公司的ID
     * @param userId
     * @return
     * @throws ParseException
     */
    public String getAccessToken(Integer code, Integer userId) throws ParseException {

        String redisKey = GlobalConstants.ESI_ACCESS_TOKEN_KEY + code;

        String accessToken = (String) redisTemplate.opsForValue().get(redisKey);

        if (StrUtil.isNotEmpty(accessToken)) {
            return accessToken;
        }

        EveAccount character = eveAccountService.lambdaQuery().eq(EveAccount::getCharacterId, code).or().eq(EveAccount::getCorpId,code).one();
        if (ObjectUtil.isNull(character)) {
            throw new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE);
        }
        AuthTokenResponse authToken = authorizeOAuth.updateAccessToken(GrantType.REFRESH_TOKEN, character.getRefreshToken()).block();
        assert authToken != null;
        accessToken = authToken.getAccessToken();
        updateRefreshToken(authToken, userId);
        return GlobalConstants.TOKEN_PERN + accessToken;
    }

    /**
     * 获取ESI接口授权
     * <p>
     * 优先从redis缓存中获取accesstoken，如果不存在 则授权已过期。
     * <p>
     * 如果code为认证code，则调用认证接口获取授权。
     * 如果code为角色ID，则调用refreshtoken获取授权
     *
     * @param code 授权码
     * @param userId
     * @return
     * @throws ParseException
     */
    public String getAccessToken(String code, Integer userId) throws ParseException {
        AuthTokenResponse authToken = authorizeOAuth.updateAccessToken(GrantType.AUTHORIZATION_CODE, code).block();
        assert authToken != null;
        String accessToken = authToken.getAccessToken();
        updateRefreshToken(authToken, userId);
        return GlobalConstants.TOKEN_PERN + accessToken;
    }

    /**
     * 更新保存 refreshtoken  用来获取accesstoken
     *
     * @param authTokenResponse
     * @throws ParseException
     */
    @SuppressWarnings("unchecked")
    @SneakyThrows
    private void updateRefreshToken(AuthTokenResponse authTokenResponse, Integer userId) {
        //获取refresh_token
        String characterRefreshToken = authTokenResponse.getRefreshToken();
        String accessToken = authTokenResponse.getAccessToken();
        //解析token
        SignedJWT signedJwt = SignedJWT.parse(accessToken);
        JWTClaimsSet jwtClaimsSet = signedJwt.getJWTClaimsSet();
        Integer characterId = Integer.parseInt(jwtClaimsSet.getStringClaim("sub").split(":")[2]);
        String characterName = jwtClaimsSet.getStringClaim("name");
        //获取角色信息
        CharacterPublicInfoResponse characterPublicInfoResponse = characterApi.queryCharacter(characterId, EsiClient.SERENITY).block();
        //联盟军团名称
        List<Id2NameResponse> nameResponses = universeApi.queryUniverseNames(List.of(characterPublicInfoResponse.getAllianceId(), characterPublicInfoResponse.getCorporationId()), EsiClient.SERENITY).collectList().block();
        Map<Integer, String> universeNameMap = nameResponses.stream().collect(Collectors.toMap(Id2NameResponse::getId, Id2NameResponse::getName, (k1, k2) -> k1));

        //redis缓存access_token
        String redisKey = GlobalConstants.ESI_ACCESS_TOKEN_KEY + characterId;
        redisTemplate.opsForValue().set(redisKey, GlobalConstants.TOKEN_PERN + accessToken, 19 * 60, TimeUnit.SECONDS);

        EveAccount eveAccount = new EveAccount();
        eveAccount.setUserId(userId);
        eveAccount.setCharacterId(Integer.valueOf(characterId));
        eveAccount.setCharacterName(characterName);
        eveAccount.setCorpId(characterPublicInfoResponse.getCorporationId());
        eveAccount.setCorpName(universeNameMap.get(characterPublicInfoResponse.getCorporationId()));
        eveAccount.setAllianceId(characterPublicInfoResponse.getAllianceId());
        eveAccount.setAllianceName(universeNameMap.get(characterPublicInfoResponse.getAllianceId()));
        eveAccount.setRefreshToken(characterRefreshToken);
        eveAccountService.insertOrUpdate(eveAccount);
    }

    /**
     * 读取生产作业线列表
     *
     * @param type
     * @param cid
     * @return
     * @throws ParseException
     */
    @Deprecated
    public List<IndustryJobDTO> getJobList(String type, String cid) throws ParseException {
        String accessToken = getAccessToken(cid, UserUtil.getUserId());
        return strategyContext.getResource(type).getJobList(cid, accessToken);
    }

    /**
     * 读取资产列表
     *
     * @param type
     * @param cid
     * @return
     * @throws ParseException
     */
    @Deprecated
    public List<Assets> getAssetsList(String type, int page, String cid) throws ParseException {
        String accessToken = getAccessToken(cid, UserUtil.getUserId());
        return strategyContext.getResource(type).getAssetsList(cid, page, accessToken);
    }

    /**
     * 获取钱包记录
     *
     * @param type
     * @param page
     * @param cid
     * @return
     */
    @Deprecated
    public List<WalletJournal> getWalletJournalList(String type, Integer page, String cid) throws ParseException {
        String accessToken = getAccessToken(cid, UserUtil.getUserId());
        return strategyContext.getResource(type).getWalletJournalList(cid, page, accessToken);
    }

    /**
     * 物品自定义名称获取
     *
     * @param type
     * @param cid
     * @return
     * @throws ParseException
     */
    @Deprecated
    public JSONArray getAssetsNamesList(String type, List itemIds, String cid) throws ParseException {
        String accessToken = getAccessToken(cid, UserUtil.getUserId());
        return strategyContext.getResource(type).getAssetsNamesList(cid, itemIds, accessToken);
    }

    /**
     * 读取蓝图列表
     *
     * @param type
     * @param cid
     * @return
     * @throws ParseException
     */
    @Deprecated
    public List<Blueprints> getBlueprintsList(String type, int page, String cid) throws ParseException {
        String accessToken = getAccessToken(cid, UserUtil.getUserId());
        return strategyContext.getResource(type).getBlueprintsList(cid, page, accessToken);
    }

    /**
     * 获取市场订单
     *
     * @param regionId
     * @param page
     * @return
     */
    @Deprecated
    public List<MarketOrder> readMarketOrder(String regionId, Integer page, Integer typeId) {
        return esiNormalClient.getMarketOrder(regionId, page, typeId);
    }

}
