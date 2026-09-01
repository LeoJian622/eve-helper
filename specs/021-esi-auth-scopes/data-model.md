# Data Model: 021-esi-auth-scopes

**Date**: 2026-09-01 | **DB 变更**: 无（无 DDL,不落库,FR-007）

## 实体与数据结构

### 1. UserAccountDTO（修改,application/dto）

| 字段 | 类型 | 语义 | 变更 |
|------|------|------|------|
| characterId / characterName / corpId / corpName / allianceId / allianceName | 原样 | 角色基本信息 | 不变 |
| authStatus | `EsiAuthStatus` | AUTHORIZED/EXPIRED/NOT_AUTHORIZED/UNKNOWN | 不变 |
| **scopes** | `List<String>`（可 null） | ESI 已授权权限名列表（原始 scope 名,如 `esi-wallet.read_character_wallet.v1`） | **新增** |

**校验规则**（响应语义,非 Bean Validation）:
- `scopes=null`：无法确定——authStatus 为 EXPIRED/NOT_AUTHORIZED/UNKNOWN,或无可用 accessToken,或解析失败/claim 缺失
- `scopes=[]`：token 有效但未声明受限权限
- 元素：原始 scope 名字符串（非空）,不做翻译/排序/过滤

### 2. EsiAuthorizationDetail（新增,domain/model/vo,record）

跨层读模型：infrastructure 网关 → application 服务传递。

| 组件 | 类型 | 语义 |
|------|------|------|
| status | `EsiAuthStatus` | 与 `getAuthorizationStatus` 判定结果一致 |
| scopes | `List<String>`（可 null） | AUTHORIZED 且解析成功时非 null;其余 null |

### 3. EveAccount（只读引用）

不新增任何持久化字段。服务层以其 `userId`/`characterId` 构造缓存键读取 accessToken。

### 4. accessToken 缓存条目（只读）

| 项 | 值 |
|-----|-----|
| 键 | `esi_access_token:{userId}:{characterId}`（现有 `accessTokenKey`,不外泄） |
| 值 | `Bearer {JWT}`,JWT 声明含 `sub`/`name`/`scp` 等 |
| TTL | 19 分钟（现有 1140s） |

## 状态转换

无新增状态机。数据流（AUTHORIZED 角色单次调用内）:

```
queryAccountListWithAuthStatus(userId)
  └─(并行,esiAuthStatusExecutor)─ EsiGateway.getAuthorizationDetail(account)
        ├─ getAuthorizationStatus(account)          # 现有逻辑:状态缓存→刷新锁→判定→回写
        └─ status==AUTHORIZED ? parseScopesFromCachedToken : null
              └─ 缓存读 accessToken → SignedJWT.parse → scp(List/String/缺失) → List/null
  └─ resolveDetail(超时/异常/null → UNKNOWN + scopes null)
  └─ DTO.authStatus / DTO.scopes 后置填充（MapStruct 不涉及 scopes）
```

## 一致性要点

- AUTHORIZED ⟹ token 已在缓存：`persistRefreshedToken` 先写 DB 后写缓存,缓存写失败 → 异常 → 判定兜底 UNKNOWN;故 detail 中 AUTHORIZED + 缓存缺失仅出现在缓存被外部清空（重启/驱逐）场景,按 spec 降级为 `scopes=null` 且 status 不变
- 并行隔离：单角色 scopes 解析异常不外溢（fail-soft → null）,与既有状态判定隔离策略一致
