package xyz.foolcat.eve.evehelper.service.esi;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.client.EsiClient;
import xyz.foolcat.eve.evehelper.client.constant.EsiConstant;
import xyz.foolcat.eve.evehelper.common.constant.GlobalConstants;
import xyz.foolcat.eve.evehelper.common.result.ResultCode;
import xyz.foolcat.eve.evehelper.domain.system.EveCharacter;
import xyz.foolcat.eve.evehelper.dto.esi.AuthTokenResponseDTO;
import xyz.foolcat.eve.evehelper.dto.esi.CharactorInfoResponseDTO;
import xyz.foolcat.eve.evehelper.dto.esi.UniverseNameResponeDTO;
import xyz.foolcat.eve.evehelper.exception.EsiException;
import xyz.foolcat.eve.evehelper.service.system.EveCharacterService;

import javax.annotation.Resource;
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
public class EsiApiService {


    @Resource
    private EsiClient esiClient;

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private EveCharacterService eveCharacterService;


    public String getAccessToken(String type, String code) throws ParseException {

        String redisKey = GlobalConstants.ESI_ACCESS_TOKEN_KEY + code + ":" + type;

        String accessToken = (String) redisTemplate.opsForValue().get(redisKey);

        if (StrUtil.isNotBlank(accessToken)) {
            return accessToken;
        }
        EveCharacter character = eveCharacterService.lambdaQuery().eq(EveCharacter::getCharacterId, code).one();
        AuthTokenResponseDTO authTokenResponseDTO;
        if (ObjectUtil.isNull(character)) {
            authTokenResponseDTO = esiClient.addCharactorAuth(EsiConstant.GRANT_TYPE_AUTHORIZATION_CODE, code, EsiConstant.CLIENT_ID);
        } else {
            String refreshToken = null;
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
                default:
                    refreshToken = "";
            }

            authTokenResponseDTO = esiClient.getAccessToken(EsiConstant.GRANT_TYPE_REFRESH_TOKEN, refreshToken, EsiConstant.CLIENT_ID, (ex, request, response) -> {
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

    private void updateRefreshTocken(String type, AuthTokenResponseDTO authTokenResponseDTO, String accessToken) throws ParseException {
        //获取refresh_token
        String charactorRefreshToken = authTokenResponseDTO.getRefresh_token();

        //解析token
        SignedJWT signedJwt = SignedJWT.parse(accessToken);
        JWTClaimsSet jwtClaimsSet = signedJwt.getJWTClaimsSet();
        String charactorId = jwtClaimsSet.getStringClaim("sub").split(":")[2];
        String charactorName = jwtClaimsSet.getStringClaim("name");
        //获取角色信息
        CharactorInfoResponseDTO charactorInfoResponseDTO = esiClient.getCharactorInfo(charactorId);
        //联盟军团信息
        List<UniverseNameResponeDTO> universeNames = esiClient.getUniverseName(Arrays.asList(charactorInfoResponseDTO.getAlliance_id(), charactorInfoResponseDTO.getCorporation_id()));
        Map<Integer, String> universeNameMap = universeNames.stream().collect(Collectors.toMap(UniverseNameResponeDTO::getId, UniverseNameResponeDTO::getName, (k1, k2) -> k1));

        //redis缓存accesstoken
        String redisKey = GlobalConstants.ESI_ACCESS_TOKEN_KEY + charactorId + ":" + type;
        redisTemplate.opsForValue().set(redisKey, GlobalConstants.TOKEN_PERN + accessToken, 19*60 ,TimeUnit.SECONDS);

        String userId = (String)SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        EveCharacter eveCharacter = new EveCharacter();
        eveCharacter.setUserId(Integer.valueOf(userId));
        eveCharacter.setCharacterId(Integer.valueOf(charactorId));
        eveCharacter.setCharacterName(charactorName);
        eveCharacter.setCorpId(charactorInfoResponseDTO.getCorporation_id());
        eveCharacter.setCorpName(universeNameMap.get(charactorInfoResponseDTO.getCorporation_id()));
        eveCharacter.setAllianceId(charactorInfoResponseDTO.getAlliance_id());
        eveCharacter.setAllianceName(universeNameMap.get(charactorInfoResponseDTO.getAlliance_id()));

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
            default:
                throw new EsiException(ResultCode.COMMON_FAIL, "添加游戏角色失败");
        }

        LambdaUpdateWrapper<EveCharacter> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        lambdaUpdateWrapper.eq(EveCharacter::getCharacterId, eveCharacter.getCharacterId());

        eveCharacterService.update(eveCharacter, lambdaUpdateWrapper);
    }

    public JSONArray getJobList(String type, String id) throws ParseException {
        String accessToken = getAccessToken(type,id);
        return esiClient.getCharactorJob(id, accessToken);
    }


}
