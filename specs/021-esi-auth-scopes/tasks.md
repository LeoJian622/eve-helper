# Tasks: 021-esi-auth-scopes

**Input**: Design documents from `specs/021-esi-auth-scopes/`
**Prerequisites**: plan.md（G2 已批准）, spec.md（G1 已批准）, research.md, data-model.md, contracts/user-accounts-api.md, quickstart.md
**Branch**: `021-esi-auth-scopes`

**Tests**: 必含——本仓库工作流为 TDD 铁律（宪法 V.1/工作流 P4），每任务先 RED 亲眼见失败再 GREEN。
**通道**: 构建/测试首选 IDEA MCP（`build_project` + 运行配置数 teamcity 事件）；`mvn` 可用时备选。任何"完成"声明必须附当前轮次验证输出（证据铁律）。

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 可并行（不同文件,无未完成依赖）
- **[Story]**: 所属用户故事（US1/US2）
- 每任务完成标准：TDD 循环闭合 + 编译零错误 + 相关测试绿

---

## Phase 1: Setup

- [x] T001 记录回归基线：运行既有测试套件（重点 `UserApplicationServiceUnitTest`、`UserApplicationServiceTest`、`EsiApiServiceTest`、`UserControllerAuthStatusIT`），记录本轮基线 results（分离 Failures/Errors，勿用固定阈值）
  - **验收**: 基线数据写入任务日志，供 T011 回归 diff

---

## Phase 2: Foundational (Blocking Prerequisites)

**⚠️ US1/US2 的实现均依赖本阶段编译锚点**

- [x] T002 新增领域读模型 `src/main/java/xyz/foolcat/eve/evehelper/domain/model/vo/EsiAuthorizationDetail.java`（record: `EsiAuthStatus status` + `List<String> scopes`，javadoc 写明 null/空列表语义，见 plan T1 代码块）
- [x] T003 在 `src/main/java/xyz/foolcat/eve/evehelper/domain/port/esi/EsiGateway.java` 新增端口方法声明 `EsiAuthorizationDetail getAuthorizationDetail(EveAccount account)`（javadoc 全，见 plan）；在 `src/main/java/xyz/foolcat/eve/evehelper/infrastructure/gateway/esi/EsiApiService.java` 加 `@Override` 骨架（`return null`，仅作编译锚点，行为由 T005 实现）
  - **依赖**: T002
  - **验收**: IDEA MCP `build_project` 零错误

**Checkpoint**: 端口与读模型就绪，US1/US2 可开始（本特性两故事共享同一实现路径，顺序执行）

---

## Phase 3: User Story 1 — AUTHORIZED 角色返回已授权 scopes (Priority: P1) 🎯 MVP

**Goal**: authStatus=AUTHORIZED 的角色，响应 `scopes` = 其当前有效 accessToken 解析出的原始权限名列表。

**Independent Test**: stub/真实通路下调用 `GET /user/{userId}`，AUTHORIZED 角色 `scopes` 非 null 且与 token 声明一致。

### Tests for User Story 1（先写,亲眼看失败）

- [x] T004 [P] [US1] RED：在 `src/test/java/xyz/foolcat/eve/evehelper/domain/service/esi/EsiApiServiceTest.java` 新增测试辅助 `jwtWithScp(Object scp)`（Nimbus `MACSigner` 自签 HS256，plan T1 代码块）+ 3 个用例：A1 scp=数组→scopes 等价列表；A2 scp=空格分隔串→两元素列表（SC-005 双格式等价）；A3 scp=空数组→`[]` 非 null。stub 模式沿用现有"刷新成功→AUTHORIZED"用例，token 缓存值=`Bearer `+JWT
  - **RED 判据**: 骨架返回 null → scopes 断言失败（亲眼确认后进 T006）
- [x] T005 [P] [US1] RED：在 `src/test/java/xyz/foolcat/eve/evehelper/application/service/UserApplicationServiceUnitTest.java` 与 `UserApplicationServiceTest.java` 新增：AUTHORIZED detail 传播（`authStatus`+`scopes` 逐角色写入 DTO）；mock 未 stub（返回 null）→ `UNKNOWN`+null 不 NPE
  - **RED 判据**: 服务仍调旧方法 → scopes 断言失败

### Implementation for User Story 1

- [x] T006 [US1] GREEN：实现 `EsiApiService.getAuthorizationDetail` + 私有 `parseScopesFromCachedToken`/`toScopeList`（plan T1 代码块：复用 `accessTokenKey`、`TOKEN_PERN` 剥前缀、`SignedJWT.parse`、Object 判型、fail-soft catch、日志不含 token）。跑 T004 用例转绿
  - **依赖**: T002-T004
- [x] T007 [US1] GREEN：`src/main/java/xyz/foolcat/eve/evehelper/application/dto/UserAccountDTO.java` 新增 `List<String> scopes`（@Schema 注明 null 语义）；`src/main/java/xyz/foolcat/eve/evehelper/application/service/UserApplicationService.java` 切换为并行调用 `getAuthorizationDetail`，`resolveStatus` 泛化为 `resolveDetail`（null-safe `UNKNOWN_DETAIL`，plan T2 代码块）；同步更新既有测试中 `getAuthorizationStatus` stub。跑 T005 用例转绿 + 既有用例全绿
  - **依赖**: T002-T005
  - **注意**: MapStruct `EveAccountAssembler` 无需改动（scopes 服务层后置填充，与 authStatus 同模式）
- [x] T008 [US1] IT：`src/test/java/xyz/foolcat/eve/evehelper/interfaces/web/controller/UserControllerAuthStatusIT.java` 的 IT 内既有用例「T014 多角色混合状态」stub 换 `getAuthorizationDetail`，AUTHORIZED 角色补 `$.data[0].scopes` jsonPath 数组断言；运行 IT 全绿（需 DB/Redis 可达）
  - **依赖**: T006-T007

**Checkpoint**: US1 独立可用——AUTHORIZED 角色 scopes 端到端可见（单元+IT 双证据）

---

## Phase 4: User Story 2 — 非 AUTHORIZED 降级与 fail-soft (Priority: P2)

**Goal**: EXPIRED/NOT_AUTHORIZED/UNKNOWN → `scopes=null`；解析失败/缓存缺失 fail-soft；不产生任何额外上游调用。

**Independent Test**: 对各降级态调用接口，断言 `scopes=null`、`authStatus` 正确、无额外 ESI/缓存副作用。

### Tests for User Story 2（先写）

- [x] T009 [P] [US2] RED：`EsiApiServiceTest` 降级矩阵 5 用例：B1 EXPIRED（4xx stub）→ scopes=null 且 `verify(cacheGateway, never()).get(ACCESS_TOKEN_KEY)`（FR-006 零额外读）；B2 refreshToken 空→NOT_AUTHORIZED+null；B3 AUTHORIZED 但 token 缓存未命中→null 且 status 不变；B4 缓存值=非 JWT 垃圾串→null 不抛错；B5 token 有效但无 scp claim→null
- [x] T010 [P] [US2] RED：`UserApplicationServiceUnitTest` 补 EXPIRED/UNKNOWN detail→DTO 双 null；future 异常→UNKNOWN+null（既有语义回归锚定）；`UserControllerAuthStatusIT` 新增 future 抛错用例→200+该角色 `authStatus=UNKNOWN`
- [x] T011 [US2] GREEN：核对/补齐实现使 T009/T010 全绿（若 RED 阶段即绿，检视用例有效性后再收口）；IT 全绿
  - **依赖**: T006-T010

**Checkpoint**: US1+US2 全部验收场景覆盖（plan 用例矩阵 A1-A3/B1-B5 + 服务层 + IT）

---

## Phase 5: Polish & Cross-Cutting

- [x] T012 全量回归：全部测试套件运行，逐用例 diff T001 基线（分离 Failures/Errors），新失败必须归因；环境具备时完成晨曦真实 token 实测（quickstart 手动验证步骤 4，验证 R1 scp 格式假设），并在任务日志记录结果
- [x] T013 覆盖率核对：新增/修改代码覆盖率 ≥80%（不达标补测或记录豁免理由）——**豁免记录**：pom 无 jacoco/surefire 覆盖率配置（栈冻结,引入需宪法修订）；定性核验：11 个新用例覆盖两个新私有方法的全部分支（toScopeList 4 分支：List/空串/String/null；parseScopesFromCachedToken 5 路径：守卫/命中/未命中/损坏/缺 claim）+ 服务层与 IT 全分支,无未覆盖可达分支
- [x] T014 [P] 文档：`docs/knowledge/` 新增本特性流程文档并登记 `docs/INDEX.md`（仓库约定）——`docs/knowledge/ESI授权详情scopes-esi-auth-scopes.md` + INDEX 行,提交 a24484b
- [x] T015 [P] 安全自查（P6 评审前自检）：响应任何位置无 token/签名材料（FR-009）；日志无 token 内容；`parseScopesFromCachedToken` 异常分支不外泄内部细节；契约与实现一致（`contracts/user-accounts-api.md`）——grep 核验：EsiApiService 全部 token 相关日志仅固定文案+ID（7 处）；DTO 无凭据字段（仅注释提及）；catch 日志不含异常对象；两次评审代理独立复核通过

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (T001)** → **Foundational (T002→T003)** → **US1 (T003-T007)** → **US2 (T008-T010)** → **Polish (T011-T014)**
- 本特性 US1/US2 共享同一组合式实现（状态判定 + 解析），**顺序执行**；[P] 仅表示文件互斥冲突不存在

### Parallel Opportunities

- T003/T004 可与 T001 后并行（不同文件）
- T008/T009 可并行（EsiApiServiceTest vs 服务层测试/IT）
- T013/T014 可并行（文档 vs 自查）

## Implementation Strategy

- **MVP = US1**（T001→T007）：AUTHORIZED scopes 端到端可验，即可停下向用户演示
- **US2 补齐降级面**：全部 Edge Cases + FR-003/005/006 固化
- 每任务小步提交（`feat:`/`test:` 前缀），任务级评审按 P4 循环执行

## Testing & Quality Gates

- 单元覆盖 ≥80%；IT 覆盖关键旅程（contracts/user-accounts-api.md 全规则）
- 安全：响应无凭据材料；访问控制不变（`requireSelfOrRoot` 既有 IT/单测锚定）
- 性能：scopes 解析不进并行等待预算；p95 与基线持平（SC-002，以 IT/本地观测佐证）
