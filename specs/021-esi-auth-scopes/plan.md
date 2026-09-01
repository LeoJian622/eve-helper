# Implementation Plan: 021-esi-auth-scopes

**Feature**: 用户角色列表接口返回 ESI 已授权权限（scopes）
**Branch**: `021-esi-auth-scopes`
**Spec**: `specs/021-esi-auth-scopes/spec.md`（G1 已批准,2026-09-01）
**Created**: 2026-09-01 | **Status**: Approved (G2, 2026-09-01)

## Summary

在 `GET /user/{userId}`（`UserAccountDTO`）响应中新增 `scopes` 字段：AUTHORIZED 角色返回其当前有效 accessToken 中声明的原始 scope 名列表，其余三态（EXPIRED/NOT_AUTHORIZED/UNKNOWN）返回 null。scopes 从缓存层已持有的 accessToken（JWT `scp` claim）运行时解析，**不改表、不落库、不新增 ESI 网络调用、不触碰 refreshToken 刷新锁逻辑**。技术路线：`EsiGateway` 新增 `getAuthorizationDetail(EveAccount)` 端口方法（返回 `EsiAuthorizationDetail` 读模型），内部组合现有 `getAuthorizationStatus` + 缓存 token 解析；应用服务切换到新方法并在 DTO 上透出。

## Technical Context

**Language/Version**: Java 17（栈冻结,宪法 IV）
**Primary Dependencies**: Spring Boot 3.5.14、Nimbus JOSE JWT 10.0.2（JWT 解析,已在 `EsiApiService` 用于 `sub`/`name`）、MapStruct 1.6.3、Mockito/JUnit 5
**Storage**: 无任何持久化变更（scopes 即时派生,不落库）
**Testing**: JUnit 5 + Mockito（纯单元）+ MockMvc IT（`@SpringBootTest` + `@MockitoBean`,需 DB/Redis 可达）
**Target Platform**: 服务端 REST（Spring MVC）
**Performance Goals**: 缓存命中路径 p95 <200ms 与现状持平（scopes 解析为本地 CPU 操作,不进入并行等待预算）
**Constraints**: 不新增依赖（宪法 IV）;不修改 `determineAuthStatus`/刷新锁安全敏感代码;响应不得含任何 token 材料（FR-009）

## Constitution Check

*GATE: Phase 0 前后各核对一次。结论：全部通过,无豁免项。*

| 条款 | 结论 | 依据 |
|------|------|------|
| I. REST First + Test-First + 200ms | ✅ | 响应仍走 `Result<T>`;每任务 RED→GREEN;scopes 解析 O(毫秒级本地),不影响 p95 |
| II. 安全 | ✅ | 不新增凭据存储;scopes 仅为权限名;沿用既有 `requireSelfOrRoot` IDOR 防护;token 值不进响应/日志 |
| III. 代码质量 + 80% 覆盖 | ✅ | 新增代码全覆盖计划见 T1-T3;无投机抽象 |
| IV. 技术栈冻结 | ✅ | 仅复用 Nimbus JOSE JWT（栈内,已在用）,零新依赖 |
| V. Spec-First + 分支 | ✅ | 特性轨;spec G1 已批准;分支 `021-esi-auth-scopes` 已检出 |
| V.1 禁止主干提交 | ✅ | 实现全部在本分支;P7 收尾才合并 |

## 架构决策

| 决策点 | 选择 | 理由 |
|--------|------|------|
| scopes 获取通路 | **组合**：新端口方法 `getAuthorizationDetail` 内部先调 `getAuthorizationStatus`（复用全部状态缓存/刷新锁/回写逻辑）,AUTHORIZED 时再从缓存层读 accessToken 解析 `scp` | 完全不触碰 `determineAuthStatus`/`persistRefreshedToken` 等安全敏感代码（刷新锁曾经历多轮评审修复,回归代价极高）;状态与 scopes 天然同源同一致性 |
| vs 改 `determineAuthStatus` 返回结构 | ❌ 否决 | 需改动已评审的状态判定核心,测试面大,收益仅省一次缓存读（毫秒级） |
| vs 新增独立 `getAuthorizationScopes` 方法 | ❌ 否决 | 应用层需调两次,状态与 scopes 可能来自两次不同判定,语义割裂 |
| scp 解析 | Nimbus `SignedJWT.parse` + `getClaim("scp")` 通用 `Object` 判型（`List`→逐项转串;`String`→空白分隔切分） | 两种历史格式一次覆盖（EVE SSO 新版数组/旧版空格分隔）;不验证签名——读取的是自家缓存中 ESI 签发的 token 声明,与现有 `sub`/`name` 解析同口径 |
| 结果载体 | `domain/model/vo/EsiAuthorizationDetail`（record: status + scopes） | 跨层读模型放 `domain/model/vo`（架构约定）;端口签名只用领域类型 |
| null 语义 | `scopes=null`=无法确定（非 AUTHORIZED/无 token/解析失败）;`scopes=[]`=token 有效但 scp 为空 | 区分"不可得"与"确认为空",spec Edge Cases 已定义 |
| 应用服务防御 | `resolveDetail` 对 future 结果 null（Mockito 未 stub 等情形）与异常/超时统一归为 `UNKNOWN + scopes=null` | IT 中 `@MockitoBean EsiGateway` 未 stub 新方法返回 null,不做防御会 NPE |

## 新增/修改文件清单

| 文件 | 层 | 变更 |
|------|-----|------|
| `domain/model/vo/EsiAuthorizationDetail.java` | domain/vo | **新增** record(status, scopes) |
| `domain/port/esi/EsiGateway.java` | domain/port | **新增** `getAuthorizationDetail(EveAccount)` 方法声明 |
| `infrastructure/gateway/esi/EsiApiService.java` | infrastructure | **新增** 实现 + `parseScopesFromCachedToken`/`toScopeList` 私有方法 |
| `application/dto/UserAccountDTO.java` | application/dto | **新增** `List<String> scopes` 字段 |
| `application/service/UserApplicationService.java` | application | **修改** 并行判定改调新方法;`resolveStatus`→`resolveDetail`（null-safe） |
| `application/assembler/system/EveAccountAssembler.java` | application/assembler | 不改（scopes 非 EveAccount 字段,MapStruct 自然映射为 null,服务层后置填充） |
| `test/.../domain/service/esi/EsiApiServiceTest.java` | test | **扩展** scopes 解析与 detail 组合用例（纯 Mockito） |
| `test/.../application/service/UserApplicationServiceUnitTest.java` + `UserApplicationServiceTest.java` | test | **扩展** detail 传播/异常降级用例 |
| `test/.../interfaces/web/controller/UserControllerAuthStatusIT.java` | test | **扩展** T014 断言补 scopes + 新增降级用例 |

## 实现步骤（TDD,每任务先 RED 亲眼看到失败再 GREEN）

### T1: 领域读模型 + 端口 + EsiApiService 实现（RED→GREEN→REFACTOR）

1. 新增 VO 与端口方法声明（含 javadoc,注释须写明 null/空列表语义）：

```java
// domain/model/vo/EsiAuthorizationDetail.java
public record EsiAuthorizationDetail(EsiAuthStatus status, List<String> scopes) {}

// domain/port/esi/EsiGateway.java 新增:
/**
 * 判定绑定角色的 ESI 授权状态,并在 AUTHORIZED 时解析已授权权限(scopes)。
 * <p>
 * scopes 取自当前有效 accessToken(缓存或本次判定中刷新所得)的已授权权限声明;
 * 非 AUTHORIZED、无可用 accessToken 或解析失败时 scopes 为 null
 * (空列表表示 token 有效但未声明任何受限权限)。
 * 不得因本方法产生额外的上游 ESI 调用或 refreshToken 刷新。
 *
 * @param account 绑定角色(需含 userId、refreshToken 与 characterId)
 * @return 授权状态 + 已授权权限(可 null)
 */
EsiAuthorizationDetail getAuthorizationDetail(EveAccount account);
```

2. **RED**：在 `EsiApiServiceTest`（纯 Mockito,沿用现有构造方式）先写失败用例——测试内用 Nimbus `MACSigner` 自签 HS256 JWT 构造缓存 token：

```java
// 测试辅助:生成带 scp 的 JWT
private String jwtWithScp(Object scp) throws Exception {
    JWTClaimsSet.Builder b = new JWTClaimsSet.Builder()
            .subject("CHARACTER:EVE:" + CID).claim("name", "TestChar");
    if (scp != null) { b.claim("scp", scp); }
    SignedJWT jwt = new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.HS256).build(), b.build());
    jwt.sign(new MACSigner("0123456789abcdef0123456789abcdef"));
    return jwt.serialize();
}
```

用例矩阵（8 个）：
| # | Given（stub） | When | Then |
|---|--------------|------|------|
| 1 | 状态判定 AUTHORIZED（沿用现有 stub 模式）,token 缓存=`Bearer `+scp 数组 JWT | getAuthorizationDetail | status=AUTHORIZED,scopes=[数组内容] |
| 2 | 同上,scp=空格分隔字符串 `"esi-a.read_assets.v1 esi-wallet.read_character_wallet.v1"` | getAuthorizationDetail | scopes=两元素列表（与数组格式等价,SC-005） |
| 3 | token 缓存=scp 为空数组 `[]` 的 JWT | getAuthorizationDetail | scopes=[]（非 null） |
| 4 | token 缓存=**无 scp claim** 的 JWT | getAuthorizationDetail | scopes=null |
| 5 | token 缓存=非 JWT 垃圾串 | getAuthorizationDetail | scopes=null,不抛错 |
| 6 | AUTHORIZED 但 token 缓存未命中(get→null) | getAuthorizationDetail | scopes=null,status 仍 AUTHORIZED |
| 7 | 状态判定 EXPIRED（4xx stub,复用现有模式） | getAuthorizationDetail | scopes=null（且**不读** token 缓存） |
| 8 | refreshToken 为空 | getAuthorizationDetail | NOT_AUTHORIZED + scopes=null |

3. **GREEN**：`EsiApiService` 最小实现：

```java
@Override
public EsiAuthorizationDetail getAuthorizationDetail(EveAccount account) {
    EsiAuthStatus status = getAuthorizationStatus(account);
    if (status != EsiAuthStatus.AUTHORIZED) {
        return new EsiAuthorizationDetail(status, null);
    }
    return new EsiAuthorizationDetail(status, parseScopesFromCachedToken(account));
}

private List<String> parseScopesFromCachedToken(EveAccount account) {
    try {
        if (account == null || account.getUserId() == null || account.getCharacterId() == null) {
            return null;
        }
        String cached = (String) cacheGateway.get(accessTokenKey(account.getUserId(), account.getCharacterId()));
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
```

要点：`parse` 用例 5 覆盖异常兜底;**不**记录 token 内容到日志;复用私有 `accessTokenKey`（键构造不外泄,与现有约定一致）。

### T2: DTO 字段 + 应用服务切换（RED→GREEN→REFACTOR）

1. **RED**：`UserApplicationServiceUnitTest`/`UserApplicationServiceTest` 新增用例（mock 新端口方法 `getAuthorizationDetail`）：
   - AUTHORIZED detail（scopes=[…]）→ DTO `authStatus=AUTHORIZED` 且 `scopes=[…]` 逐角色对应
   - EXPIRED/NOT_AUTHORIZED/UNKNOWN detail（scopes=null）→ DTO 同步 null
   - future 抛异常 → UNKNOWN + null（既有语义保留）
   - **未 stub（mock 返回 null）→ UNKNOWN + null,不 NPE**
   - 多角色混合 → 逐角色互不串扰
2. **GREEN**：

```java
// UserAccountDTO 新增字段
/**
 * ESI 已授权权限(原始 scope 名)。null=无法确定(非 AUTHORIZED/无可用 token/解析失败);
 * 空列表=token 有效但未声明受限权限。
 */
@Schema(description = "ESI 已授权权限列表(原始 scope 名);null 表示无法确定")
private List<String> scopes;

// UserApplicationService.queryAccountListWithAuthStatus 内部改为:
List<CompletableFuture<EsiAuthorizationDetail>> futures = new ArrayList<>(accounts.size());
for (EveAccount account : accounts) {
    futures.add(CompletableFuture.supplyAsync(
            () -> esiApiService.getAuthorizationDetail(account), esiAuthStatusExecutor));
}
for (int i = 0; i < dtos.size(); i++) {
    EsiAuthorizationDetail detail = resolveDetail(futures.get(i));
    dtos.get(i).setAuthStatus(detail.status());
    dtos.get(i).setScopes(detail.scopes());
}

// resolveStatus 泛化为 resolveDetail(超时/异常/null 统一 UNKNOWN + scopes null):
private static final EsiAuthorizationDetail UNKNOWN_DETAIL =
        new EsiAuthorizationDetail(EsiAuthStatus.UNKNOWN, null);

private EsiAuthorizationDetail resolveDetail(CompletableFuture<EsiAuthorizationDetail> future) {
    try {
        EsiAuthorizationDetail result = future.get(STATUS_FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        return result != null ? result : UNKNOWN_DETAIL;
    } catch (TimeoutException e) {
        future.cancel(true);
        return UNKNOWN_DETAIL;
    } catch (ExecutionException e) {
        return UNKNOWN_DETAIL;
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return UNKNOWN_DETAIL;
    }
}
```

3. MapStruct 无需改动（`scopes` 非 `EveAccount` 字段,由服务层后置填充,与 `authStatus` 同模式）。
4. 同步更新既有用例中 `getAuthorizationStatus` 的 stub → `getAuthorizationDetail`。

### T3: IT 扩展（`UserControllerAuthStatusIT`）

1. T014 用例的 stub 换为 `getAuthorizationDetail`,AUTHORIZED 角色补 `scopes` 断言（`jsonPath("$.data[0].scopes")` 数组内容）;EXPIRED/NOT_AUTHORIZED/UNKNOWN 角色断言 `scopes` 不存在或 null。
2. 新增用例：future 异常场景（stub 抛错）→ 200 + 该角色 `authStatus=UNKNOWN`（对应 FR-005 端到端）。
3. IT 依赖 test profile 的 DB/Redis 可达（既有约束,见 quickstart）。

### T4: 文档与收尾材料

- `docs/knowledge/` 新增本特性流程文档并登记 `docs/INDEX.md`（仓库约定）
- `CLAUDE.md` SPECKIT 指针随 P7 更新;`tasks.md` 勾选（P3 产物）

## 性能影响评估（宪法 I）

- 新增路径：AUTHORIZED 角色多一次 Redis 读 + 一次 JWT parse（微秒~毫秒级本地操作）;非 AUTHORIZED 零额外操作;**不新增 ESI 网络调用、不加锁、不进入 10s 并行等待预算**
- 无 DB 查询变更;无 N+1;缓存命中路径 p95 预期与现状持平（SC-002）

## 验证计划（P5,Maven/环境适配）

> 依据仓库实际通道（记忆）：`mvnw` 不稳定、测试依赖内网 DB/Redis（test profile → 192.168.12.249）。**首选 IDEA MCP**：`build_project` 编译 + 运行配置跑测试并数 teamcity 事件;`mvn` 可用时备选。

| 阶段 | 命令/通道 | 通过标准 |
|------|----------|----------|
| 构建 | IDEA MCP `build_project`（或 `mvn -q clean package -DskipTests`） | 0 错误 |
| 单元 | `EsiApiServiceTest`、`UserApplicationServiceUnitTest`、`UserApplicationServiceTest` | 0 failures,0 errors |
| 集成 | `UserControllerAuthStatusIT` | 0 failures,0 errors |
| 全量回归 | 全部测试（逐用例 diff,分离 Failures/Errors,勿用固定阈值） | 不劣于基线（基线约 893/F8/E47/S31,2026-08-27） |
| 覆盖率 | 新增代码 ≥80% | 达标或记录豁免理由 |

## 评审计划（P6）

- `ecc:java-reviewer`（Java 变更必须）
- 追加 `ecc:security-reviewer` 视角——涉及 ESI 令牌/授权数据出站（按宪法 V.1;注意记忆:该代理曾两次零产出卡死,实际审计改用 general-purpose + 详尽提纲）
- 评审记录归档 `docs/reviews/`

## Complexity Tracking

无宪法豁免项,无复杂度超限。
