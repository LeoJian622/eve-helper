# 022 IMP-1 修复波(round 3)— scoped 复评报告

- 评审人:Senior Java Code Reviewer(Spring Boot/DDD,独立对抗姿态)
- 范围:`578892b..cbd7388`(022-companion-api,15 files +540/-23);复评包与 `git diff` 实测逐字节一致
- 方式:全部经 worktree `D:/IdeaProjects/eve-helper/.worktrees/022-fixwave` 读码 + git 取证 + 自跑定向无 DB 单测(IDEA 捆绑 Maven);未 commit/checkout/改任何源文件/执行 SQL/派遣子代理
- 自跑验证:`mvn test -Dtest=EsiApiServiceErrorConsistencyTest,GlobalExceptionHandlerEsiAccessTest,TokenServiceUserIndexPruneTest,MailControllerTest,EsiApiServiceMailTest,EsiApiServiceCharacterPanelTest,EsiApiServiceMemberTrackingTest` → **Tests run: 56, Failures: 0, Errors: 0, BUILD SUCCESS**(逐类:Consistency 6 / Handler 3 / TokenPrune 3 / MailController 9 / Mail 15 / Panel 16 / MemberTracking 4,全 0F0E)

## 逐项核验

### A. callSdkMono 修复正确性 — PASS
- **互斥/无双包装**:PASS。`EsiApiService.java:1572-1579`:成功装配 → Mono 挂 `onErrorMap(EsiException.class,...)`,流内异步错误只经 onErrorMap;装配期同步抛只被 catch 接住(此时 Mono 未返回,onErrorMap 未挂)。`EsiAccessException` 与 `EsiException` 为 EveHelperException 兄弟类(非父子),转换产物永不重匹配;default 分支 `new EsiAccessException(message, e)` 保留 cause 但抛出类型正确。无双包装、无漏包装。
- **列表路径零改动**:PASS。本轮 EsiApiService diff 仅 callSdkMono 一个 hunk(+Javadoc);`doListWithClaim:1080-1099`、`doMaxPageWithClaim:1104-1126` 与 BASE 逐字相同。
- **异步 stub 钉住真实生产路径**:PASS(非换皮 mock 剧场)。抽样实证 SDK 错误进入 Mono 的方式与 stub 完全同型:
  - `MailApi.addCharacterMail:83-91` = `.retrieve().onStatus(4xx/5xx, EsiStatusUtil.dataError())`;`EsiStatusUtil.dataError():63-80` 403 → `release.then(Mono.error(forbiddenAgent()))`,`forbiddenAgent()=new EsiException(ResultCode.ESI_AUTH_PERMISSION_LOW)` — 与测试 stub `Mono.error(new EsiException(ResultCode.ESI_AUTH_PERMISSION_LOW))` 同类同构造同投递方式。
  - `CharacterApi.queryCharacterResponse:73-78` 与 `CorporationApi.queryCorporationMembersTrackingResponse:467-473` = `exchangeToMono(handleResponseWithHeaders)`;`:109-126` 4xx/5xx → `Mono.error(new EsiException(...))`。消费端 `doQueryCharacterPublicInfo:1613-1614`、`sendCharacterMail:2249-2251` 均 `callSdkMono(...).block()`,异步错误原经 block 泄漏、今经 onErrorMap 转换 — 路径钉死。
- **自跑抽验**:56 用例全绿(见上),RED 的 13F 对应集在本轮独立复现 GREEN。

### B. 工厂位移审计 — PASS(1 处报告盘点缺口,良性,见 Findings Minor-1)
- **码值逐字一致**:PASS。domain `Code`(`EsiAccessException.java:91-100`)ESI00400/ESI00403/ESI00500 与 infra `external/esi/ResultCode.java:25-33` 码值+msg 逐字相同(三串全文比对一致;msg 复用同类 MSG_* 常量,工厂 message 零变化,既有 hasMessage 断言成立)。
- **DDD 合规**:PASS。`EsiAccessException` import 仅 shared 两类(`:3-4`);worktree 实测 domain 层上层 import grep = 0。domain 本地枚举+码字符串跨层契约与仓库既有惯例一致(`WalletBalanceApplicationService.java:57-69,82` 即同款"不得 import infra,按码字符串比对,码变须同步"先例)。
- **permissionLow 400→403 = 恢复 013 FR-005**:PASS(原文支持)。`specs/013-esi-char-auth-403/spec.md:89` FR-005 明文:"ESI 数据接口返回的 HTTP 403 MUST 映射新增专属失败码(如 ESI_AUTH_PERMISSION_LOW)与可行动友好文案,并响应 HTTP 403 状态码";`checklists/implement.md:13` 登记 T001/T004 已做。且既有 IT 一直按该语义断言:`WalletJournalCorpSyncIT:199-214`、`WalletTransactionControllerCorpIT:311-323` 期望 403+ESI00403+Director 文案,修前工厂 null code 恒 400 — 该位移是修复对既定契约的回归,非新增用户可见语义。
- **authorizationFailure/serverFailure HTTP 不变仅补码**:PASS。`GlobalExceptionHandler.resolveHttpStatus:196-216` 仅 ESI00403→403,其余→400;ESI00400/ESI00500 落 default 400,body code 由 SYS00101→专属码(FR-006 三类可区分,`spec.md:90`)。
- **WalletBalance classify = 018 既定语义**:PASS。`classify:234-247` null→UPSTREAM_ERROR、"ESI00400"∈REAUTH_REQUIRED_CODES(`:65-69`)→UNAUTHORIZED、"ESI00403"→PERMISSION_DENIED;出处 `specs/018-wallet-balance-by-user/tasks.md:76`(IMPORTANT-2 方案 A)/`spec.md:68-69`/`tasks.md:90`(MEDIUM-1)。修前列表路径经 null-code 工厂落 UPSTREAM_ERROR 违背既定语义,修后归正;单对象路径修前泄漏 EsiException 本就带同码,前后一致。
- **消费点全量盘点**:基本 PASS。工厂调用点实测 = `convertToAccessException:1532-1534` + 4 处直接 throw(IndustryJobQueryApplicationService:83 / MemberTrackingApplicationService:69 / WalletJournalService:193 / WalletTransactionService:119,均 permissionLow)+ 码串消费(GlobalExceptionHandler:208 / WalletBalance:66-84),与报告 1.2 吻合;新 IResultCode 构造仅被三工厂调用;default 分支位移=零(infra ResultCode 仅 SUCCESS+三 ESI 错码,数据路径不可能产生第四种非空码;null-code EsiException 修前 400+SYS00101、修后同值)。**但**:评审补盘 `getResultCode()` 消费者发现 `JobApplicationService.syncJobs:49` null-unsafe — 修前 /job 列表路径 ESI 4xx/5xx 经 null-code 工厂在此 NPE→500,修后正确落 403+ESI00403 / 400+ESI00400 / 400+ESI00500(IndustryJobService 全文无 catch,EsiAccessException 裸传播;JobController:50-52 端点可达)。AuthorizeUtil:48/84 同模式但 EsiAccessException 不可达(纯 DB 归属路径),CharacterApplicationService:89 null-safe 仅日志透传,EsiApiService:1476 捕获 OAuth 流 infra EsiException,均零位移。测试侧:无任何单测断言 null resultCode 或 400-for-permissionLow;WalletBalanceApplicationServiceTest:692-695 用自含 IResultCode 桩,不受影响。
- **未发现声称之外的有害位移 → 不构成 BLOCKED**。

### C. MIN 批 — PASS
- **MIN-2 剪枝**:PASS。剪枝仅在 `Boolean.TRUE.equals(revoked)` 后执行(`TokenService.java:266-268`),userId 于删除前读取(`:262`),读不到跳过交集合 TTL 兜底(`pruneUserSessionIndex:311-316`);键构成 `refresh_user:`+userId 与 sAdd(`:154`)逐一对应,成员为当次撤销 sid — 只删已死成员,改密全会话失效(`UserApplicationService:174-181` 枚举 sMembers 逐个撤销)语义无冲突。撤销路径收敛核验:logout(`AuthApplicationService:102`)、changePassword(`UserApplicationService:178`)、并发轮换递归(`TokenService:295`)全部经 `revokeRefreshTokenBySession` 单点,生产无绕过删除 refresh token 的路径(REFRESH_TOKEN_PREFIX 其余用法 `:359/:383` 为读取与轮换 claim,轮换沿用同 sid 无需剪枝,非遗漏)。测试真实:RSAKeyGenerator 真签 token + parseAccessToken 取真 sid + ArgumentCaptor 定位 refresh token,正向断言 `sRemove("refresh_user:42", sid)`、负向两例(索引缺失/空 sid 不误剪)——mock 边界为 CacheGateway 端口,剪枝效果只能以端口交互断言(无 Redis 环境下的最强断言);剪枝与全会话失效语义经代码路径核读无冲突。
- **MIN-4 @NotNull**:PASS(校验织入真实生效)。`SendMailRequest.java:44` @NotNull;`MailController.send:125-127` @Valid @RequestBody — @RequestBody 体参数校验由 RequestResponseBodyMethodProcessor 执行,不依赖 @Validated 代理(MIN-B 方法级校验教训不适用于此路径);Spring 6 下 MethodArgumentNotValidException extends BindException → `GlobalExceptionHandler:48-67` → 400 + SYS00400(shared `ResultCode.java:37` PARAM_ERROR="SYS00400" 实核)。我本轮实跑 MailControllerTest 输出含 `fields=recipients[0].recipientType=must not be null`,且测试 `verify(mailService, never()).send(...)` 钉死不发 ESI,9/0F0E。
- **MIN-10 UTC 化**:PASS,零行为变更。`AppNotificationDetectService.java:162` `ChronoUnit.HOURS.between`(instant 级,OffsetDateTime 间按 epoch 计算);`:213` now 仅用于 `:240-241` `isAfter(now.plusHours(24))`/`isBefore(now.minusHours(24))` — OffsetDateTime 定偏移语义下 plus/minusHours 操作 instant 时间线、isAfter/isBefore 为 instant 级比较,系统默认区(固定区偏移)与 UTC 表同一 instant,24h 谓词/去重窗口严格不变;gmtCreate 用 `new Date()`(instant)、24h/30d 去重在 DB 侧;`now` 无 toLocalDate/format 等区敏感用法。detect 相关 23 用例全绿佐证,未触发停项条件。
- **MIN-1/3/9 零行为变更,无范围蔓延**:PASS。MIN-1 `:35-36` Javadoc 与四方法(detectFuelLow/detectIndustryComplete/detectSkillQueueEmpty/cleanupExpired 均已实现)现状对齐;MIN-3 `UserApplicationService.java:207-211` 仅注释+日志文案,`return 0` 未动,fail-open 术语更正与 `LoginRateLimiterService:84-86` 现状一致;MIN-9 `AppNotificationTask` 4 处 + `AppStructureSyncTask` 1 处 `log.error(msg, e)` 传异常对象,纯日志面。两笔提交文件集与简报批准范围一致,无越界文件。

### D. callSdkFlux 同型泄漏裁决 — 挂账可接受(判据:生产可达性 = 零)
同型缺陷面在定义上成立(`EsiApiService.java:1556-1562` 仅同步 catch、无 onErrorMap),但按简报判据两要素裁决:
- **生产可达性:不成立。** `callSdkFlux` 为 private,全 src(main+test)grep 唯一命中即其定义行,零调用点、无 `this::callSdkFlux` 方法引用 — 死代码。infra EsiException 无法经此泄漏到任何调用栈,故属简报未覆盖的**不可达同型面**,非「修复波遗漏的可达同根缺陷」。
- **用户可见影响:空集。** 因不可达。
**裁决:挂账可接受**(不得定 Important,否则违背「生产可达性+用户可见影响」判据);但挂账记录须写明事实与处置:该包裹方法是零调用死代码,后续小修**建议删除**(连带消除同型隐患);若因新需求复活,必须在复活点同步补 `onErrorMap(EsiException.class, this::convertToAccessException)`(实现参照 callSdkMono 一行)。

### E. 证据质量 — PASS
- **RED 与缺陷形态对应**:实测 RED 日志 13F 逐类分布 TokenPrune 1 / Panel 1 / Consistency 5 / Mail 1 / MemberTracking 1 / Handler 3 / MailController 1 = 13,与声称完全一致;四类失败信息原文核出——泄漏(`but was: xyz.foolcat...infrastructure.external.esi.EsiException`)、退化(`expected: 403 but was: 400`)、剪枝(`Wanted but not invoked: cacheGateway.sRemove("refresh_user:42", ...)`、校验(`Status expected:<400> but was:<200>`)。Consistency 恰 5F/6 与「修前列表路径本就转换正确、仅单对象 4 语义用例+泄漏用例失败」自洽。
- **GREEN 204/F0/33E 且 33E ⊂ 基线**:green-env-cases.txt 33 条 comm 实测全含于基线 611(差集空);分布 23(UserApplicationServiceTest)+3(其 @Nested 异常隔离与空列表)+7(AuthApplicationServiceUnitTest)= 33,全部上下文加载失败类。报告把 @Nested 3 条标注为 UserApplicationServiceUnitTest,计数与子集结论不受影响(标签小疵)。
- **全量回归方法学成立**:全量 1127/F0/E611/S0 = 基线 1114+新增 13(6+3+3+1);**评审独立重提取**:用其 awk 脚本对 worktree 现存 184 份 surefire XML 重新提取 → 611 条,与 full-fe-cases.txt 对称差为空;错误类型分解 609 × IllegalStateException + 2 × PotentialStubbingProblem(`AssetsWriteUserIdTest.saveAndUpdateAsserts_backfillsUserId` / `IndustryJobServiceBatchSyncTest.corpBatch_setsUserId`,P5 预登记项)实核;全量日志 243 次 Connection timed out 及 `Failed to load ApplicationContext`(activeProfiles=["test"])确证 RDS/Redis 不可达归因;同环境新鲜基线 + 逐用例 diff(F/E 分段、未用固定阈值)符合方法学要求。
- **报告 file:line 引用与实际一致**(防漂移抽查):修前 callSdkMono 位于 578892b :1567-1573(git show 实核,报告称 :1567-1572/:1567-1581 含 Javadoc 口径,吻合);MIN-10 引用 :159/:209 为缺陷原位(修后 :162/:213,系引用缺陷位置非漂移);SendMailRequest:44-46 / GlobalExceptionHandler:208 / 四直接 throw(:83/:69/:193/:119)/ WalletBalance:66-84+234-247 全数吻合;仅 TokenService「298-315」实际方法体至 :316(末行差 1,无碍)。

### F. 合规 — PASS
- QQ 受保护文件 + pom + StructureMapper:`git diff 578892b..cbd7388 -- StructTask/IndustryTask/StructureService/pom.xml onebot` 与 `-- *StructureMapper*` 均空(实测)。
- 复评包与 git diff 逐字节一致;两提交 `fix(022): ...` 信息规范(类型+编号+取证行号+验证摘要+Co-Authored-By);提交拆分 IMP-1 / MIN 两笔与简报建议一致。
- 未 push:`origin/022-companion-api` 无远端跟踪引用,branch -vv 无 upstream;worktree cbd7388 干净树。
- 主 checkout 未触碰:`git status --short` 与会话起始快照逐项一致(2 个 M + 5 个 ?? 原样);评审自跑的 mvn 仅写 worktree target/(简报允许,且跑前已备份原 surefire-reports 证据并核验)。

## Findings

### Critical
无。

### Important
无。B 项补盘发现的 /job sync 位移(500→403/400)不计 Important,理由:位移即修复本意(工厂补码的直接、良性后果),方向为消除 null-code NPE-500 并恢复 FR-005 既定契约,既有 IT/测试期望全部吻合,处理动作只有报告表格补注而无代码返工——以此阻断 G6 违背简报「不可控位移→上报」之本意(该项完全可控)。若用户/G6 对简报「声称之外位移=Important 起报」取严格字面,Minor-1 证据已列全,可据此升级,事实不受影响。

### Minor
1. **报告 §1.2 位移表盘点不全(评审补盘)** — `application/service/JobApplicationService.java:49` null-unsafe `e.getResultCode().getCode()`:修前 `/job/{type}/{complete}/{id}/sync` 遇 ESI 4xx/5xx 实为 NPE→HTTP 500,修后 403+ESI00403(其余两码 400+专属码)。影响:用户可见行为变更(良性方向)未见于声称表。修法:G6 签核记录把该路径位移补注为「500(NPE)→403/400,顺带消除 null-code NPE 面」;可另行登记 :49 null-safe 加固为后续小修(非必须,修后 ESI 系异常恒带码)。
2. **callSdkFlux 死代码(D 项裁决登记)** — `infrastructure/gateway/esi/EsiApiService.java:1556-1562`:private 且零调用点。修法:后续删除,或复活时同补 onErrorMap;G6 按「死代码处置」挂账而非开放缺陷。
3. **预存在 Javadoc 漂移(非本波引入,不计本波质量)** — `domain/port/esi/EsiAccessException.java:29`(MSG_AUTHORIZATION_FAILURE 注「对应 HTTP 401」)/`:36`(MSG_SERVER_FAILURE 注「HTTP 5xx」)与 resolveHttpStatus 实际 400 映射不符(P6 已 Declined 重审该全局映射);新 Code 枚举行注释无误。修法:后续注释对齐,零行为。
4. **证据标注小疵** — GREEN 33E 的 @Nested 3 条被标为独立类 UserApplicationServiceUnitTest(实为 UserApplicationServiceTest 嵌套 display name);TokenService 行区间末 315 vs 实际 316。随 Minor-1 在签核记录中一并更正即可。

## Assessment

**Approved**(可呈签 G6)。

IMP-1 三项联动实现正确且经独立复跑钉死(56/0F0E 实跑;异步 stub 与 SDK dataError()/handleResponseWithHeaders 真实错误形态同类同构造,非 mock 剧场;列表路径零改动;无双包装)。工厂位移在「工厂调用点+码串消费点+getResultCode 消费者」三维全量盘点下仍收敛:permissionLow 400→403 有 013 FR-005 原文与既有 IT 断言双重支持,authorizationFailure/serverFailure HTTP 不变,WalletBalance classify 归正与 018 IMPORTANT-2/MEDIUM-1 原文吻合;唯一声称表缺口(/job 路径 500→403)方向良性、无需代码返工,故 Important 清零。MIN 批中 MIN-2 剪枝收敛与 MIN-4 校验织入均实证为真,MIN-1/3/9/10 零行为。callSdkFlux 因零可达裁决挂账可接受(死代码处置)。证据链(RED 13F 分布/基线 611/逐用例对称差/609+2 类型分解)经评审独立重提取为真,F 合规零违例。呈签时建议将 Minor-1/2 写入 G6 记录(位移表补注 + callSdkFlux 处置),并保留「DB/Redis 恢复后复跑 WalletJournalCorpSyncIT/WalletTransactionControllerCorpIT 端到端 403 语义」为 P7 验证项。
