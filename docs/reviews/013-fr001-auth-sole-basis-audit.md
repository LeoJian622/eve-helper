# 013 — FR-001 人物授权唯一基准不变量核查清单

**Feature**: 013-esi-char-auth-403
**阶段**: Phase 5 (US2) — T023 / T024
**日期**: 2026-08-19
**分支**: feat/013-esi-char-auth-403
**不变量(FR-001)**: 系统任何 ESI 授权调用都以「人物(eve_account 角色行)」的 refreshToken 为唯一 token 来源,不存在独立军团/联盟令牌通道。

---

## T023 — 全部 ESI 授权调用点静态审查

**方法与范围**: 全库搜索 `getAccessToken(` 生产调用点,逐一核对第一参数是否为「人物角色ID」;核对 `EsiGateway`/`EsiApiService` 接口签名无 corpId/allianceId 维度 token 重载。交叉由 code-explorer 子代理独立审计 + 主会话人工核对双路验证。

### 调用点清单(全部合规)

| # | 调用点 | 第一参数来源 | 人物维度 | 合规 |
|---|--------|-------------|:---:|:---:|
| 1 | MiningTask.java:74 | `TaskConstant.CHARACTER_ID`(角色常量) | 是 | 合规 |
| 2 | MiningTask.java:101 | 角色ID 入参 | 是 | 合规 |
| 3 | AssetsService.java:83 | `eveAccount.getCharacterId()` 角色行 | 是 | 合规 |
| 4 | IndustryJobService.java:82 | 角色ID 入参 | 是 | 合规 |
| 5 | BlueprintsService.java:46 | 角色ID 入参 | 是 | 合规 |
| 6 | StructureService.java:118 | 角色ID 入参 | 是 | 合规 |
| 7 | MiningDetailService.java:44 | 角色ID 入参 | 是 | 合规 |
| 8 | WalletTransactionService.java:79 | 人物ID 入参 | 是 | 合规 |
| 9 | WalletTransactionService.java:119 | 013 统一角色ID,corpId 单独派生(:114) | 是 | 合规 |
| 10 | WalletJournalService.java:89 | 角色ID 入参 | 是 | 合规 |
| 11 | WalletJournalService.java:130 | 角色ID 入参 | 是 | 合规 |
| 12 | WalletJournalService.java:193 | 013 统一角色ID,corpId 单独派生(:188) | 是 | 合规 |
| 13 | CharacterApplicationService.java:73 | 角色查询入参(先守卫) | 是 | 合规 |
| 14 | CharacterApplicationService.java:36 | `authorize(code)` 角色授权绑定 | 是 | 合规 |

### 接口签名(无 corp/alliance token 维度)

- `EsiGateway.getAccessToken(Integer code, Integer userId)`(:40)——唯一 token 入口,第一参数人物ID
- `getAccessTokenWithExpiry(Integer characterId, Integer userId)`(:53)
- 接口内全部 `queryCorporation*`(:90/:97/:123/:135/:156/:192)只接收已取得的 `String accessToken`,不触 token 获取
- **不存在** `getAccessToken(corpId)` / `getCorpAccessToken` / `getAllianceAccessToken` 等 corp/alliance 维度入口(全库搜索无命中)

### 军团/联盟数据端点(关键确认)

所有军团查询(Blueprints/IndustryJob/Structure/MiningDetail/WalletJournal/WalletTransaction)全部遵循正确模式:**token 恒来自角色行**,军团ID只作为 ESI 数据端点目标参数,独立由 `eveAccount.getCorpId()` 派生,绝不复用为 token 维度。如:
- BlueprintsService:46 token 用角色 cid → :49 `queryCorporationBlueprintsMaxPage(eveAccount.getCorpId(), ...)`
- StructureService:118 token 用角色 cId → :123 `queryCorporationStructures(eveAccount.getCorpId(), ...)`
- WalletTransactionService:119 token 用角色 characterId → :127 `queryCorporationWalletTransactions(corpId, ...)`
- WalletJournalService:193 token 用角色 characterId → :199 `queryCorporationWalletJournal(corpId, ...)`

### 已处理提示

- **BlueprintsService `saveAndUpdateBlueprints` Javadoc**(LOW):`@param cid` 原写作「人物或公司ID」存在语义歧义,实际只有角色ID能取到 token(功能正确),且该方法为无生产调用方、注释明示未完成的桩。**已修**:Javadoc 改为「角色ID(token 恒取自该角色行)」,消除歧义,行为不变。

---

## T024 — eve_account 数据模型用法核查

**方法与范围**: 核对 `EveAccount` 实体字段归属 + `EsiApiService.doGetAccessToken` token 获取数据流,确认 refreshToken 仅存在于角色行、无独立军团/联盟令牌通道。

### eve_account 实体字段(FR-001 契约)

- `characterId`(Integer,角色标识)· `userId`(Integer,**归属用户**)· `refreshToken`(String,**授权载体,属角色行**)
- `corpId`/`corpName`/`allianceId`/`allianceName`(**从属属性**,角色身份派生视图)
- `type`/`qq`(附属属性)

→ corpId/allianceId 是角色的从属字段,refreshToken 仅存在于角色行上。满足 FR-001 AS-2。

### doGetAccessToken 数据流(唯一 token 通道)

- `EsiApiService.doGetAccessToken(characterId, userId)`(:218)
- :221 `getAccountOne(userId, characterId)`——**双键人物查询**(归属 + 角色隔离)
- :260 `character.getRefreshToken()`——换 token 仅用**角色行 refreshToken**
- :228/229/237 缓存键 `accessTokenKey(userId, ownedCharacterId)`、刷新锁 `refreshLockKey(ownedCharacterId)`——均按 characterId 隔离

→ 全程无 corpId/allianceId 参与 token 获取。满足 FR-001 AS-1。

---

## 全局结论

**满足 FR-001**。全库 14 处 ESI token 获取调用点第一参数均为人物角色ID(来自用户入参或 eve_account 角色行字段);refreshToken 唯一来源于 eve_account 角色行;军团/联盟维度只作用于 ESI 数据端点目标ID,未进入 token 获取逻辑;接口层无 corp/alliance token 重载。系统不存在任何按 corpId/allianceId 维度独立取 token 的通道。