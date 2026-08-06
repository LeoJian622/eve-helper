package xyz.foolcat.eve.evehelper.infrastructure.external.esi;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Assets;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.IndustryJob;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.InvTypes;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.MiningDetail;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Structure;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.UniverseName;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.WalletJournal;
import xyz.foolcat.eve.evehelper.domain.port.cache.CacheGateway;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.domain.service.system.EveAccountService;
import xyz.foolcat.eve.evehelper.domain.util.AuthorizeUtil;
import xyz.foolcat.eve.evehelper.infrastructure.assembler.esi.EsiAssetsConverter;
import xyz.foolcat.eve.evehelper.infrastructure.assembler.esi.EsiIndustryJobConverter;
import xyz.foolcat.eve.evehelper.infrastructure.assembler.esi.EsiInvTypesConverter;
import xyz.foolcat.eve.evehelper.infrastructure.assembler.esi.EsiMiningDetailConverter;
import xyz.foolcat.eve.evehelper.infrastructure.assembler.esi.EsiStructureConverter;
import xyz.foolcat.eve.evehelper.infrastructure.assembler.esi.EsiUniverseNameConverter;
import xyz.foolcat.eve.evehelper.infrastructure.assembler.esi.EsiWalletJournalConverter;
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
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.WalletJournalResponse;
import xyz.foolcat.eve.evehelper.shared.kernel.constants.GlobalConstants;
import xyz.foolcat.eve.evehelper.shared.kernel.enums.EsiAuthStatus;

import java.text.ParseException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 国服ESI接口封装(防腐层适配器,实现 {@link EsiGateway})。
 * <p>
 * 领域层通过 {@link EsiGateway} 端口消费 ESI 数据,不直接依赖本实现。
 *
 * @author Leojan
 * date 2021-12-07 16:54
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class EsiApiService implements EsiGateway {

    private final CacheGateway cacheGateway;

    private final EveAccountService eveAccountService;

    private final AuthorizeOAuth authorizeOAuth;

    private final CharacterApi characterApi;

    private final UniverseApi universeApi;

    private final AuthorizeUtil authorizeUtil;

    private final AssetsApi assetsApi;

    private final CorporationApi corporationApi;

    private final IndustryApi industryApi;

    private final WalletApi walletApi;

    private final EsiAssetsConverter esiAssetsConverter;

    private final EsiIndustryJobConverter esiIndustryJobConverter;

    private final EsiInvTypesConverter esiInvTypesConverter;

    private final EsiMiningDetailConverter esiMiningDetailConverter;

    private final EsiStructureConverter esiStructureConverter;

    private final EsiUniverseNameConverter esiUniverseNameConverter;

    private final EsiWalletJournalConverter esiWalletJournalConverter;

    /**
     * ESI 授权状态缓存键前缀
     */
    private static final String ESI_AUTH_STATUS_KEY = "esi_auth_status:";

    /**
     * 授权状态判定锁键前缀(per-character,防止并发刷新导致 refreshToken 轮换竞态与缓存投毒)
     */
    private static final String ESI_AUTH_STATUS_LOCK_KEY = "esi_auth_status_lock:";

    /**
     * 授权状态缓存 TTL(成功态:AUTHORIZED/EXPIRED),秒
     */
    private static final long AUTH_STATUS_TTL_SECONDS = 5 * 60;

    /**
     * 授权状态缓存 TTL(UNKNOWN 态),秒;较短以便 ESI 恢复后尽快重试
     */
    private static final long AUTH_STATUS_UNKNOWN_TTL_SECONDS = 30;

    /**
     * 授权状态判定锁 TTL,秒(覆盖 ESI 5s 超时 + DB 回写)
     */
    private static final long AUTH_STATUS_LOCK_TTL_SECONDS = 10;

    /**
     * 单角色 ESI 刷新超时(宪法:外部调用 5s 超时)
     */
    private static final Duration ESI_REFRESH_TIMEOUT = Duration.ofSeconds(5);

    /**
     * accessToken 在 Redis 中的缓存时长,秒(19 分钟,略短于 ESI 20 分钟有效期)
     */
    private static final long ACCESS_TOKEN_CACHE_TTL_SECONDS = 19 * 60;

    /**
     * 钱包流水赏金角色名解析(赏金池转移给{X})
     */
    private static final Pattern ESS_TYPE = Pattern.compile("赏金池转移给(.*)");

    /**
     * 钱包流水悬赏角色名解析({X}因在)
     */
    private static final Pattern BOUNTY_TYPE = Pattern.compile("(.*)因在");

    /**
     * 钱包流水军团报酬角色名解析(对{X}的服务给予报酬)
     */
    private static final Pattern DED_TYPE = Pattern.compile(".*对(.*)的服务给予报酬。");

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
    @Override
    public String getAccessToken(Integer code, Integer userId) throws ParseException {

        String redisKey = GlobalConstants.ESI_ACCESS_TOKEN_KEY + code;

        String accessToken = (String) cacheGateway.get(redisKey);

        if (StrUtil.isNotEmpty(accessToken)) {
            return accessToken;
        }

        EveAccount character = authorizeUtil.authorize(code);
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
    @Override
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
        CharacterPublicInfoResponse characterPublicInfoResponse = characterApi.queryCharacter(characterId, EsiClientConfig.SERENITY).block();
        //联盟军团名称
        assert characterPublicInfoResponse != null;
        List<Id2NameResponse> nameResponses = universeApi.queryUniverseNames(List.of(characterPublicInfoResponse.getAllianceId(), characterPublicInfoResponse.getCorporationId()), EsiClientConfig.SERENITY).collectList().block();
        assert nameResponses != null;
        Map<Integer, String> universeNameMap = nameResponses.stream().collect(Collectors.toMap(Id2NameResponse::getId, Id2NameResponse::getName, (k1, k2) -> k1));

        //redis缓存access_token
        String redisKey = GlobalConstants.ESI_ACCESS_TOKEN_KEY + characterId;
        cacheGateway.set(redisKey, GlobalConstants.TOKEN_PERN + accessToken, ACCESS_TOKEN_CACHE_TTL_SECONDS, TimeUnit.SECONDS);

        EveAccount eveAccount = new EveAccount();
        eveAccount.setUserId(userId);
        eveAccount.setCharacterId(characterId);
        eveAccount.setCharacterName(characterName);
        eveAccount.setCorpId(characterPublicInfoResponse.getCorporationId());
        eveAccount.setCorpName(universeNameMap.get(characterPublicInfoResponse.getCorporationId()));
        eveAccount.setAllianceId(characterPublicInfoResponse.getAllianceId());
        eveAccount.setAllianceName(universeNameMap.get(characterPublicInfoResponse.getAllianceId()));
        eveAccount.setRefreshToken(characterRefreshToken);
        eveAccountService.insertOrUpdate(eveAccount);
    }

    /**
     * 判定绑定角色的 ESI 授权状态。
     * <p>
     * 状态映射:
     * <ul>
     *     <li>refreshToken 为空 -> NOT_AUTHORIZED</li>
     *     <li>刷新成功 -> AUTHORIZED(回写新 refreshToken、缓存 accessToken)</li>
     *     <li>刷新抛 ESI_AUTHORIZATION_FAILURE(4xx) -> EXPIRED</li>
     *     <li>其余失败(5xx/网络/超时) -> UNKNOWN</li>
     * </ul>
     * 命中 Redis 状态缓存时直接返回,不触发 ESI 调用(满足 <200ms p95)。
     *
     * @param account 绑定角色(需含 refreshToken 与 characterId)
     * @return ESI 授权状态
     */
    @Override
    public EsiAuthStatus getAuthorizationStatus(EveAccount account) {
        if (account == null || StrUtil.isBlank(account.getRefreshToken())) {
            return EsiAuthStatus.NOT_AUTHORIZED;
        }
        Integer characterId = account.getCharacterId();
        String statusKey = ESI_AUTH_STATUS_KEY + characterId;
        // 1. 快路径:缓存命中直接返回
        EsiAuthStatus cached = readStatusCache(statusKey);
        if (cached != null) {
            return cached;
        }
        // 2. per-character 锁:避免并发刷新导致 refreshToken 轮换竞态与缓存投毒
        //    (ESI refreshToken 一次性使用,并发刷新会使第二个请求用已失效旧 token 误判 EXPIRED)
        String lockKey = ESI_AUTH_STATUS_LOCK_KEY + characterId;
        Boolean acquired = cacheGateway.setIfAbsent(lockKey, "1", AUTH_STATUS_LOCK_TTL_SECONDS, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(acquired)) {
            // 已有其他请求在判定:返回现有缓存或 UNKNOWN,不写缓存(避免投毒)
            EsiAuthStatus existing = readStatusCache(statusKey);
            return existing != null ? existing : EsiAuthStatus.UNKNOWN;
        }
        try {
            // double-check:持锁期间可能已被其他请求填充
            cached = readStatusCache(statusKey);
            if (cached != null) {
                return cached;
            }
            EsiAuthStatus status = determineAuthStatus(account);
            long ttl = status == EsiAuthStatus.UNKNOWN ? AUTH_STATUS_UNKNOWN_TTL_SECONDS : AUTH_STATUS_TTL_SECONDS;
            cacheGateway.set(statusKey, status.name(), ttl, TimeUnit.SECONDS);
            return status;
        } finally {
            cacheGateway.delete(lockKey);
        }
    }

    // ── 资产 ──

    @Override
    public Integer queryCharactersAssetsMaxPage(Integer characterId, String accessToken) {
        return assetsApi.queryCharactersAssetsMaxPage(characterId, EsiClientConfig.SERENITY, accessToken);
    }

    @Override
    public Flux<Assets> queryCharactersAssets(Integer characterId, int page, String accessToken) {
        return assetsApi.queryCharactersAssets(characterId, EsiClientConfig.SERENITY, page, accessToken)
                .map(esiAssetsConverter::toDomain);
    }

    // ── 蓝图 ──

    @Override
    public Integer queryCorporationBlueprintsMaxPage(Integer corporationId, String accessToken) {
        return corporationApi.queryCorporationBlueprintsMaxPage(corporationId, EsiClientConfig.SERENITY, accessToken);
    }

    // ── 工业 ──

    @Override
    public Integer queryCorporationIndustryJobsMaxPage(Integer corporationId, boolean includeCompleted, String accessToken) {
        return industryApi.queryCorporationIndustryJobsMaxPage(corporationId, EsiClientConfig.SERENITY, includeCompleted, accessToken);
    }

    @Override
    public Flux<IndustryJob> queryCorporationIndustryJobs(Integer corporationId, boolean includeCompleted, String accessToken) {
        return industryApi.queryCorporationIndustryJobs(corporationId, EsiClientConfig.SERENITY, includeCompleted, accessToken)
                .map(resp -> esiIndustryJobConverter.toDomain(resp, corporationId));
    }

    @Override
    public Flux<IndustryJob> queryCharacterIndustryJobs(Integer characterId, boolean includeCompleted, String accessToken) {
        return industryApi.queryCharacterIndustryJobs(characterId, EsiClientConfig.SERENITY, includeCompleted, accessToken)
                .map(resp -> esiIndustryJobConverter.toDomain(resp, null));
    }

    // ── 物品类型 ──

    @Override
    public Mono<InvTypes> queryUniverseType(Integer typeId, String language) {
        return universeApi.queryUniverseType(typeId, EsiClientConfig.SERENITY, language)
                .map(esiInvTypesConverter::toDomain);
    }

    // ── 采矿观察 ──

    @Override
    public Integer queryCorporationMiningObserverMaxPage(Integer corporationId, Long observerId, String accessToken) {
        return industryApi.queryCorporationMiningObserverMaxPage(corporationId, observerId, EsiClientConfig.SERENITY, accessToken);
    }

    @Override
    public Flux<MiningDetail> queryCorporationMiningObserver(Integer corporationId, Long observerId, int page, String accessToken) {
        return industryApi.queryCorporationMiningObserver(corporationId, EsiClientConfig.SERENITY, observerId, page, accessToken)
                .map(esiMiningDetailConverter::toDomain);
    }

    // ── 建筑 ──

    @Override
    public Integer queryCorporationStructuresMaxPage(Integer corporationId, String accessToken) {
        return corporationApi.queryCorporationStructuresMaxPage(corporationId, EsiClientConfig.SERENITY, accessToken);
    }

    @Override
    public Flux<Structure> queryCorporationStructures(Integer corporationId, String language, int page, String accessToken) {
        return corporationApi.queryCorporationStructures(corporationId, EsiClientConfig.SERENITY, language, page, accessToken)
                .map(esiStructureConverter::toDomain);
    }

    // ── 名称解析 ──

    @Override
    public Flux<UniverseName> queryUniverseNames(List<Integer> ids) {
        return universeApi.queryUniverseNames(ids, EsiClientConfig.SERENITY)
                .map(esiUniverseNameConverter::toDomain);
    }

    // ── 钱包流水 ──

    @Override
    public Integer queryCorporationWalletJournalMaxPage(Integer corporationId, int division, String accessToken) {
        return walletApi.queryCorporationWalletJournalMaxPage(corporationId, division, EsiClientConfig.SERENITY, accessToken);
    }

    @Override
    public Flux<WalletJournal> queryCorporationWalletJournal(Integer corporationId, int division, int page, String accessToken) {
        return walletApi.queryCorporationWalletJournal(corporationId, division, EsiClientConfig.SERENITY, page, accessToken)
                .map(wallet -> esiWalletJournalConverter.toDomain(wallet, corporationId, resolveWalletCharacter(wallet)));
    }

    /**
     * 从钱包流水描述解析关联角色名(悬赏/赏金池/军团报酬)。
     */
    private String resolveWalletCharacter(WalletJournalResponse wallet) {
        String character = "";
        if ("bounty_prizes".equals(wallet.getRefType())) {
            Matcher matcher = BOUNTY_TYPE.matcher(wallet.getDescription());
            if (matcher.find()) {
                character = matcher.group(1);
            }
        }
        if ("ess_escrow_transfer".equals(wallet.getRefType())) {
            Matcher matcher = ESS_TYPE.matcher(wallet.getDescription());
            if (matcher.find()) {
                character = matcher.group(1);
            }
        }
        if ("corporate_reward_payout".equals(wallet.getRefType())) {
            Matcher matcher = DED_TYPE.matcher(wallet.getDescription());
            if (matcher.find()) {
                character = matcher.group(1);
            }
        }
        return character;
    }

    /**
     * 读取状态缓存,非法值返回 null(触发重新判定)。
     */
    private EsiAuthStatus readStatusCache(String statusKey) {
        Object cached = cacheGateway.get(statusKey);
        if (cached instanceof String cachedName) {
            try {
                return EsiAuthStatus.valueOf(cachedName);
            } catch (IllegalArgumentException ignored) {
                // 缓存值非法,忽略后重新判定
            }
        }
        return null;
    }

    /**
     * 通过尝试 refreshToken 换取 accessToken 判定授权状态。
     * 任何异常均被兜底为 UNKNOWN 并记录日志,不向外传播(保证单角色异常不影响其他角色)。
     */
    private EsiAuthStatus determineAuthStatus(EveAccount account) {
        try {
            AuthTokenResponse authToken = authorizeOAuth
                    .updateAccessToken(GrantType.REFRESH_TOKEN, account.getRefreshToken())
                    .timeout(ESI_REFRESH_TIMEOUT)
                    .block();
            if (authToken == null || StrUtil.isBlank(authToken.getAccessToken())) {
                log.warn("ESI 授权状态判定返回空 token,characterId={}", account.getCharacterId());
                return EsiAuthStatus.UNKNOWN;
            }
            persistRefreshedToken(account, authToken);
            return EsiAuthStatus.AUTHORIZED;
        } catch (EsiException e) {
            if (ResultCode.ESI_AUTHORIZATION_FAILURE.equals(e.getResultCode())) {
                return EsiAuthStatus.EXPIRED;
            }
            log.warn("ESI 授权状态判定失败(服务端异常),characterId={}", account.getCharacterId(), e);
            return EsiAuthStatus.UNKNOWN;
        } catch (Exception e) {
            log.warn("ESI 授权状态判定异常,characterId={}", account.getCharacterId(), e);
            return EsiAuthStatus.UNKNOWN;
        }
    }

    /**
     * 刷新成功后回写新 refreshToken(ESI refreshToken 一次性使用,必须持久化新值)并缓存 accessToken。
     * 先持久化 refreshToken 再缓存 accessToken,确保 DB 写失败时不缓存无对应 refreshToken 的 accessToken。
     * 使用 insertOrUpdateSelective 仅更新 refresh_token 列,避免覆盖其他字段。
     */
    private void persistRefreshedToken(EveAccount account, AuthTokenResponse authToken) {
        Integer characterId = account.getCharacterId();
        String accessToken = authToken.getAccessToken();
        String newRefreshToken = authToken.getRefreshToken();
        if (StrUtil.isNotBlank(newRefreshToken) && !newRefreshToken.equals(account.getRefreshToken())) {
            EveAccount update = new EveAccount();
            update.setCharacterId(characterId);
            update.setRefreshToken(newRefreshToken);
            int rows = eveAccountService.insertOrUpdateSelective(update);
            if (rows <= 0) {
                log.error("ESI refreshToken 回写未命中行,characterId={},用户需重新授权", characterId);
            }
        }
        if (StrUtil.isNotBlank(accessToken)) {
            String redisKey = GlobalConstants.ESI_ACCESS_TOKEN_KEY + characterId;
            cacheGateway.set(redisKey, GlobalConstants.TOKEN_PERN + accessToken, ACCESS_TOKEN_CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        }
    }

}