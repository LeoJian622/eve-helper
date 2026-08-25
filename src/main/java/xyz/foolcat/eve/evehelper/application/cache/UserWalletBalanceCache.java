package xyz.foolcat.eve.evehelper.application.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import xyz.foolcat.eve.evehelper.domain.model.vo.UserWalletBalanceVO;
import xyz.foolcat.eve.evehelper.domain.port.cache.CacheGateway;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 用户钱包余额聚合结果缓存（2026-08-25 安全审计 HIGH-1）。
 *
 * <p>缓存 {@code GET /wallet/balance} 的<b>整个聚合结果</b>，而非单个 ESI 调用。
 * 一次命中即省掉该请求的全部开销：N 次 ESI 人物余额 + 至多 N 次 ESI 军团分账
 * + N 次 authorize 库查 + 2N 次 {@code getAccessToken}（其内部还各有一次库查）。</p>
 *
 * <h3>为什么需要它</h3>
 * <p>安全审计将该端点的 ESI 阻塞扇出定级为 HIGH（可用性类安全缺陷，非单纯性能问题），
 * 两个已验证场景均可由<b>任意最低权限的已认证用户</b>触发：</p>
 * <ul>
 *     <li><b>Tomcat 线程池耗尽</b>：25 路并发即可长期占满约 200 个工作线程，全站不可用；</li>
 *     <li><b>ESI error limit 连坐</b>：6 个无 Accountant 权限的人物（正常玩家常态）轮询本端点，
 *         每次产生 6 次 ESI 403，可打满 ESI 全应用级 error limit（100 errors/60s/IP），
 *         届时<b>所有用户的所有 ESI 功能</b>（资产、蓝图、工业、月矿、流水、总览）同时不可用。</li>
 * </ul>
 *
 * <h3>关键设计</h3>
 * <ul>
 *     <li><b>键含 userId 段实现用户隔离</b>。本仓库有过 ESI token 缓存键越权漏洞（{@code f153ed4}），
 *         缓存键缺少用户维度即等于把他人财务数据交出去。</li>
 *     <li><b>失败结果同样缓存</b>。这是消除上述场景 B 的必要条件——若只缓存成功结果，
 *         403 场景每次请求仍会打满 ESI。代价是用户修好军团权限或重新授权后，
 *         最多需等 TTL 到期才能看到更新。</li>
 *     <li><b>TTL 60 秒不损新鲜度</b>。ESI 对 {@code /characters/{id}/wallet/} 与
 *         {@code /corporations/{id}/wallets/} 自身声明 120s 缓存，60s 内的重复真调
 *         本来也拿不到更新数据，只会白耗 error limit 配额。</li>
 *     <li><b>fail-open</b>。读/写/序列化任一异常均记日志后按未命中处理，
 *         不因缓存故障阻断业务（与 015 {@code EsiResponseCache} 一致）。</li>
 * </ul>
 *
 * <p>序列化说明：全局 {@code RedisTemplate} 使用 {@code Jackson2JsonRedisSerializer<Object>}，
 * 反序列化到 {@code Object} 在无类型头时会把对象还原成 {@code LinkedHashMap}。
 * 故此处统一以 JSON 字符串存取，读取时显式按 {@link UserWalletBalanceVO} 还原
 * （与 015 {@code EsiResponseCache} 的处理一致）。</p>
 *
 * @author Leojian
 * date 2026-08-25
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserWalletBalanceCache {

    /** 键前缀；完整键为 {@code wallet:balance:user:<userId>}。 */
    private static final String KEY_PREFIX = "wallet:balance:user:";

    /** 缓存时长（见类注释「TTL 60 秒不损新鲜度」）。 */
    private static final Duration TTL = Duration.ofSeconds(60);

    private final CacheGateway cacheGateway;
    private final ObjectMapper objectMapper;

    /**
     * 读取缓存的聚合结果。
     *
     * @param userId 当前登录用户ID（键的隔离维度）
     * @return 命中的结果；未命中、缓存故障或反序列化失败均返回 null（fail-open）
     */
    public UserWalletBalanceVO get(Integer userId) {
        String key = key(userId);
        try {
            Object raw = cacheGateway.get(key);
            if (raw == null) {
                return null;
            }
            String json = raw instanceof String s ? s : objectMapper.writeValueAsString(raw);
            return objectMapper.readValue(json, UserWalletBalanceVO.class);
        } catch (Exception e) {
            log.warn("用户钱包余额缓存读取失败,按未命中处理: userId={}", userId, e);
            return null;
        }
    }

    /**
     * 写入聚合结果。<b>含失败状态的结果同样写入</b>（见类注释）。
     *
     * @param userId 当前登录用户ID
     * @param vo     聚合结果；null 不写
     */
    public void put(Integer userId, UserWalletBalanceVO vo) {
        if (vo == null) {
            return;
        }
        try {
            cacheGateway.set(key(userId), objectMapper.writeValueAsString(vo),
                    TTL.toSeconds(), TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("用户钱包余额缓存写入失败,忽略: userId={}", userId, e);
        }
    }

    /**
     * 失效指定用户的缓存。
     *
     * <p>用途：① 集成测试隔离（同一 userId 的用例间不互相污染）；
     * ② 将来若增加 {@code forceRefresh} 参数，可在真调前先失效
     * （注意：那会重新打开被本缓存关闭的 DoS 面，须配合 {@code ForceRefreshLimiter}）。</p>
     *
     * @param userId 用户ID
     */
    public void evict(Integer userId) {
        try {
            cacheGateway.delete(key(userId));
        } catch (Exception e) {
            log.warn("用户钱包余额缓存失效失败,忽略: userId={}", userId, e);
        }
    }

    private String key(Integer userId) {
        return KEY_PREFIX + userId;
    }
}
