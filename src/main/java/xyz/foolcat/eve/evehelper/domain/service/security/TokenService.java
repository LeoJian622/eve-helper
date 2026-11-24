package xyz.foolcat.eve.evehelper.domain.service.security;

import cn.hutool.core.lang.UUID;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.shared.kernel.config.JwtTokenProperties;
import xyz.foolcat.eve.evehelper.shared.kernel.constants.GlobalConstants;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysUser;
import xyz.foolcat.eve.evehelper.domain.model.vo.TokenResult;
import xyz.foolcat.eve.evehelper.domain.port.cache.CacheGateway;
import xyz.foolcat.eve.evehelper.shared.kernel.constants.SecurityConstant;
import xyz.foolcat.eve.evehelper.shared.util.SensitiveDataMasker;

import java.security.KeyPair;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Token管理服务
 * 负责生成和刷新Access Token和Refresh Token
 *
 * @author Leojan
 * date 2026-01-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final KeyPair keyPair;
    private final JwtTokenProperties jwtTokenProperties;
    private final CacheGateway cacheGateway;

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";

    /**
     * 会话索引前缀:{@code refresh_session:<sid> -> <refresh token uuid>}(007 T048)。
     *
     * <p><b>为何锚点是 sid 而不是 jti</b>(设计评审 CRITICAL):jti 每次轮换都变,
     * 且轮换<b>不</b>拉黑旧 access token,而 {@code POST /auth/tokens} 在白名单 ——
     * 若以 jti 作锚点,攻击者持被盗 refresh token 发<b>一个未认证请求</b>即可让索引永久错位:
     * 此后受害者每次登出都撤不到当前 refresh token,<b>却照样返回 204 成功</b>,
     * 即控制项在它唯一的存在理由(凭证已泄露)下失效。sid 在轮换时原样继承,不存在此问题。</p>
     *
     * <p><b>TTL 与 refresh token 同寿(而非 access TTL)</b>:若绑定 access TTL,
     * 安全正确性就被耦合到授权配置上 —— 一旦登出改为接受已过期 access token,
     * 索引会先行消失且无任何测试报警;多实例时钟偏斜亦开同样窗口。</p>
     */
    private static final String SESSION_INDEX_PREFIX = "refresh_session:";

    /**
     * 反向指针:{@code refresh_owner:<refresh uuid> -> <sid>}(007 T048)。
     *
     * <p>刷新请求只携带 refresh token(端点在白名单,可能<b>没有</b> access token),
     * 因此轮换时无法从请求中取得 sid。本键让轮换能由 refresh token 反查所属会话,
     * 从而<b>继承</b>同一 sid 并重写正向索引。</p>
     *
     * <p>与 refresh token 同生共死:轮换时随旧 token 一并删除、随新 token 一并写入,
     * 故不会随会话累积。值为 sid(不透明句柄),<b>不含</b>任何凭证。</p>
     */
    private static final String REFRESH_OWNER_PREFIX = "refresh_owner:";

    /**
     * 会话级失效标记:{@code session_revoked:<sid>}(007 T049-A)。
     *
     * <p>登出时设置,刷新在 claim 旧 refresh token 后、generate 新 token 前校验 -- 闭合
     * T048 的并发缺口(刷新 claim 旧 token -> 登出跑完 -> 刷新 generate 新 token)。
     * TTL = refresh TTL:短于 refresh TTL 则 refresh token 超活 tombstone -> fail-open。</p>
     *
     * <p><b>不可复用</b>:登出 set,轮换从不 clear。与 {@link #SESSION_INDEX_PREFIX} 不同 --
     * 后者被轮换重写,不能作「会话已登出」的信号。</p>
     */
    private static final String SESSION_REVOKED_PREFIX = "session_revoked:";

    /**
     * 会话创建时刻索引:{@code refresh_iat:<sid> -> <毫秒时间戳>}(022 T005)。
     *
     * <p>登录新建 sid 时以 {@code setIfAbsent} 写入,轮换沿用旧 sid 时不覆盖原值
     * (T006 refresh 路径以「会话创建时间 vs 改密纪元」判定,轮换若刷新会破坏 fail-closed)。
     * TTL = refresh TTL,随后 {@code expire} 续期。</p>
     */
    private static final String REFRESH_IAT_PREFIX = "refresh_iat:";

    /**
     * 用户→会话反向索引:{@code refresh_user:<userId> -> Set<sid>}(022 T005)。
     *
     * <p>用于改密全会话失效(T006/T041):经 userId 枚举该用户所有活跃 sid,
     * 逐一撤销。sAdd 后 expire 续期(TTL = refresh TTL)。</p>
     */
    private static final String REFRESH_USER_PREFIX = GlobalConstants.REFRESH_USER_KEY_PREFIX;

    /**
     * 改密纪元标记键前缀(022 T006, 收敛至 GlobalConstants.REVOKE_EPOCH_KEY_PREFIX)。
     */
    private static final String REVOKE_EPOCH_PREFIX = GlobalConstants.REVOKE_EPOCH_KEY_PREFIX;


    /**
     * 生成Token对(Access Token + Refresh Token)
     *
     * @param user        用户信息
     * @param authorities 权限列表(角色标识),写入 JWT authorities claim
     * @return Token对(领域读模型)
     */
    public TokenResult generateTokenPair(SysUser user, List<String> authorities) {
        // 007 T048:登录即开启新会话,生成新的 sid
        return generateTokenPair(user, authorities, UUID.randomUUID().toString());
    }

    /**
     * 生成Token对,并把会话标识绑定到指定 {@code sid}(007 T048)。
     *
     * <p>轮换时<b>必须</b>沿用旧 sid,使 {@code refresh_session:<sid>} 索引
     * 指向轮换后的新 refresh token —— 否则登出无法撤销当前凭证。</p>
     *
     * @param user        用户信息
     * @param authorities 权限列表(角色标识)
     * @param sessionId   会话标识:登录时新建,轮换时继承
     * @return Token对(领域读模型)
     */
    private TokenResult generateTokenPair(SysUser user, List<String> authorities, String sessionId) {
        try {
            // 生成Access Token
            String accessToken = generateAccessToken(user, authorities, sessionId);

            // 生成Refresh Token
            String refreshToken = generateRefreshToken(user);

            // 007 T048:写入/重写会话索引,使登出能由 sid 定位到当前 refresh token。
            // 与 refresh token 同 TTL —— 索引值即 refresh token 明文,不得比它活得更久。
            long refreshTtl = jwtTokenProperties.getRefreshTokenExpirationTime();
            cacheGateway.set(SESSION_INDEX_PREFIX + sessionId, refreshToken, refreshTtl, TimeUnit.SECONDS);
            // 反向指针:供轮换时由 refresh token 反查 sid(刷新请求不带 access token)
            cacheGateway.set(REFRESH_OWNER_PREFIX + refreshToken, sessionId, refreshTtl, TimeUnit.SECONDS);

            // 022 T005:会话创建时刻索引(setIfAbsent —— 轮换沿用旧 sid 时不覆盖原值,
            // T006 refresh 路径以「会话创建时间 vs 改密纪元」判定,轮换若刷新会破坏 fail-closed)
            cacheGateway.setIfAbsent(REFRESH_IAT_PREFIX + sessionId,
                    System.currentTimeMillis(), refreshTtl, TimeUnit.SECONDS);
            cacheGateway.expire(REFRESH_IAT_PREFIX + sessionId, refreshTtl, TimeUnit.SECONDS);

            // 022 T005:用户→会话反向索引(供改密全会话失效枚举)
            cacheGateway.sAdd(REFRESH_USER_PREFIX + user.getId(), sessionId);
            cacheGateway.expire(REFRESH_USER_PREFIX + user.getId(), refreshTtl, TimeUnit.SECONDS);

            return new TokenResult(
                    SecurityConstant.JWT_PREFIX + accessToken,
                    refreshToken,
                    jwtTokenProperties.getAccessTokenExpirationTime(),
                    "Bearer");

        } catch (JOSEException e) {
            log.error("生成Token失败", e);
            throw new RuntimeException("生成Token失败", e);
        }
    }

    /**
     * 生成Access Token
     *
     * @param user        用户信息
     * @param authorities 权限列表(角色标识)
     * @param sessionId   会话标识,写入 {@code sid} claim(007 T048)
     * @return JWT字符串
     */
    private String generateAccessToken(SysUser user, List<String> authorities, String sessionId) throws JOSEException {
        long expirationTime = jwtTokenProperties.getAccessTokenExpirationTime() * 1000;
        List<String> safeAuthorities = (authorities == null) ? List.of() : authorities;

        String jti = UUID.randomUUID().toString();

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(jwtTokenProperties.getSubject())
                .issuer(jwtTokenProperties.getIssuer())
                .jwtID(jti)
                .claim(SecurityConstant.USER_ID_KEY, user.getId())
                .claim(SecurityConstant.USER_NAME_KEY, user.getUsername())
                .claim(SecurityConstant.JWT_AUTHORITIES_KEY, safeAuthorities)
                .claim(SecurityConstant.SESSION_ID_KEY, sessionId)
                .issueTime(new Date())
                .expirationTime(new Date(System.currentTimeMillis() + expirationTime))
                .build();


        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).build(),
                claimsSet);

        JWSSigner signer = new RSASSASigner(keyPair.getPrivate());
        signedJWT.sign(signer);

        return signedJWT.serialize();
    }

    /**
     * 生成Refresh Token
     *
     * @param user 用户信息
     * @return Refresh Token ID
     */
    private String generateRefreshToken(SysUser user) {
        String refreshTokenId = UUID.randomUUID().toString();
        String key = REFRESH_TOKEN_PREFIX + refreshTokenId;

        // 存储到Redis: refresh_token:{jti} -> userId
        long ttl = jwtTokenProperties.getRefreshTokenExpirationTime();
        cacheGateway.set(key, user.getId(), ttl, TimeUnit.SECONDS);

        log.info("生成Refresh Token: userId={}, refreshTokenId={}, ttl={}s",
                user.getId(), SensitiveDataMasker.maskToken(refreshTokenId), ttl);

        return refreshTokenId;
    }

    /**
     * 按会话标识撤销 refresh token(007 T048,登出路径)。
     *
     * <p>由 {@code sid} 经 {@code refresh_session:<sid>} 索引定位<b>当前</b>
     * refresh token 并删除,随后清理索引自身(索引值是 refresh token 明文,
     * 不得留悬垂键)。</p>
     *
     * <p><b>不 fail-closed</b>:索引缺失(滚动部署遗留 token、缓存驱逐、
     * 会话已被轮换清理等)时只记录告警并返回 false,由调用方继续完成拉黑。
     * 理由:登出端点要求持<b>未过期</b> access token,若在此抛异常,
     * 客户端重试也无用(access token 已被拉黑)—— 只会把用户永久卡在「登不出」,
     * 而拉黑本身已生效。可用性优先于此处的严格性。</p>
     *
     * @param sessionId access token 的 {@code sid} 声明,null/空表示无会话标识
     * @return true 表示确实撤销了一个 refresh token
     */
    public boolean revokeRefreshTokenBySession(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            // 滚动部署期间,改造前签发的 access token 没有 sid —— 降级为「仅拉黑」
            log.warn("[SECURITY_ALERT:LOGOUT_REVOKE_MISS] access token 无 sid 声明,"
                    + "无法撤销 refresh token(改造前签发的旧 token?)");
            return false;
        }

        String indexKey = SESSION_INDEX_PREFIX + sessionId;
        Object refreshTokenObj = cacheGateway.get(indexKey);
        if (refreshTokenObj == null) {
            log.warn("[SECURITY_ALERT:LOGOUT_REVOKE_MISS] 会话索引缺失,refresh token 未被撤销: sid={}",
                    SensitiveDataMasker.maskToken(sessionId));
            return false;
        }

        // delete 返回 Boolean(可为 null)。此处必须用 Boolean.TRUE.equals ——
        // 拆箱进 boolean 会在 Redis 异常时 NPE,而该 NPE 发生在拉黑之前,
        // 将导致整个登出 500 且 access token 也没拉黑
        // MIN-2:删除前读取 userId(refresh_token:{uuid} -> userId),供撤销成功后剪枝 refresh_user 反向索引
        Object userIdObj = cacheGateway.get(REFRESH_TOKEN_PREFIX + refreshTokenObj);
        Boolean revoked = cacheGateway.delete(REFRESH_TOKEN_PREFIX + refreshTokenObj);
        cacheGateway.delete(REFRESH_OWNER_PREFIX + refreshTokenObj);

        if (Boolean.TRUE.equals(revoked)) {
            cacheGateway.delete(indexKey);
            pruneUserSessionIndex(userIdObj, sessionId);
            log.info("登出撤销 Refresh Token: sid={}, refreshToken={}",
                    SensitiveDataMasker.maskToken(sessionId),
                    SensitiveDataMasker.maskToken(refreshTokenObj.toString()));
            return true;
        }

        // 未删到:说明该 token 已被并发刷新抢占。此时索引很可能已被那次轮换
        // 重写为「新」refresh token —— 若在此无条件删除索引,会把新 token 的
        // 唯一锚点抹掉,用户登出后反而留下一个永远撤不到的存活凭证。
        // 故重读索引:仅当它仍指向我们刚才读到的旧 token 时才删(compare-and-delete);
        // 若已改指新 token,则递归撤销这一轮换结果 —— 递归深度由「每层都
        // 消灭一个 refresh token」保证收敛(并发刷新次数有限)。
        Object current = cacheGateway.get(indexKey);
        if (current == null) {
            log.warn("[SECURITY_ALERT:LOGOUT_REVOKE_MISS] 索引存在但 refresh token 已不在缓存中: sid={}",
                    SensitiveDataMasker.maskToken(sessionId));
            return false;
        }
        if (current.equals(refreshTokenObj)) {
            cacheGateway.delete(indexKey);
            log.warn("[SECURITY_ALERT:LOGOUT_REVOKE_MISS] 索引存在但 refresh token 已不在缓存中: sid={}",
                    SensitiveDataMasker.maskToken(sessionId));
            return false;
        }
        log.warn("登出与刷新并发:索引已被轮换重写,继续撤销轮换后的 token: sid={}",
                SensitiveDataMasker.maskToken(sessionId));
        return revokeRefreshTokenBySession(sessionId);
    }

    /**
     * MIN-2:撤销成功后剪枝用户→会话反向索引({@code refresh_user:<userId>})。
     *
     * <p>该集合生成时只 sAdd+expire,若撤销不剪枝,已死 sid 滞留至 TTL(最长 7 天),
     * 改密全会话失效(T006/T041)会枚举到死会话做无效撤销。登出与改密两条撤销路径
     * 均经 {@link #revokeRefreshTokenBySession} 收敛到本剪枝。</p>
     *
     * <p>userId 读不到(token 恰在读取前过期/被并发抢占)时跳过剪枝,交由集合 TTL 兜底,
     * 不阻断登出主流程(与 revokeRefreshTokenBySession 的可用性优先一致)。</p>
     *
     * @param userIdObj refresh_token 键存储的用户 ID(可能为 null)
     * @param sessionId 已撤销的会话标识
     */
    private void pruneUserSessionIndex(Object userIdObj, String sessionId) {
        if (userIdObj == null) {
            return;
        }
        cacheGateway.sRemove(REFRESH_USER_PREFIX + userIdObj, sessionId);
    }

    /**
     * 标记会话已登出,设置 {@code session_revoked:<sid>} tombstone(007 T049-A)。
     *
     * <p><b>必须在 {@code AuthApplicationService.logout} 中、{@link #revokeRefreshTokenBySession}
     * 返回之后无条件调用</b>(评审 HIGH-1)。不得置于 {@code revokeRefreshTokenBySession} 内部 --
     * 该方法 5 条 early-return(索引驱逐/sid 空/token 已不在缓存等)会跳过 tombstone 设置,
     * 而那恰是攻击者可利用的 fail-open 路径。</p>
     *
     * <p>TTL = refresh TTL:短于 refresh TTL 则 refresh token 超活 tombstone -> fail-open。
     * null/空 sid(pre-T048 遗留)时 no-op。</p>
     *
     * @param sessionId access token 的 {@code sid} 声明
     */
    public void markSessionRevoked(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return;
        }
        long refreshTtl = jwtTokenProperties.getRefreshTokenExpirationTime();
        cacheGateway.set(SESSION_REVOKED_PREFIX + sessionId, "1", refreshTtl, TimeUnit.SECONDS);
    }

    /**
     * 校验会话是否已登出(007 T049-A,刷新路径调用)。
     *
     * <p>tombstone absent 是<b>正常刷新的常态</b>(用户未登出时不存在),故 null 视为"未登出"(fail-open)。
     * <b>不可告警</b>(评审 MEDIUM-3):无法区分"正常未登出"与"被驱逐"。null/空 sid 视为未登出(pre-T048 遗留)。</p>
     */
    private boolean isSessionRevoked(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return false;
        }
        return cacheGateway.get(SESSION_REVOKED_PREFIX + sessionId) != null;
    }

    /**
     * 从Refresh Token获取用户ID
     *
     * @param refreshToken Refresh Token
     * @return 用户ID
     */
    public Integer getUserIdFromRefreshToken(String refreshToken) {
        String key = REFRESH_TOKEN_PREFIX + refreshToken;

        Object userIdObj = cacheGateway.get(key);
        if (userIdObj == null) {
            throw new IllegalArgumentException("Refresh Token无效或已过期");
        }

        if (userIdObj instanceof Integer number) {
            return number;
        } else {
            return Integer.parseInt(userIdObj.toString());
        }
    }

    /**
     * 验证并刷新Access Token (完整版)
     * 实现Refresh Token轮换机制,防止token重放攻击
     *
     * @param refreshToken Refresh Token
     * @param user         用户信息(从数据库重新加载)
     * @param authorities  权限列表(角色标识),写入新 JWT
     * @return 新的Token对(领域读模型)
     */
    public TokenResult refreshAccessTokenWithUser(String refreshToken, SysUser user, List<String> authorities) {
        String key = REFRESH_TOKEN_PREFIX + refreshToken;

        // 验证Refresh Token是否存在
        Object userIdObj = cacheGateway.get(key);
        if (userIdObj == null) {
            log.warn("Refresh Token不存在或已过期: refreshToken={}", SensitiveDataMasker.maskToken(refreshToken));
            throw new IllegalArgumentException("Refresh Token无效或已过期");
        }

        // 验证用户ID是否匹配
        Integer storedUserId;
        if (userIdObj instanceof Integer number) {
            storedUserId = number;
        } else {
            storedUserId = Integer.parseInt(userIdObj.toString());
        }

        if (!storedUserId.equals(user.getId())) {
            log.error("Refresh Token用户ID不匹配: expected={}, actual={}",
                    storedUserId, user.getId());
            throw new IllegalArgumentException("Refresh Token无效");
        }

        log.info("刷新Access Token: userId={}, refreshToken={}", user.getId(), SensitiveDataMasker.maskToken(refreshToken));

        // 007 T042:撤销即 claim —— 必须在签发新 token 之前「抢占」旧 token。
        // Redis DEL 本身原子,只有真正删掉的调用返回 true;并发双请求中另一个
        // 必然拿到 false/null,据此拒绝。若沿用「不看返回值的 delete」,
        // 双方都能走到 generateTokenPair,各得一套有效 token 对(轮换一次性失效)。
        // fail-closed:null(Redis 异常等语义不明)一律按抢占失败处理。
        Boolean claimed = cacheGateway.delete(key);
        if (!Boolean.TRUE.equals(claimed)) {
            log.warn("Refresh Token 抢占失败(已被并发请求用掉或恰好过期): userId={}, refreshToken={}",
                    user.getId(), SensitiveDataMasker.maskToken(refreshToken));
            throw new IllegalArgumentException("Refresh Token无效或已过期");
        }

        // 007 T048:继承会话标识 —— 轮换必须沿用同一 sid,使正向索引改指向新 refresh token。
        // 否则登出只能撤销「轮换前」的 token,攻击者刷一次即可让受害者永久撤不到当前凭证。
        // sid 缺失(滚动部署遗留 token)时新开一个会话,保证轮换本身仍可用。
        String ownerKey = REFRESH_OWNER_PREFIX + refreshToken;
        Object sessionIdObj = cacheGateway.get(ownerKey);
        cacheGateway.delete(ownerKey);
        String sessionId = (sessionIdObj == null) ? UUID.randomUUID().toString() : sessionIdObj.toString();
        if (sessionIdObj == null) {
            log.warn("Refresh Token 无会话归属(改造前签发?),轮换时新建会话: refreshToken={}",
                    SensitiveDataMasker.maskToken(refreshToken));
        }

        // 007 T049-A:会话已登出则拒绝刷新。检查在 claim 之后、generate 之前 --
        // 闭合 T048 的并发缺口(刷新 claim 旧 token -> 登出 set tombstone -> 刷新 check -> 拒绝)。
        // 抛与既有失效同一文案,不新增可区分信息(防原因枚举)。
        if (isSessionRevoked(sessionId)) {
            log.warn("会话已登出,刷新被拒绝(tombstone 命中): sid={}",
                    SensitiveDataMasker.maskToken(sessionId));
            throw new IllegalArgumentException("Refresh Token无效或已过期");
        }

        // 022 T006:改密纪元强制校验(fail-closed)。
        // 判定逻辑与 JwtAuthorizationTokenFilter 逐字一致(互相引用):
        //   epoch == null → 放行(含无 iat 旧 token,部署兼容)
        //   epoch != null && (iat == null || iat <= epoch) → 拒绝
        // iat 来源:refresh_iat:<sid>(T005 写入的会话创建毫秒)。
        // iat 秒精度:JWT iat 为秒级 NumericDate,转毫秒后相对毫秒纪元只会偏小,
        // 只会过度拒绝(fail-closed 安全方向),不得引入任何容差/放宽逻辑。
        Object epochObj = cacheGateway.get(REVOKE_EPOCH_PREFIX + user.getId());
        if (epochObj != null) {
            Object iatObj = cacheGateway.get(REFRESH_IAT_PREFIX + sessionId);
            Long iat = (iatObj != null) ? Long.parseLong(iatObj.toString()) : null;
            long epoch = Long.parseLong(epochObj.toString());
            if (iat == null || iat <= epoch) {
                log.warn("改密纪元校验拒绝: userId={}, iat={}, epoch={}",
                        user.getId(), iat, epoch);
                throw new IllegalArgumentException("Refresh Token无效或已过期");
            }
        }

        // 生成新的Token对(包含新的Refresh Token),并重写会话索引
        return generateTokenPair(user, authorities, sessionId);
    }


    /**
     * 解析 Access Token 并提取关键声明(jti / 过期时间 / 用户ID)。
     * 将 JWT 解析细节收拢到领域层,供登出等流程使用。
     *
     * @param token 原始 Access Token(不含 "Bearer " 前缀)
     * @return 解析出的关键声明
     * @throws ParseException token 格式非法
     */
    public ParsedAccessToken parseAccessToken(String token) throws ParseException {
        SignedJWT signedJWT = SignedJWT.parse(token);
        JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
        Date issueTime = claimsSet.getIssueTime();
        return new ParsedAccessToken(
                claimsSet.getJWTID(),
                claimsSet.getExpirationTime(),
                claimsSet.getClaim(SecurityConstant.USER_ID_KEY),
                claimsSet.getStringClaim(SecurityConstant.SESSION_ID_KEY),
                issueTime != null ? issueTime.getTime() : null);
    }

    /**
     * Access Token 解析结果
     *
     * @param jti            Token 唯一标识
     * @param expirationTime 过期时间
     * @param userIdClaim    用户ID声明
     * @param sessionId      会话标识({@code sid});改造前签发的 token 为 null(007 T048)
     * @param iat            签发时刻(毫秒时间戳);改造前签发的 token 为 null(022 T005)
     */
    public record ParsedAccessToken(String jti, Date expirationTime, Object userIdClaim, String sessionId, Long iat) {
    }
}

