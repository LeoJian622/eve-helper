package xyz.foolcat.eve.evehelper.service.esi;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.client.EsiCharactorClient;
import xyz.foolcat.eve.evehelper.common.constant.GlobalConstants;
import xyz.foolcat.eve.evehelper.domain.system.EveCharacter;
import xyz.foolcat.eve.evehelper.domain.system.SysUser;
import xyz.foolcat.eve.evehelper.dto.esi.AuthTokenResponseDTO;
import xyz.foolcat.eve.evehelper.dto.esi.CharactorInfoResponseDTO;
import xyz.foolcat.eve.evehelper.dto.esi.UniverseNameResponeDTO;
import xyz.foolcat.eve.evehelper.exception.EveHelperException;
import xyz.foolcat.eve.evehelper.service.system.EveCharacterService;

import javax.annotation.Resource;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 国服ESI接口封装
 *
 * @author Leojan
 * @date 2021-12-07 16:54
 */

@Service
public class EsiChApiService {

    @Resource
    private EsiCharactorClient esiCharactorClient;

    @Resource
    private EveCharacterService eveCharacterService;

    public boolean getAccessToken(String type, String code) throws ParseException {

        AuthTokenResponseDTO authTokenResponseDTO = esiCharactorClient.addCharactorAuth("authorization_code", code, "bc90aa496a404724a93f41b4f4e97761");

        String charactorRefreshToken = authTokenResponseDTO.getRefresh_token();
        String accessToken = authTokenResponseDTO.getAccess_token();
        SignedJWT signedJWT = SignedJWT.parse(accessToken);
        JWTClaimsSet jwtClaimsSet = signedJWT.getJWTClaimsSet();
        String charactorID = jwtClaimsSet.getStringClaim("sub").split(":")[2];
        String charactorName = jwtClaimsSet.getStringClaim("name");
        CharactorInfoResponseDTO charactorInfoResponseDTO = esiCharactorClient.getCharactorInfo(charactorID, accessToken);
        List<UniverseNameResponeDTO> universeNames = esiCharactorClient.getUniverseName(Arrays.asList(charactorInfoResponseDTO.getAlliance_id(), charactorInfoResponseDTO.getCorporation_id()));
        Map<Integer, String> universeNameMap = universeNames.stream().collect(Collectors.toMap(UniverseNameResponeDTO::getId, UniverseNameResponeDTO::getName, (k1, k2) -> k1));

        SysUser sysUser = (SysUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        System.out.println(sysUser);

        EveCharacter eveCharacter = new EveCharacter();
        eveCharacter.setUserId(sysUser.getId());
        eveCharacter.setCharacterId(Integer.valueOf(charactorID));
        eveCharacter.setCharacterName(charactorName);
        eveCharacter.setCorpId(charactorInfoResponseDTO.getCorporation_id());
        eveCharacter.setCorpName(universeNameMap.get(charactorInfoResponseDTO.getCorporation_id()));
        eveCharacter.setAllianceId(charactorInfoResponseDTO.getAlliance_id());
        eveCharacter.setAllianceName(universeNameMap.get(charactorInfoResponseDTO.getAlliance_id()));

        switch (type) {
            case GlobalConstants.CHAR : {
                eveCharacter.setRefreshTokenChar(charactorRefreshToken);
                break;
            }
            case  GlobalConstants.CROP: {
                eveCharacter.setRefreshTokenCrop(charactorRefreshToken);
                break;
            }
            case  GlobalConstants.SKILL: {
                eveCharacter.setRefreshTokenSkill(charactorRefreshToken);
                break;
            }
            default:
                throw new EveHelperException("绑定游戏角色失败，类型异常");
        }

        LambdaUpdateWrapper<EveCharacter> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        lambdaUpdateWrapper.eq(EveCharacter::getCharacterId, eveCharacter.getCharacterId());

        return eveCharacterService.update(eveCharacter, lambdaUpdateWrapper);
    }
}
