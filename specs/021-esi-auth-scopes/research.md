# Research: 021-esi-auth-scopes

**Date**: 2026-09-01 | **Status**: 全部已解决（无 NEEDS CLARIFICATION 遗留）

## R1: accessToken 中已授权权限声明（scp）的格式

**Decision**: 通用 `Object` 判型解析——`List`（JSON 字符串数组）→ 逐项转串;`String`（空格分隔）→ 空白切分;缺失/类型未知 → null（fail-soft）。

**Rationale**: EVE SSO 访问令牌为 JWT,已授权权限声明历史上存在两种格式（新版为字符串数组,旧版为空格分隔字符串）。国服（Serenity/晨曦）令牌格式未能在设计期实测,双格式兼容使"猜错格式"的风险归零。现有代码已用 Nimbus `SignedJWT.parse` 成功解析同一 token 的 `sub`/`name` 声明（`EsiApiService.updateRefreshToken`）,`scp` 同法可取。

**Alternatives considered**:
- 只解析数组格式：若晨曦实际为字符串格式则全线失效。
- 实现前先抓真实 token 确认格式：无法在设计期离线完成;即便确认,双格式兼容代码成本相同,保留兼容层更稳。
- 实现后以真实 token 实测验证（**保留为 P4 实现期的验证动作**,见 quickstart）。

## R2: JWT 解析库与签名验证

**Decision**: 复用栈内 Nimbus JOSE JWT 10.0.2 的 `SignedJWT.parse` + `getJWTClaimsSet().getClaim("scp")`;**不验证签名**。

**Rationale**: 解析对象是本系统缓存层中由 ESI 签发、自家 `AuthorizeOAuth` 换取回来的 token,读取声明无需验签（与现有 `sub`/`name` 解析同口径）。Nimbus 在技术栈冻结清单内（宪法 IV）,零新依赖。

**Alternatives considered**: 手写 base64+JSON 解码（重复造轮子,DRY 违例）;引入新 JWT 库（违宪 IV,直接排除）。

## R3: scopes 获取通路（不触碰安全敏感代码）

**Decision**: 新增端口方法 `EsiAuthorizationDetail getAuthorizationDetail(EveAccount)`,实现为组合式——先调现有 `getAuthorizationStatus`（完整复用状态缓存/per-character 刷新锁/回写逻辑）,返回 AUTHORIZED 时再从缓存层读 accessToken 并解析 scopes。

**Rationale**: `getAuthorizationStatus` 的刷新锁逻辑曾经历多轮评审修复（per-characterId 锁、fail-closed、compare-and-delete、防投毒）,是安全敏感区。组合式实现**零侵入**该区域;且缓存写入时机已保证：AUTHORIZED 成立 ⟹ token 必已在缓存（`persistRefreshedToken` 先写 DB 后写缓存,缓存写失败会兜底为 UNKNOWN）。状态与 scopes 同源,天然一致。

**Alternatives considered**:
- 改 `determineAuthStatus` 返回 (status, scopes)：触碰已评审核心,回归面大,仅省一次缓存读（毫秒级）。
- 独立 `getAuthorizationScopes` 端口方法：应用层两次调用,状态与 scopes 可能来自两次判定,语义割裂,且独立方法仍需重复锁/缓存逻辑或再触发刷新（FR-006 禁止）。
- scopes 解析落库（eve_account 加列）：用户已在 G1 前确认"运行时解析、无 DDL",且 FR-007 禁止。

## R4: 测试中构造含 scp 的 JWT

**Decision**: 测试内用 Nimbus `MACSigner` 自签 HS256 JWT 作为缓存 token 值（`Bearer ` 前缀 + `jwt.serialize()`）,精确控制 scp 为数组/字符串/缺失三种变体。

**Rationale**: 现有 `EsiApiServiceUnitTest` 的刷新成功用例缓存的是 `"at"`（非 JWT）,无法验证解析成功路径;自签 JWT 可确定性覆盖两种格式与空/缺失语义。`EsiApiServiceTest` 为纯 Mockito 测试（不依赖 Spring/DB/Redis）,新用例落在该文件沿用其构造方式。

**Alternatives considered**: 用线上真实 token 写死在测试（凭据入库,安全红线违例）。

## R5: null 与空列表语义

**Decision**: `scopes=null`=无法确定（非 AUTHORIZED / 无可用 accessToken / 解析失败 / claim 缺失）;`scopes=[]`=token 有效但未声明受限权限。

**Rationale**: spec Edge Cases 已定义该区分;前端可据此区分"无权限详情"与"空权限"两种 UI 态。空数组/空字符串声明 → 空列表;claim 缺失或类型未知 → null。

**Alternatives considered**: 统一空列表（丢失"不可得"语义,违反 spec 决策）。

## R6: 应用服务对 null 结果的防御

**Decision**: `resolveDetail` 将 future 结果 null、异常、超时统一归为 `UNKNOWN + scopes=null`,永不返回 null。

**Rationale**: IT 中 `@MockitoBean EsiGateway` 对未 stub 的新方法返回 null;不做防御会在 `detail.status()` 处 NPE。泛化现有 `resolveStatus`（超时/异常→UNKNOWN）为 `resolveDetail` 即可,保持既有降级语义。
