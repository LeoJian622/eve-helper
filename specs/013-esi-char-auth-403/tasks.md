---
description: "Task list for feature 013: ESI 人物授权基准与 403 友好返回"
---

# Tasks: 013 — ESI 人物授权基准与 403 友好返回

**Input**: `specs/013-esi-char-auth-403/spec.md` + `plan.md`
**Prerequisites**: plan.md（G2 已批准）、spec.md（G1 已批准）、docs/reviews/013-plan-security-review.md
**TDD**: 本项目宪法强制——每个实现任务先写失败测试（RED）→ 亲眼看失败 → GREEN 实现 → REFACTOR。

## 用户故事优先级（spec.md）

- **US1**（P1）：军团/联盟数据读取统一以关联人物ID执行 —— 端到端验收
- **US2**（P1）：人物授权为唯一基准（不变量约束）
- **US3**（P2）：ESI 数据接口 403 友好返回
- **US4**（P2）：现值错位实现被校正（回归保障）

> 执行序与 US 号非一一对应：错误码/403 工具/异常映射为 **foundation**（US2/US3 的底层），先做；随后 US3 API 层 403 识别、US4 签名校正串行（互有依赖），US1 为最终端到端验收，US2 不变量核查贯穿收尾。

---

## Phase 1: Foundation（共享基础设施，构建 US2/US3 前提）

**目的**：错误码、403 识别工具、异常状态映射——US2 不变量与 US3 403 友好的底层依赖。

- [ ] T001 在 `src/main/java/xyz/foolcat/eve/evehelper/infrastructure/external/esi/ResultCode.java` 枚举新增 `ESI_AUTH_PERMISSION_LOW("ESI00403", "该人物缺少目标军团的 Director/Bookkeeper 等角色、已离开或不在目标军团，请确认角色或重新授权")`（**落 infrastructure 枚举，勿动 shared 版**；不改动既存码值）
- [ ] T002 新增 `src/main/java/xyz/foolcat/eve/evehelper/infrastructure/external/esi/EsiStatusUtil.java` 静态工具：`isForbidden(HttpStatusCode)`（`value()==403`）与 `forbiddenAgent(HttpStatusCode)`（返回 `EsiException(ResultCode.ESI_AUTH_PERMISSION_LOW)`，**message 用枚举报好文案，不含 ESI 原始 error/errorDescription**）
- [ ] T003 写 `EsiStatusUtilTest` 单元测试（RED→GREEN）：403→ESI_AUTH_PERMISSION_LOW、其它 4xx→保持、5xx 不在本工具范围；`forbiddenAgent` message 与枚举文案一致且不含原始 ESI 串
- [ ] T004 改 `src/main/java/xyz/foolcat/eve/evehelper/interfaces/web/advice/GlobalExceptionHandler.java` 的 `resolveHttpStatus`：`ESI_AUTH_PERMISSION_LOW` → `HTTP 403`（复用 `HttpStatus.FORBIDDEN`；与 `ACCESS_UNAUTHORIZED` 并列为 403，但码不同）。写校验断言：码 vs 状态映射正确
- [ ] T005 [P] 更新 `docs/reviews/013-plan-security-review.md` 归档无误（若需）

**Checkpoint**: 错误码 + 403 工具 + 状态映射就绪——US3/US4 依赖的底层已具足。

---

## Phase 2: US3 — ESI 数据接口 403 友好返回 (Priority: P2)

**目标**：ESI 返回 403 时前端收到专属错误码 + 友好文案 + HTTP 403，区别于 5xx/网络/刷新失效。
**独立测试**：模拟 ESI 403 → 断言返回码 `ESI00403`、HTTP 403、文案不含 ESI 原串。

### 测试（先写，RED）

- [ ] T006 [US3] 写 `GlobalExceptionHandler403IT` 集成测试：`EveHelperException(ESI_AUTH_PERMISSION_LOW)` → HTTP 403 + `Result.code=ESI00403` + msg=友好文案（RED）
- [ ] T007 [P] [US3] 写 `EsiStatusUtilTest` 的 403 优先时序用例：**先判 statusCode==403 再解 body**——模拟 body 空/非 JSON 仍触发 403 分支（RED）

### API 层 403 单识实现（先修 PageTotalApi 公共分页入口，再钱包，再其余）

- [ ] T008 [US3] 修 `src/main/java/xyz/foolcat/eve/evehelper/infrastructure/external/esi/api/PageTotalApi.java`：`queryMaxPage` 的 4xx/5xx 分支 `.map().flatMap()` 未 return 的 bug——改为正确 `return Mono.error(...)`；4xx 内先判 403 → `EsiException(ESI_AUTH_PERMISSION_LOW)`，其余 4xx → `ESI_AUTHORIZATION_FAILURE`，5xx → `ESI_SERVER_FAILURE`；去 `assert`、`toEntity` 空态兜底；响应链加 5s 超时（复用或新增超时常量）
- [ ] T009 [US3] 写 `PageTotalApiTest`：4xx（403/其它）/5xx 断言抛对应 ESI 异常；空 body 403 仍走 403 分支（RED→GREEN）
- [ ] T010 [US3] 改 `src/main/java/xyz/foolcat/eve/evehelper/infrastructure/external/esi/api/WalletApi.java`：军团钱包流水（`queryCorporationWalletJournal*`）/余额（`queryCorporationWallet`）/交易（`queryCorporationWalletTransactions*`）的 `onStatus` 4xx 内加 403 先判分支（其余 4xx → `ESI_AUTHORIZATION_FAILURE`，message 不用原始串）
- [ ] T011 [US3] 核对并补 `IndustryApi.queryCorporationIndustryJobs*`、`CorporationApi.queryCorporationStructures*`/`queryCorporationBlueprints*`、`AssetsApi.queryCharactersAssets*` 的 4xx 403 先判分支（逐一核对既有 4xx 是否已分 403）。**MiningTask（工业抽提定时任务）不经 API 层，不在本次 403 改造范围**——排除
- [ ] T012 [US3] 写 `WalletApi403Test`：军团 403 → `ESI_AUTH_PERMISSION_LOW`、HTTP 透传 403、message=友好文案；5xx → `ESI_SERVER_FAILURE`（RED→GREEN）

**Checkpoint**: US3 完成——任何 ESI 军团/人物数据入口的 403 前端可辨。

---

## Phase 3: US4 — 现值错位实现校正（传角色ID→解析军团ID）

**目标**：消除「同一 ID 兼作角色与军团」双重语义，改传角色ID并从 eve_account 解析目标军团ID。
**独立测试**：传角色ID → 断言 ESI 调用收到的 corporationId == `eveAccount.getCorpId()`（非入参）。

### 测试（先写，RED）

- [ ] T013 [US4] 写 `WalletJournalServiceSyncTest`：传角色ID → mock `authorize(characterId)` 返回含 corpId 的 eveAccount → 断言 `esiApiService.queryCorporationWalletJournal(corpId,...)` 收到 `getCorpId()`（RED）
- [ ] T014 [US4] 写 `WalletTransactionServiceSyncTest`：同构，断言 `queryCorporationWalletTransactions(corpId,...)` 用解析值（RED）

### 服务签名统一实现

- [ ] T015 [US4] 改 `src/main/java/xyz/foolcat/eve/evehelper/domain/service/system/WalletJournalService.java` `syncCorporationJournal`：签名语义改 `characterId`；**userId 不出现在方法签名**——请求路径 `authorize(characterId)` 内部 `UserUtil.getUserId()` 自取、内部路径 `authorizeInternal(SYSTEM_USER_ID, characterId)`，与现服务一致；`corpId = eveAccount.getCorpId()`（null → `EsiException(ESI_AUTH_PERMISSION_LOW)`）；`getAccessToken(characterId, eveAccount.getUserId())`；逐 division pull 用解析 corpId（GREEN）
- [ ] T016 [US4] 改 `src/main/java/xyz/foolcat/eve/evehelper/domain/service/system/WalletTransactionService.java` `syncCorporationTransactions`：同构（签名 `characterId`；userId 不出现在方法签名，`UserUtil.getUserId()`/`authorizeInternal` 自取；`getCorpId()` 解析，null→403 码，pull 用解析值）

### 内部路径归属对账（M2）

- [ ] T017 [US4] 写 `WalletJournalServiceInternalAuthTest`（FR-007 契约测试）：内部路径 `authorizeInternal(SYSTEM_USER_ID, characterId)` —— 若库无对应系统绑定行 → 明确断言行为（不因系统身份绕过 owner 过滤），确认实现路径（真实拥有者定位）不旁路归属（RED→GREEN）

**Checkpoint**: US4 完成——钱包流水/交易军团同步以角色ID驱动、军团ID从角色派生。

---

## Phase 4: US1 — 军团/联盟数据读取统一以关联人物ID执行（端到端验收）

**目标**: 全链（控制器→应用服务→领域服务→ESI）以关联角色ID执行军团数据读取并通过归属校验。
**独立测试**: 传角色ID → 端到端军团钱包同步成功；传非关联 ID → 404/403 且不触发 ESI。

### 控制器契约

- [ ] T018 [US1] 改 `src/main/java/xyz/foolcat/eve/evehelper/interfaces/web/controller/WalletJournalController.java`：军团同步端点参数 `corpId` → `characterId`（URL 保留 `/wallet/journal/corp/{characterId}/sync` 或按前端协商，plan 默认保留 URL 仅参数语义改），Swagger `@Parameter` 名称/描述同步
- [ ] T019 [US1] 改 `src/main/java/xyz/foolcat/eve/evehelper/interfaces/web/controller/WalletTransactionController.java`：军团交易端点同构改 `characterId`

### RBAC（按需）

- [ ] T020 [US1] 核对 `RbacAuthorizationManager` 路径权限串：若 URL 路径未变（仅参数名 `{corpId}`→`{characterId}`），则既有 `method:path` 键不变，**无需**新增 SQL——记录结论；仅在 URL 路径真变化时，创建 `src/SQL/convert/013_rbac_permissions.sql`（幂等先删后插，参照 010/011/012）并登记权限

### 端到端验收

- [ ] T021 [US1] 写 `WalletJournalControllerIT`（集成，需 MySQL）：成功路径（传角色ID→军团钱包同步落库）、越权（非关联ID→403/404 不调 ESI）、模拟 ESI 403→前端收 403+专属文案、5xx/超时→不同于 403 的错误（RED→GREEN）
- [ ] T022 [US1] 核对资产/蓝图/工业/采矿/建筑服务（`AssetsService`/`BlueprintsService`/`IndustryJobService`/`MiningDetailService`/`StructureService`）签名已是「传入角色→`getCorpId()`」，**不改签名**，纳入静态核查证据（M3）：无客户端直传 corpId 进 ESI 的分支残留

**Checkpoint**: US1 完成——军团数据读取端到端以关联角色ID执行。

---

## Phase 5: US2 — 人物授权为唯一基准（不变量核查）

**目标**：固化为约束——全部 ESI 授权入口从人物角色行取 token，无独立军团/联盟授权。
**独立测试**: 静态核查 + 授权入口测试——无按 corpId/allianceId 单独的 token 路径。

- [ ] T023 [US2] 静态审查全部 ESI 授权调用点：确认 token 均经 `EsiApiService.getAccessToken(characterId,userId)`（人物行），无 `corpId`/`allianceId` 维度独立授权；产出核查清单注记（FR-001 证据）
- [ ] T024 [US2] 核查 `eve_account` 数据模型用法：refreshToken 仅工具角色行，corpId/allianceId 是从属字段（FR-001）；实现层不新增独立军团/联盟令牌通道

**Checkpoint**: US2 不变量核查通过。

---

## Phase 6: Polish & Cross-Cutting

**目的**: 全量验证、覆盖率、评审、知识文档、收尾。

- [ ] T025 [P] 运行 `./mvnw -q clean package -DskipTests`（build 绿）
- [ ] T026 运行 `./mvnw test`（0 failures, 0 errors）
- [ ] T027 覆盖率核验 `ecc:test-coverage`（新增代码 ≥80%）
- [ ] T028 [P] 安全扫描 `ecc:security-scan`（无 Critical）
- [ ] T029 生成 `docs/knowledge/013-esi-char-auth-403.md` 流程文档并登记 `docs/INDEX.md`（knowledge-doc-convention）
- [ ] T030 P5 VERIFICATION REPORT = READY → 交协调方过 P6
- [ ] T031 P6 `superpowers:requesting-code-review` 派遣 `ecc:java-reviewer` + `ecc:security-reviewer`（本特性涉及认证/外部 API/输入 → security-reviewer 必须），G6 无 Critical/Important
- [ ] T032 更新 `specs/013-esi-char-auth-403/tasks.md` 勾选状态与 `.specify/feature.json`（P7）

---

## Dependencies & Execution Order

### Phase 依赖

- **Phase 1 (Foundation)**: 无依赖，最先（阻塞 US3/US4 底层）
- **Phase 2 (US3)**: 依赖 T001-T004
- **Phase 3 (US4)**: 依赖 Phase 1
- **Phase 4 (US1)**: 依赖 Phase 2+3（403 与签名统一已就绪后才端到端验收）
- **Phase 5 (US2)**: 可并行于 Phase 4（不变量核查）
- **Phase 6 (Polish)**: 依赖全部实现

### 并行机会

- T005（归档）可与 T004 并行
- US2（Phase 5）与 US4（Phase 3）可并行（不同文件）
- 各实现任务的单元测试（T003/T007/T009/T012/T013/T014/T017）均为独立测试文件，可并行先行

## Implementation Strategy

### MVP 第一（US3 403 识别 + US4 签名校正）

1. Phase 1 Foundation（错误码/工具/映射）
2. Phase 2 US3（403 API 层识别，含 PageTotalApi bug 修复）
3. Phase 3 US4（钱包签名统一）
4. **STOP & VALIDATE**：US3+US4 独立测试绿
5. 增量：Phase 4 US1 端到端验收 → Phase 5 US2 不变量 → Phase 6 Polish

### 关键依赖（并行执行示例）

```bash
# US3 测试先并行写（RED）：
Task: "GlobalExceptionHandler403IT 集成测试"
Task: "EsiStatusUtilTest 403 时序用例"
Task: "WalletApi403Test"

# US4 测试先并行写（RED）：
Task: "WalletJournalServiceSyncTest"
Task: "WalletTransactionServiceSyncTest"
```

## Testing & Quality Gates

- 单元覆盖率 ≥80%（`ecc:test-coverage` 门禁）
- 集成测试覆盖关键路径（成功/越权/403/5xx）
- 安全测试覆盖认证/授权/数据校验（security-reviewer）
- 性能：REST <200ms p95，ESI 数据调用 5s 超时

## Notes

- [P] = 不同文件、无依赖，可并行
- TDD：每个实现前先写失败测试亲眼看红
- 每个逻辑组完成后提交（feat:/fix:/refactor: 前缀）
- 违反安全红线（回显 ESI 原始 error）视为实现错误——message 恒用枚举报好文案