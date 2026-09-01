# ESI 授权详情（scopes）返回流程

> 面向开发者：理清 `GET /user/{userId}` 响应中 `scopes` 字段的语义（null vs 空列表）、组合式端口方法的设计动机（零侵入刷新锁安全敏感区）、scp 双格式解析与 fail-soft 约定，以及「RED 即绿」场景下用突变检验证明用例判别力的 TDD 实践。
> 关联代码：`application/dto/UserAccountDTO.java`、`application/service/UserApplicationService.java`、`domain/port/esi/EsiGateway.java`、`domain/model/vo/EsiAuthorizationDetail.java`、`infrastructure/gateway/esi/EsiApiService.java`（`getAuthorizationDetail` / `parseScopesFromCachedToken` / `toScopeList`）。

## 1. 总览

021 在用户角色列表接口（003 起）已返回 `authStatus` 四态的基础上，补齐**授权详情**维度：每个角色当前实际被授予了哪些 ESI 权限（原始 scope 名），供前端直接渲染"该用户下该角色的授权详情"。

| 字段 | 语义 |
|------|------|
| `scopes = [esi-wallet.read_character_wallet.v1, ...]` | AUTHORIZED 且解析成功：该角色当前有效 accessToken 实际声明的权限集合 |
| `scopes = []` | token 有效但未声明任何受限权限（**确认为空**） |
| `scopes = null` | **无法确定**：非 AUTHORIZED 三态 / 无可用 accessToken / 解析失败 / scp claim 缺失 |

`null` 与 `[]` 的区分是刻意的：前端据此区分「无权限详情」与「确认为空」两种 UI 态。

## 2. 主流程（组合式端口方法）

```
GET /user/{userId}
  -> UserApplicationService.queryAccountListWithAuthStatus(userId)
       -> AccessGuard.requireSelfOrRoot(userId)          // 既有 IDOR 防护,不变
       -> eveAccountService.getAccountList(userId)
       -> 逐角色并行(esiAuthStatusExecutor):
            EsiGateway.getAuthorizationDetail(account)   // 021 新端口方法
                 -> getAuthorizationStatus(account)      // ★ 原样复用:状态缓存/刷新锁/回写
                 -> status != AUTHORIZED ?
                      new EsiAuthorizationDetail(status, null)        // 三态降级
                    : parseScopesFromCachedToken(account)            // 仅 AUTHORIZED
                       -> cacheGateway.get("esi_access_token:{userId}:{characterId}")
                       -> 剥 "Bearer " 前缀 -> SignedJWT.parse -> getClaim("scp")
                       -> toScopeList(scp)                           // 双格式解析
       -> resolveDetail(future)      // null/超时/异常/中断 -> UNKNOWN_DETAIL(UNKNOWN + scopes null)
       -> DTO.authStatus / DTO.scopes 后置填充(MapStruct 不涉 scopes,与 authStatus 同模式)
```

设计核心：`getAuthorizationDetail` 是**组合**而非重写——状态判定、per-characterId 刷新锁、refreshToken 轮换回写全部原样复用 `getAuthorizationStatus`，021 自身只做「AUTHORIZED 后从缓存层读 token 并解析 scp」这一件事。刷新锁区域经历过多轮评审修复（fail-closed、compareAndDelete、防投毒），零侵入意味着零回归面。

## 3. scopes 解析的关键约定

### 3.1 数据源与 FR-006

scopes **只能**来自「现有状态判定/取 token 流程已产出或已缓存的数据」：

- AUTHORIZED ⟹ token 必在缓存（`persistRefreshedToken` 先写 DB 后写缓存，缓存写失败会兜底为 UNKNOWN）；状态缓存 TTL(5min) < token 缓存 TTL(19min)，正常时序不会出现"状态新鲜而 token 已逐"
- 非 AUTHORIZED 分支**不读** token 缓存（测试 B1 用 `verify(never())` 锚定）——不为 scopes 产生任何额外上游调用或 refreshToken 刷新
- 唯一已知降级窗口：缓存被外部清空（重启/驱逐）→ AUTHORIZED 但 token 缓存 miss → scopes=null 而 status 不变（B3 锚定）

### 3.2 scp 双格式与 fail-soft

EVE SSO 的 `scp` claim 存在两种历史格式，`toScopeList` 用 `Object` 判型一次兼容：

| scp 实际形态 | 解析结果 |
|--------------|----------|
| JSON 字符串数组（新版） | 逐项转串（过滤 null/空段），可空列表 |
| 空格分隔字符串（旧版） | `\s+` 切分；空白串 → 空列表 |
| claim 缺失 / 类型未知 | `null`（无法确定，非空列表） |

`parseScopesFromCachedToken` 整体 catch Exception → `log.warn`(仅固定文案+characterId，**不含 token 内容与异常对象**) → null。解析失败永不抛错、永不影响 status 与其他角色。

### 3.3 与其他缓存读的差异（勿"统一"）

token 缓存读侧惯用 `StrUtil.isNotEmpty`，本方法用 `StrUtil.isBlank`——空串/空白同样视为"无 token 可解析"，属有意的 fail-soft 选择（代码内有注释说明，防后人误改回）。

## 4. null-safe 兜底（应用服务）

`resolveDetail` 对 future 结果 null、超时、ExecutionException、中断统一归 `UNKNOWN_DETAIL`（UNKNOWN + scopes null 的静态不可变 record）。动机之一：IT 中 `@MockitoBean EsiGateway` 未 stub 的新方法返回 null，不防御即 NPE（IT 用例 `detailFailure_isolatedAsUnknown` 端到端锚定 FR-005）。

## 5. TDD 实践：突变检验证明降级用例判别力

US2 降级行为随 T006 组合实现自然就绪，B 组用例"RED 阶段即绿"（tasks.md T011 预期受控情形）。为证明用例不是"摆设"，对两个代表用例做**临时突变检验**：

| 用例 | 突变 | 预期 | 实测 |
|------|------|------|------|
| B1 EXPIRED 不读 token 缓存 | 删掉 `status != AUTHORIZED` 短路 | verify never 失败 | ✅ 红（`NeverWantedButInvoked`）后还原回绿 |
| B4 垃圾 token fail-soft | catch 块改抛 RuntimeException | 异常逃逸致用例红 | ✅ 红后还原回绿 |

突变后 `git diff -- src/main/java/` 必须为 0 行（还原零残留，用例有效性有了真实证据而非推理）。B2/B3/B5 以推理说明判别力（绕过守卫重排/miss 误返空列表/缺 claim 误判空 claim）。

## 6. 测试落点速查

| 文件 | 用例 |
|------|------|
| `EsiApiServiceTest` | A1 scp 数组 / A2 空格串（SC-005 双格式等价）/ A3 空数组→[]；B1-B5 降级矩阵（自签 HS256 JWT 辅助 `jwtWithScp`） |
| `UserApplicationServiceUnitTest` | AUTHORIZED 传播 / null-detail→UNKNOWN / EXPIRED、UNKNOWN detail→双 null |
| `UserControllerAuthStatusIT` | T014 扩展（scopes 数组逐元素 + 三态 nullValue）/ FR-005 单角色异常隔离端到端 |

## 7. 已知边界

- 基线遗留：`EsiApiServiceTest` 中 9 个**既有**失败（锁释放 compareAndDelete vs 旧断言、015-H2 缓存键规范化 vs 旧键、旧签名 stub NPE）——与本特性无关，属缺陷轨候选
- `scopes` 不落库：EXPIRED 角色不展示"最近一次授权"的历史 scopes（产品口径，运行时解析无 DDL）
- 晨曦真实 token 实测：实现期以测试环境验证；scp 双格式兼容使格式假设失效风险归零
