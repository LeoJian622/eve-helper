---
description: "Task list for 007-jwt-key-rotation"
---

# Tasks: JWT 签名密钥轮换(007-jwt-key-rotation)

**Input**: Design documents from `/specs/007-jwt-key-rotation/`
**Prerequisites**: plan.md(v3,第三轮评审有条件 APPROVE,放行条件已闭合)、spec.md(第三次修订)

**Tests**: 项目宪法强制 TDD(AI_WORKFLOW §2),所有实现任务前先写失败测试。

**Organization**: 按用户故事分组。US1(干净登出/401 链路)、US2(keystore 外置)、US3(清空 refresh + 审计,主体为用户运维动作)。

**基线**: 509 tests / Failures 4 / Errors 219(2026-08-11 21:47,序 0 后实测)。回归门禁用**同环境逐用例 diff**,不用固定 Errors 阈值。

**分支**: 当前无 007 分支(L-7),工作在 006 分支上 —— T001 处理。

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 可并行(不同文件、无未完成依赖)
- **[Story]**: US1/US2/US3;Setup/Foundational/Polish 无标签

---

## Phase 1: Setup

**Purpose**: 分支与版本控制就绪

- [x] T001 创建 feature 分支 `007-jwt-key-rotation`(当前在 `006-character-access-token-api` 上,评审 L-7)。是否把已有 007 文档提交迁移过来由用户定;不迁移则从当前 HEAD 建分支
  - AC: `git branch --show-current` = `007-jwt-key-rotation`;spec/plan/tasks 在工作区可见 ✅(2026-08-12,用户确认从 006 HEAD 建分支,文档随工作区带入)
- [x] T002 更新 `.specify/feature.json` 的 `feature_directory` 为 `specs/007-jwt-key-rotation`(现指向 005,stale,导致 speckit 脚本解析错误)
  - AC: setup-tasks 脚本解析到 007 目录 ✅
- [x] T003 `git add` 序 0 产物:`.gitignore`(反向规则)、`src/main/resources/test-only.jks`、spec/plan/tasks、`application-prod.yml.example`、`docs/reviews/2026-08-11-007-*`(三个评审记录)。按 Conventional Commits 提交,关联 007(评审 L-6:test-only.jks 的存在意义就是入库,未提交则「新克隆直跑」不成立)
  - AC: `git ls-files | grep test-only.jks` 命中;提交信息含 feature 编号 ✅(commit `0e5b942`,8 文件;.claude/settings.local.json 按惯例排除)

---

## Phase 2: Foundational(阻塞 US1)

**Purpose**: 白名单共享匹配组件 —— FR-016 与 FR-020 的共同前提(CRITICAL-1)。**US2 不依赖本阶段,可并行启动**

**⚠️ CRITICAL**: US1 的任何实现任务不得先于本阶段完成

- [x] T004 白名单契约测试(RED)`src/test/java/xyz/foolcat/eve/evehelper/infrastructure/config/security/WhiteUrlMatcherContractTest.java`:①`WhiteUrlMatcher` 与 `RbacAuthorizationManager` 白名单判定对同一组 method+URI 输入结果一致;②`POST:/auth/tokens` 在白名单时命中;③大小写/尾斜杠不匹配(精确字符串语义固化)
  - AC: 测试编译失败或断言失败(WhiteUrlMatcher 尚不存在)= RED ✅(2026-08-12,`./mvnw test -Dtest=WhiteUrlMatcherContractTest` 编译失败 `cannot find symbol: class WhiteUrlMatcher`;含①参数化一致性 8 例、②配置驱动命中/移除 2 断言、③变体不命中 5 例,另加 null/空列表与 OPTIONS 有意分歧边界用例)
- [x] T005 实现 `WhiteUrlMatcher` `src/main/java/xyz/foolcat/eve/evehelper/infrastructure/config/security/WhiteUrlMatcher.java`:`isWhiteListed(HttpServletRequest)`,语义 = `method + ":" + getRequestURI()` 精确相等,数据源 `EveHelperSecurityConfig.getWhiteUrlList()`,null 列表安全
  - AC: T004 转 GREEN ✅(2026-08-12,契约测试 17/17 通过;期间修复两处测试侧问题:`getAuthorities()` 通配泛型改 `doReturn`、CSV 前导空格用例加引号防修剪,实现本身一次通过)
- [x] T006 重构 `RbacAuthorizationManager.java:67-73`:内联白名单 stream 替换为 `whiteUrlMatcher.isWhiteListed(request)`(单一事实来源)。OPTIONS 短路(:56-58)保持原样,并加注释说明与 filter 对 OPTIONS 的语义分歧是**有意的**(round3 §1.3)
  - AC: T004 契约测试仍 GREEN;`POST:/user` 匿名注册行为不变(既有用例 diff 无新增失败)✅(2026-08-12,重构后契约测试 17/17 GREEN;顺带移除已无引用的 `eveHelperSecurityConfig` 字段,构造签名变更仅契约测试手动构造处同步;行为等价性由契约测试「重构前 GREEN(对照内联实现)→ 重构后 GREEN(委托实现)」双向证明)
- [x] T007 回归验证:跑白名单相关既有用例 + 全量用例级 diff 快照存档(作为 US1 改造前基准)
  - AC: 无新增失败用例名;diff 基准文件留存于评审记录或 tasks 附录 ✅(2026-08-12,全量 526/F4/E218/S2,见附录 A)

**Checkpoint**: 白名单组件就绪,US1 可以开始

### 附录 A:T007 全量回归快照(US1 改造前基准,2026-08-12 01:00)

**汇总**:526 / Failures 4 / Errors 218 / Skipped 2(序 0 基线 509/F4/E219/S2;+17 为本阶段新增契约测试全 GREEN,E218 较基线 −1,属允许方向)

**Failures(4,与基线名单完全一致)**:
- `AssertsControllerTest.syncAssets`(500,需真实登录态)
- `BlueprintsControllerTest.addBlueprintsList`(500)
- `BlueprintsControllerTest.getBlueprintsList`(500)
- `CharacterControllerTest.addCharacterAuth`(401,基线即 401 —— T019 特别核对项)

**Errors(218)顶层异常分布(全部为已知外部依赖/既有类别,无新类别)**:
| 顶层异常 | 数量 | 类别 |
|---|---|---|
| `EveHelperException`(访问未授权) | 204 | 测试库缺角色 2112818290 的 ESI 授权数据 |
| `FileNotFoundException` | 9 | 缺 `esi-mock-data/*.json` mock 文件 |
| `EsiException`(ESI 未授权/过期) | 2 | `AuthorizeOAuthTest`,同上数据缺失 |
| `InvalidCookieException`(TOKEN过期) | 2 | `JwtFilterDiagnosticTest` —— **既有异常逃逸现状,即 T008/T013 要修的 RED 态** |
| `BadSqlGrammarException`(Unknown column 'character') | 1 | `WalletJournalServiceTest.countBoundsReturn`,SQL 语法既有问题 |

**关键阴性证据**:`Failed to load ApplicationContext` = 0;全部报告无 NPE、无涉及 `WhiteUrlMatcher`/`RbacAuthorizationManager` 的异常。

**US1 回归门禁(T019/T031 对照本快照)**:Failures 名单只允许不变或减少;Errors 只允许「原 ERROR 转 PASS」;`JwtFilterDiagnosticTest` 的 2 个 InvalidCookieException 在 T013 后应消失(被 T008 改写替代)。

---

## Phase 3: User Story 1 - 密钥轮换后旧会话干净终止 (Priority: P1) 🎯 MVP

**Goal**: 旧 token → HTTP 401 + `AUT00210`(非异常逃逸);`POST /auth/tokens` 在白名单下带旧/无效 token 也能到达 controller 并返回明确业务错误;洪泛有检测 + 速率整形

**Independent Test**: 用旧密钥签发的 token 请求受保护端点得 401;带该 token 请求 `/auth/tokens` 到达 controller

### Tests for User Story 1(先写,必须失败)⚠️

- [x] T008 [P] [US1] `src/test/java/.../web/JwtAuthFailureResponseTest.java`(由 `JwtFilterDiagnosticTest` 改写):验签失败 → **401** + body code `AUT00210`;过期 token → 401;黑名单 token → 401;响应体不含 token 片段;带旧 token 请求**非白名单**端点 → 401(AC#3)
  - AC: 当前失败(现状是异常逃逸/400)= RED ✅(2026-08-12,3 例全 ERROR:`InvalidCookieException` 异常逃逸;诊断测试文件已随改写删除,git rm 已暂存)
- [x] T009 [P] [US1] `src/test/java/.../web/RefreshTokenEndpointAccessTest.java`:带旧签名 token / ParseException 格式非法 token 请求 `POST /auth/tokens` → **到达 controller**(断言 controller 被调用,AC#1/#2);无 token → 到达 controller;refresh token 不存在 → 明确业务错误(400 + 「无效或已过期」)
  - AC: 当前失败(filter 拦截)= RED ✅(2026-08-12,3 例全失败:2 例 InvalidCookieException 逃逸 + 1 例 401 AUT00201「用户未登录」未达 controller;「到达 controller」判据 = 响应含仅由 AuthApplicationService:116 生成的「无效或已过期」消息)
- [x] T010 [P] [US1] `src/test/java/.../domain/service/security/RefreshRateLimiterTest.java`:L2 超阈值 → 响应被**固定延迟**且仍返回业务错误(**断言非硬拒**);成功路径无延迟;Redis 故障 fail-open(不新增失败);L1 计数存在但**无任何锁定动作**
  - AC: 实现不存在 → RED ✅(2026-08-12,编译失败 `cannot find symbol: RefreshRateLimiterService`;T016 实现后 6/6 GREEN)
- [x] T011 [P] [US1] 更新既有 `ResponseUtilsTest`:`TOKEN_ACCESS_EXPIRED` 断言从 400 改为 **401**;`Content-Type` 断言含 `charset=UTF-8`
  - AC: 当前失败(现落 default → 400)= RED ✅(2026-08-12,2 例失败:`expected 401 but was 400`、Content-Type 无 charset;T012 落地后 4/4 GREEN)

### Implementation for User Story 1

- [x] T012 [US1] `ResponseUtils.java:26-35`:switch 增 `TOKEN_ACCESS_EXPIRED` → 401 分支;`setContentType` 补 `charset=UTF-8`;`isCommitted()` 守卫由调用方负责(见 T013)
  - AC: T011 GREEN ✅(2026-08-12,ResponseUtilsTest 4/4;注释写明统一 AUT00210 为防信息泄露有意决策 LOW-2)
- [x] T013 [US1] `JwtAuthorizationTokenFilter.java:52-109` 重写为 `rejectOrPass` 形态(plan §5 伪码):**全部 5 个失败出口**(验签/过期/黑名单/ParseException/JOSEException)统一处理 —— 白名单路径匿名放行(`filterChain.doFilter`,不写 SecurityContext),非白名单直写 401 + return;写响应前 `response.isCommitted()` 守卫;`log.error(...,e)` 降级为 `log.warn` + 只打异常类名;不再抛 `InvalidCookieException`。注释写明「统一返回 AUT00210 是防信息泄露的有意决策」(LOW-2)
  - AC: T008、T009 GREEN ✅(2026-08-12,注入 WhiteUrlMatcher;5 出口统一 rejectOrPass;LOW-2 注释已写入方法与类 javadoc)
- [x] T014 [US1] `src/main/resources/application.yml:122-124`:`whiteUrlList` 增 `POST:/auth/tokens`
  - AC: 启动后 `RbacAuthorizationManager` 白名单含该项(test profile 实测端点可达)✅(2026-08-12,同步 `application-test.yml` 与 `application-prod.yml.example`(序 0 已含)—— List 属性 profile 整体覆盖(HIGH-1/SC-016);⚠️ ali/aliw/prod 真实文件不入库,T033 人工核验)
- [x] T015 [US1] `AuthApplicationService.refreshToken:114-119`:③ `hasKey` + ④ `get` 合并为单次 `get`(null 即无效),消除 TOCTOU;`:158` 撤销前权威校验保留(M-5)
  - AC: T009 仍 GREEN;`TokenServiceTest` 相关用例无新增失败 ✅(2026-08-12,合并后经 `getUserIdFromRefreshToken` 单次 get;`:158` 权威校验未动;调用方归零的 `isRefreshTokenValid` 已删,单测桩同步)
- [x] T016 [US1] 实现 `RefreshRateLimiterService` `src/main/java/.../domain/service/security/RefreshRateLimiterService.java`:L1 键 `refresh:fail:{userId}`(TTL 60s,仅告警,**禁止锁定**);L2 键 `refresh:invalid:global`(固定窗口 INCR + 每次 INCR 重设 EXPIRE;超阈值 → Prometheus 计数器告警 + 失败路径固定延迟 100–300ms;Redis 异常 try-catch fail-open;成功路径零影响)。依赖 `CacheGateway` 端口,不直连 Redis(DDD 分层)
  - AC: T010 GREEN ✅(2026-08-12,6/6;阈值 L2_THRESHOLD=1000/60s 窗口集中定义于常量;延迟 100~300ms 区间随机防同步重试风暴;指标名 `eve.helper.refresh.invalid.flood`)
- [x] T017 [US1] 接线:`AuthApplicationService.refreshToken` 失败路径(②/③ 阶段)调用 L2 计数 + 延迟;④ 之后失败调 L1 计数;成功路径不调用
  - AC: T010、T009 GREEN;阈值常量集中定义(不硬编码散落)✅(2026-08-12,②格式非法+③token无效 → L2;用户不存在 → L1(userId);成功路径零调用;阈值集中在 RefreshRateLimiterService 常量;单测增接线断言)
- [x] T018 [US1] `AuthenticationFailureServletHandler.java:69-70`:清理 `InvalidCookieException` 死分支(filter 不再抛该异常)
  - AC: 编译通过;登录失败路径既有用例无新增失败 ✅(2026-08-12,分支与 import 已删,留注释说明删除原因)
- [x] T019 [US1] US1 回归:全量 `./mvnw test` 与 T007 基准做**用例名集合 diff**;特别核对 `CharacterControllerTest.addCharacterAuth`(基线即 401,断言不得因本改动漂移)
  - AC: 无新增失败用例;4 个基线 Failures 名单不变 ✅(2026-08-12,**535/F4/E216/S2**:总数 = 附录 A 526 − 3(删除的诊断测试)+ 12(US1 新测试)算术吻合;Failures 4 例名单与基线完全一致,addCharacterAuth 仍 401 未漂移;Errors 216 = 218 − 2(删除的诊断测试错误,附录 A 已预言);顶层异常 204 EveHelperException + 9 FileNotFound + 2 EsiException + 1 BadSqlGrammar,无新类别;context 加载失败 0;旧 surefire 报告中的 2 个 InvalidCookieException 为删除前残留文件,非本轮产物)

**Checkpoint**: US1 独立可验 —— 旧 token 得 401,refresh 端点可达,洪泛有延迟整形

---

## Phase 4: User Story 2 - keystore 不再随源码分发 (Priority: P1)

**Goal**: 生产 keystore 从文件系统绝对路径加载;classpath 加载仅限 test profile(fail-closed);生产 jar 不含生产 keystore

**Independent Test**: `KEYSTORE_LOCATION` 指向文件系统路径可启动;指向 classpath + 生产 profile → 拒绝启动

### Tests for User Story 2(先写,必须失败)⚠️

- [ ] T020 [P] [US2] `src/test/java/.../infrastructure/config/security/KeyStoreKeyFactoryTest.java`:别名不存在 → 错误信息含「别名」;口令错 → 信息**不含口令**;文件不存在 → 明确路径;RSA <2048 → 拒绝;**PKCS12 与 JKS 两种物理格式 fixture 均可加载**(M-4);InputStream 关闭(无资源泄漏)
  - AC: 对旧实现的断言失败 = RED
- [ ] T021 [P] [US2] `src/test/java/.../infrastructure/config/security/SecurityBaselineValidatorTest.java`:4 项基线各自违规 → 拒绝启动;profile 缺失/未知 → **按生产校验(fail-closed)**;`test` profile → 允许 classpath;**生产 profile + `classpath:test-only.jks` → 拒绝启动**(round3 §3.1 指定用例)
  - AC: Validator 不存在 → RED

### Implementation for User Story 2

- [ ] T022 [US2] 重写 `KeyStoreKeyFactory.java`(FR-023):去除双重 synchronized 与可变 store 字段;try-with-resources;异常分类(文件不存在/口令错/别名错/非 RSA)各给独立信息且**不含口令**;加载后断言 RSA modulus ≥ 2048;支持 PKCS12 与 JKS 双格式(不得破坏 DualFormat 现状)
  - AC: T020 GREEN
- [ ] T023 [US2] `KeyPairConfig.java:43-51`:`classpath:` 前缀 → ClassPathResource,否则 FileSystemResource;加载前存在性检查,fail-fast 拒绝启动并指明缺失项;日志只打文件名不打完整路径(LOW-5)
  - AC: T021 部分 GREEN(加载路径分派正确)
- [ ] T024 [US2] `SecurityProperties.java:26`:移除 `location` 默认值 `"eve-jwt.jks"`(H4a)
  - AC: 未配 location 时启动报「缺失配置」而非静默用默认
- [ ] T025 [US2] 实现 `SecurityBaselineValidator` `src/main/java/.../infrastructure/config/security/SecurityBaselineValidator.java`(`ApplicationRunner`,fail-closed 正向白名单):校验 `log-impl` / `logging.level.web` / `access-token-endpoint.enabled` / `KEYSTORE_LOCATION` 四项;**仅当 profile 明确属于 `{test}` 才允许 classpath**,未知/缺失按生产处理(006 L-10 合并实现)
  - AC: T021 GREEN
- [ ] T026 [P] [US2] 配置模板:`src/main/resources/application.yml:117` `${KEYSTORE_ALIAS}` → `${KEYSTORE_ALIAS:eve-jwt}`;`.env.example` 补 `KEYSTORE_LOCATION`、`KEYSTORE_ALIAS`;`application-prod.yml.example` 补 `security.keystore` 段(location 用 `/etc/eve-helper/eve-jwt.jks` 占位 + 口令为 `${KEYSTORE_PASSWORD}`)
  - AC: 模板中无任何真实口令;环境变量名与 `SecurityProperties` 一致
- [ ] T027 [US2] SC-003 验证:`./mvnw clean package` 后 `unzip -l target/*.jar | grep '\.jks'` 断言**仅含 `test-only.jks`**(此时 eve-jwt.jks 仍在库中 → 预期失败,RED 保留至 T028)。记录实测输出
  - AC: 验证脚本/命令可重复执行;RED 状态留证

**Checkpoint**: US2 独立可验 —— 生产可从文件系统加载、fail-closed 生效、jar 内容可断言

---

## Phase 5: User Story 3 - 清空 refresh token 与审计 (Priority: P1)

**Goal**: 代码侧清理就绪;轮换/清空/审计的执行与结论由用户完成(运维动作,AI 不代执行)

**Independent Test**: `git ls-files | grep eve-jwt.jks` 为空;DEPLOYMENT.md 含全部清单

- [ ] T028 [US3] **FR-012(有门禁)**:`git rm src/main/resources/eve-jwt.jks`。⚠️ 前置条件(先验证再执行):①`application-aliw.yml`(不入库,用户提供证据)的 `security.keystore.location` 已改为文件系统绝对路径;②新生产密钥已按 `docs/DEPLOYMENT.md` 部署到 `/etc/eve-helper/`(权限 600/目录 700,SC-009 留证)。前置不满足则**停止并报告**,不得执行
  - AC: 前置证据齐备;`git rm` 后 T027 转 GREEN(jar 仅含 test-only.jks)
- [ ] T029 [US3] `docs/DEPLOYMENT.md` 轮换章节:轮换步骤(keytool 命令用占位符口令)+ 六表审计清单(SQL + 判断方法,结论留空由用户填)+ 公告模板(Q5)+ `refresh_token:*` 清空前后计数记录项(SC-008)+ **新生产密钥指纹 ≠ test-only.jks 指纹 `FD:9F:19:27:61:...:CA:0F:B4` 核对项**(L-5)+ 「旧密钥已泄露」声明(SC-007);全文不得含真实口令
  - AC: 对照 SC-007/008/013 逐项可勾选;无口令明文
- [ ] T030 [US3] `specs/006-character-access-token-api/spec.md` 的 L-10 条目:标注「已由 007 实现(SecurityBaselineValidator)」
  - AC: 006 spec 无悬空待办

**Checkpoint**: 代码交付完成;生产轮换窗口由用户按 DEPLOYMENT.md 执行(SC-001/006/014 人工验证)

---

## Phase 6: Polish & Cross-Cutting

- [ ] T031 [P] 全量回归:`./mvnw clean test`,与序 0 基线(509/F4/E219)做用例级 diff;`target/classes/` 确认无 `jwt.jks` 类残留文件
  - AC: 无新增失败用例名
- [ ] T032 [P] 变异测试五项(plan §6):删 `ResponseUtils` 新 case → 401 断言失败;删 filter 白名单放行分支 → T009 失败;删 filter `return` → 有测试捕获;fail-closed 改 fail-open → T021 失败;L2 改硬拒(503)→ T010 失败
  - AC: 五项变异全部被既有测试捕获,记录结果
- [ ] T033 人工核验清单(用户执行,留证):SC-011(生产 profile 口令均为环境变量)+ SC-016(生产 profile whiteUrlList 含 `POST:/auth/tokens` 或未定义)+ SC-006 在**生产 profile 实际配置**下验收 + SC-009(stat 权限)
  - AC: 四项核验记录归档至 `docs/reviews/` 或 DEPLOYMENT.md
- [ ] T034 评审记录收尾:`docs/reviews/` 确认三轮评审 + 本轮 tasks 执行记录齐全(AI_WORKFLOW 要求评审归档)
  - AC: docs/INDEX.md(如有)或目录自洽

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: 无依赖
- **Foundational (Phase 2)**: 依赖 Phase 1;**阻塞 US1**
- **US2 (Phase 4)**: 仅依赖 Phase 1 —— 可与 Phase 2/US1 **并行**(不碰过滤器链)
- **US1 (Phase 3)**: 依赖 Phase 2(WhiteUrlMatcher)
- **US3 (Phase 5)**: T028 依赖 US2 完成(aliw 切路径是 FR-012 前置);T029/T030 可提前
- **Polish (Phase 6)**: 依赖 US1+US2 代码完成

### 关键串行链(CRITICAL,防互相拆台)

```
T004/T005(WhiteUrlMatcher)→ T006(Rbac 重构)→ T013(filter 重写)→ T014(加白)→ T017(限流接线)
```
T013 先于 T005/T006 落地 = CRITICAL-1 复现(401 与加白互拆),禁止。

### Within Each User Story

- 测试先写且必须失败(T008~T011、T020~T021 为 RED 起点)
- 共享组件先于消费方;接线后必须回归

### Parallel Opportunities

- T008/T009/T010/T011(US1 四个测试文件)互相并行
- T020/T021(US2 测试)并行;T026 与 T022~T025 并行
- **团队级**:US1 链与 US2 链可两人同时推进(无文件交集,除 `application.yml` —— T014 与 T026 都改它,串行处理该行)

---

## Implementation Strategy

### MVP(US1 + US2 代码就绪)

1. Phase 1 → Phase 2 → Phase 3(US1)→ 验证 401 链路
2. Phase 4(US2,可提前并行)→ 验证 fail-closed
3. **STOP**:T027 此时仍 RED(eve-jwt.jks 在库)——这是预期的,等 T028 门禁

### 生产轮换(用户执行,非 AI 任务)

部署新生产密钥 → 改 aliw location → 重启(禁止滚动,FR-022)→ 清空 refresh_token:* → 六表审计 → 公告。全程按 T029 的 DEPLOYMENT.md 清单。

### 提交节奏

每个 Phase(或 US1 内 T012+T013+T014 一组)一个 Conventional Commit,关联 `007` 与任务号;每次提交前 `./mvnw test` 用例级 diff 通过。

---

## Testing & Quality Gates

- 回归门禁:**用例名集合 diff**(基线 509/F4/E219),只允许「原 ERROR 转 PASS」,不接受新失败用例名
- 新增测试全部 AAA 结构、描述性命名(项目 testing 规范)
- 安全敏感改动(过滤器/限流/keystore 加载)已由三轮 `ecc:security-reviewer` 设计评审覆盖;实现阶段代码评审按 AI_WORKFLOW ⑦ 执行 `ecc:java-reviewer` + `ecc:security-reviewer`
- 禁止:提交真实口令、`git add -f` 绕过 gitignore、注释测试过 CI

## Notes

- 序 0(测试 keystore + 基线)已完成,不在本任务表内
- `eve-jwt.jks` 用户决定暂留 —— T028 是唯一删除点,且有门禁
- 变异测试(T032)是本项目既有实践(006 同款),非可选项
