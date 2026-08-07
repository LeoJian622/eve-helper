package xyz.foolcat.eve.evehelper.domain.service.esi;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.port.cache.CacheGateway;
import xyz.foolcat.eve.evehelper.domain.service.system.EveAccountService;
import xyz.foolcat.eve.evehelper.infrastructure.assembler.esi.EsiAssetsConverter;
import xyz.foolcat.eve.evehelper.infrastructure.assembler.esi.EsiIndustryJobConverter;
import xyz.foolcat.eve.evehelper.infrastructure.assembler.esi.EsiInvTypesConverter;
import xyz.foolcat.eve.evehelper.infrastructure.assembler.esi.EsiMiningDetailConverter;
import xyz.foolcat.eve.evehelper.infrastructure.assembler.esi.EsiStructureConverter;
import xyz.foolcat.eve.evehelper.infrastructure.assembler.esi.EsiUniverseNameConverter;
import xyz.foolcat.eve.evehelper.infrastructure.assembler.esi.EsiWalletJournalConverter;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.EsiApiService;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.EsiClientConfig;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.EsiException;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.ResultCode;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.api.AssetsApi;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.api.CharacterApi;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.api.CorporationApi;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.api.IndustryApi;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.api.UniverseApi;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.api.WalletApi;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.auth.AuthorizeOAuth;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.auth.GrantType;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.AuthTokenResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.CharacterPublicInfoResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.Id2NameResponse;
import xyz.foolcat.eve.evehelper.shared.kernel.constants.GlobalConstants;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.text.ParseException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * EsiApiService 授权获取单元测试(Mockito,不启动 Spring 上下文)。
 * <p>
 * 覆盖 HIGH 1 修复:{@code getAccessToken(Integer, Integer)} 的归属校验前置、
 * 缓存键用户隔离({@code esi_access_token:{userId}:{characterId}})、缓存命中/未命中三态。
 *
 * @author Leojan
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ESI 授权获取测试 - 归属校验与缓存键隔离")
class EsiApiServiceTest {

    private static final Integer USER_ID = 100;
    private static final Integer CHARACTER_ID = 95465499;
    private static final Integer CORP_ID = 98000001;
    private static final Integer ALLIANCE_ID = 99000002;

    @Mock private CacheGateway cacheGateway;
    @Mock private EveAccountService eveAccountService;
    @Mock private AuthorizeOAuth authorizeOAuth;
    @Mock private CharacterApi characterApi;
    @Mock private UniverseApi universeApi;
    @Mock private AssetsApi assetsApi;
    @Mock private CorporationApi corporationApi;
    @Mock private IndustryApi industryApi;
    @Mock private WalletApi walletApi;
    @Mock private EsiAssetsConverter esiAssetsConverter;
    @Mock private EsiIndustryJobConverter esiIndustryJobConverter;
    @Mock private EsiInvTypesConverter esiInvTypesConverter;
    @Mock private EsiMiningDetailConverter esiMiningDetailConverter;
    @Mock private EsiStructureConverter esiStructureConverter;
    @Mock private EsiUniverseNameConverter esiUniverseNameConverter;
    @Mock private EsiWalletJournalConverter esiWalletJournalConverter;

    @InjectMocks private EsiApiService esiApiService;

    /**
     * 构造 RS256 签名 JWT(含 sub=CHARACTER:EVE:{characterId} 与 name claim),
     * 供 updateRefreshToken 内 SignedJWT.parse 提取 characterId。
     */
    private String buildSignedJwt(Integer characterId, String name) throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("CHARACTER:EVE:" + characterId)
                .claim("name", name)
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        jwt.sign(new RSASSASigner(kp.getPrivate()));
        return jwt.serialize();
    }

    @Test
    @DisplayName("归属校验失败统一抛 ESI_AUTHORIZATION_FAILURE,不泄露账户存在性 oracle")
    void getAccessToken_ownershipCheckFails_throwsEsiAuthorizationFailure() {
        // EveAccountService.getAccountOne 找不到账户抛 EveHelperException(USER_ACCOUNT_NOT_EXIST)
        when(eveAccountService.getAccountOne(USER_ID, CHARACTER_ID))
                .thenThrow(new EveHelperException(
                        xyz.foolcat.eve.evehelper.shared.result.ResultCode.USER_ACCOUNT_NOT_EXIST));

        EsiException ex = assertThrows(EsiException.class,
                () -> esiApiService.getAccessToken(CHARACTER_ID, USER_ID));

        assertEquals(ResultCode.ESI_AUTHORIZATION_FAILURE, ex.getResultCode());
        // 归属校验前置:缓存读取与 ESI 换 token 均不应触发
        verify(cacheGateway, never()).get(anyString());
        verify(authorizeOAuth, never()).updateAccessToken(any(), any());
    }

    @Test
    @DisplayName("缓存命中直接返回 token 且不触发 ESI 换 token")
    void getAccessToken_cacheHit_returnsCachedTokenAndSkipsRefresh() throws ParseException {
        EveAccount account = new EveAccount();
        account.setCharacterId(CHARACTER_ID);
        when(eveAccountService.getAccountOne(USER_ID, CHARACTER_ID)).thenReturn(account);
        String cachedToken = GlobalConstants.TOKEN_PERN + "cached-access";
        when(cacheGateway.get(GlobalConstants.ESI_ACCESS_TOKEN_KEY + USER_ID + ":" + CHARACTER_ID))
                .thenReturn(cachedToken);

        String token = esiApiService.getAccessToken(CHARACTER_ID, USER_ID);

        assertEquals(cachedToken, token);
        verify(authorizeOAuth, never()).updateAccessToken(any(), any());

        // 缓存键格式:esi_access_token:{userId}:{characterId}(用户隔离核心)
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(cacheGateway).get(keyCaptor.capture());
        assertEquals(GlobalConstants.ESI_ACCESS_TOKEN_KEY + USER_ID + ":" + CHARACTER_ID,
                keyCaptor.getValue());
    }

    @Test
    @DisplayName("缓存未命中换 token 成功,缓存键含 userId 并写入新 token")
    void getAccessToken_cacheMiss_refreshesAndCachesToken() throws Exception {
        EveAccount account = new EveAccount();
        account.setCharacterId(CHARACTER_ID);
        account.setRefreshToken("old-refresh");
        when(eveAccountService.getAccountOne(USER_ID, CHARACTER_ID)).thenReturn(account);
        when(cacheGateway.get(anyString())).thenReturn(null);

        String accessToken = buildSignedJwt(CHARACTER_ID, "TestPilot");
        AuthTokenResponse authToken = new AuthTokenResponse();
        authToken.setAccessToken(accessToken);
        authToken.setRefreshToken("new-refresh");
        when(authorizeOAuth.updateAccessToken(GrantType.REFRESH_TOKEN, "old-refresh"))
                .thenReturn(Mono.just(authToken));

        // updateRefreshToken 依赖:角色公开信息 + 名称解析
        CharacterPublicInfoResponse charInfo = new CharacterPublicInfoResponse();
        charInfo.setCorporationId(CORP_ID);
        charInfo.setAllianceId(ALLIANCE_ID);
        when(characterApi.queryCharacter(eq(CHARACTER_ID), eq(EsiClientConfig.SERENITY)))
                .thenReturn(Mono.just(charInfo));
        Id2NameResponse corp = new Id2NameResponse();
        corp.setId(CORP_ID);
        corp.setName("TestCorp");
        Id2NameResponse ally = new Id2NameResponse();
        ally.setId(ALLIANCE_ID);
        ally.setName("TestAlly");
        when(universeApi.queryUniverseNames(anyList(), eq(EsiClientConfig.SERENITY)))
                .thenReturn(Flux.just(corp, ally));

        String token = esiApiService.getAccessToken(CHARACTER_ID, USER_ID);

        assertEquals(GlobalConstants.TOKEN_PERN + accessToken, token);

        // 写缓存键格式:esi_access_token:{userId}:{characterId}
        ArgumentCaptor<String> writeKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(cacheGateway).set(writeKeyCaptor.capture(),
                eq(GlobalConstants.TOKEN_PERN + accessToken),
                anyLong(), eq(TimeUnit.SECONDS));
        assertEquals(GlobalConstants.ESI_ACCESS_TOKEN_KEY + USER_ID + ":" + CHARACTER_ID,
                writeKeyCaptor.getValue());
    }
}
