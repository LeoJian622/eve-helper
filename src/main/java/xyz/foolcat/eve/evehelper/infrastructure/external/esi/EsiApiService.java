package xyz.foolcat.eve.evehelper.infrastructure.external.esi;

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
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterAccessTokenResult;
import xyz.foolcat.eve.evehelper.domain.port.cache.CacheGateway;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.domain.service.system.EveAccountService;
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
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;
import xyz.foolcat.eve.evehelper.shared.kernel.constants.GlobalConstants;
import xyz.foolcat.eve.evehelper.shared.kernel.enums.EsiAuthStatus;

import java.text.ParseException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
     * 授权状态缓存 TTL(成功态:AUTHORIZED/EXPIRED),秒
     */
    private static final long AUTH_STATUS_TTL_SECONDS = 5 * 60;

    /**
     * 授权状态缓存 TTL(UNKNOWN 态),秒;较短以便 ESI 恢复后尽快重试
     */
    private static final long AUTH_STATUS_UNKNOWN_TTL_SECONDS = 30;

    /**
     * 单角色 ESI 刷新超时(宪法:外部调用 5s 超时)
     */
    private static final Duration ESI_REFRESH_TIMEOUT = Duration.ofSeconds(5);

    /**
     * accessToken 在 Redis 中的缓存时长,秒(19 分钟,略短于 ESI 20 分钟有效期)
     */
    private static final long ACCESS_TOKEN_CACHE_TTL_SECONDS = 19 * 60;

    /**
     * ESI refreshToken 刷新锁键前缀(per-characterId)
     * <p>
     * ESI refreshToken 一次性使用,并发刷新会使后到的请求用已失效旧 token,
     * 且竞态回写可能落库已作废值导致角色绑定永久失效,故刷新必须串行化。
     * <p>
     * 键按 characterId 而非 (userId,characterId):refreshToken 是"每角色一份"的资源。
     * {@code doGetAccessToken} 与 {@code getAuthorizationStatus} 共用此前缀以保证互斥。
     */
    private static final String ESI_REFRESH_LOCK_KEY = "esi_refresh_lock:";

    /**
     * ESI 刷新锁 TTL,秒
     * <p>
     * 需覆盖临界区最坏耗时:token 刷新 5s + updateRefreshToken 内两次 ESI 调用(各 5s)+ DB 回写。
     */
    private static final long REFRESH_LOCK_TTL_SECONDS = 20;

    /**
     * 争用时等待持锁方填充缓存的轮询次数与间隔(合计约 1 秒)
     */
    private static final int LOCK_WAIT_ATTEMPTS = 10;

    /**
     * 争用轮询间隔,毫秒
     */
    private static final long LOCK_WAIT_INTERVAL_MILLIS = 100;

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
     * characterId为角色ID，则调用refreshtoken获取授权
     *
     * @param characterId 人物或者公司的ID
     * @param userId      当前用户 ID
     * @return 带前缀的 accessToken
     * @throws ParseException JWT 解析失败
     */
    @Override
    public String getAccessToken(Integer characterId, Integer userId) throws ParseException {
        return doGetAccessToken(characterId, userId).accessToken();
    }

    /**
     * 获取 accessToken 及其剩余有效期。
     * <p>
     * 归属校验、并发锁与缓存键构造均封装在本层,调用方无需(也不得)自行拼装缓存键。
     *
     * @param characterId 角色 ID
     * @param userId      当前用户 ID
     * @return 含 accessToken、characterId 与剩余有效秒数的读模型
     * @throws ParseException JWT 解析失败
     */
    @Override
    public CharacterAccessTokenResult getAccessTokenWithExpiry(Integer characterId, Integer userId) throws ParseException {
        return doGetAccessToken(characterId, userId);
    }

    /**
     * accessToken 获取的统一实现。
     * <p>
     * 归属校验前置:用传入 userId(来自 eveAccount.getUserId()),不依赖 SecurityContext,
     * HTTP 路径与内部路径(MiningTask 无安全上下文)统一适用。
     * catch 归属失败统一转 ESI_AUTHORIZATION_FAILURE(不暴露 USER_ACCOUNT_NOT_EXIST 账户存在性 oracle)。
     * <p>
     * 缓存未命中时以 per-(userId,characterId) 锁串行化刷新:ESI refreshToken 一次性使用,
     * 并发刷新会使后到的请求用已失效旧 token,且竞态回写可能落库已作废值导致角色绑定永久失效。
     */
    private CharacterAccessTokenResult doGetAccessToken(Integer characterId, Integer userId) throws ParseException {
        EveAccount character;
        try {
            character = eveAccountService.getAccountOne(userId, characterId);
        } catch (EveHelperException e) {
            log.warn("ESI accessToken 归属校验失败: userId={}, characterId={}", userId, characterId);
            throw new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE);
        }

        // 缓存键含 userId + characterId:用户隔离 + 角色隔离,读写统一 characterId
        Integer ownedCharacterId = character.getCharacterId();
        String redisKey = accessTokenKey(userId, ownedCharacterId);
        String cached = (String) cacheGateway.get(redisKey);
        if (StrUtil.isNotEmpty(cached)) {
            return cachedResult(redisKey, cached, ownedCharacterId);
        }

        // 刷新锁按 characterId 而非 (userId,characterId):refreshToken 存在 eve_account 的角色行上,
        // 与 getAuthorizationStatus 共用同一锁键,否则两条刷新路径不互斥,轮换竞态依然可达。
        String lockKey = refreshLockKey(ownedCharacterId);
        String lockOwner = newLockOwner();
        Boolean acquired = cacheGateway.setIfAbsent(lockKey, lockOwner, REFRESH_LOCK_TTL_SECONDS, TimeUnit.SECONDS);
        // fail-closed:Redis 异常或返回 null 时视为未获得锁。
        // 若写成 Boolean.FALSE.equals(acquired),null 会被当作"已持锁"而落入刷新,锁形同虚设。
        if (!Boolean.TRUE.equals(acquired)) {
            // 争用不是故障:持锁方通常会在数秒内填充缓存,故有界轮询后再读一次,
            // 而非直接把本地争用报成上游失败(那会误导调用方且无重试语义)。
            String filled = awaitCachedToken(redisKey);
            if (filled != null) {
                return cachedResult(redisKey, filled, ownedCharacterId);
            }
            log.warn("ESI accessToken 刷新争用未在等待窗口内获得结果: userId={}, characterId={}", userId, ownedCharacterId);
            throw new EsiException(ResultCode.ESI_SERVER_FAILURE);
        }
        try {
            // double-check:等锁期间可能已被其他请求填充
            cached = (String) cacheGateway.get(redisKey);
            if (StrUtil.isNotEmpty(cached)) {
                return cachedResult(redisKey, cached, ownedCharacterId);
            }

            AuthTokenResponse authToken = authorizeOAuth
                    .updateAccessToken(GrantType.REFRESH_TOKEN, character.getRefreshToken())
                    .timeout(ESI_REFRESH_TIMEOUT)
                    .block();
            // 不用 assert:JVM 默认不带 -ea,assert 为空语句,NPE 会退化为 500
            if (authToken == null || StrUtil.isBlank(authToken.getAccessToken())) {
                log.warn("ESI accessToken 刷新返回空 token: userId={}, characterId={}", userId, ownedCharacterId);
                throw new EsiException(ResultCode.ESI_SERVER_FAILURE);
            }
            updateRefreshToken(authToken, userId);
            Integer expiresIn = authToken.getExpiresIn();
            return new CharacterAccessTokenResult(
                    GlobalConstants.TOKEN_PERN + authToken.getAccessToken(),
                    ownedCharacterId,
                    expiresIn == null ? 0L : expiresIn.longValue());
        } finally {
            releaseRefreshLock(lockKey, lockOwner);
        }
    }

    /**
     * 构造 accessToken 缓存键。键格式仅在本层出现,不外泄给上层。
     */
    private String accessTokenKey(Integer userId, Integer characterId) {
        return GlobalConstants.ESI_ACCESS_TOKEN_KEY + userId + ":" + characterId;
    }

    /**
     * 构造 ESI 刷新锁键。
     * <p>
     * 按 characterId 而非 (userId, characterId):refreshToken 存储在 eve_account 的角色行上,
     * 是"每角色一份"的资源。{@code doGetAccessToken} 与 {@code getAuthorizationStatus}
     * 必须共用此键,否则两条刷新路径互不排斥,一次性 refreshToken 的轮换竞态依然可达。
     */
    private String refreshLockKey(Integer characterId) {
        return ESI_REFRESH_LOCK_KEY + characterId;
    }

    /**
     * 生成锁持有者标识。
     * <p>
     * 锁值必须唯一:若固定为 "1",则临界区超过锁 TTL 后锁自然过期、他人获得新锁,
     * 而慢请求的 finally 会无条件删除,把别人的锁删掉(误释放)。
     */
    private String newLockOwner() {
        return UUID.randomUUID().toString();
    }

    /**
     * 释放刷新锁,仅当锁未被他人接管(compare-and-delete)。
     * <p>
     * 只在读到的 owner 与本次**不同且非空**时跳过删除 —— 那是"临界区超时、锁已过期、
     * 他人重新获得"的情形,此时删除会误释放别人的锁。
     * 读到 null 仍执行删除:键可能刚过期(删除是 no-op),或 Redis 读瞬时失败,
     * 此时宁可删除也不要把锁留到 TTL 结束阻塞后续请求。
     * <p>
     * 非原子实现:CacheGateway 端口未暴露 Lua/CAS 能力。读与删之间的极窄窗口属残余风险,
     * 与原先的无条件删除相比严格更优。
     */
    private void releaseRefreshLock(String lockKey, String lockOwner) {
        try {
            Object currentOwner = cacheGateway.get(lockKey);
            if (currentOwner != null && !lockOwner.equals(currentOwner)) {
                // 临界区耗时超过锁 TTL,锁已被他人重新获得 —— 不可删除
                log.warn("ESI 刷新锁已被其他请求接管,跳过释放: lockKey={}", lockKey);
                return;
            }
            cacheGateway.delete(lockKey);
        } catch (Exception e) {
            // 释放失败不应掩盖业务异常;锁有 TTL 兜底,最多阻塞一个 TTL 周期
            log.warn("ESI 刷新锁释放失败: lockKey={}", lockKey, e);
        }
    }

    /**
     * 争用时有界等待持锁方填充 accessToken 缓存。
     * <p>
     * 并发刷新同一角色不是故障:持锁方成功后会写入缓存,等待方直接读取即可,
     * 无需也不应各自刷新(一次性 refreshToken 会因此失效)。
     *
     * @return 缓存中的 accessToken;等待窗口内未出现则返回 null
     */
    private String awaitCachedToken(String redisKey) {
        for (int i = 0; i < LOCK_WAIT_ATTEMPTS; i++) {
            try {
                TimeUnit.MILLISECONDS.sleep(LOCK_WAIT_INTERVAL_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
            String cached = (String) cacheGateway.get(redisKey);
            if (StrUtil.isNotEmpty(cached)) {
                return cached;
            }
        }
        return null;
    }

    /**
     * 由缓存命中构造读模型。抽出以保证缓存命中的两处路径(锁前快路径与持锁 double-check)不会漂移。
     */
    private CharacterAccessTokenResult cachedResult(String redisKey, String cached, Integer characterId) {
        return new CharacterAccessTokenResult(cached, characterId, remainingTtlSeconds(redisKey));
    }

    /**
     * 读取缓存键剩余有效秒数,并归一化 CacheGateway 的哨兵值。
     * 键不存在返回 -2、无 TTL 返回 -1;两者与 null 均回退为缓存 TTL 常量,
     * 避免向调用方报出 0(会被读作"已过期",诱发对刚取得的有效 token 立即重取)。
     */
    private long remainingTtlSeconds(String redisKey) {
        Long ttl = cacheGateway.getExpire(redisKey, TimeUnit.SECONDS);
        if (ttl == null || ttl < 0) {
            return ACCESS_TOKEN_CACHE_TTL_SECONDS;
        }
        return ttl;
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
    public String authorize(String code, Integer userId) throws ParseException {
        AuthTokenResponse authToken = authorizeOAuth.updateAccessToken(GrantType.AUTHORIZATION_CODE, code)
                .timeout(ESI_REFRESH_TIMEOUT)
                .block();
        // 不用 assert:JVM 默认不带 -ea,assert 为空语句,NPE 会退化为 500
        if (authToken == null || StrUtil.isBlank(authToken.getAccessToken())) {
            log.warn("ESI 授权码换取 token 返回空: userId={}", userId);
            throw new EsiException(ResultCode.ESI_SERVER_FAILURE);
        }
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
        // 加超时:这两次调用在刷新锁的临界区内,无超时会使临界区无界延长,
        // 锁 TTL 到期后他人获得新锁,一次性 refreshToken 的轮换竞态重新出现。
        CharacterPublicInfoResponse characterPublicInfoResponse = characterApi
                .queryCharacter(characterId, EsiClientConfig.SERENITY)
                .timeout(ESI_REFRESH_TIMEOUT)
                .block();
        //联盟军团名称
        // 不用 assert:JVM 默认不带 -ea,assert 为空语句,NPE 会退化为 500
        if (characterPublicInfoResponse == null) {
            log.warn("ESI 角色公开信息查询返回空: characterId={}", characterId);
            throw new EsiException(ResultCode.ESI_SERVER_FAILURE);
        }
        List<Id2NameResponse> nameResponses = universeApi
                .queryUniverseNames(List.of(characterPublicInfoResponse.getAllianceId(), characterPublicInfoResponse.getCorporationId()), EsiClientConfig.SERENITY)
                .collectList()
                .timeout(ESI_REFRESH_TIMEOUT)
                .block();
        if (nameResponses == null) {
            log.warn("ESI 名称解析返回空: characterId={}", characterId);
            throw new EsiException(ResultCode.ESI_SERVER_FAILURE);
        }
        Map<Integer, String> universeNameMap = nameResponses.stream().collect(Collectors.toMap(Id2NameResponse::getId, Id2NameResponse::getName, (k1, k2) -> k1));

        //redis缓存access_token
        String redisKey = accessTokenKey(userId, characterId);
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
     * @param account 绑定角色(需含 userId、refreshToken 与 characterId;userId 用于 accessToken 缓存键隔离)
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
        // 2. per-character 刷新锁:避免并发刷新导致 refreshToken 轮换竞态与缓存投毒
        //    (ESI refreshToken 一次性使用,并发刷新会使第二个请求用已失效旧 token 误判 EXPIRED)
        //    与 doGetAccessToken 共用同一锁键:两者都会调 updateAccessToken(REFRESH_TOKEN) 并轮换
        //    同一角色行上的 refreshToken,若各用一把锁则互不排斥,竞态依然可达。
        String lockKey = refreshLockKey(characterId);
        String lockOwner = newLockOwner();
        Boolean acquired = cacheGateway.setIfAbsent(lockKey, lockOwner, REFRESH_LOCK_TTL_SECONDS, TimeUnit.SECONDS);
        // fail-closed:Redis 异常或返回 null 时视为未获得锁
        if (!Boolean.TRUE.equals(acquired)) {
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
            releaseRefreshLock(lockKey, lockOwner);
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

    @Override
    public Integer queryCharacterWalletJournalMaxPage(Integer characterId, String accessToken) {
        return walletApi.queryCharacterWalletJournalMaxPage(characterId, EsiClientConfig.SERENITY, accessToken);
    }

    @Override
    public Flux<WalletJournal> queryCharacterWalletJournal(Integer characterId, int page, String accessToken) {
        return walletApi.queryCharacterWalletJournal(characterId, EsiClientConfig.SERENITY, page, accessToken)
                .map(wallet -> esiWalletJournalConverter.toDomain(wallet, characterId, resolveWalletCharacter(wallet)));
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
            // userId 为空时拒绝写缓存:否则键退化为 esi_access_token:null:{cid},
            // 多个用户共享同一键,构成跨用户 token 泄露风险(评审 H3 纵深防御)
            if (account.getUserId() == null) {
                log.warn("ESI accessToken 缓存写入被拒:角色缺少 userId,characterId={}", characterId);
                return;
            }
            String redisKey = accessTokenKey(account.getUserId(), characterId);
            cacheGateway.set(redisKey, GlobalConstants.TOKEN_PERN + accessToken, ACCESS_TOKEN_CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        }
    }

}