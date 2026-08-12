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
- [x] T010 [P] [US1] `src/test/java/.../domain/service/security/RefreshRateLimiterTest.java`:L2 超阈值 → 响应被~~**固定延迟**~~且仍返回业务错误(**断言非硬拒**);成功路径无延迟;Redis 故障 fail-open(不新增失败);L1 计数存在但**无任何锁定动作**
  - ⚠️ **「固定延迟」部分已于 T036 推翻**:该断言(`elapsed >= 100ms`)固化了 DoS 放大器设计,现已反转为「不得阻塞调用线程」。本条其余三项(非硬拒、fail-open、无锁定)仍有效
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
- [x] T016 [US1] 实现 `RefreshRateLimiterService` `src/main/java/.../domain/service/security/RefreshRateLimiterService.java`:L1 键 `refresh:fail:{userId}`(TTL 60s,仅告警,**禁止锁定**);L2 键 `refresh:invalid:global`(固定窗口 INCR + 每次 INCR 重设 EXPIRE;超阈值 → Prometheus 计数器告警 + ~~失败路径固定延迟 100–300ms~~;Redis 异常 try-catch fail-open;成功路径零影响)。依赖 `CacheGateway` 端口,不直连 Redis(DDD 分层)
  - ⚠️ **「固定延迟 100–300ms」已于 T036 移除**(DoS 放大器);「Prometheus 计数器告警」通路不存在,由 T037 处理
  - AC: T010 GREEN ✅(2026-08-12,6/6;阈值 L2_THRESHOLD=1000/60s 窗口集中定义于常量;延迟 100~300ms 区间随机防同步重试风暴;指标名 `eve.helper.refresh.invalid.flood`)
- [x] T017 [US1] 接线:`AuthApplicationService.refreshToken` 失败路径(②/③ 阶段)调用 L2 计数 + 延迟;④ 之后失败调 L1 计数;成功路径不调用
  - AC: T010、T009 GREEN;阈值常量集中定义(不硬编码散落)✅(2026-08-12,②格式非法+③token无效 → L2;用户不存在 → L1(userId);成功路径零调用;阈值集中在 RefreshRateLimiterService 常量;单测增接线断言)
- [x] T018 [US1] `AuthenticationFailureServletHandler.java:69-70`:清理 `InvalidCookieException` 死分支(filter 不再抛该异常)
  - AC: 编译通过;登录失败路径既有用例无新增失败 ✅(2026-08-12,分支与 import 已删,留注释说明删除原因)
- [x] T019 [US1] US1 回归:全量 `./mvnw test` 与 T007 基准做**用例名集合 diff**;特别核对 `CharacterControllerTest.addCharacterAuth`(基线即 401,断言不得因本改动漂移)
  - AC: 无新增失败用例;4 个基线 Failures 名单不变 ✅(2026-08-12,**535/F4/E216/S2**:总数 = 附录 A 526 − 3(删除的诊断测试)+ 12(US1 新测试)算术吻合;Failures 4 例名单与基线完全一致,addCharacterAuth 仍 401 未漂移;Errors 216 = 218 − 2(删除的诊断测试错误,附录 A 已预言);顶层异常 204 EveHelperException + 9 FileNotFound + 2 EsiException + 1 BadSqlGrammar,无新类别;context 加载失败 0;旧 surefire 报告中的 2 个 InvalidCookieException 为删除前残留文件,非本轮产物)

**Checkpoint**: US1 独立可验 —— 旧 token 得 401,refresh 端点可达,洪泛有观测计数(~~延迟整形~~ T036 已移除;告警导出待 T037)

---

## Phase 4: User Story 2 - keystore 不再随源码分发 (Priority: P1)

**Goal**: 生产 keystore 从文件系统绝对路径加载;classpath 加载仅限 test profile(fail-closed);生产 jar 不含生产 keystore

**Independent Test**: `KEYSTORE_LOCATION` 指向文件系统路径可启动;指向 classpath + 生产 profile → 拒绝启动

### Tests for User Story 2(先写,必须失败)⚠️

- [x] T020 [P] [US2] `src/test/java/.../infrastructure/config/security/KeyStoreKeyFactoryTest.java`:别名不存在 → 错误信息含「别名」;口令错 → 信息**不含口令**;文件不存在 → 明确路径;RSA <2048 → 拒绝;**PKCS12 与 JKS 两种物理格式 fixture 均可加载**(M-4);InputStream 关闭(无资源泄漏)
  - AC: 对旧实现的断言失败 = RED ✅(2026-08-12,旧实现 4 失败/3 回归守卫通过;fixture 为 keytool 合成密钥入库 `src/test/resources/keystore-fixtures/`,.gitignore 例外规则已加;PKCS12 单口令限制:keypass=storepass)T022 后 7/7 GREEN
- [x] T021 [P] [US2] `src/test/java/.../infrastructure/config/security/SecurityBaselineValidatorTest.java`:4 项基线各自违规 → 拒绝启动;profile 缺失/未知 → **按生产校验(fail-closed)**;`test` profile → 允许 classpath;**生产 profile + `classpath:test-only.jks` → 拒绝启动**(round3 §3.1 指定用例)
  - AC: Validator 不存在 → RED ✅(2026-08-12,编译失败;T025 后 11/11 GREEN)

### Implementation for User Story 2

- [x] T022 [US2] 重写 `KeyStoreKeyFactory.java`(FR-023):去除双重 synchronized 与可变 store 字段;try-with-resources;异常分类(文件不存在/口令错/别名错/非 RSA)各给独立信息且**不含口令**;加载后断言 RSA modulus ≥ 2048;支持 PKCS12 与 JKS 双格式(不得破坏 DualFormat 现状)
  - AC: T020 GREEN ✅(2026-08-12,7/7;DualFormat `getInstance("jks")` 沿用,双 fixture 回归守卫通过)
- [x] T023 [US2] `KeyPairConfig.java:43-51`:`classpath:` 前缀 → ClassPathResource,否则 FileSystemResource;加载前存在性检查,fail-fast 拒绝启动并指明缺失项;日志只打文件名不打完整路径(LOW-5)
  - AC: T021 部分 GREEN(加载路径分派正确)✅(2026-08-12,存在性检查由工厂 `resource.exists()` 承担;`application-test.yml` 同步为 `classpath:test-only.jks`(裸文件名按文件系统处理,fail-closed 约定);JwtAuthFailureResponseTest 上下文冒烟验证分派生效)
- [x] T024 [US2] `SecurityProperties.java:26`:移除 `location` 默认值 `"eve-jwt.jks"`(H4a)
  - AC: 未配 location 时启动报「缺失配置」而非静默用默认 ✅(2026-08-12,KeyPairConfig fail-fast + Validator 缺失拒绝双保险)
- [x] T025 [US2] 实现 `SecurityBaselineValidator` `src/main/java/.../infrastructure/config/security/SecurityBaselineValidator.java`(`ApplicationRunner`,fail-closed 正向白名单):校验 `log-impl` / `logging.level.web` / `access-token-endpoint.enabled` / `KEYSTORE_LOCATION` 四项;**仅当 profile 明确属于 `{test}` 才允许 classpath**,未知/缺失按生产处理(006 L-10 合并实现)
  - AC: T021 GREEN ✅(2026-08-12,11/11;test 判定为「active profile 明确且仅为 test」,比草案更严)
- [x] T026 [P] [US2] 配置模板:`src/main/resources/application.yml:117` `${KEYSTORE_ALIAS}` → `${KEYSTORE_ALIAS:eve-jwt}`;`.env.example` 补 `KEYSTORE_LOCATION`、`KEYSTORE_ALIAS`;`application-prod.yml.example` 补 `security.keystore` 段(location 用 `/etc/eve-helper/eve-jwt.jks` 占位 + 口令为 `${KEYSTORE_PASSWORD}`)
  - AC: 模板中无任何真实口令;环境变量名与 `SecurityProperties` 一致 ✅(2026-08-12,三处已补;口令全部环境变量占位)
- [x] T027 [US2] SC-003 验证:`./mvnw clean package` 后 `unzip -l target/*.jar | grep '\.jks'` 断言**仅含 `test-only.jks`**(此时 eve-jwt.jks 仍在库中 → 预期失败,RED 保留至 T028)。记录实测输出
  - AC: 验证脚本/命令可重复执行;RED 状态留证 ✅(2026-08-12,实测 `jar tf target/eve-helper-0.0.2-SNAPSHOT.jar | grep '\.jks'`(unzip 等价命令)= `BOOT-INF/classes/eve-jwt.jks` + `BOOT-INF/classes/test-only.jks` —— **RED 留证**:eve-jwt.jks 在库即入 jar;T028 后应仅剩 test-only.jks)
  - **GREEN 转换留证(T028 执行后,2026-08-12)**:同命令输出仅 `BOOT-INF/classes/test-only.jks` → SC-003 达成

**Checkpoint**: US2 独立可验 —— 生产可从文件系统加载、fail-closed 生效、jar 内容可断言

---

## Phase 5: User Story 3 - 清空 refresh token 与审计 (Priority: P1)

**Goal**: 代码侧清理就绪;轮换/清空/审计的执行与结论由用户完成(运维动作,AI 不代执行)

**Independent Test**: `git ls-files | grep eve-jwt.jks` 为空;DEPLOYMENT.md 含全部清单

- [~] T028 [US3] **FR-012(有门禁)**:`git rm src/main/resources/eve-jwt.jks`。⚠️ 前置条件(先验证再执行):①`application-aliw.yml`(不入库,用户提供证据)的 `security.keystore.location` 已改为文件系统绝对路径;②新生产密钥已按 `docs/DEPLOYMENT.md` 部署到 `/etc/eve-helper/`(权限 600/目录 700,SC-009 留证)。前置不满足则**停止并报告**,不得执行
  - 🚨 **状态回退为「部分完成」(2026-08-12,T034 评审推翻原结论)**:`git rm` 动作已执行且 SC-003 已 GREEN,但**前置条件①实际未满足**,详见 `docs/reviews/2026-08-12-007-security-reviewer.md` CRITICAL-1
  - **推翻依据(AI 实测,md5 逐字节对照)**:`application-aliw.yml:159` 的 `location` = `D:\IdeaProjects\eve-jwt.jks`,该文件 md5 `1be3633d52ebcaa3cd9fd18e1045aa72` 与 `git show 67333f7:src/main/resources/eve-jwt.jks` **完全相同** —— 即生产 profile 仍在加载**同一把已泄露的私钥**。用户生成的新密钥 `D:\IdeaProjects\eve-helper.jks`(md5 `612ee34cedff607245ace2a39ccfbc4b`)**未被任何 profile 引用**。前置①只做到「路径从 classpath 改成文件系统」,没做到「指向新密钥」
  - **后果**:SC-002(旧密钥签发的 token 一律 401)与 SC-014 在**部署态不成立**;旧 token 仍能通过验签。本任务的目标(移除泄露密钥的误用风险)只在仓库层面达成,**生产层面未达成**
  - 已完成部分(保留留证):`git rm` 前确认索引内两个 jks(`eve-jwt.jks` md5 `1be3633d…`、`test-only.jks` md5 `05a19e52…`),`eve-jwt.jks` 溯源 `5d139d8「增加token认证」`;`git rm` 后 `git ls-files src/main/resources | grep jks` 仅剩 `test-only.jks`
  - **SC-003 已由 RED 转 GREEN**:`./mvnw clean package -DskipTests` 后 `jar tf` 输出仅 `BOOT-INF/classes/test-only.jks`;`target/classes/` 亦仅 `test-only.jks`
  - **回归**:全量 `./mvnw test` = **553/F4/E216/S2**,与 T031 基线逐位一致;Failures 仍为同 4 例(`AssertsControllerTest.syncAssets`、`BlueprintsControllerTest.addBlueprintsList`、`BlueprintsControllerTest.getBlueprintsList`、`CharacterControllerTest.addCharacterAuth`),**零新增失败用例名**
  - ⚠️ **git 历史仍含旧私钥**(FR-014 有意不改写历史,禁止强推共享分支):旧密钥的失效依赖**生产完成轮换**,而非本次删除。**当前轮换未完成 → 该项是活跃威胁,不是残余风险**
  - **闭合条件**:`application-aliw.yml`(及 ali/prod)的 `location` 切到新密钥 + SC-014 现场验证(旧私钥新签 token → 401、新旧 modulus 不同)通过后,方可勾 `[x]`
  - 🔶 **前一半已满足,后一半不可及(2026-08-12,保持 `[~]`)**:
    - ✅ **配置侧**:三个 profile 已由用户从工程中移除(T035),泄露密钥的误指路径消失;md5 `612ee34c…` 证实新密钥在位
    - ❌ **SC-014 现场验证未做**:需要运行态应用 —— 用旧私钥现签一个 token 打到线上,断言 401。AI 无法执行(无部署环境访问权)
    - **为何不因前一半满足就勾 `[x]`**:本任务的目标是「旧 token 一律失效」,而配置正确只是**必要条件**。若 keystore 里恰好还含旧密钥的别名、或部署环境残留旧 profile,旧 token 仍可能通过验签。这正是本 feature 已犯过一次的错 —— 据「配置看起来对了」推断「效果达成」
    - **可由你在部署后闭合**:线上执行「带旧密钥签发的 token 请求任一非白名单端点 → 应得 401 + `AUT00210`」,贴结果即可勾 `[x]`
- [x] T029 [US3] `docs/DEPLOYMENT.md` 轮换章节:轮换步骤(keytool 命令用占位符口令)+ 六表审计清单(SQL + 判断方法,结论留空由用户填)+ 公告模板(Q5)+ `refresh_token:*` 清空前后计数记录项(SC-008)+ **新生产密钥指纹 ≠ test-only.jks 指纹 `FD:9F:19:27:61:...:CA:0F:B4` 核对项**(L-5)+ 「旧密钥已泄露」声明(SC-007);全文不得含真实口令
  - AC: 对照 SC-007/008/013 逐项可勾选;无口令明文 ✅(2026-08-12:新增「🔑 JWT 签名密钥轮换(007)」章节 = 事件声明(SC-007/FR-013)+ 公告模板(Q5)+ 9 步轮换流程(生成→部署 SC-009→指纹核对 L-5→配置切换 fail-closed 警示→FR-022 禁滚动重启→SC-014/SC-006 验证→`refresh_token:{jti}` 清空前后计数 SC-008→六表审计 SQL+判断方法+能力边界声明→SC-013 强制措辞归档)+ 轮换记录表;同步修正既有部署章节(.env 加 KEYSTORE_LOCATION/ALIAS、keystore 部署改 /etc/eve-helper/ 权限 700/600);grep 自查全文无真实口令,仅 `<STORE_PASS>` 类占位符;六表列名逐一对照 PO 实证(sys_user/sys_user_role/sys_permission/sys_role_permission/sys_role/eve_account + BaseEntity gmt_create/gmt_modified),Redis 键模式对照 TokenService:44)
- [x] T030 [US3] `specs/006-character-access-token-api/spec.md` 的 L-10 条目:标注「已由 007 实现(SecurityBaselineValidator)」
  - AC: 006 spec 无悬空待办 ✅(2026-08-12,commit `3f73358`:L-10 行(:170)追加实现指向 `SecurityBaselineValidator`(`ApplicationRunner`,校验 4 项)+ 说明「判定方向按 fail-closed 反转」(不枚举生产 profile,改为仅豁免明确且仅为 `test` 的 active profile)+ 测试指向 `SecurityBaselineValidatorTest`(11 例);优先级列改为 ~~高~~ → **已解决**)

**Checkpoint**: 代码交付完成;生产轮换窗口由用户按 DEPLOYMENT.md 执行(SC-001/006/014 人工验证)

---

## Phase 6: Polish & Cross-Cutting

- [x] T031 [P] 全量回归:`./mvnw clean test`,与序 0 基线(509/F4/E219)做用例级 diff;`target/classes/` 确认无 `jwt.jks` 类残留文件
  - AC: 无新增失败用例名 ✅(2026-08-12,US2 后全量 **553/F4/E216/S2**:总数 = T019 535 + 18(US2 新测试)算术吻合;Failures 仍为基线 3 类 4 例;Errors 216 = 204 EveHelperException + 9 FileNotFound + 2 EsiException + 1 BadSqlGrammar(surefire 目录未 clean,2 个 InvalidCookieException 为已删除诊断测试的残留报告文件,非本轮产物);context 加载失败 0;target/classes 仅 eve-jwt.jks(待 T028)与 test-only.jks(设计内)两个已知资源,无意外残留)
- [x] T032 [P] 变异测试五项(plan §6):删 `ResponseUtils` 新 case → 401 断言失败;删 filter 白名单放行分支 → T009 失败;删 filter `return` → 有测试捕获;fail-closed 改 fail-open → T021 失败;L2 改硬拒(503)→ T010 失败
  - AC: 五项变异全部被既有测试捕获,记录结果 —— **实测 4/5 被捕获;第 5 项(变异 ③)经查证为不可观测的防御性冗余,AC 按实测结论修正(2026-08-12)**
  - **结果明细**(每项植入后立即还原;`git diff --stat src/main/java` 空为还原判据)
    - ✅ 变异 ①(`ResponseUtils` 删 `case TOKEN_ACCESS_EXPIRED`)→ `ResponseUtilsTest.writeErrorInfo_tokenAccessExpired_maps401` FAIL(`expected: <401> but was: <400>`)
    - ✅ 变异 ②(filter 删白名单放行分支)→ `RefreshTokenEndpointAccessTest` 2 例 FAIL(`expected: <400> but was: <401>`,body `{"code":"AUT00210"}`):白名单删除后 `POST /auth/tokens` 被 401 挡住、到不了 controller,正是 CRITICAL-1 要防的失效模式
    - ⚠️ 变异 ③(filter 删放行分支的 `return`)→ **未被捕获,且经查证为「不可捕获」而非测试无力**:
      - 单删 `return`:`RefreshTokenEndpointAccessTest` + `JwtAuthFailureResponseTest` 6/6 GREEN;日志出现 `JwtAuthorizationTokenFilter : 响应已提交,跳过写入 401` → 证明控制流确实继续往下走,但被 `response.isCommitted()` 守卫吸收
      - 进一步同时移除 `return` **与** `isCommitted` 守卫:仍 6/6 GREEN。原因:白名单请求经 controller 处理后响应已提交,Servlet 规范下 `setStatus` 对已提交响应无效,追加字节不改变 status 与断言所查 body 片段
      - **结论**:该 `return` 与 `isCommitted` 守卫构成双保险(纵深防御),在当前架构下**无可观测的外部行为差异**,故无法用黑盒测试约束。保留二者(防御性冗余,后续若有 filter 在 controller 之后写响应的场景即成为必要),但**不虚构一个测试去假装覆盖它**
    - ✅ 变异 ④(`SecurityBaselineValidator` fail-closed 改 fail-open:`length==0 || "test".equals(...)`)→ `SecurityBaselineValidatorTest.noProfile_failClosedAsProduction` FAIL(`Expected IllegalStateException to be thrown, but nothing was thrown`)
    - ✅ 变异 ⑤(L2 `applyFixedDelay()` 改抛 503)→ `RefreshRateLimiterTest.l2_overThreshold_fixedDelayButNotHardReject` FAIL(`Unexpected exception thrown`)+ `l2_overThreshold_prometheusCounterIncrements` ERROR
      - ⚠️ **该记录已过期(T036)**:`applyFixedDelay()` 与 `l2_overThreshold_fixedDelayButNotHardReject` 均已删除。等效的现行变异见 T036 记录(插回 `Thread.sleep` → `l2_overThreshold_doesNotBlockCallingThread` FAIL)
  - **执行纪律留证**:①②③ 依赖 `@SpringBootTest`,首轮(10:00 前后)因 MySQL 测试库 `Connection timed out` 全部 context 加载失败,**已用未变异代码复跑确认属环境问题**,未据此下任何结论;10:29 数据库恢复后先跑未变异基线 3/3 GREEN 作对照,再逐项植入变异。还原后 6/6 GREEN
- [~] T033 人工核验清单(用户执行,留证):SC-011(生产 profile 口令均为环境变量)+ SC-016(生产 profile whiteUrlList 含 `POST:/auth/tokens` 或未定义)+ SC-006 在**生产 profile 实际配置**下验收 + SC-009(stat 权限)
  - 🚨 **原结论已被推翻(2026-08-12,T034 评审)**:用户 2026-08-12 回复「T028 T033核验通过」,但 AI 在 T034 评审中实测发现**至少两项不成立**,故本任务回退为未完成。详见 `docs/reviews/2026-08-12-007-security-reviewer.md` CRITICAL-2 / HIGH-2
  - ❌ **SC-011 不成立**:`application-aliw.yml:160,162` 的 keystore 口令与 key 口令是**字面量明文**,非 `${KEYSTORE_PASSWORD}` / `${KEY_PASSWORD}` 占位符(同文件 DB/Redis 口令亦为字面量)。加固只落在 `application-prod.yml.example`,未落在生效配置
  - ❌ **SC-016 不成立**:`application-aliw.yml:142-144` 的 `whiteUrlList` 只有 `- POST:/user`,**无 `POST:/auth/tokens`**。List 属性 profile 整体覆盖 → refresh 端点在该 profile 恒不可达(正是 T014 注释与 `DEPLOYMENT.md` 都警告过的失效模式)。`application-{ali,prod}.yml` 同型问题由评审 agent 报告,**AI 未亲自核验**(读凭证文件被安全策略正确拦截),须用户自查
  - ⚠️ **SC-006 / SC-009 仍未获独立验证**:两项涉及服务器文件系统与生产运行态,AI 不可访问(FR-002 职责边界)。鉴于 SC-011/SC-016 已被推翻,**原「核验通过」的整体可信度需重新建立** —— 建议逐项重新核验并留下可核对的证据(命令输出/截图),而非仅口头确认
  - **教训(写入流程改进)**:AI 不可验证的核验项,不应仅凭一句「核验通过」就标记完成。后续同类门禁须要求**可核对的证据形式**(如 `grep -c '\${' <file>` 的输出、`stat` 原文、实际 HTTP 响应),否则 spec 的验收标准形同虚设
  - 🔶 **四项中两项已由 T035 的方案变更满足,两项仍不可及(2026-08-12)**:
    - ✅ **SC-011(生产口令均为环境变量)**:三个 profile 已删 → 口令只能来自 `application.yml`,其中全部为 `${...}` 占位符。**这项现在是结构性保证,不再依赖人工核验** —— 没有副本可以写明文
    - ✅ **SC-016(whiteUrlList 含 `POST:/auth/tokens`)**:同理,List 整体覆盖的风险随副本消失
    - ❌ **SC-009(keystore 文件权限 600 / 目录 700)**:需服务器文件系统访问,AI 不可及
    - ❌ **SC-006(轮换后合法客户端可正常 refresh)**:需运行态验证,AI 不可及
    - **判读要点**:SC-011/SC-016 的满足方式从「人工检查三个文件」变成了「不存在可出错的文件」—— 后者强度更高。这是删除 profile 副本带来的**结构性收益**,值得记录:**消除分叉源比检查分叉更可靠**
- [x] T034 评审记录收尾:`docs/reviews/` 确认三轮评审 + 本轮 tasks 执行记录齐全(AI_WORKFLOW 要求评审归档)
  - AC: docs/INDEX.md(如有)或目录自洽 ✅(2026-08-12,`docs/reviews/` 现含 007 五份记录:三轮设计评审(`…design-review.md` / `-round2` / `-round3`)+ 实现阶段两份(`2026-08-12-007-java-reviewer.md` / `2026-08-12-007-security-reviewer.md`),命名符合 README 规范)
  - 🚨 **实现阶段评审结论:双 BLOCK**。两个 reviewer 独立收敛到同一批阻塞项:
    - `ecc:java-reviewer` → **BLOCK**(3 HIGH / 6 MEDIUM / 8 LOW)
    - `ecc:security-reviewer` → **BLOCK**(2 CRITICAL / 2 HIGH / 6 MEDIUM / 4 LOW)
  - **007 当前不满足合并条件**(AI_WORKFLOW §4「禁止忽略 CRITICAL 或 HIGH」)。阻塞项已拆解为 T035~T038(见下)

---

## Phase 7: 评审阻塞项修复(T034 评审产出,CRITICAL/HIGH 必须闭合)

**Purpose**: 关闭实现阶段两份评审的全部 CRITICAL 与 HIGH。**未完成前 007 不得合并**

- [x] T035 🚨 **[CRITICAL-1 + CRITICAL-2 + HIGH-2]生产 profile 三项修正**(用户执行,涉 gitignore 私有配置,AI 不代改):`application-{aliw,ali,prod}.yml` 各自 ①`security.keystore.location` 切到**新密钥**(现 aliw 指向的 `D:\IdeaProjects\eve-jwt.jks` 与已泄露密钥字节相同);②keystore/key 口令改 `${KEYSTORE_PASSWORD}` / `${KEY_PASSWORD}` 占位符;③`whiteUrlList` 补 `- POST:/auth/tokens`
  - AC: 每项留下**可核对的证据**(而非口头确认)—— location 指向的文件 md5 ≠ `1be3633d52ebcaa3cd9fd18e1045aa72`;`grep -c 'password.*\${' ` 输出;`grep -A5 whiteUrlList` 输出含 auth/tokens
  - ④ **顺带自查(T036 M-2)**:确认三个 profile **未覆盖** `server.tomcat.threads.max` / `accept-count` / `max-connections`,或覆盖后的值是有意为之。`application.yml` 已显式声明这四项(即 Boot 默认值),但标量属性会被 profile **整体覆盖** —— 与 whiteUrlList 同型的陷阱
  - 依赖:本任务闭合后 T028 / T033 才能勾 `[x]`
  - 🔶 **用户 2026-08-12 声明「我已执行」,但 AI 无法核验 —— 故标 `[~]` 而非 `[x]`**。这些文件 gitignore 且含真实凭证,读取尝试已被权限门禁拒绝两次(判定为 Credential Exploration,边界正确,不再尝试)。**AC 明确要求「可核对的证据而非口头确认」,该证据尚未提供**
  - ⚠️ **本 feature 有同型前科**:密钥轮换曾据「核验通过」的口头结论判定完成,实测却发现生产仍在用泄露密钥(见记忆 `unverifiable-gate-needs-evidence`)。故此处不因声明而勾 `[x]`
  - 🆕 **T038 新增第 ⑤ 项自查(必须在部署前执行,否则应用拒启)**:新校验要求「全部 `logging.level.*` 均非 debug/trace」且「`logging.level.root` 必须显式声明」。三个 profile 若含任何 debug logger(如 `reactor.netty`、mapper 包),**启动即被拒**。自检命令见 `docs/DEPLOYMENT.md`「启动安全基线:4 项硬门禁」节
  - 关闭条件:粘贴四项 grep/md5 输出(可脱敏路径,保留 md5 与命中行数)即可勾 `[x]`
  - 🔄 **方案变更(用户 2026-08-12):「已经从工程中移除 application-ali aliw prod 配置文件」** —— 不再逐个修三个 profile,而是**把它们从工程中删除**。已实测确认:`src/main/resources/` 下仅剩 `application.yml`(入库)与 `application-test.yml`(不入库);`git ls-files` 确认三者均未被跟踪
    - ✅ **这比逐项修改更彻底**:CRITICAL-1/CRITICAL-2/HIGH-2 三项的共同根因是「profile 副本与 `application.yml` 分叉,且分叉不可见」。删除副本 → 生产直接继承 `application.yml` 的安全形态(口令全为 `${...}` 占位符、`whiteUrlList` 含 `POST:/auth/tokens`、`log-impl` 为 Slf4jImpl、`root`/`web` 均 info),**分叉源本身消失**
    - ✅ **md5 证据已提供**:`612ee34cedff607245ace2a39ccfbc4b`,与新密钥一致,**≠ 泄露旧密钥** `1be3633d52ebcaa3cd9fd18e1045aa72`
    - ⚠️ **但风险转移到了 `application-prod.yml.example`** —— 它现在是生产配置的**唯一指引**,下次部署照抄即可重新引入被删掉的问题。故 T035 的关闭前提由「修三个 profile」改为「**修正模板**」,见新增 T047
    - ⚠️ **未验证项(AI 不可及)**:实际部署环境是否仍存在旧的 `application-{ali,aliw,prod}.yml`(删的是工程内文件,服务器上的副本 AI 无从得知);`KEYSTORE_LOCATION` 等环境变量是否已在部署环境设置 —— 无这些变量时 `application.yml` 的 `${KEYSTORE_LOCATION}` 无默认值会**启动失败**(fail-fast,属期望行为)
  - ✅ **关闭(2026-08-12)**。三项 CRITICAL/HIGH 均以「删除 profile 副本」这一更彻底的方式消除,而非逐项修补:
    - **CRITICAL-1(生产加载泄露私钥)**:副本删除后 `location` 由 `application.yml` 的 `${KEYSTORE_LOCATION}` 提供;用户提供 md5 `612ee34cedff607245ace2a39ccfbc4b` = 新密钥,**≠** 泄露旧密钥 `1be3633d52ebcaa3cd9fd18e1045aa72`
    - **CRITICAL-2(口令明文)**:`application.yml` 全部口令均为 `${...}` 占位符,无副本可覆盖
    - **HIGH-2(加白静默失效)**:`whiteUrlList` 含 `POST:/auth/tokens`,List 整体覆盖的风险随副本消失
    - ④⑤ 两项自查(tomcat 参数覆盖、debug logger)**随副本删除自动满足** —— 无副本即无覆盖
  - ⚠️ **风险已转移而非消失**:`application-prod.yml.example` 成为唯一指引,其缺陷会在下次部署被照抄。已由 **T047 修正并加契约测试**
  - ⚠️ **仍需你在部署时确认(AI 不可及,非本任务阻塞)**:①部署环境的环境变量已设(`KEYSTORE_LOCATION` 等,缺失会 fail-fast 拒启,属期望行为);②服务器上若有旧的 `application-{ali,aliw,prod}.yml` 残留,需一并清理 —— 工程内删除不影响已部署副本
- [x] T036 🚨 **[HIGH-1]移除请求线程上的 `Thread.sleep`**(`RefreshRateLimiterService:118-125`):当前实现是 DoS 放大器 —— `POST /auth/tokens` 已加白(未认证可达),超阈值后每个失败请求占住一个 Tomcat 工作线程 100~300ms,默认 200 线程下约 1000 req/s 即可拖垮**全站**;且清空 `refresh_token:*` 后全体客户端同时 refresh 失败会**自我触发**该路径
  - **采用方案①(仅保留计数器 + 告警,去掉延迟)**。否决理由:②Servlet 异步延迟需把 `DeferredResult` 一路穿透 controller→application service,为价值可疑的整形动作付出架构复杂度(违 KISS/YAGNI);③429/硬拒在 plan §7 已否决且理由仍成立(L2 是**全局单键**,超阈值会拒绝所有用户,而 FR-015 轮换窗口恰好自我触发 → 与 SC-006 冲突)
  - **先改规格后改代码**(AI_WORKFLOW §5.1):`plan.md` §3.3 表格 + 降级裁决段 + 配套约束表 + 多实例说明 + 测试矩阵 2 处 + 变异测试(新增第 6 条)共 7 处;`spec.md` SC-015 洪泛观测项。v3「锁定为固定延迟」裁决**已标记 v4 推翻,注明不得恢复**
  - RED:`l2_overThreshold_doesNotBlockCallingThread`(断言方向由 `elapsed >= 100ms` **反转**为 `< 80ms`)+ 新增 `l2_noDelayConstantsRemain`(反射断言无 `*DELAY*` 常量残留,不依赖计时,防慢机误判) → 2 Failures,失败信息 `实际耗时 146ms` 恰落在 v3 的 100~300ms 区间,证明测到的是真实缺陷
  - GREEN:删 `applyFixedDelay()` + `MIN_DELAY_MS`/`MAX_DELAY_MS` + `ThreadLocalRandom` import;`RefreshRateLimiterTest` 耗时 **1.275s → 0.087s**(延迟真实消失,非放宽断言蒙过);14/14 通过(含 `AuthApplicationServiceUnitTest`)
  - ✅ **变异测试**:临时插回 `Thread.sleep(150L)` → `l2_overThreshold_doesNotBlockCallingThread` FAIL(`实际耗时 163ms`)。仅 1 项失败(用字面量而非常量),恰验证两条断言**互补**:计时断言抓行为,常量断言抓设计意图残留
  - ✅ **回归**:`554/F4/E216/S2`,Failures 用例名与基线**逐项一致**(`AssertsControllerTest.syncAssets`、`BlueprintsControllerTest.{addBlueprintsList,getBlueprintsList}`、`CharacterControllerTest.addCharacterAuth`),**零新增失败用例名**;总数 553→554 因 1 个用例拆为 2 个
  - ✅ **显式容量配置**:`application.yml` 增 `server.tomcat.threads.max=200` / `min-spare=10` / `accept-count=100` / `max-connections=8192` —— 把「洪泛容忍度的分母」从 Boot 隐含默认值变为可见可审项;经 `@SpringBootTest`(6/6)验证绑定无误
  - 同步修正 `RefreshRateLimiterService` 类注释(新增「为何不做延迟整形」段,含成本不对称与自我触发的完整推理)与 `AuthApplicationService:115,127` 两处调用点注释
  - ⚠️ **遗留**:超阈值仍只递增 `MeterRegistry` Counter,而告警通路实际不存在 → **由 T037 闭合**(本任务不越界处理)
- [x] T037 🚨 **[HIGH-1(java)+ MEDIUM-4]告警通路名实一致**:`RefreshRateLimiterService` 注册 Counter 到 `MeterRegistry` 并声称「Prometheus 抓取触发告警」,但 `pom.xml` **无 `micrometer-registry-prometheus`**、全部 yml **无 `management:` 配置** → 指标写入 `SimpleMeterRegistry`,永不被抓取;L1 计数亦只写不读
  - 🔍 **诊断比原记录更严重**:`grep -rln 'io.prometheus' src/main/java` = **0** → pom 里三个 `io.prometheus` 依赖(`-core`/`-instrumentation-jvm`/`-exporter-httpserver`,均 1.0.0)**从未接线**(无 `PrometheusRegistry`、无 exporter 启动、无 `JvmMetrics` 注册),是**死依赖**;`grep '@Bean.*MeterRegistry\|SimpleMeterRegistry' src/main/java` = 0 → 由 actuator 自动配置兜底
  - **采用方案②(不引依赖,改结构化日志)+ 用户指示彻底移除 Prometheus**。方案①否决:补 registry 依赖须走宪法修订,且本项目**根本没有任何 metrics 通路**(不只是缺桥接),建通路远超 T037 边界
  - **实现**:新增 `public static final String ALERT_MARKER = "[SECURITY_ALERT:REFRESH_FLOOD]"`;超阈值打 `log.warn("{} refresh 无效请求洪泛: count={}, threshold={}, windowSeconds={}", ...)`;删 `Counter` 字段 + `MeterRegistry` 构造参数 + micrometer import → **构造器由 2 参降为 1 参**
  - **删除死依赖**:`pom.xml` 移除三个 `io.prometheus`。`mvnw dependency:tree` 确认 classpath 中 **prometheus 与 micrometer 双双消失**(micrometer 原经其传递引入)—— 证明「只删依赖不改代码会直接编译失败」,二者必须同改。**保留 `spring-boot-starter-actuator`**(提供 `/actuator/health`,与本问题无关)
  - **文档去误导**:`docs/DEPLOYMENT.md` 删除约 150 行从未可用的 Prometheus/Grafana 运维指南(`/actuator/prometheus` 端点不存在、`jvm_memory_used_bytes` 等指标名一个都不会出现、`alerts.yml` 规则永不触发),改写为「健康检查 / 日志监控(唯一可用通路,含 grep+cron 与 Loki 两种告警配置 + 告警标记表 + 需关注的日志模式表)/ 容量基线」;`management:` 配置示例的 `include` 由 `health,info,metrics,prometheus` 改为 `health,info`
  - **冻结表同步**:`CLAUDE.md` 删除「Prometheus metrics 1.0.0」行。⚠️ **须你复核**:宪法第四条禁的是"升级或替换",而这是**删除零引用死依赖**,对运行时行为零影响(已实证),我判定不属修订范畴 —— 若你认为仍需走修订程序请驳回
  - RED:构造器签名不匹配(`:78`)+ `ALERT_MARKER` 找不到符号(`:173/174/194`)→ 编译失败(与 T010 同型的 RED)
  - GREEN:9/9(`RefreshRateLimiterTest`);`AuthApplicationServiceUnitTest` 7/7 不受影响(mock 该服务)
  - ✅ **变异测试 ×2**:①日志去掉 `ALERT_MARKER` → `l2_overThreshold_emitsStructuredWarnForAlerting` FAIL(报出实际日志文本);②插回 `MeterRegistry` 字段 → `noUnreachableMetricsDependency` FAIL。两条断言各守一个方向
  - ✅ **回归**(`clean test` 全新编译):`556/F4/E216/S2`,Failures 用例名与基线**逐项一致**,**零新增**;用例 553→556(T036 +1,T037 +2)
  - 新增用例:`l2_overThreshold_emitsStructuredWarnForAlerting`(断言日志含标记+count+threshold)、`l2_belowThreshold_noAlert`(防告警疲劳)、`noUnreachableMetricsDependency`(防名实不符回退);删除立论不成立的 `l2_overThreshold_prometheusCounterIncrements`
  - 同步 `plan.md` §3.3 告警约束行 + 合宪性检查表第四条
  - 📌 **顺带闭合 T036 评审 L-2**:`MeterRegistry` 整体移除,domain 层的分层气味自然消失(无需抽 `MetricsGateway` 端口)
  - ⚠️ **遗留**:L1(`refresh:fail:{userId}`)仍只写不读、无告警标记 —— 定向刷失败无法触发告警。**未修**:L1 的定位是「观测,非防护」(plan §3.3 明确 L1 看不到主攻击向量),加告警需先定义「什么算定向攻击」的阈值,属新需求。已登记为 T046
- [x] T038 🚨 **[HIGH-3]基线校验改正向白名单**(`SecurityBaselineValidator`):`checkAccessTokenEndpoint` 用 `Boolean.parseBoolean(null)` = false → 生产 profile 该键根本不存在 → **恒放行**,却照打「基线校验通过(4/4)」,是虚假保证。同类:`checkWebLogLevel` 只查 `logging.level.web`,`root: debug` 可绕过
  - 改法:3 项 boolean/枚举基线从「查到违规值才拒」改为「**必须显式配置为安全值**」(与该类 `isTestProfile` 自身的正向白名单思路一致);日志级别校验扩展到 `root` / `org.springframework.web` / `org.springframework.security`
  - AC: 补「键缺失 → 拒启」与「root:debug → 拒启」用例,RED → GREEN
  - ✅ **已完成(2026-08-12)**。spec-first:先补 `spec.md` §8.1(校验语义)与 §8.1.1(连带删除死配置),再改代码
  - **危害等级修正(实测)**:入库 `application.yml` 已给出 `enabled: ${ACCESS_TOKEN_ENDPOINT_ENABLED:false}`、`web: info`、`log-impl: Slf4jImpl`,故「键缺失」在真实启动中**不会发生**。HIGH-3 的实际危害是**校验器保证不可信**(宣称强度 > 实际强度),不是当前敞口。**日志绕过路径则是真实可用的**
  - **日志基线改为全扫而非枚举**:AC 原写「扩展到 root / org.springframework.web / org.springframework.security」,实现时改为 `Binder` 枚举**全部** `logging.level.*`。原因:枚举法无法穷尽 —— 实测发现两条 AC 未列出的路径同样致命,`reactor.netty: debug` 使 ESI 请求头(`Authorization: Bearer`)落盘,`...persistence.mapper: debug` 配合 Slf4jImpl 打 SQL 绑定参数使 `refresh_token` 明文入日志(**与 log-impl 基线等效,是同一泄露的另一侧**)
  - **连带删除 `application.yml:103-106` 的 `com.bolingcavalry.druidtwosource.mapper: debug`**:该包全树 grep **零命中**(本项目包名 `xyz.foolcat.eve.evehelper`),是脚手架残留。**不删则新校验会让生产必然拒启**;且它是「给 mapper 包开 debug」的可直接照抄模板,若有人把包名改对即造成 refresh_token 泄露。删除属修复必要组成(理由记于 spec §8.1.1)
  - **消除一个死分支**:变异测试发现 `checkLogImpl` 的 `hasText` 分支存活变异 —— 因 `!SLF4J_IMPL.equalsIgnoreCase(null)` 已覆盖 null,该分支是死代码。冗余分支会让人误以为有两层保护,已合并为单一检查
  - RED: 22 用例中 **8 个新用例全失败、原 14 个全通过**,失败原因统一为「应拒未拒」 → GREEN: **22/22**
  - 变异测试 3 项全部精确命中:①`!"false".equals` 退回 `Boolean.parseBoolean` → 2 用例失败;②全扫退回只查 web/root → 3 条绕过路径用例失败;③log-impl 退回黑名单 → 1 用例失败
  - 回归 `clean test`: **567/F4/E216/S2**,4 个 Failures 用例名与基线**逐项一致,零新增**
  - `docs/DEPLOYMENT.md` 补「启动安全基线 4 项硬门禁」表 + 各 profile 自检命令(**升级后首次部署前须执行**,否则可能因遗留 debug logger 拒启)

### 建议同期完成(MEDIUM,使 fail-closed 名副其实)

- [-] ~~T039 [MEDIUM-1(security)]**密钥身份门禁**~~:`SecurityBaselineValidator` 当前只校验路径**语法**不校验密钥**身份**,这正是 CRITICAL-1 穿过全部 4 项校验的原因。加:启动期读实际加载的 `RSAPublicKey`,拒绝命中「已知禁用指纹表」(至少含 `test-only.jks` 与旧 `eve-jwt.jks` 公钥 SHA-256),拒绝 alias == `test-only`。指纹表入库无风险(公钥非秘密)
  - AC: 用 `test-only.jks` 配生产 profile → 拒启;新密钥 → 通过
  - 🚫 **用户明确否决(2026-08-12):「不做T039」**。不再提议,后续如需重启须由用户主动提出
  - **未实现所留下的能力边界(须知情而非遗忘)**:
    - 校验器**只保证 keystore 路径是文件系统绝对路径,不保证该文件里的密钥不是已泄露的那一把**。把 `location` 指向旧 `eve-jwt.jks`(md5 `1be3633d52ebcaa3cd9fd18e1045aa72`)可通过全部 4 项基线 —— 这正是 CRITICAL-1 的原始成因,该路径**依然敞开**
    - 因此「生产是否用了正确密钥」**只能靠人工留证**(T035 的 md5 比对),无法由机器在启动期拦住。同型误判本 feature 已发生过一次(见 `docs/reviews/2026-08-12-007-java-reviewer.md`)
    - 若日后再次发生密钥误配,排查时应首先想到此处**无自动门禁**
- [ ] T040 [MEDIUM-2(security)/LOW-6(java)]**fail-closed 时机前移**:`ApplicationRunner` 在 web 容器已监听端口**之后**才执行,拒启前存在可服务请求的窗口。改为 `EnvironmentPostProcessor` / `ApplicationContextInitializer` / `@PostConstruct`(与 `KeyPairConfig` fail-fast 同阶段)
- [ ] T041 [MEDIUM-4(security)]**审计能力边界补全**(`docs/DEPLOYMENT.md`):六表判断方法依赖 `gmt_create`/`gmt_modified`,而**有写权限的攻击者可伪造这两列** → 「未发现新增/篡改痕迹」对精心操作的写入型入侵亦是可能假阴性。补一句边界声明 + 步骤 8 增加「检查 binlog/慢日志保留期」
- [ ] T042 [MEDIUM-3(java)]**TOCTOU 残留窗口**:T015 只消除一半 —— `TokenService:158` 的 `refreshAccessTokenWithUser` **又做了一次 `cacheGateway.get(key)`**,窗口平移到「第 1 次与第 3 次 get 间」且跨越两次 DB 往返,**被显著拉长**。并发双请求可各得一套 token 对。要么加 `GETDEL`/Lua 原语,要么在 `spec.md` 显式记录该残留窗口与接受理由(不得留在已勾选的 T015 之下)

### 登记但不在本 feature 修(超出 007 边界,须另开 spec)

- [ ] T043 [MEDIUM-5(security)]登录端点**用户名枚举**:`SecurityConfig:75` 的 `setHideUserNotFoundExceptions(false)` + `AuthenticationFailureServletHandler:66` 使「用户账号不存在」与「用户名或密码错误,剩余尝试次数: N」可区分 → 可枚举有效账号并探知锁定状态。**既存缺陷,非 007 引入**,与 007「统一 AUT00210 防原因区分」是同类问题的相反做法
- [ ] T044 [MEDIUM-6(security)]`MODE_INHERITABLETHREADLOCAL` + 线程池 → **认证上下文跨用户泄漏**:请求线程提交任务时 `Authentication` 被继承给池化线程,而池化线程无人 `clearContext()`。**既存缺陷**
- [ ] T045 LOW 项汇总(两份评审共 12 条,择机 polish):`writeTokenInfo` 通配 ACAO + `no-cache` 应改 `no-store`(LOW-1s);refresh DTO 缺 `@Size` + `maskToken` 可 CRLF 注入日志(LOW-2s);孤立 `public.key`(LOW-3s);`redis-cli -a` 改 `REDISCLI_AUTH`(LOW-4s);`KeyStoreKeyFactory` 死代码重载/全限定名/自重抛/硬编码位数文案(LOW-1~4j);`ResponseUtils` 两方法编码不一致(LOW-5j);登录失败回显原始 message(LOW-7j)
- [x] T046 [T037 遗留]**L1 告警缺口**:`refresh:fail:{userId}` 计数只写不读、无 `ALERT_MARKER` → 针对单一账号的定向刷失败**无法触发任何告警**,L1 当前是纯哑计数器
  - **未在 T037 修的理由**:L1 定位是「观测,非防护」(plan §3.3:随机 UUID 洪泛在 userId 解析前就被挡,L1 看不到主攻击向量);加告警需先定义「多少次/多长窗口算定向攻击」的阈值,以及为何该阈值不会被正常用户的 token 过期误触发 —— 这是**新需求**,不是 T037 的名实一致修正
  - 若不修,应在 `spec.md` 显式记录「L1 仅为事后取证提供 Redis 计数,不产生实时告警」,避免下一个读者再次误以为有告警能力
  - AC: 要么加阈值 + 告警标记 + 测试,要么在规格中写明能力边界。**二者必居其一,不得留空**
  - ✅ **已完成(2026-08-12,选 AC 前者)**。阈值由用户决策:「**1分钟10次算攻击**」→ `L1_THRESHOLD = 10L`,窗口沿用既有 `OBSERVE_WINDOW_SECONDS = 60L`(恰好即「1 分钟」,无需新增常量)
  - **标记有意与 L2 分开**:新增 `ALERT_MARKER_TARGETED = "[SECURITY_ALERT:REFRESH_TARGETED]"`。两类事件运维响应动作不同 —— 洪泛(L2)→ 入口层限流;定向刷单账号(L1)→ 查该账号凭证是否外泄。**有专门断言强制二者不得相同**
  - **告警含 userId**:否则运维拿到告警也无法定位被攻击账号。有断言保护(变异测试③验证)
  - **告警未演化为锁定**:`verifyNoMoreInteractions` 断言超阈值时仍只有 increment + expire 两次网关操作 —— 按 userId 锁定就是 plan §3.3 明令禁止的定向锁死 DoS
  - **误报边界已记入代码注释**:轮换时单客户端只失败 1 次即转重新登录,不会触及 10 次;无退避重试循环的客户端会误报,但那是应修的客户端缺陷,告警正确
  - RED: 编译失败(缺两个常量) → GREEN: **14/14**(原 9 + 新 5)
  - 变异测试 3 项全部精确命中:①两标记设为相同 → `l1_alertMarkerDistinctFromL2` 失败;②`>` 改 `>=` → `l1_atThreshold_noAlert` 失败;③日志去掉 userId → `l1_overThreshold_emitsTargetedAlert` 失败
  - 回归 `clean test`: **572/F4/E216/S2**,Failures 用例名与基线逐项一致,零新增
  - `docs/DEPLOYMENT.md` 告警标记表补第二行 + 新增「两者响应动作不同」处置对照表

- [x] T047 🚨 **[T035 方案变更衍生]`application-prod.yml.example` 修正** —— 三个真实 profile 已删除后,本模板成为生产配置的**唯一指引**,其缺陷会在下次部署时**重新引入刚被消除的问题**
  - **① `location` 默认值指向泄露密钥的文件名(最要紧)**:`location: ${KEYSTORE_LOCATION:/etc/eve-helper/eve-jwt.jks}` —— 忘设环境变量时静默 fallback 到 `eve-jwt.jks`,而那正是已泄露密钥的文件名。**T039(密钥身份门禁)已被用户否决 → 校验器不会拦**(路径语法合法即放行)。改法:去掉默认值改为 `${KEYSTORE_LOCATION}`,与 `application.yml` 一致 —— 缺失即启动失败优于静默指向可疑文件
  - **② `bolingcavalry` 死配置残留**:模板 `:118-121` 仍有 `com.bolingcavalry.druidtwosource.mapper: info`。该包全树零命中(本项目包名 `xyz.foolcat.eve.evehelper`)。当前值是 `info` 故不触发拒启,但它是「给 mapper 包开 debug」的可照抄模板 —— 改对包名 + 调 debug 即泄露 `refresh_token`。`application.yml` 的同源配置已随 T038 删除,模板须同步
  - **③ 注释与 T038 新校验不一致**:模板称「🔒 三项基线」,实际现为 4 项且语义更严 —— `logging.level.root` **必须显式声明**、**全部** `logging.level.*` 均不得 debug/trace(不限 `web`)、`enabled` 只接受**字面 false**(`yes`/`1`/`on` 均拒)。照旧注释理解会漏掉新要求
  - **④ 缺 ali/aliw 模板**:实测仅有 `application-prod.yml.example`,而被删除的是三个 profile。若 ali/aliw 仍在使用,需各自模板或在文档说明「三环境共用 prod 模板 + 环境变量差异化」
  - AC: 逐项修正后,用模板复制出的配置跑一次 `SecurityBaselineValidator`(可加一个以模板真实键值构造 `MockEnvironment` 的测试),断言**放行** —— 防「模板照抄即拒启」
  - 依赖:T035 的关闭以本任务完成为前提(风险已从真实 profile 转移至模板)
  - ✅ **已完成(2026-08-12)**。四项逐一修正:
    - ① `location: ${KEYSTORE_LOCATION:/etc/eve-helper/eve-jwt.jks}` → **`${KEYSTORE_LOCATION}`(去默认值)**,并在注释写明「曾指向已泄露密钥文件名 + 校验器不验密钥身份(T039 已否决)→ 缺失即启动失败优于静默加载可疑密钥」
    - ② 删除 `com.bolingcavalry.druidtwosource.mapper`(与 `application.yml` 的 T038 删除同源),并把三条同型泄露路径(`reactor.netty` / `...persistence.mapper` / `druid.sql.Statement`)写进注释
    - ③ 文件头「🔒 三项基线」→ **4 项**,补上 T038 的正向白名单语义(root 必须显式声明、全部 logger 非 debug/trace、`enabled` 只接受字面 false)
    - ④ 补「**你真的需要这个文件吗**」引导段:说明三个 profile 已移除、生产直接继承 `application.yml` 的安全形态、差异化靠环境变量、若必须建则**只写差异项不要整体复制**。ali/aliw 不再单独出模板 —— 三环境共用本模板 + 环境变量差异化,与「删除 profile 副本」的方向一致
  - 新增 `prodTemplate_asWritten_passesBaseline`(模板↔校验器**契约测试**):以模板真实键值构造环境断言放行。23/23 通过
  - ✅ **变异测试**:在该用例中插入 `logging.level.com.bolingcavalry...mapper=debug`(模拟模板残留 debug logger)→ 精确失败,报错指明违规键名
  - ⚠️ **契约测试的局限已写进 Javadoc(不假装已覆盖)**:它是**手抄**模板键值而非解析该 yml —— 模板若新增 debug logger 本测试不会自动发现。真正闭合需解析文件,但 `.example` 后缀 Spring 不识别,须自行解析,成本高于收益
  - 回归 `clean test`: **573 tests / Failures 4**,Failures 用例名与基线**逐项一致**;007 相关 5 个测试类 **65/65 全绿**;全量输出中 `启动基线校验失败` 命中 **0 次**(证明基线校验未参与任何失败)
  - 📌 **诊断记录(避免重犯)**:本轮首次判读误把 Errors 段当 Failures,一度以为新增 250+ 失败。实为 grep 未按段边界切分。Errors 216→271 的波动源于该次 MySQL 连接超时范围更大;`BlueprintsApplicationServiceUnitTest` 等纯 Mockito 单元测试的「用户未授权」错误经 `git stash` 在**未改动代码上复跑同样失败**,与本次改动无关。方法已存入记忆 `regression-diff-must-separate-failures-errors`

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
