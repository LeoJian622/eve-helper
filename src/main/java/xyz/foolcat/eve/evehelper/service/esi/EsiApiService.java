package xyz.foolcat.eve.evehelper.service.esi;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.common.constant.GlobalConstants;
import xyz.foolcat.eve.evehelper.domain.system.*;
import xyz.foolcat.eve.evehelper.dto.esi.CharacterInfoResponseDTO;
import xyz.foolcat.eve.evehelper.dto.esi.IndustryJobDTO;
import xyz.foolcat.eve.evehelper.dto.esi.UniverseNameResponeDTO;
import xyz.foolcat.eve.evehelper.esi.EsiNormalClient;
import xyz.foolcat.eve.evehelper.esi.auth.AuthorizeOAuth;
import xyz.foolcat.eve.evehelper.esi.auth.GrantType;
import xyz.foolcat.eve.evehelper.esi.model.AuthTokenResponse;
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

    /**
     * 获取ESI接口授权
     * <p>
     * 优先从redis缓存中获取accesstoken，如果不存在 则授权已过期。
     * <p>
     * 如果code为认证code，则调用认证接口获取授权。
     * 如果code为角色ID，则调用refreshtoken获取授权
     *
     * @param code
     * @return
     * @throws ParseException
     */
    public String getAccessToken(String code) throws ParseException {

        String redisKey = GlobalConstants.ESI_ACCESS_TOKEN_KEY + code;

        String accessToken = (String) redisTemplate.opsForValue().get(redisKey);

        if (StrUtil.isNotBlank(accessToken)) {
            return accessToken;
        }

        EveAccount character = eveAccountService.lambdaQuery().eq(EveAccount::getCharacterId, code).one();

        GrantType grantType = GrantType.AUTHORIZATION_CODE;
        if (ObjectUtil.isNotNull(character)) {
            grantType = GrantType.REFRESH_TOKEN;
            code = character.getRefreshToken();
        }

        AuthTokenResponse authToken = authorizeOAuth.updateAccessToken(grantType, code).block();
        assert authToken != null;
        accessToken = authToken.getAccessToken();
        updateRefreshToken(authToken);

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
    private void updateRefreshToken(AuthTokenResponse authTokenResponse) {
        //获取refresh_token
        String characterRefreshToken = authTokenResponse.getRefreshToken();
        String accessToken = authTokenResponse.getAccessToken();
        //解析token
        SignedJWT signedJwt = SignedJWT.parse(accessToken);
        JWTClaimsSet jwtClaimsSet = signedJwt.getJWTClaimsSet();
        String characterId = jwtClaimsSet.getStringClaim("sub").split(":")[2];
        String characterName = jwtClaimsSet.getStringClaim("name");
        //获取角色信息
        CharacterInfoResponseDTO characterInfoResponseDTO = esiNormalClient.getCharacterInfo(characterId);
        //联盟军团信息
        List<UniverseNameResponeDTO> universeNames = esiNormalClient.getUniverseName(List.of(characterInfoResponseDTO.getAlliance_id(), characterInfoResponseDTO.getCorporation_id()));
        Map<Integer, String> universeNameMap = universeNames.stream().collect(Collectors.toMap(UniverseNameResponeDTO::getId, UniverseNameResponeDTO::getName, (k1, k2) -> k1));

        //redis缓存access_token
        String redisKey = GlobalConstants.ESI_ACCESS_TOKEN_KEY + characterId;
        redisTemplate.opsForValue().set(redisKey, GlobalConstants.TOKEN_PERN + accessToken, 19 * 60, TimeUnit.SECONDS);

        EveAccount eveAccount = new EveAccount();
        eveAccount.setUserId(UserUtil.getUserId());
        eveAccount.setCharacterId(Integer.valueOf(characterId));
        eveAccount.setCharacterName(characterName);
        eveAccount.setCorpId(characterInfoResponseDTO.getCorporation_id());
        eveAccount.setCorpName(universeNameMap.get(characterInfoResponseDTO.getCorporation_id()));
        eveAccount.setAllianceId(characterInfoResponseDTO.getAlliance_id());
        eveAccount.setAllianceName(universeNameMap.get(characterInfoResponseDTO.getAlliance_id()));
        eveAccount.setRefreshToken(characterRefreshToken);

        LambdaUpdateWrapper<EveAccount> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        lambdaUpdateWrapper.eq(EveAccount::getCharacterId, eveAccount.getCharacterId());

        eveAccountService.saveOrUpdate(eveAccount, lambdaUpdateWrapper);
    }

    /**
     * 读取生产作业线列表
     *
     * @param type
     * @param cid
     * @return
     * @throws ParseException
     */
    public List<IndustryJobDTO> getJobList(String type, String cid) throws ParseException {
        String accessToken = getAccessToken(cid);
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
    public List<Assets> getAssetsList(String type, int page, String cid) throws ParseException {
        String accessToken = getAccessToken(cid);
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
    public List<WalletJournal> getWalletJournalList(String type, Integer page, String cid) throws ParseException {
        String accessToken = getAccessToken(cid);
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
    public JSONArray getAssetsNamesList(String type, List itemIds, String cid) throws ParseException {
        String accessToken = getAccessToken(cid);
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
    public List<Blueprints> getBlueprintsList(String type, int page, String cid) throws ParseException {
        String accessToken = getAccessToken(cid);
        return strategyContext.getResource(type).getBlueprintsList(cid, page, accessToken);
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
