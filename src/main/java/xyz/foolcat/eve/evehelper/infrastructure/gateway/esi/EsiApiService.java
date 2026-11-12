package xyz.foolcat.eve.evehelper.infrastructure.gateway.esi;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.*;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterAccessTokenResult;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterAttributesInfo;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterLocationInfo;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterOnlineInfo;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterPublicInfo;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterSkillList;
import xyz.foolcat.eve.evehelper.domain.model.vo.EsiAuthorizationDetail;
import xyz.foolcat.eve.evehelper.domain.model.vo.MailDetailInfo;
import xyz.foolcat.eve.evehelper.domain.model.vo.MailHeaderPage;
import xyz.foolcat.eve.evehelper.domain.model.vo.MailLabelsInfo;
import xyz.foolcat.eve.evehelper.domain.model.vo.MailRecipient;
import xyz.foolcat.eve.evehelper.domain.model.vo.RecipientResolution;
import xyz.foolcat.eve.evehelper.domain.model.vo.SendMailCommand;
import xyz.foolcat.eve.evehelper.domain.model.vo.SkillQueueItem;
import xyz.foolcat.eve.evehelper.domain.model.vo.WalletDivisionBalance;
import xyz.foolcat.eve.evehelper.domain.port.cache.CacheGateway;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiAccessException;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.domain.service.system.EveAccountService;
import xyz.foolcat.eve.evehelper.domain.util.UserUtil;
import xyz.foolcat.eve.evehelper.infrastructure.assembler.esi.*;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.EsiClientConfig;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.EsiClientConstants;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.EsiException;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.ResultCode;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.api.*;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.auth.AuthorizeOAuth;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.auth.GrantType;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.cache.EsiCacheTtlExtractor;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.cache.EsiResponseCache;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.cache.EsiResponseCacheProperties;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.cache.ForceRefreshLimiter;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.*;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.send.NewMail;
import xyz.foolcat.eve.evehelper.shared.kernel.constants.GlobalConstants;
import xyz.foolcat.eve.evehelper.shared.kernel.enums.EsiAuthStatus;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;

import java.text.ParseException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
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

    private final LocationApi locationApi;

    private final SkillApi skillApi;

    private final MailApi mailApi;

    private final EsiAssetsConverter esiAssetsConverter;

    private final EsiIndustryJobConverter esiIndustryJobConverter;

    private final EsiInvTypesConverter esiInvTypesConverter;

    private final EsiMiningDetailConverter esiMiningDetailConverter;

    private final EsiStructureConverter esiStructureConverter;

    private final EsiUniverseNameConverter esiUniverseNameConverter;

    private final EsiWalletJournalConverter esiWalletJournalConverter;

    private final EsiWalletTransactionConverter esiWalletTransactionConverter;

    private final EsiResponseCache esiResponseCache;

    private final EsiCacheTtlExtractor esiCacheTtlExtractor;

    private final ForceRefreshLimiter forceRefreshLimiter;

    private final EsiResponseCacheProperties esiResponseCacheProperties;

    private final ObjectMapper objectMapper;

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
     * 防并发穿透占位锁 TTL(FR-008):覆盖真调 + 回填最坏耗时,参照 refresh 锁 20s
     * <p>
     * EsiResponseCache.tryClaim/releaseClaim 用 {@code key:lock} 的 setIfAbsent 原子占位,
     * 持锁方承担真实 ESI 调用并回填,未抢到锁的并发请求有界轮询复用其回填结果。
     */
    private static final Duration CLAIM_TTL = Duration.ofSeconds(REFRESH_LOCK_TTL_SECONDS);

    /**
     * 防穿透未抢锁方的有界轮询预算(H1 修正):~5s,与 ESI_DATA_TIMEOUT(5s)对齐。
     * <p>
     * 真实 ESI 真调常 &gt;1s,原 10×100ms≈1s 预算在持锁方真调期间就耗尽,并发请求各自
     * fallback 真调 → N~N 恰好绕过防穿透(SC-002 10→1 不成立)。扩至 50×100ms≈5s 覆盖真实
     * 真调全程(真调上限 5s timeout),总量 &lt;= CLAIM_TTL(20s)。仅争用路径(claimed=false)付费。</p>
     */
    private static final int CLAIM_WAIT_ATTEMPTS = 50;

    /**
     * 防穿透轮询间隔,毫秒(25 行内以 100ms 节奏,合计 5s)。
     */
    private static final long CLAIM_WAIT_INTERVAL_MILLIS = 100;

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
     * 释放刷新锁,使用原子性的 compare-and-delete。
     * <p>
     * 通过 CacheGateway.compareAndDelete 实现原子性比较并删除,
     * 避免在读取和删除之间的窗口期内锁被其他线程获取后误释放。
     * <p>
     * 仅当锁的当前值等于本次持有的 owner 时才删除;
     * 若锁已被他人接管(临界区超时、锁过期后他人重新获得),则跳过删除。
     */
    private void releaseRefreshLock(String lockKey, String lockOwner) {
        try {
            boolean deleted = cacheGateway.compareAndDelete(lockKey, lockOwner);
            if (!deleted) {
                // 锁已被他人接管或已过期,跳过释放
                log.warn("ESI 刷新锁已被其他请求接管或已过期,跳过释放: lockKey={}", lockKey);
            }
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

    /**
     * 判定绑定角色的 ESI 授权状态,并在 AUTHORIZED 时解析已授权权限(scopes)。
     * <p>
     * 组合 {@link #getAuthorizationStatus}(复用全部状态缓存/刷新锁/回写逻辑),
     * 仅 AUTHORIZED 时再从缓存层已持有的 accessToken(JWT {@code scp} claim)运行时解析 scopes,
     * 不因本方法产生额外的上游 ESI 调用或 refreshToken 刷新。
     *
     * @param account 绑定角色(需含 userId、refreshToken 与 characterId)
     * @return 授权状态 + 已授权权限(scopes 为 null 表示无法确定;空列表表示 token 有效但未声明受限权限)
     */
    @Override
    public EsiAuthorizationDetail getAuthorizationDetail(EveAccount account) {
        EsiAuthStatus status = getAuthorizationStatus(account);
        if (status != EsiAuthStatus.AUTHORIZED) {
            return new EsiAuthorizationDetail(status, null);
        }
        return new EsiAuthorizationDetail(status, parseScopesFromCachedToken(account));
    }

    /**
     * 从缓存层已持有的 accessToken 解析已授权权限(scopes)。
     * <p>
     * userId/characterId 缺失、缓存未命中或解析失败时返回 null(无法确定);
     * 日志不记录任何 token 内容(FR-009)。
     */
    private List<String> parseScopesFromCachedToken(EveAccount account) {
        // 守卫置于 try 之外:保证 catch 中 account 必非 null(不变量结构化,评审 Minor-1)
        if (account == null || account.getUserId() == null || account.getCharacterId() == null) {
            return null;
        }
        try {
            String cached = (String) cacheGateway.get(accessTokenKey(account.getUserId(), account.getCharacterId()));
            // isBlank 而非兄弟缓存读的 isNotEmpty:空串/空白同样视为无 token 可解析,属有意的 fail-soft 选择(评审 Minor-2)
            if (StrUtil.isBlank(cached)) {
                return null;
            }
            String token = cached.startsWith(GlobalConstants.TOKEN_PERN)
                    ? cached.substring(GlobalConstants.TOKEN_PERN.length()) : cached;
            return toScopeList(SignedJWT.parse(token).getJWTClaimsSet().getClaim("scp"));
        } catch (Exception e) {
            log.warn("ESI scopes 解析失败,fail-soft 返回 null,characterId={}", account.getCharacterId());
            return null;
        }
    }

    /**
     * scp 声明通用解析:JSON 数组 → 逐项字符串;空格分隔字符串 → 切分;
     * 缺失/类型未知 → null(无法确定)。空集合/空串 → 空列表(确认为空)。
     */
    private List<String> toScopeList(Object scpClaim) {
        if (scpClaim instanceof List<?> list) {
            return list.stream().filter(Objects::nonNull).map(String::valueOf)
                    .filter(s -> !s.isBlank()).toList();
        }
        if (scpClaim instanceof String s) {
            return s.isBlank() ? List.of() : Arrays.stream(s.trim().split("\\s+"))
                    .filter(seg -> !seg.isBlank()).toList();
        }
        return null;
    }

    // ── 资产 ──

    @Override
    public Integer queryCharactersAssetsMaxPage(Integer characterId, String accessToken) {
        return doQueryCharactersAssetsMaxPage(characterId, accessToken, false);
    }

    @Override
    public Integer queryCharactersAssetsMaxPage(Integer characterId, String accessToken, boolean forceRefresh) {
        return doQueryCharactersAssetsMaxPage(characterId, accessToken, forceRefresh);
    }

    private Integer doQueryCharactersAssetsMaxPage(Integer characterId, String accessToken, boolean forceRefresh) {
        Integer userId = UserUtil.getUserId();
        if (userId == null || userId <= 0) {
            // 未认证/无上下文:不缓存,直接真调(安全优先)
            return callSdk(() -> assetsApi.queryCharactersAssetsMaxPage(characterId, EsiClientConfig.SERENITY, accessToken));
        }
        // maxPage 用独立 :maxPage 键,不与列表首页 page=1 共用(避免互相覆盖震荡,T008 评审 M1 修复回归)
        String key = esiResponseCache.cacheKey(esiResponseCacheProperties.getDatasource(), userId,
                "/characters/" + characterId + "/assets/", "maxPage");
        boolean forceGranted = forceRefresh && forceRefreshLimiter.tryAcquire(key, forceRefreshWindow(), forceRefreshLimit());
        if (!forceGranted) {
            Integer cached = esiResponseCache.getInt(key);
            if (cached != null) {
                return cached;
            }
        }
        // 防穿透:持锁承担真调回填,未抢锁有界轮询复用回填,超时 fail-open 真调
        return doMaxPageWithClaim(key, forceGranted, () -> assetsApi.queryCharactersAssetsMaxPageResponse(
                characterId, EsiClientConfig.SERENITY, accessToken));
    }

    @Override
    public Flux<Assets> queryCharactersAssets(Integer characterId, int page, String accessToken) {
        return doQueryCharactersAssets(characterId, page, accessToken, false);
    }

    @Override
    public Flux<Assets> queryCharactersAssets(Integer characterId, int page, String accessToken, boolean forceRefresh) {
        return doQueryCharactersAssets(characterId, page, accessToken, forceRefresh);
    }

    private Flux<Assets> doQueryCharactersAssets(Integer characterId, int page, String accessToken, boolean forceRefresh) {
        Integer userId = UserUtil.getUserId();
        if (userId == null || userId <= 0) {
            // 未认证/无上下文:不缓存,直接真调(安全优先)
            return callSdk(() -> callSdk(() -> assetsApi.queryCharactersAssets(characterId, EsiClientConfig.SERENITY, page, accessToken)))
                    .map(esiAssetsConverter::toDomain);
        }
        String key = esiResponseCache.cacheKey(esiResponseCacheProperties.getDatasource(), userId,
                "/characters/" + characterId + "/assets/", page);
        boolean forceGranted = forceRefresh && forceRefreshLimiter.tryAcquire(key, forceRefreshWindow(), forceRefreshLimit());
        if (!forceGranted) {
            List<Assets> cached = esiResponseCache.getList(key, objectMapper.constructType(Assets.class));
            if (cached != null) {
                return Flux.fromIterable(cached);
            }
        }
        // 防穿透:持锁承担真调回填,未抢锁有界轮询复用回填,超时 fail-open 真调
        return doListWithClaim(key, Assets.class, forceGranted, () ->
                assetsApi.queryCharactersAssetsResponse(characterId, EsiClientConfig.SERENITY, page, accessToken)
                        .mapNotNull(response -> {
                            Duration ttl = esiCacheTtlExtractor.extract(response.getHeaders());
                            // 空 body → 空列表回填(命中时空列表与未命中应以 !=null 区分)
                            List<Assets> domain = response.getBody() == null ? List.of()
                                    : response.getBody().stream().map(esiAssetsConverter::toDomain).toList();
                            esiResponseCache.put(key, domain, ttl);   // ttl null → put 内部跳过
                            return domain;
                        }));
    }

    // ── 蓝图 ──

    @Override
    public Integer queryCorporationBlueprintsMaxPage(Integer corporationId, String accessToken) {
        return doQueryCorporationBlueprintsMaxPage(corporationId, accessToken, false);
    }

    @Override
    public Integer queryCorporationBlueprintsMaxPage(Integer corporationId, String accessToken, boolean forceRefresh) {
        return doQueryCorporationBlueprintsMaxPage(corporationId, accessToken, forceRefresh);
    }

    private Integer doQueryCorporationBlueprintsMaxPage(Integer corporationId, String accessToken, boolean forceRefresh) {
        Integer userId = UserUtil.getUserId();
        if (userId == null || userId <= 0) {
            // 未认证/无上下文:不缓存,直接真调(安全优先)
            return callSdk(() -> corporationApi.queryCorporationBlueprintsMaxPage(corporationId, EsiClientConfig.SERENITY, accessToken));
        }
        // maxPage 用独立 :maxPage 键,不与列表首页 page=1 共用(避免互相覆盖震荡,T008 评审 M1)
        String key = esiResponseCache.cacheKey(esiResponseCacheProperties.getDatasource(), userId,
                "/corporations/" + corporationId + "/blueprints/", "maxPage");
        boolean forceGranted = forceRefresh && forceRefreshLimiter.tryAcquire(key, forceRefreshWindow(), forceRefreshLimit());
        if (!forceGranted) {
            Integer cached = esiResponseCache.getInt(key);
            if (cached != null) {
                return cached;
            }
        }
        // 防穿透:持锁承担真调回填,未抢锁有界轮询复用回填,超时 fail-open 真调
        return doMaxPageWithClaim(key, forceGranted, () -> corporationApi.queryCorporationBlueprintsMaxPageResponse(
                corporationId, EsiClientConfig.SERENITY, accessToken));
    }

    // ── 工业 ──

    @Override
    public Integer queryCorporationIndustryJobsMaxPage(Integer corporationId, boolean includeCompleted, String accessToken) {
        return doQueryCorporationIndustryJobsMaxPage(corporationId, includeCompleted, accessToken, false);
    }

    @Override
    public Integer queryCorporationIndustryJobsMaxPage(Integer corporationId, boolean includeCompleted, String accessToken, boolean forceRefresh) {
        return doQueryCorporationIndustryJobsMaxPage(corporationId, includeCompleted, accessToken, forceRefresh);
    }

    private Integer doQueryCorporationIndustryJobsMaxPage(Integer corporationId, boolean includeCompleted, String accessToken, boolean forceRefresh) {
        Integer userId = UserUtil.getUserId();
        if (userId == null || userId <= 0) {
            // 未认证/无上下文:不缓存,直接真调(安全优先)
            return callSdk(() -> industryApi.queryCorporationIndustryJobsMaxPage(corporationId, EsiClientConfig.SERENITY, includeCompleted, accessToken));
        }
        // 军团工业列表统一归一化 uri;maxPage 用独立 :maxPage 键
        String key = esiResponseCache.cacheKey(esiResponseCacheProperties.getDatasource(), userId,
                "/corporations/" + corporationId + "/industry/jobs/", "maxPage");
        boolean forceGranted = forceRefresh && forceRefreshLimiter.tryAcquire(key, forceRefreshWindow(), forceRefreshLimit());
        if (!forceGranted) {
            Integer cached = esiResponseCache.getInt(key);
            if (cached != null) {
                return cached;
            }
        }
        // 防穿透:持锁承担真调回填,未抢锁有界轮询复用回填,超时 fail-open 真调
        return doMaxPageWithClaim(key, forceGranted, () -> industryApi.queryCorporationIndustryJobsMaxPageResponse(
                corporationId, EsiClientConfig.SERENITY, includeCompleted, accessToken));
    }

    @Override
    public Flux<IndustryJob> queryCorporationIndustryJobs(Integer corporationId, boolean includeCompleted, String accessToken) {
        return doQueryCorporationIndustryJobs(corporationId, includeCompleted, accessToken, false);
    }

    @Override
    public Flux<IndustryJob> queryCorporationIndustryJobs(Integer corporationId, boolean includeCompleted, String accessToken, boolean forceRefresh) {
        return doQueryCorporationIndustryJobs(corporationId, includeCompleted, accessToken, forceRefresh);
    }

    private Flux<IndustryJob> doQueryCorporationIndustryJobs(Integer corporationId, boolean includeCompleted, String accessToken, boolean forceRefresh) {
        Integer userId = UserUtil.getUserId();
        if (userId == null || userId <= 0) {
            // 未认证/无上下文:不缓存,直接真调(安全优先)
            return callSdk(() -> callSdk(() -> industryApi.queryCorporationIndustryJobs(corporationId, EsiClientConfig.SERENITY, includeCompleted, accessToken)))
                    .map(resp -> esiIndustryJobConverter.toDomain(resp, corporationId));
        }
        // includeCompleted 入键,避免 true/false 变体互相命中(T009/H2)
        String key = esiResponseCache.cacheKey(esiResponseCacheProperties.getDatasource(), userId,
                "/corporations/" + corporationId + "/industry/jobs/?include_completed=" + includeCompleted, 0);
        boolean forceGranted = forceRefresh && forceRefreshLimiter.tryAcquire(key, forceRefreshWindow(), forceRefreshLimit());
        if (!forceGranted) {
            List<IndustryJob> cached = esiResponseCache.getList(key, objectMapper.constructType(IndustryJob.class));
            if (cached != null) {
                return Flux.fromIterable(cached);
            }
        }
        // 防穿透:持锁承担真调回填,未抢锁有界轮询复用回填,超时 fail-open 真调
        return doListWithClaim(key, IndustryJob.class, forceGranted, () ->
                industryApi.queryCorporationIndustryJobsResponse(corporationId, EsiClientConfig.SERENITY, includeCompleted, accessToken)
                        .mapNotNull(response -> {
                            Duration ttl = esiCacheTtlExtractor.extract(response.getHeaders());
                            // 空 body → 空列表回填(命中时空列表与未命中应以 !=null 区分)
                            List<IndustryJob> domain = response.getBody() == null ? List.of()
                                    : response.getBody().stream().map(resp -> esiIndustryJobConverter.toDomain(resp, corporationId)).toList();
                            esiResponseCache.put(key, domain, ttl);   // ttl null → put 内部跳过
                            return domain;
                        }));
    }

    @Override
    public Flux<IndustryJob> queryCharacterIndustryJobs(Integer characterId, boolean includeCompleted, String accessToken) {
        return doQueryCharacterIndustryJobs(characterId, includeCompleted, accessToken, false);
    }

    @Override
    public Flux<IndustryJob> queryCharacterIndustryJobs(Integer characterId, boolean includeCompleted, String accessToken, boolean forceRefresh) {
        return doQueryCharacterIndustryJobs(characterId, includeCompleted, accessToken, forceRefresh);
    }

    private Flux<IndustryJob> doQueryCharacterIndustryJobs(Integer characterId, boolean includeCompleted, String accessToken, boolean forceRefresh) {
        Integer userId = UserUtil.getUserId();
        if (userId == null || userId <= 0) {
            // 未认证/无上下文:不缓存,直接真调(安全优先)
            return callSdk(() -> callSdk(() -> industryApi.queryCharacterIndustryJobs(characterId, EsiClientConfig.SERENITY, includeCompleted, accessToken)))
                    .map(resp -> esiIndustryJobConverter.toDomain(resp, null));
        }
        // includeCompleted 入键,避免 true/false 变体互相命中(T009/H2)
        String key = esiResponseCache.cacheKey(esiResponseCacheProperties.getDatasource(), userId,
                "/characters/" + characterId + "/industry/jobs/?include_completed=" + includeCompleted, 0);
        boolean forceGranted = forceRefresh && forceRefreshLimiter.tryAcquire(key, forceRefreshWindow(), forceRefreshLimit());
        if (!forceGranted) {
            List<IndustryJob> cached = esiResponseCache.getList(key, objectMapper.constructType(IndustryJob.class));
            if (cached != null) {
                return Flux.fromIterable(cached);
            }
        }
        // 防穿透:持锁承担真调回填,未抢锁有界轮询复用回填,超时 fail-open 真调
        return doListWithClaim(key, IndustryJob.class, forceGranted, () ->
                industryApi.queryCharacterIndustryJobsResponse(characterId, EsiClientConfig.SERENITY, includeCompleted, accessToken)
                        .mapNotNull(response -> {
                            Duration ttl = esiCacheTtlExtractor.extract(response.getHeaders());
                            // 空 body → 空列表回填(命中时空列表与未命中应以 !=null 区分)
                            List<IndustryJob> domain = response.getBody() == null ? List.of()
                                    : response.getBody().stream().map(resp -> esiIndustryJobConverter.toDomain(resp, null)).toList();
                            esiResponseCache.put(key, domain, ttl);   // ttl null → put 内部跳过
                            return domain;
                        }));
    }

    // ── 物品类型 ──

    @Override
    public Mono<InvTypes> queryUniverseType(Integer typeId, String language) {
        // 公开数据:userId 段置 pub 全局共享(不查 UserUtil、不做强刷限流);language 进键
        String key = esiResponseCache.cacheKey(esiResponseCacheProperties.getDatasource(), null,
                "/universe/types/" + typeId + "/" + language + "/", 0);
        // 组件无 getObject:用单元素列表语义读(命中=非空单元素)
        List<InvTypes> cached = esiResponseCache.getList(key, objectMapper.constructType(InvTypes.class));
        if (cached != null && !cached.isEmpty()) {
            return Mono.just(cached.get(0));
        }
        // 防穿透:持锁承担真调回填,未抢锁有界轮询复用回填,超时 fail-open 真调
        return doListWithClaim(key, InvTypes.class, false, () ->
                universeApi.queryUniverseTypeResponse(typeId, EsiClientConfig.SERENITY, language)
                        .mapNotNull(response -> {
                            Duration ttl = esiCacheTtlExtractor.extract(response.getHeaders());
                            // 单值 body 先转领域类型,再包成单元素列表回填(与 getList 语义对齐;空 body → 空列表)
                            TypeInfoResponse body = response.getBody();
                            InvTypes domain = body == null ? null : esiInvTypesConverter.toDomain(body);
                            List<InvTypes> wrapper = domain == null ? List.of() : List.of(domain);
                            esiResponseCache.put(key, wrapper, ttl);   // ttl null → put 内部跳过
                            return wrapper;
                        }))
                .next();
    }

    // ── 采矿观察 ──

    @Override
    public Integer queryCorporationMiningObserverMaxPage(Integer corporationId, Long observerId, String accessToken) {
        return doQueryCorporationMiningObserverMaxPage(corporationId, observerId, accessToken, false);
    }

    @Override
    public Integer queryCorporationMiningObserverMaxPage(Integer corporationId, Long observerId, String accessToken, boolean forceRefresh) {
        return doQueryCorporationMiningObserverMaxPage(corporationId, observerId, accessToken, forceRefresh);
    }

    private Integer doQueryCorporationMiningObserverMaxPage(Integer corporationId, Long observerId, String accessToken, boolean forceRefresh) {
        Integer userId = UserUtil.getUserId();
        if (userId == null || userId <= 0) {
            // 未认证/无上下文:不缓存,直接真调(安全优先)
            return callSdk(() -> industryApi.queryCorporationMiningObserverMaxPage(corporationId, observerId, EsiClientConfig.SERENITY, accessToken));
        }
        // observerId 进归一化 uri(变体参数必须进键,T010 规避 T009 MEDIUM-1);maxPage 用独立 :maxPage 键
        String key = esiResponseCache.cacheKey(esiResponseCacheProperties.getDatasource(), userId,
                "/corporations/" + corporationId + "/mining/observers/" + observerId + "/", "maxPage");
        boolean forceGranted = forceRefresh && forceRefreshLimiter.tryAcquire(key, forceRefreshWindow(), forceRefreshLimit());
        if (!forceGranted) {
            Integer cached = esiResponseCache.getInt(key);
            if (cached != null) {
                return cached;
            }
        }
        // 防穿透:持锁承担真调回填,未抢锁有界轮询复用回填,超时 fail-open 真调
        return doMaxPageWithClaim(key, forceGranted, () -> industryApi.queryCorporationMiningObserverMaxPageResponse(
                corporationId, EsiClientConfig.SERENITY, observerId, accessToken));
    }

    @Override
    public Flux<MiningDetail> queryCorporationMiningObserver(Integer corporationId, Long observerId, int page, String accessToken) {
        return doQueryCorporationMiningObserver(corporationId, observerId, page, accessToken, false);
    }

    @Override
    public Flux<MiningDetail> queryCorporationMiningObserver(Integer corporationId, Long observerId, int page, String accessToken, boolean forceRefresh) {
        return doQueryCorporationMiningObserver(corporationId, observerId, page, accessToken, forceRefresh);
    }

    private Flux<MiningDetail> doQueryCorporationMiningObserver(Integer corporationId, Long observerId, int page, String accessToken, boolean forceRefresh) {
        Integer userId = UserUtil.getUserId();
        if (userId == null || userId <= 0) {
            // 未认证/无上下文:不缓存,直接真调(安全优先)
            return callSdk(() -> callSdk(() -> industryApi.queryCorporationMiningObserver(corporationId, EsiClientConfig.SERENITY, observerId, page, accessToken)))
                    .map(esiMiningDetailConverter::toDomain);
        }
        // observerId 进归一化 uri,页面参数用实际 page
        String key = esiResponseCache.cacheKey(esiResponseCacheProperties.getDatasource(), userId,
                "/corporations/" + corporationId + "/mining/observers/" + observerId + "/", page);
        boolean forceGranted = forceRefresh && forceRefreshLimiter.tryAcquire(key, forceRefreshWindow(), forceRefreshLimit());
        if (!forceGranted) {
            List<MiningDetail> cached = esiResponseCache.getList(key, objectMapper.constructType(MiningDetail.class));
            if (cached != null) {
                return Flux.fromIterable(cached);
            }
        }
        // 防穿透:持锁承担真调回填,未抢锁有界轮询复用回填,超时 fail-open 真调
        return doListWithClaim(key, MiningDetail.class, forceGranted, () ->
                industryApi.queryCorporationMiningObserverResponse(corporationId, EsiClientConfig.SERENITY, observerId, page, accessToken)
                        .mapNotNull(response -> {
                            Duration ttl = esiCacheTtlExtractor.extract(response.getHeaders());
                            // 空 body → 空列表回填(命中时空列表与未命中应以 !=null 区分)
                            List<MiningDetail> domain = response.getBody() == null ? List.of()
                                    : response.getBody().stream().map(esiMiningDetailConverter::toDomain).toList();
                            esiResponseCache.put(key, domain, ttl);   // ttl null → put 内部跳过
                            return domain;
                        }));
    }

    // ── 建筑 ──

    @Override
    public Integer queryCorporationStructuresMaxPage(Integer corporationId, String accessToken) {
        return doQueryCorporationStructuresMaxPage(corporationId, accessToken, false);
    }

    @Override
    public Integer queryCorporationStructuresMaxPage(Integer corporationId, String accessToken, boolean forceRefresh) {
        return doQueryCorporationStructuresMaxPage(corporationId, accessToken, forceRefresh);
    }

    private Integer doQueryCorporationStructuresMaxPage(Integer corporationId, String accessToken, boolean forceRefresh) {
        Integer userId = UserUtil.getUserId();
        if (userId == null || userId <= 0) {
            // 未认证/无上下文:不缓存,直接真调(安全优先)
            return callSdk(() -> corporationApi.queryCorporationStructuresMaxPage(corporationId, EsiClientConfig.SERENITY, accessToken));
        }
        // 端口签名无 language,maxPage 页数与语言无关 → 用语言无关的 :maxPage 独立键
        String key = esiResponseCache.cacheKey(esiResponseCacheProperties.getDatasource(), userId,
                "/corporations/" + corporationId + "/structures/", "maxPage");
        boolean forceGranted = forceRefresh && forceRefreshLimiter.tryAcquire(key, forceRefreshWindow(), forceRefreshLimit());
        if (!forceGranted) {
            Integer cached = esiResponseCache.getInt(key);
            if (cached != null) {
                return cached;
            }
        }
        // 防穿透:持锁承担真调回填,未抢锁有界轮询复用回填,超时 fail-open 真调
        return doMaxPageWithClaim(key, forceGranted, () -> corporationApi.queryCorporationStructuresMaxPageResponse(
                corporationId, EsiClientConfig.SERENITY, accessToken));
    }

    @Override
    public Flux<Structure> queryCorporationStructures(Integer corporationId, String language, int page, String accessToken) {
        return doQueryCorporationStructures(corporationId, language, page, accessToken, false);
    }

    @Override
    public Flux<Structure> queryCorporationStructures(Integer corporationId, String language, int page, String accessToken, boolean forceRefresh) {
        return doQueryCorporationStructures(corporationId, language, page, accessToken, forceRefresh);
    }

    private Flux<Structure> doQueryCorporationStructures(Integer corporationId, String language, int page, String accessToken, boolean forceRefresh) {
        Integer userId = UserUtil.getUserId();
        if (userId == null || userId <= 0) {
            // 未认证/无上下文:不缓存,直接真调(安全优先)
            return callSdk(() -> callSdk(() -> corporationApi.queryCorporationStructures(corporationId, EsiClientConfig.SERENITY, language, page, accessToken)))
                    .map(esiStructureConverter::toDomain);
        }
        // language 进归一化 uri(变体参数必须进键:T010 规避 T009 MEDIUM-1,换语种不得互命中)
        String key = esiResponseCache.cacheKey(esiResponseCacheProperties.getDatasource(), userId,
                "/corporations/" + corporationId + "/structures/" + language + "/", page);
        boolean forceGranted = forceRefresh && forceRefreshLimiter.tryAcquire(key, forceRefreshWindow(), forceRefreshLimit());
        if (!forceGranted) {
            List<Structure> cached = esiResponseCache.getList(key, objectMapper.constructType(Structure.class));
            if (cached != null) {
                return Flux.fromIterable(cached);
            }
        }
        // 防穿透:持锁承担真调回填,未抢锁有界轮询复用回填,超时 fail-open 真调
        return doListWithClaim(key, Structure.class, forceGranted, () ->
                corporationApi.queryCorporationStructuresResponse(corporationId, EsiClientConfig.SERENITY, language, page, accessToken)
                        .mapNotNull(response -> {
                            Duration ttl = esiCacheTtlExtractor.extract(response.getHeaders());
                            // 空 body → 空列表回填(命中时空列表与未命中应以 !=null 区分)
                            List<Structure> domain = response.getBody() == null ? List.of()
                                    : response.getBody().stream().map(esiStructureConverter::toDomain).toList();
                            esiResponseCache.put(key, domain, ttl);   // ttl null → put 内部跳过
                            return domain;
                        }));
    }

    // ── 名称解析 ──

    @Override
    public Flux<UniverseName> queryUniverseNames(List<Integer> ids) {
        // 公开数据:userId 段置 pub 全局共享(不查 UserUtil、不限流);ids 排序确定编码进键
        String key = esiResponseCache.cacheKey(esiResponseCacheProperties.getDatasource(), null,
                encodeIds(ids), 0);
        List<UniverseName> cached = esiResponseCache.getList(key, objectMapper.constructType(UniverseName.class));
        if (cached != null) {
            return Flux.fromIterable(cached);
        }
        // 防穿透:持锁承担真调回填,未抢锁有界轮询复用回填,超时 fail-open 真调
        return doListWithClaim(key, UniverseName.class, false, () ->
                universeApi.queryUniverseNamesResponse(ids, EsiClientConfig.SERENITY)
                        .mapNotNull(response -> {
                            Duration ttl = esiCacheTtlExtractor.extract(response.getHeaders());
                            // 空 body → 空列表回填(命中时空列表与未命中应以 !=null 区分)
                            List<UniverseName> domain = response.getBody() == null ? List.of()
                                    : response.getBody().stream().map(esiUniverseNameConverter::toDomain).toList();
                            esiResponseCache.put(key, domain, ttl);   // ttl null → put 内部跳过
                            return domain;
                        }));
    }

    /**
     * ids 集合确定性编码进缓存键:排序后按序拼接,保证"同组同键、异组异键"(顺序无关)。
     */
    private String encodeIds(List<Integer> ids) {
        String encoded = ids.stream().sorted().map(String::valueOf).collect(Collectors.joining("-"));
        return "/universe/names/" + encoded + "/";
    }

    /**
     * 强刷新时间窗(来自配置,分钟)。
     */
    private Duration forceRefreshWindow() {
        return Duration.ofMinutes(esiResponseCacheProperties.getForceRefreshWindowMinutes());
    }

    /**
     * 强刷新单键固定窗口内允许的真实刷新次数(来自配置)。
     */
    private int forceRefreshLimit() {
        return esiResponseCacheProperties.getForceRefreshLimit();
    }

    // ── 防并发穿透(FR-008):未命中真调前 setIfAbsent 串行化,复用回填结果 ──

    /**
     * 列表查询的防穿透包裹:持锁承担真调回填并释放;未抢锁有界轮询读缓存命中复用;超时 fail-open 真调。
     * <p>
     * 真调异常照常上抛勿吞;占位锁经 {@code doFinally} 在流终止(回填后)释放。
     */
    private <T> Flux<T> doListWithClaim(String key, Class<T> elementType, boolean forceGranted, Supplier<Mono<List<T>>> realCaller) {
        boolean claimed = esiResponseCache.tryClaim(key, CLAIM_TTL);
        if (!claimed && !forceGranted) {
            // 未抢锁且非强刷:有界轮询复用持锁方回填结果(~5s),命中则不重复真调
            List<T> filled = awaitCachedList(key, objectMapper.constructType(elementType));
            if (filled != null) {
                return Flux.fromIterable(filled);
            }
        }
        // 持锁承担真调;或强刷有额度未抢锁(保新鲜,直接真调不读陈旧缓存);或轮询超时 fail-open
        Mono<List<T>> mono;
        try {
            mono = realCaller.get();
        } catch (EsiException e) {
            throw convertToAccessException(e);
        }
        Flux<T> out = mono.flatMapIterable(Function.identity())
                .onErrorMap(EsiException.class, this::convertToAccessException);
        return claimed ? out.doFinally(sig -> esiResponseCache.releaseClaim(key)) : out;
    }

    /**
     * maxPage(Integer)查询的防穿透包裹:同列表语义,轮询用 getInt;同步真调用 try/finally 释放。
     */
    private Integer doMaxPageWithClaim(String key, boolean forceGranted, Supplier<PageTotalApi.PageCountResult> realCaller) {
        boolean claimed = esiResponseCache.tryClaim(key, CLAIM_TTL);
        if (!claimed && !forceGranted) {
            Integer filled = awaitCachedInt(key);
            if (filled != null) {
                return filled;
            }
        }
        if (claimed) {
            try {
                return fillMaxPage(key, realCaller.get());
            } catch (EsiException e) {
                throw convertToAccessException(e);
            } finally {
                esiResponseCache.releaseClaim(key);
            }
        }
        try {
            return fillMaxPage(key, realCaller.get());
        } catch (EsiException e) {
            throw convertToAccessException(e);
        }
    }

    /**
     * maxPage 真调结果回填,返回 maxPage。
     */
    private Integer fillMaxPage(String key, PageTotalApi.PageCountResult count) {
        Duration ttl = esiCacheTtlExtractor.extract(count.headers());
        esiResponseCache.put(key, count.maxPage(), ttl);   // ttl null → put 内部跳过
        return count.maxPage();
    }

    /**
     * 有界轮询读列表缓存(未抢锁方等待持锁方回填)。窗口内返回缓存即命中;超时返回 null。
     */
    private <T> List<T> awaitCachedList(String key, JavaType elementType) {
        for (int i = 0; i < CLAIM_WAIT_ATTEMPTS; i++) {
            try {
                TimeUnit.MILLISECONDS.sleep(CLAIM_WAIT_INTERVAL_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
            List<T> cached = esiResponseCache.getList(key, elementType);
            if (cached != null) {
                return cached;
            }
        }
        return null;
    }

    /**
     * 有界轮询读 maxPage 缓存(未抢锁方等待持锁方回填)。超时返回 null(fail-open 真调)。
     */
    private Integer awaitCachedInt(String key) {
        for (int i = 0; i < CLAIM_WAIT_ATTEMPTS; i++) {
            try {
                TimeUnit.MILLISECONDS.sleep(CLAIM_WAIT_INTERVAL_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
            Integer cached = esiResponseCache.getInt(key);
            if (cached != null) {
                return cached;
            }
        }
        return null;
    }

    // ── 钱包流水 ──

    @Override
    public Integer queryCorporationWalletJournalMaxPage(Integer corporationId, int division, String accessToken) {
        return doQueryCorporationWalletJournalMaxPage(corporationId, division, accessToken, false);
    }

    @Override
    public Integer queryCorporationWalletJournalMaxPage(Integer corporationId, int division, String accessToken, boolean forceRefresh) {
        return doQueryCorporationWalletJournalMaxPage(corporationId, division, accessToken, forceRefresh);
    }

    private Integer doQueryCorporationWalletJournalMaxPage(Integer corporationId, int division, String accessToken, boolean forceRefresh) {
        Integer userId = UserUtil.getUserId();
        if (userId == null || userId <= 0) {
            // 未认证/无上下文:不缓存,直接真调(安全优先)
            return callSdk(() -> walletApi.queryCorporationWalletJournalMaxPage(corporationId, division, EsiClientConfig.SERENITY, accessToken));
        }
        // division 进归一化 uri(变体参数进键,规避 MEDIUM-1);maxPage 用独立 :maxPage 键
        String key = esiResponseCache.cacheKey(esiResponseCacheProperties.getDatasource(), userId,
                "/corporations/" + corporationId + "/wallets/" + division + "/journal/", "maxPage");
        boolean forceGranted = forceRefresh && forceRefreshLimiter.tryAcquire(key, forceRefreshWindow(), forceRefreshLimit());
        if (!forceGranted) {
            Integer cached = esiResponseCache.getInt(key);
            if (cached != null) {
                return cached;
            }
        }
        // 防穿透:持锁承担真调回填,未抢锁有界轮询复用回填,超时 fail-open 真调
        return doMaxPageWithClaim(key, forceGranted, () -> walletApi.queryCorporationWalletJournalMaxPageResponse(
                corporationId, division, EsiClientConfig.SERENITY, accessToken));
    }

    @Override
    public Flux<WalletJournal> queryCorporationWalletJournal(Integer corporationId, int division, int page, String accessToken) {
        return doQueryCorporationWalletJournal(corporationId, division, page, accessToken, false);
    }

    @Override
    public Flux<WalletJournal> queryCorporationWalletJournal(Integer corporationId, int division, int page, String accessToken, boolean forceRefresh) {
        return doQueryCorporationWalletJournal(corporationId, division, page, accessToken, forceRefresh);
    }

    private Flux<WalletJournal> doQueryCorporationWalletJournal(Integer corporationId, int division, int page, String accessToken, boolean forceRefresh) {
        Integer userId = UserUtil.getUserId();
        if (userId == null || userId <= 0) {
            // 未认证/无上下文:不缓存,直接真调(安全优先)
            return callSdk(() -> callSdk(() -> walletApi.queryCorporationWalletJournal(corporationId, division, EsiClientConfig.SERENITY, page, accessToken)))
                    .map(wallet -> esiWalletJournalConverter.toDomain(wallet, corporationId, resolveWalletCharacter(wallet)));
        }
        // division 进归一化 uri
        String key = esiResponseCache.cacheKey(esiResponseCacheProperties.getDatasource(), userId,
                "/corporations/" + corporationId + "/wallets/" + division + "/journal/", page);
        boolean forceGranted = forceRefresh && forceRefreshLimiter.tryAcquire(key, forceRefreshWindow(), forceRefreshLimit());
        if (!forceGranted) {
            List<WalletJournal> cached = esiResponseCache.getList(key, objectMapper.constructType(WalletJournal.class));
            if (cached != null) {
                return Flux.fromIterable(cached);
            }
        }
        // 防穿透:持锁承担真调回填,未抢锁有界轮询复用回填,超时 fail-open 真调
        return doListWithClaim(key, WalletJournal.class, forceGranted, () ->
                walletApi.queryCorporationWalletJournalResponse(corporationId, division, EsiClientConfig.SERENITY, page, accessToken)
                        .mapNotNull(response -> {
                            Duration ttl = esiCacheTtlExtractor.extract(response.getHeaders());
                            // 空 body → 空列表回填;三参转换在回填时计算(含 resolveWalletCharacter 赏金角色名解析)
                            List<WalletJournal> domain = response.getBody() == null ? List.of()
                                    : response.getBody().stream()
                                    .map(w -> esiWalletJournalConverter.toDomain(w, corporationId, resolveWalletCharacter(w))).toList();
                            esiResponseCache.put(key, domain, ttl);   // ttl null → put 内部跳过
                            return domain;
                        }));
    }

    @Override
    public Integer queryCharacterWalletJournalMaxPage(Integer characterId, String accessToken) {
        return doQueryCharacterWalletJournalMaxPage(characterId, accessToken, false);
    }

    @Override
    public Integer queryCharacterWalletJournalMaxPage(Integer characterId, String accessToken, boolean forceRefresh) {
        return doQueryCharacterWalletJournalMaxPage(characterId, accessToken, forceRefresh);
    }

    private Integer doQueryCharacterWalletJournalMaxPage(Integer characterId, String accessToken, boolean forceRefresh) {
        Integer userId = UserUtil.getUserId();
        if (userId == null || userId <= 0) {
            // 未认证/无上下文:不缓存,直接真调(安全优先)
            return callSdk(() -> walletApi.queryCharacterWalletJournalMaxPage(characterId, EsiClientConfig.SERENITY, accessToken));
        }
        String key = esiResponseCache.cacheKey(esiResponseCacheProperties.getDatasource(), userId,
                "/characters/" + characterId + "/wallet/journal/", "maxPage");
        boolean forceGranted = forceRefresh && forceRefreshLimiter.tryAcquire(key, forceRefreshWindow(), forceRefreshLimit());
        if (!forceGranted) {
            Integer cached = esiResponseCache.getInt(key);
            if (cached != null) {
                return cached;
            }
        }
        // 防穿透:持锁承担真调回填,未抢锁有界轮询复用回填,超时 fail-open 真调
        return doMaxPageWithClaim(key, forceGranted, () -> walletApi.queryCharacterWalletJournalMaxPageResponse(
                characterId, EsiClientConfig.SERENITY, accessToken));
    }

    @Override
    public Flux<WalletJournal> queryCharacterWalletJournal(Integer characterId, int page, String accessToken) {
        return doQueryCharacterWalletJournal(characterId, page, accessToken, false);
    }

    @Override
    public Flux<WalletJournal> queryCharacterWalletJournal(Integer characterId, int page, String accessToken, boolean forceRefresh) {
        return doQueryCharacterWalletJournal(characterId, page, accessToken, forceRefresh);
    }

    private Flux<WalletJournal> doQueryCharacterWalletJournal(Integer characterId, int page, String accessToken, boolean forceRefresh) {
        Integer userId = UserUtil.getUserId();
        if (userId == null || userId <= 0) {
            // 未认证/无上下文:不缓存,直接真调(安全优先)
            return callSdk(() -> callSdk(() -> walletApi.queryCharacterWalletJournal(characterId, EsiClientConfig.SERENITY, page, accessToken)))
                    .map(wallet -> esiWalletJournalConverter.toDomain(wallet, characterId, resolveWalletCharacter(wallet)));
        }
        String key = esiResponseCache.cacheKey(esiResponseCacheProperties.getDatasource(), userId,
                "/characters/" + characterId + "/wallet/journal/", page);
        boolean forceGranted = forceRefresh && forceRefreshLimiter.tryAcquire(key, forceRefreshWindow(), forceRefreshLimit());
        if (!forceGranted) {
            List<WalletJournal> cached = esiResponseCache.getList(key, objectMapper.constructType(WalletJournal.class));
            if (cached != null) {
                return Flux.fromIterable(cached);
            }
        }
        // 防穿透:持锁承担真调回填,未抢锁有界轮询复用回填,超时 fail-open 真调
        return doListWithClaim(key, WalletJournal.class, forceGranted, () ->
                walletApi.queryCharacterWalletJournalResponse(characterId, EsiClientConfig.SERENITY, page, accessToken)
                        .mapNotNull(response -> {
                            Duration ttl = esiCacheTtlExtractor.extract(response.getHeaders());
                            List<WalletJournal> domain = response.getBody() == null ? List.of()
                                    : response.getBody().stream()
                                    .map(w -> esiWalletJournalConverter.toDomain(w, characterId, resolveWalletCharacter(w))).toList();
                            esiResponseCache.put(key, domain, ttl);   // ttl null → put 内部跳过
                            return domain;
                        }));
    }

    // ── 钱包交易 ──

    @Override
    public Flux<WalletTransaction> queryCharacterWalletTransactions(Integer characterId, Long fromId, String accessToken) {
        return doQueryCharacterWalletTransactions(characterId, fromId, accessToken, false);
    }

    @Override
    public Flux<WalletTransaction> queryCharacterWalletTransactions(Integer characterId, Long fromId, String accessToken, boolean forceRefresh) {
        return doQueryCharacterWalletTransactions(characterId, fromId, accessToken, forceRefresh);
    }

    private Flux<WalletTransaction> doQueryCharacterWalletTransactions(Integer characterId, Long fromId, String accessToken, boolean forceRefresh) {
        Integer userId = UserUtil.getUserId();
        if (userId == null || userId <= 0) {
            // 未认证/无上下文:不缓存,直接真调(安全优先)
            return callSdk(() -> callSdk(() -> walletApi.queryCharacterWalletTransactions(characterId, EsiClientConfig.SERENITY, fromId, accessToken)))
                    .map(esiWalletTransactionConverter::toDomain);
        }
        // fromId 是游标须进键:null 与非 null 由归一化 uri 是否含 from_id 段区分(M06 规避 MEDIUM-1)
        String base = "/characters/" + characterId + "/wallet/transactions/";
        String key = esiResponseCache.cacheKey(esiResponseCacheProperties.getDatasource(), userId,
                fromId == null ? base : base + "?from_id=" + fromId, 0);
        boolean forceGranted = forceRefresh && forceRefreshLimiter.tryAcquire(key, forceRefreshWindow(), forceRefreshLimit());
        if (!forceGranted) {
            List<WalletTransaction> cached = esiResponseCache.getList(key, objectMapper.constructType(WalletTransaction.class));
            if (cached != null) {
                return Flux.fromIterable(cached);
            }
        }
        // 防穿透:持锁承担真调回填,未抢锁有界轮询复用回填,超时 fail-open 真调
        return doListWithClaim(key, WalletTransaction.class, forceGranted, () ->
                walletApi.queryCharacterWalletTransactionsResponse(characterId, EsiClientConfig.SERENITY, fromId, accessToken)
                        .mapNotNull(response -> {
                            Duration ttl = esiCacheTtlExtractor.extract(response.getHeaders());
                            List<WalletTransaction> domain = response.getBody() == null ? List.of()
                                    : response.getBody().stream().map(esiWalletTransactionConverter::toDomain).toList();
                            esiResponseCache.put(key, domain, ttl);   // ttl null → put 内部跳过
                            return domain;
                        }));
    }

    @Override
    public Flux<WalletTransaction> queryCorporationWalletTransactions(Integer corporationId, Integer division, Long fromId, String accessToken) {
        return doQueryCorporationWalletTransactions(corporationId, division, fromId, accessToken, false);
    }

    @Override
    public Flux<WalletTransaction> queryCorporationWalletTransactions(Integer corporationId, Integer division, Long fromId, String accessToken, boolean forceRefresh) {
        return doQueryCorporationWalletTransactions(corporationId, division, fromId, accessToken, forceRefresh);
    }

    private Flux<WalletTransaction> doQueryCorporationWalletTransactions(Integer corporationId, Integer division, Long fromId, String accessToken, boolean forceRefresh) {
        Integer userId = UserUtil.getUserId();
        if (userId == null || userId <= 0) {
            // 未认证/无上下文:不缓存,直接真调(安全优先)
            return callSdk(() -> callSdk(() -> walletApi.queryCorporationWalletTransactions(corporationId, division, EsiClientConfig.SERENITY, fromId, accessToken)))
                    .map(esiWalletTransactionConverter::toDomain);
        }
        // division 进归一化 uri;fromId null/非 null 由 from_id 段区分
        String base = "/corporations/" + corporationId + "/wallets/" + division + "/transactions/";
        String key = esiResponseCache.cacheKey(esiResponseCacheProperties.getDatasource(), userId,
                fromId == null ? base : base + "?from_id=" + fromId, 0);
        boolean forceGranted = forceRefresh && forceRefreshLimiter.tryAcquire(key, forceRefreshWindow(), forceRefreshLimit());
        if (!forceGranted) {
            List<WalletTransaction> cached = esiResponseCache.getList(key, objectMapper.constructType(WalletTransaction.class));
            if (cached != null) {
                return Flux.fromIterable(cached);
            }
        }
        // 防穿透:持锁承担真调回填,未抢锁有界轮询复用回填,超时 fail-open 真调
        return doListWithClaim(key, WalletTransaction.class, forceGranted, () ->
                walletApi.queryCorporationWalletTransactionsResponse(corporationId, division, EsiClientConfig.SERENITY, fromId, accessToken)
                        .mapNotNull(response -> {
                            Duration ttl = esiCacheTtlExtractor.extract(response.getHeaders());
                            List<WalletTransaction> domain = response.getBody() == null ? List.of()
                                    : response.getBody().stream().map(esiWalletTransactionConverter::toDomain).toList();
                            esiResponseCache.put(key, domain, ttl);   // ttl null → put 内部跳过
                            return domain;
                        }));
    }

    // ── 钱包余额(直查,overview 实时通路) ──

    @Override
    public Double queryCharacterWalletBalance(Integer characterId, String accessToken) {
        return callSdk(() -> walletApi.queryCharacterWallet(characterId, EsiClientConfig.SERENITY, accessToken))
                .timeout(EsiClientConstants.ESI_DATA_TIMEOUT)
                .block();
    }

    @Override
    public List<WalletDivisionBalance> queryCorporationWalletBalances(Integer corporationId, String accessToken) {
        return callSdk(() -> walletApi.queryCorporationWallet(corporationId, EsiClientConfig.SERENITY, accessToken))
                .map(r -> new WalletDivisionBalance(r.getDivision(), r.getBalance()))
                .collectList()
                .timeout(EsiClientConstants.ESI_DATA_TIMEOUT)
                .block();
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

    /**
     * 将 SDK 异常转换为 domain 层异常(防腐层职责)。
     * <p>
     * SDK 调用抛出的 {@link EsiException} 在此转换为 {@link EsiAccessException},
     * 使 domain 层不依赖 infrastructure 的异常类型。
     *
     * @param e SDK 抛出的 EsiException
     * @return domain 层异常
     */
    private EsiAccessException convertToAccessException(EsiException e) {
        String message = e.getMessage();
        if (!(e.getResultCode() instanceof ResultCode code)) {
            return new EsiAccessException(message, e);
        }
        return switch (code) {
            case ESI_AUTHORIZATION_FAILURE -> EsiAccessException.authorizationFailure();
            case ESI_AUTH_PERMISSION_LOW -> EsiAccessException.permissionLow();
            case ESI_SERVER_FAILURE -> EsiAccessException.serverFailure();
            default -> new EsiAccessException(message, e);
        };
    }

    /**
     * 包裹 SDK 调用,捕获 EsiException 并转换为 EsiAccessException。
     * 用于同步返回值的 SDK 调用。
     */
    private <T> T callSdk(Callable<T> sdkCall) {
        try {
            return sdkCall.call();
        } catch (EsiException e) {
            throw convertToAccessException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 包裹 SDK 调用(Flux 返回类型),捕获 EsiException 并转换为 EsiAccessException。
     */
    private <T> Flux<T> callSdkFlux(Supplier<Flux<T>> sdkCall) {
        try {
            return sdkCall.get();
        } catch (EsiException e) {
            throw convertToAccessException(e);
        }
    }

    /**
     * 包裹 SDK 调用(Mono 返回类型),捕获 EsiException 并转换为 EsiAccessException。
     */
    private <T> Mono<T> callSdkMono(Supplier<Mono<T>> sdkCall) {
        try {
            return sdkCall.get();
        } catch (EsiException e) {
            throw convertToAccessException(e);
        }
    }

    // ── 角色面板(伴侣 APP T009) ──

    @Override
    public CharacterPublicInfo queryCharacterPublicInfo(Integer characterId) {
        return doQueryCharacterPublicInfo(characterId, false);
    }

    @Override
    public CharacterPublicInfo queryCharacterPublicInfo(Integer characterId, boolean forceRefresh) {
        return doQueryCharacterPublicInfo(characterId, forceRefresh);
    }

    private CharacterPublicInfo doQueryCharacterPublicInfo(Integer characterId, boolean forceRefresh) {
        // 公开数据:userId 段置 pub 全局共享(不查 UserUtil)
        String key = esiResponseCache.cacheKey(esiResponseCacheProperties.getDatasource(), null,
                "/characters/" + characterId + "/", 0);
        boolean forceGranted = forceRefresh && forceRefreshLimiter.tryAcquire(key, forceRefreshWindow(), forceRefreshLimit());
        if (!forceGranted) {
            List<CharacterPublicInfo> cached = esiResponseCache.getList(key, objectMapper.constructType(CharacterPublicInfo.class));
            if (cached != null && !cached.isEmpty()) {
                return cached.get(0);
            }
        }
        // 防穿透:持锁承担真调回填,未抢锁有界轮询复用回填,超时 fail-open 真调
        boolean claimed = esiResponseCache.tryClaim(key, CLAIM_TTL);
        if (!claimed && !forceGranted) {
            List<CharacterPublicInfo> filled = awaitCachedList(key, objectMapper.constructType(CharacterPublicInfo.class));
            if (filled != null && !filled.isEmpty()) {
                return filled.get(0);
            }
        }
        try {
            ResponseEntity<CharacterPublicInfoResponse> response = callSdkMono(() -> characterApi.queryCharacterResponse(characterId, EsiClientConfig.SERENITY))
                    .block();
            if (response == null || response.getBody() == null) {
                return null;
            }
            CharacterPublicInfoResponse resp = response.getBody();
            CharacterPublicInfo domain = new CharacterPublicInfo(
                    characterId,
                    resp.getName(),
                    resp.getCorporationId(),
                    resp.getAllianceId(),
                    resp.getBirthday(),
                    resp.getSecurityStatus()
            );
            Duration ttl = esiCacheTtlExtractor.extract(response.getHeaders());
            List<CharacterPublicInfo> wrapped = List.of(domain);
            esiResponseCache.put(key, wrapped, ttl);
            return domain;
        } finally {
            if (claimed) {
                esiResponseCache.releaseClaim(key);
            }
        }
    }

    @Override
    public CharacterOnlineInfo queryCharacterOnline(Integer characterId, String accessToken) {
        return doQueryCharacterOnline(characterId, accessToken, false);
    }

    @Override
    public CharacterOnlineInfo queryCharacterOnline(Integer characterId, String accessToken, boolean forceRefresh) {
        return doQueryCharacterOnline(characterId, accessToken, forceRefresh);
    }

    private CharacterOnlineInfo doQueryCharacterOnline(Integer characterId, String accessToken, boolean forceRefresh) {
        Integer userId = UserUtil.getUserId();
        if (userId == null || userId <= 0) {
            // 未认证/无上下文:不缓存,直接真调(安全优先)
            OnlineStatusResponse resp = callSdkMono(() -> locationApi.queryCharacterOnline(characterId, EsiClientConfig.SERENITY, accessToken))
                    .timeout(EsiClientConstants.ESI_DATA_TIMEOUT)
                    .block();
            return resp == null ? null : new CharacterOnlineInfo(
                    Boolean.TRUE.equals(resp.getOnline()),
                    resp.getLastLogin(),
                    resp.getLastLogout()
            );
        }
        String key = esiResponseCache.cacheKey(esiResponseCacheProperties.getDatasource(), userId,
                "/characters/" + characterId + "/online/", 0);
        boolean forceGranted = forceRefresh && forceRefreshLimiter.tryAcquire(key, forceRefreshWindow(), forceRefreshLimit());
        if (!forceGranted) {
            List<CharacterOnlineInfo> cached = esiResponseCache.getList(key, objectMapper.constructType(CharacterOnlineInfo.class));
            if (cached != null && !cached.isEmpty()) {
                return cached.get(0);
            }
        }
        // 防穿透
        boolean claimed = esiResponseCache.tryClaim(key, CLAIM_TTL);
        if (!claimed && !forceGranted) {
            List<CharacterOnlineInfo> filled = awaitCachedList(key, objectMapper.constructType(CharacterOnlineInfo.class));
            if (filled != null && !filled.isEmpty()) {
                return filled.get(0);
            }
        }
        try {
            ResponseEntity<OnlineStatusResponse> response = callSdkMono(() -> locationApi.queryCharacterOnlineResponse(characterId, EsiClientConfig.SERENITY, accessToken))
                    .block();
            if (response == null || response.getBody() == null) {
                return null;
            }
            OnlineStatusResponse resp = response.getBody();
            CharacterOnlineInfo domain = new CharacterOnlineInfo(
                    Boolean.TRUE.equals(resp.getOnline()),
                    resp.getLastLogin(),
                    resp.getLastLogout()
            );
            Duration ttl = esiCacheTtlExtractor.extract(response.getHeaders());
            List<CharacterOnlineInfo> wrapped = List.of(domain);
            esiResponseCache.put(key, wrapped, ttl);
            return domain;
        } finally {
            if (claimed) {
                esiResponseCache.releaseClaim(key);
            }
        }
    }

    // ── 位置 + 舰船(伴侣 APP T010) ──

    @Override
    public CharacterLocationInfo queryCharacterLocation(Integer characterId, String accessToken) {
        return doQueryCharacterLocation(characterId, accessToken, false);
    }

    @Override
    public CharacterLocationInfo queryCharacterLocation(Integer characterId, String accessToken, boolean forceRefresh) {
        return doQueryCharacterLocation(characterId, accessToken, forceRefresh);
    }

    private CharacterLocationInfo doQueryCharacterLocation(Integer characterId, String accessToken, boolean forceRefresh) {
        Integer userId = UserUtil.getUserId();
        if (userId == null || userId <= 0) {
            // 未认证/无上下文:不缓存,直接真调(安全优先)
            LocationResponse resp = callSdkMono(() -> locationApi.queryCharacterLocation(characterId, EsiClientConfig.SERENITY, accessToken))
                    .timeout(EsiClientConstants.ESI_DATA_TIMEOUT)
                    .block();
            return resp == null ? null : new CharacterLocationInfo(
                    resp.getSolarSystemId(),
                    resp.getStationId(),
                    resp.getStructureId()
            );
        }
        String key = esiResponseCache.cacheKey(esiResponseCacheProperties.getDatasource(), userId,
                "/characters/" + characterId + "/location/", 0);
        boolean forceGranted = forceRefresh && forceRefreshLimiter.tryAcquire(key, forceRefreshWindow(), forceRefreshLimit());
        if (!forceGranted) {
            List<CharacterLocationInfo> cached = esiResponseCache.getList(key, objectMapper.constructType(CharacterLocationInfo.class));
            if (cached != null && !cached.isEmpty()) {
                return cached.get(0);
            }
        }
        // 防穿透
        boolean claimed = esiResponseCache.tryClaim(key, CLAIM_TTL);
        if (!claimed && !forceGranted) {
            List<CharacterLocationInfo> filled = awaitCachedList(key, objectMapper.constructType(CharacterLocationInfo.class));
            if (filled != null && !filled.isEmpty()) {
                return filled.get(0);
            }
        }
        try {
            ResponseEntity<LocationResponse> response = callSdkMono(() -> locationApi.queryCharacterLocationResponse(characterId, EsiClientConfig.SERENITY, accessToken))
                    .block();
            if (response == null || response.getBody() == null) {
                return null;
            }
            LocationResponse resp = response.getBody();
            CharacterLocationInfo domain = new CharacterLocationInfo(
                    resp.getSolarSystemId(),
                    resp.getStationId(),
                    resp.getStructureId()
            );
            Duration ttl = esiCacheTtlExtractor.extract(response.getHeaders());
            List<CharacterLocationInfo> wrapped = List.of(domain);
            esiResponseCache.put(key, wrapped, ttl);
            return domain;
        } finally {
            if (claimed) {
                esiResponseCache.releaseClaim(key);
            }
        }
    }

    @Override
    public Integer queryCharacterShipTypeId(Integer characterId, String accessToken) {
        return doQueryCharacterShipTypeId(characterId, accessToken, false);
    }

    @Override
    public Integer queryCharacterShipTypeId(Integer characterId, String accessToken, boolean forceRefresh) {
        return doQueryCharacterShipTypeId(characterId, accessToken, forceRefresh);
    }

    private Integer doQueryCharacterShipTypeId(Integer characterId, String accessToken, boolean forceRefresh) {
        Integer userId = UserUtil.getUserId();
        if (userId == null || userId <= 0) {
            // 未认证/无上下文:不缓存,直接真调(安全优先)
            CurrentShipResponse resp = callSdkMono(() -> locationApi.queryCharacterShip(characterId, EsiClientConfig.SERENITY, accessToken))
                    .timeout(EsiClientConstants.ESI_DATA_TIMEOUT)
                    .block();
            return resp == null ? null : resp.getShipTypeId();
        }
        String key = esiResponseCache.cacheKey(esiResponseCacheProperties.getDatasource(), userId,
                "/characters/" + characterId + "/ship/", 0);
        boolean forceGranted = forceRefresh && forceRefreshLimiter.tryAcquire(key, forceRefreshWindow(), forceRefreshLimit());
        if (!forceGranted) {
            List<Integer> cached = esiResponseCache.getList(key, objectMapper.constructType(Integer.class));
            if (cached != null && !cached.isEmpty()) {
                return cached.get(0);
            }
        }
        // 防穿透
        boolean claimed = esiResponseCache.tryClaim(key, CLAIM_TTL);
        if (!claimed && !forceGranted) {
            List<Integer> filled = awaitCachedList(key, objectMapper.constructType(Integer.class));
            if (filled != null && !filled.isEmpty()) {
                return filled.get(0);
            }
        }
        try {
            ResponseEntity<CurrentShipResponse> response = callSdkMono(() -> locationApi.queryCharacterShipResponse(characterId, EsiClientConfig.SERENITY, accessToken))
                    .block();
            if (response == null || response.getBody() == null) {
                return null;
            }
            Integer shipTypeId = response.getBody().getShipTypeId();
            Duration ttl = esiCacheTtlExtractor.extract(response.getHeaders());
            List<Integer> wrapped = List.of(shipTypeId);
            esiResponseCache.put(key, wrapped, ttl);
            return shipTypeId;
        } finally {
            if (claimed) {
                esiResponseCache.releaseClaim(key);
            }
        }
    }

    // ── 技能列表 + 属性 + 技能队列(伴侣 APP T011) ──

    @Override
    public CharacterSkillList queryCharacterSkills(Integer characterId, String accessToken) {
        return doQueryCharacterSkills(characterId, accessToken, false);
    }

    @Override
    public CharacterSkillList queryCharacterSkills(Integer characterId, String accessToken, boolean forceRefresh) {
        return doQueryCharacterSkills(characterId, accessToken, forceRefresh);
    }

    private CharacterSkillList doQueryCharacterSkills(Integer characterId, String accessToken, boolean forceRefresh) {
        Integer userId = UserUtil.getUserId();
        if (userId == null || userId <= 0) {
            CharacterSkillResponse resp = callSdkMono(() -> skillApi.queryCharactersSkills(characterId, EsiClientConfig.SERENITY, accessToken))
                    .timeout(EsiClientConstants.ESI_DATA_TIMEOUT)
                    .block();
            return resp == null ? null : mapSkillList(resp);
        }
        String key = esiResponseCache.cacheKey(esiResponseCacheProperties.getDatasource(), userId,
                "/characters/" + characterId + "/skills/", 0);
        boolean forceGranted = forceRefresh && forceRefreshLimiter.tryAcquire(key, forceRefreshWindow(), forceRefreshLimit());
        if (!forceGranted) {
            List<CharacterSkillList> cached = esiResponseCache.getList(key, objectMapper.constructType(CharacterSkillList.class));
            if (cached != null && !cached.isEmpty()) {
                return cached.get(0);
            }
        }
        boolean claimed = esiResponseCache.tryClaim(key, CLAIM_TTL);
        if (!claimed && !forceGranted) {
            List<CharacterSkillList> filled = awaitCachedList(key, objectMapper.constructType(CharacterSkillList.class));
            if (filled != null && !filled.isEmpty()) {
                return filled.get(0);
            }
        }
        try {
            ResponseEntity<CharacterSkillResponse> response = callSdkMono(() -> skillApi.queryCharactersSkillsResponse(characterId, EsiClientConfig.SERENITY, accessToken))
                    .block();
            if (response == null || response.getBody() == null) {
                return null;
            }
            CharacterSkillList domain = mapSkillList(response.getBody());
            Duration ttl = esiCacheTtlExtractor.extract(response.getHeaders());
            List<CharacterSkillList> wrapped = List.of(domain);
            esiResponseCache.put(key, wrapped, ttl);
            return domain;
        } finally {
            if (claimed) {
                esiResponseCache.releaseClaim(key);
            }
        }
    }

    private CharacterSkillList mapSkillList(CharacterSkillResponse resp) {
        List<CharacterSkillList.SkillItem> items = resp.getSkills() == null ? List.of()
                : resp.getSkills().stream().map(s -> new CharacterSkillList.SkillItem(
                        s.getSkillId(),
                        s.getTrainedSkillLevel(),
                        s.getActiveSkillLevel(),
                        s.getSkillpointsInSkill()
                )).toList();
        return new CharacterSkillList(resp.getTotalSp(), resp.getUnallocatedSp(), items);
    }

    @Override
    public CharacterAttributesInfo queryCharacterAttributes(Integer characterId, String accessToken) {
        return doQueryCharacterAttributes(characterId, accessToken, false);
    }

    @Override
    public CharacterAttributesInfo queryCharacterAttributes(Integer characterId, String accessToken, boolean forceRefresh) {
        return doQueryCharacterAttributes(characterId, accessToken, forceRefresh);
    }

    private CharacterAttributesInfo doQueryCharacterAttributes(Integer characterId, String accessToken, boolean forceRefresh) {
        Integer userId = UserUtil.getUserId();
        if (userId == null || userId <= 0) {
            CharacterAttributesResponse resp = callSdkMono(() -> skillApi.queryCharactersAttributes(characterId, EsiClientConfig.SERENITY, accessToken))
                    .timeout(EsiClientConstants.ESI_DATA_TIMEOUT)
                    .block();
            return resp == null ? null : mapAttributes(resp);
        }
        String key = esiResponseCache.cacheKey(esiResponseCacheProperties.getDatasource(), userId,
                "/characters/" + characterId + "/attributes/", 0);
        boolean forceGranted = forceRefresh && forceRefreshLimiter.tryAcquire(key, forceRefreshWindow(), forceRefreshLimit());
        if (!forceGranted) {
            List<CharacterAttributesInfo> cached = esiResponseCache.getList(key, objectMapper.constructType(CharacterAttributesInfo.class));
            if (cached != null && !cached.isEmpty()) {
                return cached.get(0);
            }
        }
        boolean claimed = esiResponseCache.tryClaim(key, CLAIM_TTL);
        if (!claimed && !forceGranted) {
            List<CharacterAttributesInfo> filled = awaitCachedList(key, objectMapper.constructType(CharacterAttributesInfo.class));
            if (filled != null && !filled.isEmpty()) {
                return filled.get(0);
            }
        }
        try {
            ResponseEntity<CharacterAttributesResponse> response = callSdkMono(() -> skillApi.queryCharactersAttributesResponse(characterId, EsiClientConfig.SERENITY, accessToken))
                    .block();
            if (response == null || response.getBody() == null) {
                return null;
            }
            CharacterAttributesInfo domain = mapAttributes(response.getBody());
            Duration ttl = esiCacheTtlExtractor.extract(response.getHeaders());
            List<CharacterAttributesInfo> wrapped = List.of(domain);
            esiResponseCache.put(key, wrapped, ttl);
            return domain;
        } finally {
            if (claimed) {
                esiResponseCache.releaseClaim(key);
            }
        }
    }

    private CharacterAttributesInfo mapAttributes(CharacterAttributesResponse resp) {
        return new CharacterAttributesInfo(
                resp.getIntelligence(),
                resp.getMemory(),
                resp.getCharisma(),
                resp.getPerception(),
                resp.getWillpower(),
                resp.getBonusRemaps()
        );
    }

    @Override
    public List<SkillQueueItem> queryCharacterSkillQueue(Integer characterId, String accessToken) {
        return doQueryCharacterSkillQueue(characterId, accessToken, false);
    }

    @Override
    public List<SkillQueueItem> queryCharacterSkillQueue(Integer characterId, String accessToken, boolean forceRefresh) {
        return doQueryCharacterSkillQueue(characterId, accessToken, forceRefresh);
    }

    private List<SkillQueueItem> doQueryCharacterSkillQueue(Integer characterId, String accessToken, boolean forceRefresh) {
        Integer userId = UserUtil.getUserId();
        if (userId == null || userId <= 0) {
            // 未认证/无上下文:不缓存,直接真调(安全优先)
            List<SkillQueueResponse> body = callSdkMono(() -> skillApi.queryCharactersSkillqueueResponse(characterId, EsiClientConfig.SERENITY, accessToken))
                    .timeout(EsiClientConstants.ESI_DATA_TIMEOUT)
                    .map(ResponseEntity::getBody)
                    .block();
            return body == null ? List.of() : body.stream().map(this::mapSkillQueueItem).toList();
        }
        String key = esiResponseCache.cacheKey(esiResponseCacheProperties.getDatasource(), userId,
                "/characters/" + characterId + "/skillqueue/", 0);
        boolean forceGranted = forceRefresh && forceRefreshLimiter.tryAcquire(key, forceRefreshWindow(), forceRefreshLimit());
        if (!forceGranted) {
            List<SkillQueueItem> cached = esiResponseCache.getList(key, objectMapper.constructType(SkillQueueItem.class));
            if (cached != null) {
                return cached;
            }
        }
        // 防穿透
        boolean claimed = esiResponseCache.tryClaim(key, CLAIM_TTL);
        if (!claimed && !forceGranted) {
            List<SkillQueueItem> filled = awaitCachedList(key, objectMapper.constructType(SkillQueueItem.class));
            if (filled != null) {
                return filled;
            }
        }
        try {
            ResponseEntity<List<SkillQueueResponse>> response = callSdkMono(() -> skillApi.queryCharactersSkillqueueResponse(characterId, EsiClientConfig.SERENITY, accessToken))
                    .block();
            if (response == null) {
                return null;
            }
            // 空 body → 空列表回填(015 约定,与查询失败可区分——失败抛 EsiAccessException)
            List<SkillQueueItem> domain = response.getBody() == null || response.getBody().isEmpty()
                    ? List.of()
                    : response.getBody().stream().map(this::mapSkillQueueItem).toList();
            Duration ttl = esiCacheTtlExtractor.extract(response.getHeaders());
            esiResponseCache.put(key, domain, ttl);
            return domain;
        } finally {
            if (claimed) {
                esiResponseCache.releaseClaim(key);
            }
        }
    }

    private SkillQueueItem mapSkillQueueItem(SkillQueueResponse resp) {
        return new SkillQueueItem(
                resp.getSkillId(),
                resp.getFinishedLevel(),
                resp.getTrainingStartSp() == null ? null : resp.getTrainingStartSp().longValue(),
                resp.getLevelEndSp() == null ? null : resp.getLevelEndSp().longValue(),
                resp.getStartDate(),
                resp.getFinishDate(),
                resp.getQueuePosition()
        );
    }

    // ── 邮件读侧(伴侣 APP T016) ──

    @Override
    public MailHeaderPage queryCharacterMailHeaders(Integer characterId, Integer lastMailId, String accessToken) {
        return doQueryCharacterMailHeaders(characterId, lastMailId, accessToken, false);
    }

    @Override
    public MailHeaderPage queryCharacterMailHeaders(Integer characterId, Integer lastMailId, String accessToken, boolean forceRefresh) {
        return doQueryCharacterMailHeaders(characterId, lastMailId, accessToken, forceRefresh);
    }

    private MailHeaderPage doQueryCharacterMailHeaders(Integer characterId, Integer lastMailId, String accessToken, boolean forceRefresh) {
        Integer userId = UserUtil.getUserId();
        if (userId == null || userId <= 0) {
            // 未认证/无上下文:不缓存,直接真调(安全优先)
            List<RequestedMailResponse> body = callSdkMono(() -> mailApi.queryCharacterMailsResponse(characterId, EsiClientConfig.SERENITY, null, lastMailId, accessToken))
                    .timeout(EsiClientConstants.ESI_DATA_TIMEOUT)
                    .map(ResponseEntity::getBody)
                    .block();
            return body == null ? null : mapMailHeaderPage(body);
        }
        // lastMailId 编入键 page 段(仿工业列表)
        String key = esiResponseCache.cacheKey(esiResponseCacheProperties.getDatasource(), userId,
                "/characters/" + characterId + "/mail/?last_mail_id=" + lastMailId, 0);
        boolean forceGranted = forceRefresh && forceRefreshLimiter.tryAcquire(key, forceRefreshWindow(), forceRefreshLimit());
        if (!forceGranted) {
            List<MailHeaderPage> cached = esiResponseCache.getList(key, objectMapper.constructType(MailHeaderPage.class));
            if (cached != null && !cached.isEmpty()) {
                return cached.get(0);
            }
        }
        // 防穿透
        boolean claimed = esiResponseCache.tryClaim(key, CLAIM_TTL);
        if (!claimed && !forceGranted) {
            List<MailHeaderPage> filled = awaitCachedList(key, objectMapper.constructType(MailHeaderPage.class));
            if (filled != null && !filled.isEmpty()) {
                return filled.get(0);
            }
        }
        try {
            ResponseEntity<List<RequestedMailResponse>> response = callSdkMono(() -> mailApi.queryCharacterMailsResponse(characterId, EsiClientConfig.SERENITY, null, lastMailId, accessToken))
                    .block();
            if (response == null || response.getBody() == null) {
                return null;
            }
            MailHeaderPage domain = mapMailHeaderPage(response.getBody());
            Duration ttl = esiCacheTtlExtractor.extract(response.getHeaders());
            List<MailHeaderPage> wrapped = List.of(domain);
            esiResponseCache.put(key, wrapped, ttl);
            return domain;
        } finally {
            if (claimed) {
                esiResponseCache.releaseClaim(key);
            }
        }
    }

    private MailHeaderPage mapMailHeaderPage(List<RequestedMailResponse> mails) {
        List<MailHeaderPage.MailHeaderItem> items = mails.stream()
                .map(m -> new MailHeaderPage.MailHeaderItem(
                        m.getMailId(),
                        m.getFrom(),
                        m.getSubject(),
                        m.getTimestamp(),
                        m.getIsRead(),
                        m.getLabels()
                ))
                .toList();
        // 满 50 条 → nextLastMailId = 最后一条 mailId;不足 → null(无下一页)
        Integer nextLastMailId = items.size() >= 50 ? items.get(items.size() - 1).mailId() : null;
        return new MailHeaderPage(items, nextLastMailId);
    }

    @Override
    public MailDetailInfo queryCharacterMail(Integer characterId, Integer mailId, String accessToken) {
        return doQueryCharacterMail(characterId, mailId, accessToken, false);
    }

    @Override
    public MailDetailInfo queryCharacterMail(Integer characterId, Integer mailId, String accessToken, boolean forceRefresh) {
        return doQueryCharacterMail(characterId, mailId, accessToken, forceRefresh);
    }

    private MailDetailInfo doQueryCharacterMail(Integer characterId, Integer mailId, String accessToken, boolean forceRefresh) {
        Integer userId = UserUtil.getUserId();
        if (userId == null || userId <= 0) {
            MailResponse resp = callSdkMono(() -> mailApi.queryCharacterMail(characterId, EsiClientConfig.SERENITY, mailId, accessToken))
                    .timeout(EsiClientConstants.ESI_DATA_TIMEOUT)
                    .block();
            return resp == null ? null : mapMailDetail(mailId, resp);
        }
        String key = esiResponseCache.cacheKey(esiResponseCacheProperties.getDatasource(), userId,
                "/characters/" + characterId + "/mail/" + mailId + "/", 0);
        boolean forceGranted = forceRefresh && forceRefreshLimiter.tryAcquire(key, forceRefreshWindow(), forceRefreshLimit());
        if (!forceGranted) {
            List<MailDetailInfo> cached = esiResponseCache.getList(key, objectMapper.constructType(MailDetailInfo.class));
            if (cached != null && !cached.isEmpty()) {
                return cached.get(0);
            }
        }
        boolean claimed = esiResponseCache.tryClaim(key, CLAIM_TTL);
        if (!claimed && !forceGranted) {
            List<MailDetailInfo> filled = awaitCachedList(key, objectMapper.constructType(MailDetailInfo.class));
            if (filled != null && !filled.isEmpty()) {
                return filled.get(0);
            }
        }
        try {
            ResponseEntity<MailResponse> response = callSdkMono(() -> mailApi.queryCharacterMailResponse(characterId, EsiClientConfig.SERENITY, mailId, accessToken))
                    .block();
            if (response == null || response.getBody() == null) {
                return null;
            }
            MailDetailInfo domain = mapMailDetail(mailId, response.getBody());
            Duration ttl = esiCacheTtlExtractor.extract(response.getHeaders());
            List<MailDetailInfo> wrapped = List.of(domain);
            esiResponseCache.put(key, wrapped, ttl);
            return domain;
        } finally {
            if (claimed) {
                esiResponseCache.releaseClaim(key);
            }
        }
    }

    private MailDetailInfo mapMailDetail(Integer mailId, MailResponse resp) {
        List<MailRecipient> recipients = resp.getRecipients() == null ? List.of()
                : resp.getRecipients().stream()
                        .map(r -> new MailRecipient(r.getRecipientId(), r.getRecipientType()))
                        .toList();
        return new MailDetailInfo(
                mailId,
                resp.getFrom(),
                recipients,
                resp.getSubject(),
                resp.getTimestamp(),
                resp.getBody(),
                resp.getRead()
        );
    }

    @Override
    public MailLabelsInfo queryCharacterMailLabels(Integer characterId, String accessToken) {
        return doQueryCharacterMailLabels(characterId, accessToken, false);
    }

    @Override
    public MailLabelsInfo queryCharacterMailLabels(Integer characterId, String accessToken, boolean forceRefresh) {
        return doQueryCharacterMailLabels(characterId, accessToken, forceRefresh);
    }

    private MailLabelsInfo doQueryCharacterMailLabels(Integer characterId, String accessToken, boolean forceRefresh) {
        Integer userId = UserUtil.getUserId();
        if (userId == null || userId <= 0) {
            MailLabelsAndUnreadCountsResponse resp = callSdkMono(() -> mailApi.queryCharacterMailLabels(characterId, EsiClientConfig.SERENITY, accessToken))
                    .timeout(EsiClientConstants.ESI_DATA_TIMEOUT)
                    .block();
            return resp == null ? null : mapMailLabels(resp);
        }
        String key = esiResponseCache.cacheKey(esiResponseCacheProperties.getDatasource(), userId,
                "/characters/" + characterId + "/mail/labels/", 0);
        boolean forceGranted = forceRefresh && forceRefreshLimiter.tryAcquire(key, forceRefreshWindow(), forceRefreshLimit());
        if (!forceGranted) {
            List<MailLabelsInfo> cached = esiResponseCache.getList(key, objectMapper.constructType(MailLabelsInfo.class));
            if (cached != null && !cached.isEmpty()) {
                return cached.get(0);
            }
        }
        boolean claimed = esiResponseCache.tryClaim(key, CLAIM_TTL);
        if (!claimed && !forceGranted) {
            List<MailLabelsInfo> filled = awaitCachedList(key, objectMapper.constructType(MailLabelsInfo.class));
            if (filled != null && !filled.isEmpty()) {
                return filled.get(0);
            }
        }
        try {
            ResponseEntity<MailLabelsAndUnreadCountsResponse> response = callSdkMono(() -> mailApi.queryCharacterMailLabelsResponse(characterId, EsiClientConfig.SERENITY, accessToken))
                    .block();
            if (response == null || response.getBody() == null) {
                return null;
            }
            MailLabelsInfo domain = mapMailLabels(response.getBody());
            Duration ttl = esiCacheTtlExtractor.extract(response.getHeaders());
            List<MailLabelsInfo> wrapped = List.of(domain);
            esiResponseCache.put(key, wrapped, ttl);
            return domain;
        } finally {
            if (claimed) {
                esiResponseCache.releaseClaim(key);
            }
        }
    }

    private MailLabelsInfo mapMailLabels(MailLabelsAndUnreadCountsResponse resp) {
        List<MailLabelsInfo.MailLabel> labels = resp.getLabels() == null ? List.of()
                : resp.getLabels().stream()
                        .map(l -> new MailLabelsInfo.MailLabel(
                                l.getLabelId(),
                                l.getName(),
                                l.getUnreadCount() == null ? null : l.getUnreadCount().longValue()
                        ))
                        .toList();
        Long totalUnread = resp.getTotalUnreadCount() == null ? null : resp.getTotalUnreadCount().longValue();
        return new MailLabelsInfo(labels, totalUnread);
    }

    // ── 邮件写侧(伴侣 APP T017) ──

    @Override
    public void markCharacterMailRead(Integer characterId, Integer mailId, boolean read, String accessToken) {
        // 写操作:直穿 SDK,不缓存、不限流
        callSdkMono(() -> mailApi.updateCharacterMail(characterId, EsiClientConfig.SERENITY, mailId, null, read, accessToken))
                .timeout(EsiClientConstants.ESI_DATA_TIMEOUT)
                .block();
    }

    @Override
    public Integer sendCharacterMail(Integer characterId, SendMailCommand command, String accessToken) {
        // 写操作:直穿 SDK,不缓存、不限流
        NewMail newMail = new NewMail();
        newMail.setSubject(command.subject());
        newMail.setBody(command.body());
        List<xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.sub.Recipient> sdkRecipients = command.recipients().stream()
                .map(r -> {
                    xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.sub.Recipient sdkR = new xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.sub.Recipient();
                    sdkR.setRecipientId(r.recipientId());
                    sdkR.setRecipientType(r.recipientType());
                    return sdkR;
                })
                .toList();
        newMail.setRecipients(sdkRecipients);
        return callSdkMono(() -> mailApi.addCharacterMail(characterId, EsiClientConfig.SERENITY, newMail, accessToken))
                .timeout(EsiClientConstants.ESI_DATA_TIMEOUT)
                .block();
    }

    // ── universe/ids 精确名称解析(伴侣 APP T018) ──

    @Override
    public List<RecipientResolution> resolveUniverseIds(List<String> names) {
        // 公开数据:userId 段置 pub 全局共享(不查 UserUtil、不限流);names 排序确定编码进键
        String key = esiResponseCache.cacheKey(esiResponseCacheProperties.getDatasource(), null,
                encodeNames(names), 0);
        List<RecipientResolution> cached = esiResponseCache.getList(key, objectMapper.constructType(RecipientResolution.class));
        if (cached != null) {
            return cached;
        }
        // 防穿透
        boolean claimed = esiResponseCache.tryClaim(key, CLAIM_TTL);
        if (!claimed) {
            List<RecipientResolution> filled = awaitCachedList(key, objectMapper.constructType(RecipientResolution.class));
            if (filled != null) {
                return filled;
            }
        }
        try {
            ResponseEntity<Name2IdResponse> response = callSdkMono(() -> universeApi.queryUniverseIdsResponse(names, EsiClientConfig.SERENITY, null))
                    .block();
            if (response == null || response.getBody() == null) {
                // 全量未命中
                return names.stream().map(n -> new RecipientResolution(n, null, null, false)).toList();
            }
            List<RecipientResolution> domain = mapRecipientResolutions(names, response.getBody());
            Duration ttl = esiCacheTtlExtractor.extract(response.getHeaders());
            esiResponseCache.put(key, domain, ttl);
            return domain;
        } finally {
            if (claimed) {
                esiResponseCache.releaseClaim(key);
            }
        }
    }

    private List<RecipientResolution> mapRecipientResolutions(List<String> requestedNames, Name2IdResponse resp) {
        // 按分组收集已解析的 name→(id, type)
        java.util.Map<String, RecipientResolution> resolved = new java.util.LinkedHashMap<>();
        if (resp.getCharacters() != null) {
            resp.getCharacters().forEach(n -> resolved.put(n.getName(),
                    new RecipientResolution(n.getName(), n.getId(), "character", true)));
        }
        if (resp.getCorporations() != null) {
            resp.getCorporations().forEach(n -> resolved.put(n.getName(),
                    new RecipientResolution(n.getName(), n.getId(), "corporation", true)));
        }
        if (resp.getAlliances() != null) {
            resp.getAlliances().forEach(n -> resolved.put(n.getName(),
                    new RecipientResolution(n.getName(), n.getId(), "alliance", true)));
        }
        // 保持请求名全量返回,未命中项 resolved=false
        return requestedNames.stream()
                .map(name -> resolved.getOrDefault(name, new RecipientResolution(name, null, null, false)))
                .toList();
    }

    /**
     * names 集合确定性编码进缓存键:排序后按序拼接,保证"同组同键、异组异键"(顺序无关)。
     */
    private String encodeNames(List<String> names) {
        String encoded = names.stream().sorted().collect(Collectors.joining("-"));
        return "/universe/ids/" + encoded + "/";
    }

}