package xyz.foolcat.eve.evehelper.infrastructure.gateway.esi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import xyz.foolcat.eve.evehelper.domain.port.cache.CacheGateway;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiAccessException;
import xyz.foolcat.eve.evehelper.domain.service.system.EveAccountService;
import xyz.foolcat.eve.evehelper.infrastructure.assembler.esi.*;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.EsiException;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.ResultCode;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.api.*;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.auth.AuthorizeOAuth;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.cache.EsiCacheTtlExtractor;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.cache.EsiResponseCache;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.cache.EsiResponseCacheProperties;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.cache.ForceRefreshLimiter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * IMP-1 回归:单对象路径(callSdkMono)与列表路径(doListWithClaim)错误语义一致性。
 * <p>
 * SDK 真实错误是异步 {@code Mono.error(EsiException)}({@code EsiStatusUtil.dataError()} /
 * {@code handleResponseWithHeaders}),两条路径必须把同一 ESI 错误折叠为同一
 * {@link EsiAccessException} 结果码:403→ESI00403、其余 4xx→ESI00400、5xx→ESI00500,
 * 使全局异常处理器对同一 ESI 状态在两类端点映射同一 HTTP 语义。
 * <p>
 * 单对象样本:{@code queryCharacterPublicInfo}(callSdkMono + block);
 * 列表样本:{@code queryUniverseNames}(doListWithClaim + onErrorMap)。
 *
 * @author Leojan
 * date 2026-11-24
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IMP-1 单对象/列表路径 ESI 错误语义一致性")
class EsiApiServiceErrorConsistencyTest {

    @Mock private CacheGateway cacheGateway;
    @Mock private EveAccountService eveAccountService;
    @Mock private AuthorizeOAuth authorizeOAuth;
    @Mock private CharacterApi characterApi;
    @Mock private UniverseApi universeApi;
    @Mock private AssetsApi assetsApi;
    @Mock private CorporationApi corporationApi;
    @Mock private IndustryApi industryApi;
    @Mock private WalletApi walletApi;
    @Mock private LocationApi locationApi;
    @Mock private SkillApi skillApi;
    @Mock private MailApi mailApi;
    @Mock private EsiAssetsConverter esiAssetsConverter;
    @Mock private EsiIndustryJobConverter esiIndustryJobConverter;
    @Mock private EsiInvTypesConverter esiInvTypesConverter;
    @Mock private EsiMiningDetailConverter esiMiningDetailConverter;
    @Mock private EsiStructureConverter esiStructureConverter;
    @Mock private EsiUniverseNameConverter esiUniverseNameConverter;
    @Mock private EsiWalletJournalConverter esiWalletJournalConverter;
    @Mock private EsiWalletTransactionConverter esiWalletTransactionConverter;
    @Mock private EsiResponseCache esiResponseCache;
    @Mock private EsiCacheTtlExtractor esiCacheTtlExtractor;
    @Mock private ForceRefreshLimiter forceRefreshLimiter;
    @Mock private EsiResponseCacheProperties esiResponseCacheProperties;

    private EsiApiService esiApiService;

    private static final Integer CHARACTER_ID = 95465499;
    private static final String DATASOURCE = "serenity";

    @BeforeEach
    void setUp() {
        esiApiService = new EsiApiService(
                cacheGateway, eveAccountService, authorizeOAuth,
                characterApi, universeApi, assetsApi, corporationApi, industryApi, walletApi, locationApi, skillApi, mailApi,
                esiAssetsConverter, esiIndustryJobConverter, esiInvTypesConverter,
                esiMiningDetailConverter, esiStructureConverter,
                esiUniverseNameConverter, esiWalletJournalConverter, esiWalletTransactionConverter,
                esiResponseCache, esiCacheTtlExtractor, forceRefreshLimiter,
                esiResponseCacheProperties, new ObjectMapper()
        );
        lenient().when(esiResponseCacheProperties.getDatasource()).thenReturn(DATASOURCE);
        lenient().when(esiResponseCache.cacheKey(anyString(), any(), anyString(), anyInt())).thenReturn("dummy-key");
        lenient().when(esiResponseCache.getList(anyString(), any())).thenReturn(null);
        lenient().when(esiResponseCache.tryClaim(anyString(), any())).thenReturn(true);
    }

    /** 单对象路径真调注入异步错误。 */
    private void stubSingleObjectError(EsiException error) {
        when(characterApi.queryCharacterResponse(eq(CHARACTER_ID), eq(DATASOURCE)))
                .thenReturn(Mono.error(error));
    }

    /** 列表路径真调注入异步错误。 */
    private void stubListError(EsiException error) {
        when(universeApi.queryUniverseNamesResponse(anyList(), eq(DATASOURCE)))
                .thenReturn(Mono.error(error));
    }

    private EsiAccessException catchSingleObject() {
        return catchThrowableOfType(
                EsiAccessException.class,
                () -> esiApiService.queryCharacterPublicInfo(CHARACTER_ID));
    }

    private EsiAccessException catchList() {
        return catchThrowableOfType(
                EsiAccessException.class,
                () -> esiApiService.queryUniverseNames(List.of(1)).collectList().block());
    }

    @Test
    @DisplayName("ESI 403 → 两路径均 EsiAccessException + ESI00403(HTTP 403 语义一致)")
    void esi403_bothPaths_permissionLowCode() {
        stubSingleObjectError(new EsiException(ResultCode.ESI_AUTH_PERMISSION_LOW));
        stubListError(new EsiException(ResultCode.ESI_AUTH_PERMISSION_LOW));

        EsiAccessException single = catchSingleObject();
        EsiAccessException list = catchList();

        assertThat(single).as("单对象路径必须转换为 EsiAccessException").isNotNull();
        assertThat(list).as("列表路径必须转换为 EsiAccessException").isNotNull();
        assertThat(single.getResultCode()).isNotNull();
        assertThat(single.getResultCode().getCode()).isEqualTo("ESI00403");
        assertThat(list.getResultCode()).isNotNull();
        assertThat(list.getResultCode().getCode()).isEqualTo(single.getResultCode().getCode());
        assertThat(single.getMessage()).isEqualTo(EsiAccessException.MSG_PERMISSION_LOW);
        assertThat(list.getMessage()).isEqualTo(single.getMessage());
    }

    @Test
    @DisplayName("ESI 其余 4xx(含 404) → 两路径均 ESI00400(授权失效语义一致)")
    void esiOther4xx_bothPaths_authorizationFailureCode() {
        stubSingleObjectError(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE));
        stubListError(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE));

        EsiAccessException single = catchSingleObject();
        EsiAccessException list = catchList();

        assertThat(single).isNotNull();
        assertThat(list).isNotNull();
        assertThat(single.getResultCode()).isNotNull();
        assertThat(single.getResultCode().getCode()).isEqualTo("ESI00400");
        assertThat(list.getResultCode()).isNotNull();
        assertThat(list.getResultCode().getCode()).isEqualTo(single.getResultCode().getCode());
        assertThat(single.getMessage()).isEqualTo(EsiAccessException.MSG_AUTHORIZATION_FAILURE);
        assertThat(list.getMessage()).isEqualTo(single.getMessage());
    }

    @Test
    @DisplayName("ESI 5xx → 两路径均 ESI00500(服务端错误语义一致)")
    void esi5xx_bothPaths_serverFailureCode() {
        stubSingleObjectError(new EsiException(ResultCode.ESI_SERVER_FAILURE));
        stubListError(new EsiException(ResultCode.ESI_SERVER_FAILURE));

        EsiAccessException single = catchSingleObject();
        EsiAccessException list = catchList();

        assertThat(single).isNotNull();
        assertThat(list).isNotNull();
        assertThat(single.getResultCode()).isNotNull();
        assertThat(single.getResultCode().getCode()).isEqualTo("ESI00500");
        assertThat(list.getResultCode()).isNotNull();
        assertThat(list.getResultCode().getCode()).isEqualTo(single.getResultCode().getCode());
        assertThat(single.getMessage()).isEqualTo(EsiAccessException.MSG_SERVER_FAILURE);
        assertThat(list.getMessage()).isEqualTo(single.getMessage());
    }

    @Test
    @DisplayName("无 ResultCode 的 EsiException → 两路径均保留原 message 且 resultCode=null(默认 400 语义一致)")
    void esiUnknown_bothPaths_nullResultCodeConsistent() {
        EsiException raw = new EsiException("esi unknown failure", new RuntimeException("cause"));
        stubSingleObjectError(raw);
        stubListError(raw);

        EsiAccessException single = catchSingleObject();
        EsiAccessException list = catchList();

        assertThat(single).isNotNull();
        assertThat(list).isNotNull();
        assertThat(single.getResultCode()).isNull();
        assertThat(list.getResultCode()).isNull();
        assertThat(single.getMessage()).isEqualTo("esi unknown failure");
        assertThat(list.getMessage()).isEqualTo(single.getMessage());
    }

    @Test
    @DisplayName("单对象路径异步错误不得以 infra EsiException 原样泄漏(防腐层职责)")
    void singleObject_asyncError_notLeakInfraException() {
        stubSingleObjectError(new EsiException(ResultCode.ESI_AUTH_PERMISSION_LOW));

        assertThatThrownBy(() -> esiApiService.queryCharacterPublicInfo(CHARACTER_ID))
                .isInstanceOf(EsiAccessException.class)
                .isNotInstanceOf(EsiException.class);
    }

    @Test
    @DisplayName("列表路径异步错误不得以 infra EsiException 原样泄漏(既有行为保持)")
    void list_asyncError_notLeakInfraException() {
        stubListError(new EsiException(ResultCode.ESI_AUTH_PERMISSION_LOW));

        assertThatThrownBy(() -> esiApiService.queryUniverseNames(List.of(1)).collectList().block())
                .isInstanceOf(EsiAccessException.class)
                .isNotInstanceOf(EsiException.class);
    }
}