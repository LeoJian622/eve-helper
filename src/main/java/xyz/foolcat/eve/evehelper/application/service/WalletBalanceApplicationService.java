package xyz.foolcat.eve.evehelper.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterBalanceVO;
import xyz.foolcat.eve.evehelper.domain.model.vo.CorporationBalanceVO;
import xyz.foolcat.eve.evehelper.domain.model.vo.UserWalletBalanceVO;
import xyz.foolcat.eve.evehelper.domain.model.vo.WalletDivisionBalance;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.domain.repository.system.EveAccountRepository;
import xyz.foolcat.eve.evehelper.domain.util.AuthorizeUtil;
import xyz.foolcat.eve.evehelper.domain.util.UserUtil;
import xyz.foolcat.eve.evehelper.shared.kernel.enums.BalanceQueryStatus;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;
import xyz.foolcat.eve.evehelper.shared.result.IResultCode;
import xyz.foolcat.eve.evehelper.shared.result.ResultCode;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户钱包余额聚合应用服务（018:按用户聚合余额查询）。
 *
 * <p>提供按用户维度聚合的钱包余额查询用例：返回当前用户绑定的所有人物余额
 * 和关联军团余额（多角色权限合并）。</p>
 *
 * <p>关键设计：</p>
 * <ul>
 *     <li>人物余额：ESI 直查 /characters/{id}/wallet/</li>
 *     <li>军团余额：获取用户在该军团的所有角色，对每个角色尝试获取 division 余额，
 *         合并结果（union by division），每个 division 取第一个有数据的值</li>
 *     <li>军团按 corpId 去重，同一军团只出现一次</li>
 * </ul>
 *
 * @author Leojian
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletBalanceApplicationService {

    /** EVE 军团分账合法编号下界。 */
    private static final int MIN_DIVISION = 1;
    /** EVE 军团分账合法编号上界。 */
    private static final int MAX_DIVISION = 7;

    /**
     * 表示「需重新授权」的错误码集合（018 IMPORTANT-2 的状态归类判据）。
     *
     * <p>{@code ESI00400} 来自 {@code infrastructure/external/esi/ResultCode.ESI_AUTHORIZATION_FAILURE}
     * （refreshToken 失效或过期）。此处以码字符串比对而非直接引用该枚举，
     * 因为 application 层不得 import infrastructure 类型（DDD 分层规则）。
     * <b>若该码值变更，须同步此处。</b></p>
     *
     * <p>不在本集合内的码（如 {@code ESI00500} ESI 服务器失败）归为 UPSTREAM_ERROR：
     * 与授权无关，重试可能恢复。</p>
     */
    private static final Set<String> REAUTH_REQUIRED_CODES = Set.of(
            ResultCode.ACCESS_UNAUTHORIZED.getCode(),     // AUT00301 未认证/主体不可识别
            ResultCode.USER_ACCOUNT_NOT_EXIST.getCode(),  // AUT00208 归属不符或账号已解绑
            "ESI00400"                                    // ESI 未授权或授权过期
    );

    private final EveAccountRepository eveAccountRepository;
    private final AuthorizeUtil authorizeUtil;
    private final EsiGateway esiGateway;

    /**
     * 获取当前登录用户的钱包余额聚合。
     *
     * <p>018 IMPORTANT-4：身份由服务自行从安全上下文取得，不接受调用方传入的 userId。</p>
     *
     * <p>原签名 {@code getUserWalletBalance(Integer userId)} 把信任边界外移到调用点：
     * 服务只校验 {@code userId > 0} 便直接 {@code getAccountList(userId)}，没有任何断言
     * 保证该 userId 就是当前登录主体。一旦将来有人加一个"管理员查看指定用户余额"的入口
     * 并传入请求参数里的 targetUserId，就会返回他人的全部人物 ID／人物名／军团 ID／军团名，
     * 而下游 {@code authorize(cid)} 抛出的归属失败又会被 fail-open 的 catch 吞成余额 0，
     * 最终 HTTP 200 静默泄漏——正是本仓库 IDOR 历史缺陷的同一形态。
     * 改为服务内自取身份后，该误用在结构上不可能发生。</p>
     *
     * @return 用户钱包余额聚合读模型
     */
    public UserWalletBalanceVO getUserWalletBalance() {
        // 身份自取：未认证/匿名/主体不可识别时 UserUtil 返回 -1，统一走下方拒绝分支
        Integer userId = UserUtil.getUserId();
        if (userId == null || userId <= 0) {
            log.warn("用户钱包余额查询:未认证或主体无法识别, userId={}", userId);
            throw new EveHelperException(ResultCode.ACCESS_UNAUTHORIZED);
        }

        // 获取用户所有绑定人物
        List<EveAccount> accounts = eveAccountRepository.getAccountList(userId);
        if (accounts == null || accounts.isEmpty()) {
            return new UserWalletBalanceVO(Collections.emptyList(), Collections.emptyList());
        }

        // 018 IMPORTANT-5：请求内 authorize 结果缓存。
        // 同一人物原先被 authorize 两次（人物余额路径 + 军团余额路径），每次都查一次库；
        // 此处按 characterId 缓存后，每个人物在单次请求内只校验一次，省去 N 次单行查询。
        // 注意：这里缓存的是"已做过归属校验"的结果，而非跳过校验——归属校验对每个人物
        // 仍实际执行过一次。不采用"直接复用 getAccountList 结果替代 authorize"的做法，
        // 因为那会把双重归属校验降为单重（getAccountList 的 where user_id 是唯一防线），
        // 对财务数据不值得；且两个查询的 SELECT 字段集并不一致（后者少 type/qq 等列）。
        Map<Integer, AuthResult> authCache = new HashMap<>();

        // 人物余额列表
        List<CharacterBalanceVO> characters = accounts.stream()
                .map(a -> buildCharacterBalance(a, authCache))
                .toList();

        // 军团余额列表（按 corpId 分组去重）
        Map<Integer, List<EveAccount>> byCorp = accounts.stream()
                .filter(a -> a.getCorpId() != null)
                .collect(Collectors.groupingBy(EveAccount::getCorpId));

        List<CorporationBalanceVO> corporations = byCorp.entrySet().stream()
                .map(e -> buildCorporationBalance(e.getKey(), e.getValue(), authCache))
                .toList();

        return new UserWalletBalanceVO(characters, corporations);
    }

    /**
     * 构建人物余额。
     *
     * <p>018 IMPORTANT-2：查询失败时 balance 为 null 并带上具体状态，
     * 不再降级为 0.0（那会与真实的零余额同形）。</p>
     */
    private CharacterBalanceVO buildCharacterBalance(EveAccount account, Map<Integer, AuthResult> authCache) {
        Integer cid = account.getCharacterId();
        AuthResult auth = authorizeOnce(cid, authCache);
        if (auth.status() != BalanceQueryStatus.OK) {
            return new CharacterBalanceVO(cid, account.getCharacterName(), null, auth.status());
        }
        try {
            String token = esiGateway.getAccessToken(cid, auth.account().getUserId());
            Double balance = esiGateway.queryCharacterWalletBalance(cid, token);
            // ESI 返回 null 视为 0：此时调用成功，零余额是可信结果
            return new CharacterBalanceVO(cid, account.getCharacterName(),
                    balance == null ? 0.0 : balance, BalanceQueryStatus.OK);
        } catch (EveHelperException e) {
            BalanceQueryStatus status = classify(e);
            logFailure("人物余额查询失败", status, e, "cid=" + cid);
            return new CharacterBalanceVO(cid, account.getCharacterName(), null, status);
        } catch (ParseException e) {
            log.warn("人物余额查询失败(JWT 解析): cid={}", cid, e);
            return new CharacterBalanceVO(cid, account.getCharacterName(), null,
                    BalanceQueryStatus.UPSTREAM_ERROR);
        } catch (RuntimeException e) {
            log.warn("人物余额查询失败(上游故障): cid={}", cid, e);
            return new CharacterBalanceVO(cid, account.getCharacterName(), null,
                    BalanceQueryStatus.UPSTREAM_ERROR);
        }
    }

    /**
     * 单次请求内对同一人物只做一次 authorize（含失败态缓存，避免重复重试）。
     *
     * @param cid   人物ID
     * @param cache 请求内缓存
     * @return 授权结果（status 非 OK 时 account 为 null）
     */
    private AuthResult authorizeOnce(Integer cid, Map<Integer, AuthResult> cache) {
        return cache.computeIfAbsent(cid, id -> {
            try {
                return new AuthResult(authorizeUtil.authorize(id), BalanceQueryStatus.OK);
            } catch (EveHelperException e) {
                BalanceQueryStatus status = classify(e);
                logFailure("余额查询归属校验失败", status, e, "cid=" + id);
                return new AuthResult(null, status);
            } catch (RuntimeException e) {
                log.warn("余额查询归属校验异常(上游故障): cid={}", id, e);
                return new AuthResult(null, BalanceQueryStatus.UPSTREAM_ERROR);
            }
        });
    }

    /**
     * 把异常归类为「需重新授权」或「上游故障」。
     *
     * <p>依据错误码而非异常类型：ESI 侧异常 {@code EsiException} 与其错误码枚举都定义在
     * infrastructure 层，application 层不得 import（DDD 分层规则），故以 {@link IResultCode}
     * 暴露的码字符串比对。</p>
     */
    private BalanceQueryStatus classify(EveHelperException e) {
        IResultCode rc = e.resultCode;
        String code = rc == null ? null : rc.getCode();
        return code != null && REAUTH_REQUIRED_CODES.contains(code)
                ? BalanceQueryStatus.UNAUTHORIZED
                : BalanceQueryStatus.UPSTREAM_ERROR;
    }

    /**
     * 分级记录失败：授权类问题是用户可自行修复的常态，不打堆栈以免刷屏；
     * 上游故障需要堆栈定位。
     */
    private void logFailure(String what, BalanceQueryStatus status, EveHelperException e, String ctx) {
        if (status == BalanceQueryStatus.UNAUTHORIZED) {
            log.warn("{}(需重新授权): {}, code={}", what, ctx,
                    e.resultCode == null ? "-" : e.resultCode.getCode());
        } else {
            log.warn("{}(上游故障): {}", what, ctx, e);
        }
    }

    /**
     * 构建军团余额（多角色权限合并）。
     *
     * <p>018 IMPORTANT-2：合并过程中若有人物查询失败，标记 partial 并列出失败人物ID，
     * 使调用方能区分「军团账上就这么多」与「有分账没查到」。</p>
     */
    private CorporationBalanceVO buildCorporationBalance(Integer corpId, List<EveAccount> charsInCorp,
                                                        Map<Integer, AuthResult> authCache) {
        // 多角色权限合并
        MergeOutcome outcome = mergeCorporationBalances(corpId, charsInCorp, authCache);
        List<WalletDivisionBalance> mergedDivisions = outcome.divisions();

        // 总余额 = 已取到的各分账之和（partial 为 true 时不代表军团总资产）
        Double total = mergedDivisions.stream()
                .mapToDouble(b -> b.balance() == null ? 0.0 : b.balance())
                .sum();

        // 军团名称取第一个角色的
        String corpName = charsInCorp.get(0).getCorpName();
        List<Integer> failed = outcome.failedCharacterIds();
        return new CorporationBalanceVO(corpId, corpName, total, mergedDivisions,
                !failed.isEmpty(), failed);
    }

    /**
     * 合并用户在军团中的所有角色的 division 余额。
     *
     * <p>每个 division 取第一个非空值（去重）。不同角色可能有不同的 division 读取权限，
     * 通过尝试所有角色并合并结果，最大化权限覆盖。行序由
     * {@code queryAccountList} 的 {@code order by character_id} 固定（MINOR-1），
     * 故"首个胜出"等价于"characterId 最小者胜出"，结果可复现。</p>
     *
     * <p>018 IMPORTANT-3：入表前校验 division 合法性（1..7）。ESI 若返回 division 为 null
     * 的畸形记录，HashMap 会接受 null 作为 key，随后 {@code Comparator.comparingInt} 对
     * {@code Integer} 拆箱即抛 NPE；而该 NPE 位于单角色 try/catch 之外，会让整个
     * /wallet/balance 返回 500，连人物余额与其他军团的正常数据一并丢失。</p>
     *
     * <p>018 IMPORTANT-5：1..7 全部取到后提前终止。ESI 单次调用即返回该 token 可见的全部
     * 分账，若前一个人物已覆盖 1..7，后续人物的调用对结果零贡献，纯属浪费配额。</p>
     *
     * @param corpId      军团ID
     * @param charsInCorp 用户在该军团的所有角色
     * @param authCache   请求内 authorize 缓存
     * @return 合并结果（分账列表 + 失败人物ID）
     */
    private MergeOutcome mergeCorporationBalances(Integer corpId, List<EveAccount> charsInCorp,
                                                  Map<Integer, AuthResult> authCache) {
        Map<Integer, Double> merged = new HashMap<>();
        List<Integer> failedCharacterIds = new ArrayList<>();
        for (EveAccount account : charsInCorp) {
            // 已覆盖全部分账，后续人物零贡献，提前终止
            if (merged.size() >= MAX_DIVISION) {
                break;
            }
            Integer charId = account.getCharacterId();
            List<WalletDivisionBalance> balances = fetchSingleCharCorpBalances(corpId, charId, authCache);
            if (balances == null) {
                // null 表示该人物查询失败（区别于"成功但无可见分账"的空列表）
                failedCharacterIds.add(charId);
                continue;
            }
            for (WalletDivisionBalance b : balances) {
                // 防御畸形 ESI 响应：division 必须为 1..7，否则跳过（见方法注释）
                if (b.division() == null || b.division() < MIN_DIVISION || b.division() > MAX_DIVISION) {
                    log.warn("军团余额 ESI 返回非法 division，跳过: corpId={}, charId={}, division={}",
                            corpId, charId, b.division());
                    continue;
                }
                // 每个 division 只取第一个有数据的
                merged.putIfAbsent(b.division(), b.balance() == null ? 0.0 : b.balance());
            }
        }
        List<WalletDivisionBalance> divisions = merged.entrySet().stream()
                .map(e -> new WalletDivisionBalance(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingInt(WalletDivisionBalance::division))
                .toList();
        return new MergeOutcome(divisions, failedCharacterIds);
    }

    /**
     * 获取单个角色的军团余额（ESI 直查）。
     *
     * @return 分账列表；<b>查询失败返回 null</b>，成功但无可见分账返回空列表。
     *         二者必须可区分，否则无法判定 partial（IMPORTANT-2）。
     */
    private List<WalletDivisionBalance> fetchSingleCharCorpBalances(Integer corpId, Integer charId,
                                                                   Map<Integer, AuthResult> authCache) {
        AuthResult auth = authorizeOnce(charId, authCache);
        if (auth.status() != BalanceQueryStatus.OK) {
            return null;
        }
        try {
            String token = esiGateway.getAccessToken(charId, auth.account().getUserId());
            List<WalletDivisionBalance> balances = esiGateway.queryCorporationWalletBalances(corpId, token);
            return balances == null ? Collections.emptyList() : balances;
        } catch (EveHelperException e) {
            logFailure("军团余额查询失败", classify(e), e, "corpId=" + corpId + ", charId=" + charId);
            return null;
        } catch (ParseException e) {
            log.warn("军团余额查询失败(JWT 解析): corpId={}, charId={}", corpId, charId, e);
            return null;
        } catch (RuntimeException e) {
            log.warn("军团余额查询失败(上游故障): corpId={}, charId={}", corpId, charId, e);
            return null;
        }
    }

    /** authorize 结果（status 非 OK 时 account 为 null）。 */
    private record AuthResult(EveAccount account, BalanceQueryStatus status) {
    }

    /** 军团分账合并结果。 */
    private record MergeOutcome(List<WalletDivisionBalance> divisions, List<Integer> failedCharacterIds) {
    }

}
