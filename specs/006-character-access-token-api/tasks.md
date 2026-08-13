---

description: "Task list for 角色 ESI AccessToken 查询接口"
---

# Tasks: 角色 ESI AccessToken 查询接口

**Input**: Design documents from `/specs/006-character-access-token-api/`
**Prerequisites**: spec.md ✅, plan.md ✅, contracts/api-contract.md ✅, 设计评审 ✅(BLOCK 已消化)

**Tests**: 宪法第一条 Test-First(NON-NEGOTIABLE)+ SC-004(覆盖率 ≥80%)。每个任务 RED 先于 GREEN。

**Organization**: Phase 1 加固地基(消化评审 C1/C2/H3),Phase 2~3 按用户故事(US1/US2),Phase 4 收尾。**Phase 1 必须先完成** —— 评审门禁要求 C1/C2 修完前不得实现端点。

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 可并行(不同文件,无未完成依赖)
- **[Story]**: 所属用户故事;Foundational/Polish 无此标签
- 描述含精确文件路径

## 路径约定

路径基于 `src/main/java/xyz/foolcat/eve/evehelper/`(下记 `~/`)、`src/main/resources/`、`src/test/java/xyz/foolcat/eve/evehelper/`(下记 `~test/`)。

**命令**:`./mvnw`(项目无 `mvn`)。
**回归基线**:动手前先跑一次全量测试记录 errors 数(约 267,属本地无法连 MySQL 的既有环境问题),后续对比该基线判断回归。

---

## Phase 1: Foundational(加固地基 — 消化评审 CRITICAL,阻塞后续全部阶段)

**Purpose**: 修复会被新端点放大的既有缺陷。评审明确要求这些修完前不得进入端点实现。

### T001 建立回归基线

- [x] 运行 `./mvnw test` 并记录 errors/failures 数到本任务备注,作为后续回归判据(不可只看"有无失败") — **基线(阶段⑦更正): Tests run 498, Failures 4, Errors 216**。初记的 `465/0/282` 是**坏配置下的伪基线** —— 当时 `application-test.yml` 的 springdoc `group-configs` 被 sed 改坏,所有 `@ActiveProfiles("test")` 上下文加载失败;修复后恢复 66 个测试。Errors 216 的真实根因是**测试库缺角色 `2112818290`(全部测试共用,即 `TaskConstant.CHARACTER_ID`)的有效 ESI 授权行**,非 DB 不可达。回归判据: 与同一组测试的 stash 前后逐用例对比,而非只比汇总数字

**AC**: 基线数字已记录。

### T002 [P] 修复 `queryAccountList` 漏 select `user_id`(评审 H3,FR-015)

- [x] **RED**: 在 `~test/infrastructure/persistence/mapper/system/EveAccountMapperSqlTest.java` 新增测试,用 `XMLMapperBuilder` 离线解析 `EveAccountMapper.xml`,断言 `queryAccountList` 的 SQL 含 `user_id` 列(参考既有 `BlueprintsMapperSqlTest` 的离线解析手法)
- [x] **GREEN**: 修改 `src/main/resources/mappers/system/EveAccountMapper.xml:319`,在 select 列表补 `user_id`

**AC**: 测试通过;`EveAccount.userId` 不再恒为 null。
**依据**: `BaseResultMap:18` 映射了 `user_id` 但 `queryAccountList` 未 select,导致 `EsiApiService:486` 写入 `esi_access_token:null:{cid}` 孤儿键。

### T003 [P] `persistRefreshedToken` 加 userId 空值防御(评审 H3 纵深)

- [x] **RED**: 在 `~test/infrastructure/external/esi/EsiApiServiceAccessTokenTest.java` 新增用例:`account.userId == null` 时**不写**缓存
- [x] **GREEN**: 修改 `~/infrastructure/external/esi/EsiApiService.java:485-488`,`account.getUserId() == null` 时跳过缓存写入并 `log.warn`

**AC**: 即便 T002 回退,也不会再产生跨用户共享的 null 键。

### T004 给 `getAccessToken` 补 per-(userId,characterId) 锁 + 5s 超时(评审 C2,FR-013)

- [x] **RED**: 在 `~test/infrastructure/external/esi/EsiApiServiceAccessTokenTest.java` 新增 3 个用例:
  - 抢不到锁(`setIfAbsent` 返回 false)时**不调用** `authorizeOAuth.updateAccessToken`,抛 `EsiException`
  - 持锁成功后正常刷新,`finally` 中释放锁(`verify(cacheGateway).delete(lockKey)`)
  - `updateAccessToken` 返回的 Mono 超时时抛异常而非无限挂起
- [x] **GREEN**: 修改 `~/infrastructure/external/esi/EsiApiService.java:183`:
  - 新增锁键常量 `ESI_ACCESS_TOKEN_LOCK_KEY`,锁键含 userId + characterId
  - 用 `cacheGateway.setIfAbsent` 抢锁,TTL 参照 `AUTH_STATUS_LOCK_TTL_SECONDS`(10s,覆盖 5s 超时 + DB 回写)
  - 持锁后 double-check 缓存(避免等锁期间已被填充)
  - `.timeout(ESI_REFRESH_TIMEOUT)` 补上
  - `finally` 释放锁

**AC**: 三个用例通过;既有 `EsiApiServiceTest`/`EsiApiServiceUnitTest` 不回归。
**依据**: 与 `determineAuthStatus:282-288,447` 同构。`:281` 注释已明写「refreshToken 一次性使用,并发刷新会使第二个请求用已失效旧 token」。

### T005 [P] 替换 3 处 `assert` 为显式空值检查(评审 M3,FR-019)

- [x] **RED**: 新增用例:`updateAccessToken` 返回空 Mono 时抛 `EsiException(ESI_SERVER_FAILURE)` 而非 NPE→500
- [x] **GREEN**: 修改 `~/infrastructure/external/esi/EsiApiService.java:184`、`:232`、`:234`,改为显式 null 检查抛 `EsiException`

**AC**: JVM 不带 `-ea` 时空值仍被拦截。

### T006 [P] 日志配置去除 token 明文(评审 C1,FR-014)

- [x] 修改 `src/main/resources/application-prod.yml:110`、`application-ali.yml:111`、`application-aliw.yml:134`:`log-impl` 由 `StdOutImpl` 改为 `org.apache.ibatis.logging.slf4j.Slf4jImpl`
- [x] 修改 `src/main/resources/application.yml:90`:`logging.level.web` 由 `debug` 改为 `info`
- [x] `application.yml:102` 的 `log-impl` 一并改 `Slf4jImpl`(公共配置)
- [x] 如需保留开发期 SQL 调试,仅在 dev/test profile 保留 StdOutImpl 并在 yml 注释说明"测试库无真实 token"

**AC**: prod/ali/aliw 三个 profile 不再通过 MyBatis 参数日志打印 `refresh_token`;`logging.level.web` 不再打印响应体片段。
**注**: FR-009 的最终验收需**实测日志输出**,非仅检查配置 —— 记入 T017。

### T007 [P] ESI 上游错误文案归一化(评审 M2,FR-020)

- [x] **RED**: 新增用例:上游返回 `invalid_grant:...` 时,异常 message 为固定文案而非上游原文
- [x] **GREEN**: 修改 `~/infrastructure/external/esi/auth/AuthorizeOAuth.java:88`,上游原文改为仅 `log.warn` 记录,抛出的 `EsiException` 用无 message 构造器(取 ResultCode 默认文案)

**AC**: 客户端收不到 ESI 内部错误细节;「本人角色但 token 已废」与归属失败文案一致。

**Checkpoint 1**: 地基加固完成,评审 C1/C2/H3/M2/M3 已消化,可进入端点实现。

---

## Phase 2: US1 — 获取本人角色的 AccessToken(Priority: P1)

**Goal**: 端点可用,能正确返回本人角色的 token 与过期时间。

### T008 [P] [US1] 创建领域读模型 `CharacterAccessTokenResult`

- [x] 创建 `~/domain/model/vo/CharacterAccessTokenResult.java`:record(String accessToken, Integer characterId, Long expiresIn),带 `@Schema` 注解

**AC**: 编译通过;风格与既有 `~/domain/model/vo/TokenResult.java` 一致。

### T009 [US1] `EsiGateway` 新增 `getAccessTokenWithExpiry` 端口方法(FR-018,评审 M1)

- [x] **RED**: 在 `~test/infrastructure/external/esi/EsiApiServiceAccessTokenTest.java` 新增用例:
  - 缓存命中:`expiresIn` 取 Redis TTL;TTL 返回 `-2`(键不存在)或 `-1`(无 TTL)时归一化为 `0`,**不透出哨兵值**
  - 缓存未命中:`expiresIn` 取自 `AuthTokenResponse.getExpiresIn()`
- [x] **GREEN**:
  - `~/domain/port/esi/EsiGateway.java` 新增 `CharacterAccessTokenResult getAccessTokenWithExpiry(Integer characterId, Integer userId) throws ParseException`
  - `~/infrastructure/external/esi/EsiApiService.java` 实现:归属校验 + 锁 + 超时(复用 T004 成果),缓存键构造留在本层

**AC**: 两个用例通过;缓存键格式不出 infrastructure 层。
**依据**: `CacheGateway.java:69-70` 约定 TTL 键不存在返回 -2、无 TTL 返回 -1。

### T010 [US1] 应用服务 `queryAccessToken`

- [x] **RED**: 扩展既有 `~test/application/service/CharacterApplicationServiceUnitTest.java`(避免重复测试类),用例:正常返回 `CharacterAccessTokenResult`;`ParseException` 被转为 `EveHelperException`
- [x] **GREEN**: 在 `~/application/service/CharacterApplicationService.java` 新增 `queryAccessToken(Integer characterId, Integer userId)`
  - 显式标注 `@Transactional(propagation = Propagation.NOT_SUPPORTED)` 或确保不被事务包裹(评审 M6)
  - **不得**沿用 `:34` 那种把凭证写进日志的模式(评审 L3)

**AC**: 用例通过;方法不在 DB 事务内发起 ESI 调用。

### T011 [US1] Controller 端点

- [x] **GREEN**: 新建独立 `~/interfaces/web/controller/CharacterAccessTokenController.java`(开关需隔离,不能与授权端点同类) `@GetMapping("/{characterId}/access-token")`
  - `@Hidden`(springdoc,FR-017)
  - `@Validated` + `@Positive @PathVariable Integer characterId`(FR-011,评审 M5)
  - 从 `UserUtil.getUserId()` 取当前用户
  - 返回 `Result<CharacterAccessTokenResult>`

**AC**: 端点可路由;不出现在 `/v3/api-docs`。

### T012 [US1] 配置开关(FR-017,评审 M4)

- [x] 用 `@ConditionalOnProperty(name = "eve.helper.debug.access-token-endpoint.enabled", havingValue = "true")` 控制端点注册(若加在既有 `CharacterController` 上会连带禁用授权端点,故须拆独立 Controller 或用其它隔离手段 —— 实现时确认)
- [x] `src/main/resources/application.yml` 新增 `eve.helper.debug.access-token-endpoint.enabled: ${ACCESS_TOKEN_ENDPOINT_ENABLED:false}`
- [x] `.env.example` 文档化该变量,注明"凭证查询端点,生产保持 false"

**AC**: 开关为 false(默认)时请求返回 404;为 true 时正常;既有 `POST /character/{code}` 授权端点不受开关影响。

**Checkpoint 2**: US1 完成,端点可用。

---

## Phase 3: US2 — 拒绝访问非本人所有的角色(Priority: P1,核心安全)

**Goal**: 5 类越权场景全部拒绝且响应不可区分。

### T013 [US2] 应用层显式 userId 门禁(FR-012,评审 H1)

- [x] **RED**: 在 `~test/application/service/CharacterApplicationServiceTest.java` 新增用例:`userId` 为 `null` / `0` / `-1` 时抛 `EveHelperException(ACCESS_UNAUTHORIZED)`,**且不调用** `esiGateway`
- [x] **GREEN**: 在 `CharacterApplicationService.queryAccessToken` 入口加 `if (userId == null || userId <= 0) { log.warn(...); throw new EveHelperException(ResultCode.ACCESS_UNAUTHORIZED); }`

**AC**: 三个用例通过。
**依据**: 当前 `getAccountOne(-1, cid)` 的拒绝仅依赖"库中无 user_id≤0 的行"这一数据巧合,DDL 无 `CHECK (user_id>0)`,`UserUtil.java:39-41` 对 Number 主体不校验正数。此任务把巧合变为显式门禁。

### T014 [US2] 越权场景测试(FR-002~FR-006,SC-005、SC-007)

- [x] **RED/GREEN**: 在 `~test/infrastructure/external/esi/EsiApiServiceAccessTokenTest.java` 新增用例,断言以下均抛 `EsiException(ESI_AUTHORIZATION_FAILURE)` 且**错误码与 message 完全一致**:
  1. 角色属他人(`getAccountOne` 抛 `USER_ACCOUNT_NOT_EXIST`)
  2. 角色不存在(同上,须与 1 断言同码同文案)
  3. 同军团他人角色(mock `getAccountOne` 抛异常 —— 验证走的是精确查询而非 corpId 放行)
  4. ROOT/ADMIN 但非本人角色(设置带 ADMIN 权限的 SecurityContext,断言仍拒绝)
- [x] 用例 4 需 `mockStatic(SecurityContextHolder)` 或设置真实 `SecurityContext`,`@AfterEach` 清理

**AC**: 4 个用例通过;用例 1 与 2 的断言字符串完全相同(不可区分性由测试固化)。

### T015 [US2] 审计日志(FR-016)

- [x] **GREEN**: 在 `CharacterApplicationService.queryAccessToken` 补审计日志:
  - 成功 `log.info("角色AccessToken查询成功: userId={}, characterId={}", ...)`
  - 归属失败 `log.warn("角色AccessToken查询越权: userId={}, characterId={}", ...)` —— 捕获 `EveHelperException` 记录后重抛
  - **禁止**记录 token 明文;需标识时用 `SensitiveDataMasker.maskToken`
- [x] 补 `EsiApiService:171-174` 的 catch:当前静默吞异常,补 `log.warn`

**AC**: 拒绝路径不再零审计(评审 H4);grep 确认无 token 明文输出点。

**Checkpoint 3**: US2 完成,核心安全约束由测试固化。

---

## Phase 4: US3 + Polish

### T016 [P] [US3] 验证 `Cache-Control: no-store`(FR-010,评审 L1)

- [x] **先实测**:确认 Spring Security 默认 `CacheControlHeadersWriter` 是否已全局提供该头(`SecurityConfig.java:51-69` 未 disable headers)
- [x] 若已提供:仅在契约文档记录,**不新增代码**;若未提供:用 `ResponseEntity` 显式设置
- [x] 确认错误响应也带该头

**AC**: 有实测证据,不盲目新增代码。

### T017 覆盖率与验证(SC-004、⑥阶段)

- [x] 运行新增测试类,确认全绿且可独立跑通(不依赖 Spring/DB)
- [x] 运行全量 `./mvnw test`,与 T001 基线对比 errors 数,确认无回归 — **结果: Tests run 502, Failures 4, Errors 216 = 更正后基线,无回归**。已用 `git stash push -u -- src/` 做确定性对比:stash 前后跑同一组失败测试,**逐用例、逐状态码完全一致**(`AssertsControllerTest.syncAssets:33`→500、`BlueprintsControllerTest.addBlueprintsList:35`→500、`getBlueprintsList:47`→500、`CharacterControllerTest.addCharacterAuth:34`→401)
- [ ] **实测** FR-009/FR-014:启动应用触发一次 cache-miss,确认日志中无 refreshToken/accessToken 明文
- [x] 逐条核对 spec 的 FR-001~FR-020 与 SC-001~SC-008
- [ ] `superpowers:verification-before-completion`

**AC**: 无回归;所有 AC 有证据。

### T018 评审(⑦阶段)

- [ ] `ecc:java-reviewer` 必审
- [ ] `ecc:security-reviewer` 复审,**重点复验**:C1 的日志实测、C2 的并发锁测试、US2 的不可区分性
- [ ] 评审记录归档 `docs/reviews/2026-08-11-006-character-access-token-code-review.md`
- [ ] CRITICAL/HIGH 全部修复后方可提交

### T019 遗留待办登记

- [x] 登记:限流、refreshToken 加密存储、`refresh_token varchar(64)` 加宽 DDL、`characterId` 改 Long、`ACCESS_UNAUTHORIZED` → 403、RBAC 既有 pattern 审计(H2) — **已登记至 spec.md「遗留待办」章节,共 L-1~L-9 九项;其中 L-6(RBAC pattern 审计)与 L-9(环境配置不入库)标注为上线前必做**

### T020 提交(⑧阶段)

- [ ] `./mvnw test` 全绿(以基线为准)后按 Conventional Commits 提交,关联 feature 编号
  > 示例:`feat: 角色 ESI AccessToken 查询接口(006 US1~US3)`

---

## 依赖关系

```
T001 (基线)
  ↓
Phase 1: T002/T005/T006/T007 [P] → T003 → T004 (锁,依赖 T003 的测试类)
  ↓ Checkpoint 1 (评审门禁)
Phase 2: T008 [P] → T009 (依赖 T004 的锁实现 + T008 的 record) → T010 → T011 → T012
  ↓ Checkpoint 2
Phase 3: T013 → T014 → T015
  ↓ Checkpoint 3
Phase 4: T016 [P] → T017 → T018 → T019 → T020
```

**关键约束**: Phase 1 未完成不得开始 Phase 2(评审门禁:C1/C2 修完前不得实现端点)。
