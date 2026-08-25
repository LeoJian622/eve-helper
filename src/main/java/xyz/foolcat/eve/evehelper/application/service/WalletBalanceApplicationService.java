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
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;
import xyz.foolcat.eve.evehelper.shared.result.ResultCode;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

        // 人物余额列表
        List<CharacterBalanceVO> characters = accounts.stream()
                .map(this::buildCharacterBalance)
                .toList();

        // 军团余额列表（按 corpId 分组去重）
        Map<Integer, List<EveAccount>> byCorp = accounts.stream()
                .filter(a -> a.getCorpId() != null)
                .collect(Collectors.groupingBy(EveAccount::getCorpId));

        List<CorporationBalanceVO> corporations = byCorp.entrySet().stream()
                .map(e -> buildCorporationBalance(e.getKey(), e.getValue()))
                .toList();

        return new UserWalletBalanceVO(characters, corporations);
    }

    /**
     * 构建人物余额。
     */
    private CharacterBalanceVO buildCharacterBalance(EveAccount account) {
        Double balance = fetchCharacterBalance(account.getCharacterId());
        return new CharacterBalanceVO(
                account.getCharacterId(),
                account.getCharacterName(),
                balance);
    }

    /**
     * 构建军团余额（多角色权限合并）。
     */
    private CorporationBalanceVO buildCorporationBalance(Integer corpId, List<EveAccount> charsInCorp) {
        // 多角色权限合并
        List<WalletDivisionBalance> mergedDivisions = mergeCorporationBalances(corpId, charsInCorp);

        // 总余额 = 各分账之和
        Double total = mergedDivisions.stream()
                .mapToDouble(b -> b.balance() == null ? 0.0 : b.balance())
                .sum();

        // 军团名称取第一个角色的
        String corpName = charsInCorp.get(0).getCorpName();
        return new CorporationBalanceVO(corpId, corpName, total, mergedDivisions);
    }

    /**
     * 合并用户在军团中的所有角色的 division 余额。
     *
     * <p>每个 division 取第一个非空值（去重）。不同角色可能有不同的 division 读取权限，
     * 通过尝试所有角色并合并结果，最大化权限覆盖。</p>
     *
     * <p>018 IMPORTANT-3：入表前校验 division 合法性（1..7）。ESI 若返回 division 为 null
     * 的畸形记录，HashMap 会接受 null 作为 key，随后 {@code Comparator.comparingInt} 对
     * {@code Integer} 拆箱即抛 NPE；而该 NPE 位于单角色 try/catch 之外，会让整个
     * /wallet/balance 返回 500，连人物余额与其他军团的正常数据一并丢失。</p>
     *
     * @param corpId      军团ID
     * @param charsInCorp 用户在该军团的所有角色
     * @return 合并后的分账余额列表（按 division 排序）
     */
    private List<WalletDivisionBalance> mergeCorporationBalances(Integer corpId, List<EveAccount> charsInCorp) {
        Map<Integer, Double> merged = new HashMap<>();
        for (EveAccount account : charsInCorp) {
            try {
                List<WalletDivisionBalance> balances = fetchSingleCharCorpBalances(
                        corpId, account.getCharacterId());
                for (WalletDivisionBalance b : balances) {
                    // 防御畸形 ESI 响应：division 必须为 1..7，否则跳过（见方法注释）
                    if (b.division() == null || b.division() < MIN_DIVISION || b.division() > MAX_DIVISION) {
                        log.warn("军团余额 ESI 返回非法 division，跳过: corpId={}, charId={}, division={}",
                                corpId, account.getCharacterId(), b.division());
                        continue;
                    }
                    // 每个 division 只取第一个有数据的
                    merged.putIfAbsent(b.division(), b.balance() == null ? 0.0 : b.balance());
                }
            } catch (Exception e) {
                log.warn("军团余额查询失败: corpId={}, charId={}", corpId, account.getCharacterId(), e);
                // 继续尝试下一个角色
            }
        }
        return merged.entrySet().stream()
                .map(e -> new WalletDivisionBalance(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingInt(WalletDivisionBalance::division))
                .toList();
    }

    /**
     * 获取人物钱包余额（ESI 直查）。
     */
    private Double fetchCharacterBalance(Integer cid) {
        try {
            EveAccount account = authorizeUtil.authorize(cid);
            String token = esiGateway.getAccessToken(cid, account.getUserId());
            Double balance = esiGateway.queryCharacterWalletBalance(cid, token);
            return balance == null ? 0.0 : balance;
        } catch (Exception e) {
            log.warn("人物余额查询失败: cid={}", cid, e);
            return 0.0;
        }
    }

    /**
     * 获取单个角色的军团余额（ESI 直查）。
     */
    private List<WalletDivisionBalance> fetchSingleCharCorpBalances(Integer corpId, Integer charId) {
        try {
            EveAccount account = authorizeUtil.authorize(charId);
            String token = esiGateway.getAccessToken(charId, account.getUserId());
            List<WalletDivisionBalance> balances = esiGateway.queryCorporationWalletBalances(corpId, token);
            return balances == null ? Collections.emptyList() : balances;
        } catch (Exception e) {
            log.warn("军团余额查询失败: corpId={}, charId={}", corpId, charId, e);
            return Collections.emptyList();
        }
    }
}
