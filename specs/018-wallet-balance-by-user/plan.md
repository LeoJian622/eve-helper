# Implementation Plan: 018-wallet-balance-by-user

**Feature**: 按用户聚合钱包余额
**Branch**: `018-wallet-balance-by-user`
**Spec**: `specs/018-wallet-balance-by-user/spec.md`
**Created**: 2026-08-24

## 实现策略

### 架构决策

| 决策点 | 选择 | 理由 |
|--------|------|------|
| 新 Service vs 复用 WalletOverviewApplicationService | **新增 WalletBalanceApplicationService** | 职责分离：余额查询 vs 总览聚合；避免修改现有稳定代码 |
| VO 位置 | `domain/model/vo/` | 跨层共享的查询结果载体 |
| Controller 位置 | `interfaces/web/controller/` | REST 端点 |
| 军团余额合并策略 | 多角色尝试 + union by division | 最大化权限覆盖 |

### 新增文件清单

| 文件 | 层 | 职责 |
|------|-----|------|
| `UserWalletBalanceVO.java` | domain/vo | 顶层返回结构 |
| `CharacterBalanceVO.java` | domain/vo | 人物余额子结构 |
| `CorporationBalanceVO.java` | domain/vo | 军团余额子结构（含 divisions） |
| `WalletBalanceApplicationService.java` | application/service | 业务逻辑：聚合人物/军团余额 |
| `WalletBalanceController.java` | interfaces/web/controller | REST 端点 |

### 依赖现有代码

| 现有组件 | 用途 |
|----------|------|
| `EveAccountRepository.getAccountList(userId)` | 获取用户所有人物 |
| `AuthorizeUtil.authorize(cid)` | 获取 EveAccount（含 refreshToken） |
| `EsiGateway.getAccessToken(cid, userId)` | 换取 accessToken |
| `EsiGateway.queryCharacterWalletBalance(cid, token)` | 人物余额 ESI 直查 |
| `EsiGateway.queryCorporationWalletBalances(corpId, token)` | 军团余额 ESI 直查 |

### 实现步骤

#### Step 1: 创建 VO（领域读模型）

```java
// domain/model/vo/CharacterBalanceVO.java
public record CharacterBalanceVO(
    Integer characterId,
    String characterName,
    Double balance
) {}

// domain/model/vo/CorporationBalanceVO.java
public record CorporationBalanceVO(
    Integer corpId,
    String corpName,
    Double totalBalance,
    List<WalletDivisionBalance> divisions
) {}

// domain/model/vo/UserWalletBalanceVO.java
public record UserWalletBalanceVO(
    List<CharacterBalanceVO> characters,
    List<CorporationBalanceVO> corporations
) {}
```

#### Step 2: 创建 Application Service

```java
// application/service/WalletBalanceApplicationService.java
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletBalanceApplicationService {

    private final EveAccountRepository eveAccountRepository;
    private final AuthorizeUtil authorizeUtil;
    private final EsiGateway esiGateway;

    public UserWalletBalanceVO getUserWalletBalance() {
        Integer userId = UserUtil.getUserId();
        if (userId == null || userId <= 0) {
            throw new EveHelperException(ResultCode.ACCESS_UNAUTHORIZED);
        }

        List<EveAccount> accounts = eveAccountRepository.getAccountList(userId);
        if (accounts == null || accounts.isEmpty()) {
            return new UserWalletBalanceVO(Collections.emptyList(), Collections.emptyList());
        }

        // 人物余额
        List<CharacterBalanceVO> characters = accounts.stream()
                .map(this::buildCharacterBalance)
                .toList();

        // 军团余额（按 corpId 分组，去重）
        Map<Integer, List<EveAccount>> byCorp = accounts.stream()
                .filter(a -> a.getCorpId() != null)
                .collect(Collectors.groupingBy(EveAccount::getCorpId));

        List<CorporationBalanceVO> corporations = byCorp.entrySet().stream()
                .map(e -> buildCorporationBalance(e.getKey(), e.getValue()))
                .toList();

        return new UserWalletBalanceVO(characters, corporations);
    }

    private CharacterBalanceVO buildCharacterBalance(EveAccount account) {
        Double balance = fetchCharacterBalance(account.getCharacterId());
        return new CharacterBalanceVO(
                account.getCharacterId(),
                account.getCharacterName(),
                balance
        );
    }

    private CorporationBalanceVO buildCorporationBalance(Integer corpId, List<EveAccount> charsInCorp) {
        // 多角色权限合并
        List<WalletDivisionBalance> mergedDivisions = mergeCorporationBalances(corpId, charsInCorp);

        Double total = mergedDivisions.stream()
                .mapToDouble(b -> b.balance() == null ? 0.0 : b.balance())
                .sum();

        String corpName = charsInCorp.get(0).getCorpName();
        return new CorporationBalanceVO(corpId, corpName, total, mergedDivisions);
    }

    private List<WalletDivisionBalance> mergeCorporationBalances(Integer corpId, List<EveAccount> charsInCorp) {
        Map<Integer, Double> merged = new HashMap<>();
        for (EveAccount account : charsInCorp) {
            try {
                List<WalletDivisionBalance> balances = fetchSingleCharCorpBalances(
                        corpId, account.getCharacterId());
                for (WalletDivisionBalance b : balances) {
                    merged.putIfAbsent(b.division(), b.balance() == null ? 0.0 : b.balance());
                }
            } catch (Exception e) {
                log.warn("军团余额查询失败: corpId={}, charId={}", corpId, account.getCharacterId(), e);
            }
        }
        return merged.entrySet().stream()
                .map(e -> new WalletDivisionBalance(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingInt(WalletDivisionBalance::division))
                .toList();
    }

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

    private List<WalletDivisionBalance> fetchSingleCharCorpBalances(Integer corpId, Integer charId) {
        try {
            EveAccount account = authorizeUtil.authorize(charId);
            String token = esiGateway.getAccessToken(charId, account.getUserId());
            return esiGateway.queryCorporationWalletBalances(corpId, token);
        } catch (Exception e) {
            log.warn("军团余额查询失败: corpId={}, charId={}", corpId, charId, e);
            return Collections.emptyList();
        }
    }
}
```

#### Step 3: 创建 Controller

```java
// interfaces/web/controller/WalletBalanceController.java
@Tag(name = "用户钱包余额聚合")
@RestController
@RequestMapping("/wallet/balance")
@RequiredArgsConstructor
public class WalletBalanceController {

    private final WalletBalanceApplicationService walletBalanceApplicationService;

    @Operation(summary = "获取当前用户所有人物和军团的余额")
    @GetMapping
    public Result<UserWalletBalanceVO> getUserWalletBalance() {
        return Result.success(walletBalanceApplicationService.getUserWalletBalance());
    }
}
```

#### Step 4: RBAC 权限配置

在 `012_rbac_permissions.sql` 中追加：
```sql
-- 018: 用户钱包余额聚合端点
INSERT INTO sys_permission (url_perm, description) VALUES ('GET:/wallet/balance', '用户钱包余额聚合查询');
-- 映射到普通用户角色
INSERT INTO sys_role_permission (role_id, permission_id) 
SELECT r.id, p.id FROM sys_role r, sys_permission p 
WHERE r.role_code = 'USER' AND p.url_perm = 'GET:/wallet/balance';
```

### 测试策略

| 测试类型 | 覆盖点 |
|----------|--------|
| 单元测试 | `WalletBalanceApplicationServiceTest` |
| - 空账户列表 | 返回空结构 |
| - 单人物无军团 | characters 1条，corporations 空 |
| - 多人物同军团 | corporations 按 corpId 去重 |
| - 多角色权限合并 | 验证 division 合并逻辑 |
| 集成测试 | `WalletBalanceControllerIT` |
| - 认证校验 | 无 JWT 返回 401 |
| - 正常查询 | 返回正确结构 |

### 风险评估

| 风险 | 等级 | 缓解措施 |
|------|------|----------|
| ESI 调用过多（多角色尝试） | 中 | 每个角色只调一次；失败即跳过 |
| 性能（大量人物/军团） | 低 | 用户绑定的人物通常有限（<10） |
| 权限合并遗漏 | 低 | 失败不影响其他角色继续尝试 |

### 部署清单

- [ ] 执行 RBAC SQL（新增权限项）
- [ ] 验证 `/wallet/balance` 端点可访问
- [ ] 验证返回结构符合预期
