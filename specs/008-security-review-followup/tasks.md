# Tasks: 安全评审遗留修复(008)

**Input**: Design documents from `/specs/008-security-review-followup/`(plan.md / spec.md / research.md v2 / data-model.md / contracts/ / quickstart.md)
**Prerequisites**: plan.md(required)、spec.md(required)、research.md v2(9 项决策 R1~R9)、contracts/auth-response-contracts.md
**Feature Branch**: `008-security-review-followup`

**Tests**: 本项目宪法与 AI_WORKFLOW 强制 TDD(RED -> GREEN -> REFACTOR),**测试非可选**。每个实现任务内含「先写失败测试再实现」的 TDD 循环;测试文件路径与描述同列。

**Organization**: 按用户故事分阶段(US1 P1 -> US2 P2 -> US3 P3),每阶段可独立测试。`SensitiveDataMasker` 控制字符免疫为 US2+US3 共享前置,提至 Phase 2。

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 可并行(不同文件、无未完成依赖)
- **[Story]**: 所属用户故事(US1/US2/US3);Setup/Foundational/Polish 阶段无 Story 标签
- 描述含精确文件路径;每任务含 AC(验收标准)

---

## Phase 1: Setup

**Purpose**: 实现前基线锁定,供 SC-004 回归 diff

- [ ] T001 捕获实现前测试基线:运行 `mvn test`,记录 Failures 段与 Errors 段的**用例名清单**(非计数)到 `$CLAUDE_JOB_DIR/tmp/008-baseline.txt`,作为 SC-004 逐用例 diff 基线
  - **AC**: 基线清单含 Failures 段用例名 + Errors 段用例名各一组;按 memory `test-baseline-volatility` 规则,后续回归判定用逐用例 diff 而非固定阈值
  - **注意**: 按 memory `springboot-test-db-dependency`,MySQL/Redis 不可达导致的环境性失败须标记,不计入回归;若环境不可达,先打通环境再开实现

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: US2 与 US3 共享的安全卫生原语,须先于用户故事完成

**⚠️ CRITICAL**: US2 handler 重写依赖此阶段的 `SensitiveDataMasker` 控制字符免疫

- [ ] T002 [P] TDD `SensitiveDataMasker` 控制字符免疫(R5 共享原语;覆盖 FR-008/LOW-2s 的 masker 部分,US2 handler + US3 共享)
  - **RED**: 扩展 `src/test/java/xyz/foolcat/eve/evehelper/shared/util/SensitiveDataMaskerTest.java`,新增用例:`maskToken`/`maskUsername`/`maskEmail` 输入含 CR/LF/ESC/NUL(`\p{Cc}` 全类)时,输出剔除全部控制字符、仅保留可见掩码;合法输入(无控制字符)输出规则不变
  - **GREEN**: 在 `src/main/java/xyz/foolcat/eve/evehelper/shared/util/SensitiveDataMasker.java` 三个 mask 方法返回前剔除 `\p{Cc}`(抽取共享私有方法);剔除而非转义,不破坏既有日志消费方解析假设
  - **AC**: 含控制字符的 token/username/email 经 mask 后输出零控制字符;合法输入掩码行为与现状一致

**Checkpoint**: masker 原语就绪,US2 handler 可安全改用 `maskUsername`

---

## Phase 3: User Story 1 - 后台任务身份跨用户隔离 (Priority: P1) 🎯 MVP

**Goal**: 池化/复用工作线程执行新任务前不携带先前任务或提交线程身份;需提交者身份时显式传播并清理;定时任务匿名执行
**Independent Test**: SC-002 - 两用户任务先后复用同一工作线程,后者读到空身份;任务结束后身份恒空

### Implementation for User Story 1

- [ ] T003 [P] [US1] TDD `SecurityContextTaskDecorator`(R1)
  - **RED**: 新建 `src/test/java/xyz/foolcat/eve/evehelper/infrastructure/config/SecurityContextTaskDecoratorTest.java`,覆盖:① 提交线程有身份 -> 工作线程读到同一身份(快照传播);② 工作线程执行前 clearContext + 执行后 finally clearContext(双向清理);③ **同线程直通**:`Thread.currentThread()==submittingThread` 时不触碰上下文(CallerRunsPolicy 防护);④ 快照为 null(匿名/调度线程提交)-> 任务匿名执行;⑤ 快照为 `createEmptyContext()` 副本,提交线程后续变更身份不被工作线程观察(防串扰)
  - **GREEN**: 新建 `src/main/java/xyz/foolcat/eve/evehelper/infrastructure/config/SecurityContextTaskDecorator.java`(~30 行),实现上述五点语义
  - **AC**: 五条用例全绿;装饰器不依赖 `MODE_INHERITABLETHREADLOCAL`

- [ ] T004 [US1] TDD `AsyncConfiguration` 装配装饰器(R1 实现注记 1、2)
  - **RED**: 扩展/新建 `AsyncConfiguration` 测试,断言两个 `ThreadPoolTaskExecutor`(市场订单池 `EsiMarketOrderRequestExecutor`、ESI 状态池 `esiAuthStatusExecutor`)均在 `initialize()` **之前**调用 `setTaskDecorator`;含「`@Async("beanName")` 通路装饰生效」用例
  - **GREEN**: 修改 `src/main/java/xyz/foolcat/eve/evehelper/infrastructure/config/AsyncConfiguration.java`,两个线程池均 `setTaskDecorator(new SecurityContextTaskDecorator())` 且在 `initialize()` 前(若由 Spring 容器管理初始化,确保装配顺序)
  - **AC**: 两池均装配装饰器;`@Async` 与 `CompletableFuture.supplyAsync` 两条通路装饰均生效
  - **依赖**: T003

- [ ] T005 [P] [US1] TDD `SecurityConfig` 移除继承策略(R1)
  - **RED**: 测试断言 `SecurityContextHolder` 策略为默认 `MODE_THREADLOCAL`(非 `MODE_INHERITABLETHREADLOCAL`)
  - **GREEN**: 删除 `src/main/java/xyz/foolcat/eve/evehelper/infrastructure/config/security/SecurityConfig.java` 中 `setStrategyName(MODE_INHERITABLETHREADLOCAL)` 的 `@PostConstruct` 覆写块(:45-48)
  - **AC**: 策略回默认 `MODE_THREADLOCAL`;池化线程不再在创建时继承提交线程身份
  - **注意**: 本任务与 T009(US2)同改 `SecurityConfig.java`,T005 先于 T009(Phase 3 -> Phase 4)

- [ ] T006 [US1] 端到端回归 `ThreadPoolIdentityIsolationTest`(SC-002)
  - 新建 `src/test/java/xyz/foolcat/eve/evehelper/infrastructure/config/ThreadPoolIdentityIsolationTest.java`:复用同一工作线程先后执行用户 A(带身份)与用户 B(无身份需求)任务,断言 B 任务读到空身份;任务结束后工作线程身份恒空;CallerRuns 路径下请求线程自身身份不被抹掉(后续 `AccessGuard` 不 fail-closed)
  - **AC**: SC-002 通过率 100%;CallerRuns 路径可用性不回归
  - **依赖**: T003、T004、T005

- [ ] T007 [P] [US1] 任务提交点审计复核(spec edge case;research R1 实测结论的实现期复核)
  - grep 全仓 `SecurityContextHolder` 读取点 + `@Async`/`CompletableFuture.supplyAsync`/`@Scheduled` 提交点,逐一标注是否读身份、是否在请求线程;确认零任务依赖继承身份(对照 research R1 审计表);新增读取点须显式评估异步传播(评审检查项固化)
  - **AC**: 审计结论与 R1 表一致(零依赖继承);若有新发现读点,登记并评估
  - **产出**: 审计结论记入任务备注 / 评审记录

**Checkpoint**: US1 独立可测 - 工作线程零身份残留,CallerRuns 不破坏请求线程身份

---

## Phase 4: User Story 2 - 登录端点不可枚举 (Priority: P2)

**Goal**: 所有凭证类登录失败对外单一措辞;细节仅服务端日志;不回显内部异常
**Independent Test**: SC-001 - 「已注册+错误口令」与「未注册+任意口令」响应成对比对 100% 一致

### Implementation for User Story 2

- [ ] T008 [US2] TDD `AuthenticationFailureServletHandler` 重写(R2/R3 + LOW-7j + 评审 LOW-1 限流守护)
  - **RED**: 新建 `src/test/java/xyz/foolcat/eve/evehelper/infrastructure/config/security/handler/AuthenticationFailureHandlerEnumerationTest.java`,覆盖:① 账号存在+口令错 vs 账号不存在+任意口令 -> 响应消息+状态码 100% 一致(均「用户名或密码错误」/401);② 锁定账号同一措辞,无「剩余次数」;③ 无 `exception.getMessage()` 回显(LOW-7j 删除);④ 限流 Redis 故障时仍统一 401(try/catch 守护,不退化为 500);⑤ 服务端日志含 `maskUsername(username)` + 异常类名 + isLocked + remainingAttempts
  - **GREEN**: 重写 `src/main/java/xyz/foolcat/eve/evehelper/infrastructure/config/security/handler/AuthenticationFailureServletHandler.java`:全部 `AuthenticationException` 统一措辞「用户名或密码错误」+ 401 + `Result.failed` 结构;细节仅 `log.warn`(`maskUsername` + 类名 + locked + remaining);删 LOW-7j `getMessage()` 回显分支;`recordFailedAttempt`/`getRemainingAttempts`/`getLockRemainingTime` 调用 try/catch 守护(Redis 故障恒写出统一 401)
  - **AC**: SC-001 成对比对通过;FR-004/005/006 全满足;限流故障不破坏统一响应
  - **依赖**: T002(maskUsername 已免疫控制字符)

- [ ] T009 [P] [US2] `SecurityConfig` 保留 `hide=false` 并注释理由(R2 偏离论证)
  - 在 `src/main/java/xyz/foolcat/eve/evehelper/infrastructure/config/security/SecurityConfig.java` `setHideUserNotFoundExceptions(false)`(:75)处补注释:hide=true 时 `DaoAuthenticationProvider` 在 provider 层把 `UsernameNotFoundException` 转 `BadCredentialsException`,FR-005 存在性日志不可实现,故保留 false + handler 统一响应
  - **AC**: 注释固化偏离理由;无行为变更
  - **注意**: 同 `SecurityConfig.java`,须在 T005 之后

**Checkpoint**: US2 独立可测 - 登录失败响应零信息泄漏,细节仅日志

---

## Phase 5: User Story 3 - 安全卫生项批量整治 (Priority: P3)

**Goal**: 007 两份评审 12 条 LOW 项 + 设计评审 v2 纳入项(HIGH-1/MEDIUM-1/MEDIUM-2)逐条修复
**Independent Test**: SC-003 - 逐条对照评审记录建议处置完成,自检归档 `docs/reviews/`

### Implementation for User Story 3

- [ ] T010 [P] [US3] TDD `ResponseUtils` 删死方法 + 编码一致(R6 重写 + R7 重锚定)
  - **RED**: 修改 `src/test/java/xyz/foolcat/eve/evehelper/shared/util/ResponseUtilsTest.java`:删除 `writeTokenInfo` 用例;新增 `writeErrorInfo` 编码断言(`setContentType`+`setCharacterEncoding` 组合产出等价于 `application/json;charset=UTF-8`)
  - **GREEN**: 在 `src/main/java/xyz/foolcat/eve/evehelper/shared/util/ResponseUtils.java` 删除死方法 `writeTokenInfo`(:50,零调用方已核);`writeErrorInfo` 废弃 `";charset=UTF-8"` 拼接,改 `setContentType(MediaType.APPLICATION_JSON_VALUE)` + `setCharacterEncoding(StandardCharsets.UTF_8.name())`
  - **AC**: `grep -rn writeTokenInfo src/` 为空;`writeErrorInfo` 编码与全仓响应写出风格一致

- [ ] T011 [P] [US3] TDD token 响应头契约测试(R6 活路径钉死)
  - **RED**: 新建 `src/test/java/xyz/foolcat/eve/evehelper/infrastructure/config/security/TokenResponseHeadersContractTest.java`,断言登录成功(`AuthenticationSuccessServletHandler` 路径)与 refresh 成功(`AuthController` MVC 路径)响应:`Cache-Control` 含 `no-store`(Spring Security `CacheControlHeadersWriter` 默认)、无 `Access-Control-Allow-Origin`
  - **GREEN**: 无生产代码变更(框架默认头行为);仅契约测试钉死,防未来 `.headers(...)` 定制静默回退
  - **AC**: 登录成功/refresh 成功两活路径 `no-store` + 无 ACAO 断言通过;FR-007 语义受测保证

- [ ] T012 [P] [US3] TDD `RefreshTokenRequest` 边界校验(R4)
  - **RED**: 测试 `@Size(max=64)` 拒绝超长(如 10KB 载荷)、`@Pattern`(UUID 正则,与 `AuthApplicationService.UUID_PATTERN` 同源)拒绝非 UUID;合法 36 字符 UUID 通过
  - **GREEN**: 在 `src/main/java/xyz/foolcat/eve/evehelper/application/dto/request/RefreshTokenRequest.java` `refreshToken` 字段加 `@Size(max=64)` + `@Pattern`(UUID);`AuthController:52` 既有 `@Valid` 即边界生效
  - **AC**: 超长/非 UUID 载荷在 DTO 边界被拒,不触达 `AuthApplicationService`;合法 UUID 不误伤

- [ ] T013 [P] [US3] TDD `KeyStoreKeyFactory` 四处卫生(LOW-1j~4j)
  - **RED**: 修改 `src/test/java/xyz/foolcat/eve/evehelper/infrastructure/config/security/KeyStoreKeyFactoryTest.java`:删除单参 `getKeyPair(String alias)` 用例(LOW-1j);位数不足文案断言含 `MIN_RSA_MODULUS_BITS` 常量值(LOW-4j);`containsAlias` 检查在 try 块外(LOW-3j)
  - **GREEN**: 修改 `src/main/java/xyz/foolcat/eve/evehelper/infrastructure/config/security/KeyStoreKeyFactory.java`:① 删单参重载 `getKeyPair(String alias)`(:63-65);② `import java.security.Key`,局部变量改 `final Key key`(LOW-2j);③ `containsAlias` 提出 try 块,删 `catch(IllegalStateException e){throw e;}` 自重抛(LOW-3j);④ 「最低要求 2048 位」改由 `MIN_RSA_MODULUS_BITS` 拼接(LOW-4j)
  - **AC**: 死代码重载清零;文案随常量;自抛自捕分支消失;全测试绿

- [ ] T014 [P] [US3] TDD `LoginRateLimiterService` username 脱敏(R5 扩围 MEDIUM-2)
  - **RED**: 测试 `:42,53,55,83,119` 共 5 处日志经 `maskUsername` 脱敏,无原文 username
  - **GREEN**: 修改 `src/main/java/xyz/foolcat/eve/evehelper/domain/service/security/LoginRateLimiterService.java`,5 处原文 username 日志改 `SensitiveDataMasker.maskUsername`(domain -> shared 依赖方向合法)
  - **AC**: `/login` 路径限流日志零原文 username;CRLF 免疫(依赖 T002)

- [ ] T015 [P] [US3] TDD `GlobalExceptionHandler` BindException 日志加固(R9, HIGH-1)
  - **RED**: 新建 `src/test/java/xyz/foolcat/eve/evehelper/interfaces/web/advice/GlobalExceptionHandlerLogTest.java`,断言 `BindException` 分支日志含 objectName+field+defaultMessage+错误计数,**不含 rejected value**、不打印异常对象本体;级别 warn(非 error)
  - **GREEN**: 修改 `src/main/java/xyz/foolcat/eve/evehelper/interfaces/web/advice/GlobalExceptionHandler.java` `:48-49` BindException 分支:改结构化摘要日志,不回显 rejected value,不传异常对象;`log.error` -> `log.warn`;响应行为不变(400 + field->defaultMessage)
  - **AC**: 10KB/CRLF 载荷经 refresh 端点拒绝后日志零 rejected value;C-2 断言 4 可达成

- [ ] T016 [P] [US3] `DEPLOYMENT.md` 运维命令凭证暴露面(LOW-4s)
  - 修改 `docs/DEPLOYMENT.md` :593、:688、:691、:946-957 的 `redis-cli -a <密码>` 改用 `REDISCLI_AUTH` 环境变量(:566 已有该指导)
  - **AC**: `grep -n 'redis-cli.*-a' docs/DEPLOYMENT.md` 为空;凭证不出现在进程列表/shell 历史

- [ ] T017 [US3] 孤立 `public.key` 移除(LOW-3s / R8)-- **GATE: 须先经用户确认**
  - **前置**: ① 全仓 grep 复核 `src/`、`pom.xml`、`application*.yml` 零引用(计划期已完成,实现期复核);② **向用户确认后执行**
  - 执行 `git rm src/main/resources/public.key` + 提交;**不改写 git 历史**(FR-014 同精神)
  - **AC**: `public.key` 从版本控制移除;历史保留;全仓零引用
  - **⚠️ 用户确认门**: 未获确认前不得执行 git rm

**Checkpoint**: US3 独立可测 - 12 LOW + v2 纳入项逐条闭环

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: 跨故事决策、文档同步、验证与评审

- [ ] T018 LOW-D 处置决策(plan 风险 #6;`GlobalExceptionHandler:101-105` `MethodArgumentTypeMismatchException` 分支同类 CWE-117)
  - **二选一**(决策记入任务备注):
    - ① 顺手推广 R9「不回显外部输入」原则到该分支:结构化摘要日志 + 测试,归入 T015 同次提交
    - ② 登记为后续 polish:在 `docs/reviews/` 记录残余项,本 feature 不改
  - **AC**: 决策有记录;若选 ① 则该分支日志零原始输入且有测试;若选 ② 则残余项登记可追溯
  - **注意**: 该分支仅认证后可达,暴露面低于 HIGH-1,二选一皆可接受

- [ ] T019 [P] MEDIUM-3 文档同步(R4;L2 观测输入语义变化)
  - 在 `specs/007-jwt-key-rotation/tasks.md` 或相关 007 文档补注记:DTO 边界拒绝(`@Size`/`@Pattern`)发生在 `AuthApplicationService` 之前,非 UUID 洪泛不再触达 `observeInvalidRefresh()`,L2 计数器对该向量归零;L2 此后只覆盖「UUID 形状但无效」探针;不改 `RefreshRateLimiterService` 代码(008 范围外)
  - **AC**: 007 文档对 L2 语义的描述与 008 实际行为一致

- [ ] T020 [P] 执行 `quickstart.md` 验证(SC-001~006)
  - SC-001: 「已注册+错口令」与「未注册+任意口令」curl 成对比对 100% 一致
  - SC-002: 线程复用零身份残留(T006 端到端用例)
  - SC-003: 12 LOW + v2 纳入项逐条对照评审记录核对表(含 `grep -rn writeTokenInfo src/` 为空、`grep -n 'redis-cli.*-a' docs/DEPLOYMENT.md` 为空、日志三层覆盖)
  - SC-004: `mvn test` 全量,Failures 段逐名 diff 对照 T001 基线(Errors 段环境性失败不计)
  - SC-005: 变更代码行覆盖率 ≥80%
  - SC-003 自检记录归档 `docs/reviews/`
  - **AC**: SC-001~005 全通过;自检记录归档

- [ ] T021 独立安全复审(SC-006)
  - 调用 `ecc:security-reviewer` 对全部变更评审;归档 `docs/reviews/2026-08-XX-008-security-reviewer.md`
  - **AC**: 零 CRITICAL/HIGH;残余项登记

- [ ] T022 最终 `mvn test` 全绿 + 提交
  - `mvn test` 全绿(Failures 段逐名 diff 零新增);敏感核验 `git status --porcelain | grep -Ei 'application-.*\.yml|\.jks|\.env|\.p12'` clean;按 Conventional Commits 提交,关联 feature 008
  - **AC**: 测试全绿;敏感文件零入库;提交规范

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: 无依赖,T001 先行
- **Foundational (Phase 2)**: T002 无依赖;**阻塞 US2**(T008 依赖 maskUsername 免疫)
- **US1 (Phase 3)**: T003 -> T004 -> T006;T005、T007 可与 T003 并行
- **US2 (Phase 4)**: T008 依赖 T002;T009 依赖 T005(同文件)
- **US3 (Phase 5)**: T010~T016 互不冲突可并行;T017 须用户确认;T014 依赖 T002
- **Polish (Phase 6)**: T018 可在 T015 后任意时点;T019 独立;T020 依赖全部实现完成;T021 依赖 T020;T022 依赖 T021

### User Story Dependencies

- **US1 (P1)**: 无跨故事依赖,Foundational 后即可开始
- **US2 (P2)**: 依赖 T002(masker 原语);与 US1 仅 T009/T005 同文件顺序约束
- **US3 (P3)**: T014 依赖 T002;其余独立;T017 用户确认门

### Parallel Opportunities

- Phase 2:T002 独立
- Phase 3:T003、T005、T007 可并行(不同文件);T004 串行于 T003 之后
- Phase 4:T008、T009 顺序受 T005 约束,否则可并行
- Phase 5:T010、T011、T012、T013、T014、T015、T016 互不冲突,可全并行(7 路);T017 用户确认门独立
- Phase 6:T018、T019 可并行;T020~T022 串行

---

## Parallel Example: User Story 3

```bash
# 7 路并行(不同文件,无冲突):
Task T010: "ResponseUtils 删死方法 + 编码一致" (shared/util/ResponseUtils.java)
Task T011: "TokenResponseHeadersContractTest 钉死活路径" (新增测试)
Task T012: "RefreshTokenRequest @Size+@Pattern" (application/dto/request)
Task T013: "KeyStoreKeyFactory 四处卫生" (infrastructure/config/security)
Task T014: "LoginRateLimiterService 脱敏" (domain/service/security)
Task T015: "GlobalExceptionHandler 日志加固" (interfaces/web/advice)
Task T016: "DEPLOYMENT.md REDISCLI_AUTH" (docs/)
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. T001 基线 -> T002 原语 -> T003~T007 完成 US1
2. **STOP and VALIDATE**: SC-002 线程复用零残留 + CallerRuns 不破坏请求线程身份
3. US1 独立交付「跨用户身份泄漏面消除」价值

### Incremental Delivery

1. Setup + Foundational -> masker 原语就绪
2. US1 -> SC-002 验证 -> 交付(MVP)
3. US2 -> SC-001 成对比对 -> 交付
4. US3 -> SC-003 逐条核对 -> 交付
5. Polish -> SC-004~006 -> 评审 -> 提交

---

## Testing & Quality Gates

### Test Coverage Requirements

- 变更代码行覆盖率 ≥80%(SC-005)
- US1:装饰器单元 + 端到端线程复用回归(SC-002)
- US2:登录失败成对比对端到端契约(SC-001)
- US3:逐条 LOW 项测试映射(SC-003)
- 回归:`mvn test` Failures 段逐名 diff 零新增(SC-004,基线环境性失败不计)

### Quality Assurance

- 每任务 TDD:RED(失败测试) -> GREEN(最小实现) -> REFACTOR
- 每任务完成后 `mvn test` 相关用例绿
- 敏感文件核验:提交前 `git status --porcelain | grep -Ei 'application-.*\.yml|\.jks|\.env|\.p12'` clean;`.claude/settings.local.json` 按惯例排除
- 独立安全复审零 CRITICAL/HIGH(SC-006)

## Notes

- [P] = 不同文件、无未完成依赖
- [Story] 标签映射用户故事可追溯性
- T017 `public.key` 删除须用户确认(spec edge case),不改写历史
- T018 LOW-D 二选一决策须记录
- T019 MEDIUM-3 同步 007 文档 L2 语义描述
- 测试基线按 memory `test-baseline-volatility` / `regression-diff-must-separate-failures-errors` 规则:逐用例 diff,Failures/Errors 分段,不用固定阈值
- 生产密钥材料(`eve-helper.jks`/`test-only.jks`/`eve-jwt.jks`)由用户管理,AI 不触碰;`application-{test,ali,aliw,prod}.yml` gitignored 不读不改
