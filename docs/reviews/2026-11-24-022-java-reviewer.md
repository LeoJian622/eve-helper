# 022 伴侣 APP 后端支撑端点（第一期）— P6 最终 Java 代码评审

- 评审人：Senior Java Code Reviewer（Spring Boot / DDD）
- 范围：d403b11(P3 checkpoint) .. 578892b(022-companion-api tip)，129 文件 +15542/-8，51 提交
- 方式：独立对抗性重派；全部经 git show / git grep / git diff 读取分支代码，未 checkout、未改任何生产/测试代码、未触碰工作区脏文件、未执行 SQL、未派遣子代理。
- 测试基线采信前序（全量 1114/F15/E26/S29，F/E 已坐实为预存在环境噪声，022 新类全 GREEN）；本轮做静态审读 + git 取证，未重跑全量。
- 安全面（RBAC/IDOR/注入/JWT epoch/ws-ticket/改密撤销/解绑守卫）已由 security-reviewer APPROVED，本轮不重审，仅聚焦 Java 正确性/规范/架构/测试质量。

## 八维度结论速览

| 维度 | 结论 | 关键取证 |
|---|---|---|
| 1 正确性与边界 | PASS（含 Minor） | IMP-A maxPage/null 守卫镜像 StructureService；分页 @Min/@Max 真实生效；去重键/24h 窗口正确；fail-soft 跳过而非误发；时间比较为 instant 级，时区安全 |
| 2 DDD 分层 | PASS | git grep 行首锚定 domain 上层 import 零命中（exit 1）；EsiGateway 端口无 infra import；跨层 VO 合规 |
| 3 Spring/Java 规范 | PASS（含 1 Important + Minor） | 全构造器注入；@Transactional(readOnly=true) 类级 + changePassword 正确覆盖；@Validated 方法级校验真实；无空 catch。Important-1：单对象网关路径异步错误未转换 |
| 4 测试质量 | PASS（含 Important-1 测试保真缺口 + Minor） | 断言钉住真实行为（ArgumentCaptor/never/times）；RED→GREEN 提交链可信；3 个网关错误路径测试用同步 thenThrow 钉住非生产路径 |
| 5 技术栈冻结 | PASS | git diff d403b11..578892b -- pom.xml 为空 |
| 6 QQ 侧零变更 | PASS | StructTask/IndustryTask/StructureService/onebot/pom diff 全空；StructureMapper 既有 batchInsertOrUpdate 一字不动，仅新增 PreserveOwner/removeStale 变体 |
| 7 并发/性能 | PASS（含 Minor） | 600s 锁 + compareAndDelete 防误删；SETNX 每军团 3600s 节流；24h 去重；新查询点全部走 *Response 变体（TTL 依赖 headers）；sRemove 未接线致 refresh_user 集合无界增长（Minor） |
| 8 信息泄露 | PASS | 复用 EsiStatusUtil.dataError/handleResponseWithHeaders 范式，4xx/5xx 折叠为枚举友好码，不回显原始 ESI body/token；日志脱敏 |

---

## Strengths（具体到 file:line）

- IMP-A 防误删守卫真实落地：AppStructureSyncService.java:118-124 maxPage==null 跳过且不清理；StructureMapper.xml 新增 batchInsertOrUpdatePreserveOwner（ON DUPLICATE 去掉 user_id）与 removeStaleByCorporation（keepIds 空集用 if 规避 NOT IN 非法），既有 QQ batchInsertOrUpdate 零改动。AppStructureSyncServiceTest 用 ArgumentCaptor 钉死 keepIds/ownerUserId/never(旧变体)/throttle times(1)/单军团失败隔离。
- 去重与扇出测试钉住真实行为：NotificationEventDetectorTest 对 dedupKey、userId、characterId 隔离、times/never 精确断言；AppNotificationDetectServiceFuelTest 覆盖全局去重、势力铁壁排除、fuelExpires=null、离线仅入库、锁未获取跳过、MIN-C 扇出每军团一次。
- fail-soft 一致：AppNotificationDetectService 三检测逐角色/逐军团 try-catch + log.error，单点失败不阻断其余；技能队列 queue==null || !queue.isEmpty() 正确区分失败(null)与真空(empty)，避免误发。
- 改密全会话失效链路严谨：UserApplicationService.changePassword:120-186 限流→验旧→新旧相同拒绝→更新→写纪元(7d)→逐 sid 撤销+标记→WS 关闭→清计数；UserApplicationServicePasswordTest 用 InOrder 钉住 markSessionRevoked 先于 closeUserSessions、纪元键/TTL/值类型。
- JWT epoch fail-closed 双路径一致：JwtAuthorizationTokenFilter.java:111-140 与 TokenService.refreshAccessTokenWithUser 判定逐字对齐，iat 秒精度只会过度拒绝；解析失败 fail-closed 拒绝 + 脱敏告警。
- ws-ticket 原子消费：AppNotificationWebSocket.onOpen get→instanceof Integer fail-closed→compareAndDelete 原子消费→先消费后注册；AppWebSocketSessionRegistry.unregister computeIfPresent 原子双清理规避 OneBot 泄漏先例。
- 分页/输入校验真实生效：NotificationController/JobController 类级 @Validated + 参数 @Min(1)/@Max(1000)；SendMailRequest/ResolveNamesRequest 嵌套 @Valid + 元素级约束；内存分页 fromIndex>=total 守卫 + 控制器 @Min(1) 杜绝负索引。
- 构造器注入 + 端口隔离：全部新服务构造器注入（无 @Autowired 字段）；EsiGateway/CacheGateway/AppNotificationPushGateway 端口仅用领域类型，domain 零上层 import。
- 缓存变体纪律：022 所有新查询点（panel/online/location/ship/skills/attributes/skillqueue/mail/roles/membertracking/universe-ids）均走 *Response 变体提取 header TTL，规避裸 Mono 静默跳写；encodeNames 分隔符 "-"→"|" 防碰撞（EsiApiService.java:820-823）。

---

## Issues

### Critical (Must Fix)

无。

### Important (Should Fix)

#### IMP-1 单对象网关路径异步 ESI 错误未转换为 EsiAccessException，且错误路径测试钉住非生产同步分支
- 位置：
  - infrastructure/gateway/esi/EsiApiService.java:1567-1572（callSdkMono 仅 catch 同步 EsiException，无 .onErrorMap）；消费方为全部 022 单对象方法（doQueryCharacterPublicInfo/Online/Location/Ship/Skills/Attributes/SkillQueue/Mail/MailLabels/Roles/MemberTracking、markCharacterMailRead:2221、sendCharacterMail、resolveUniverseIds）。
  - 测试：EsiApiServiceMailTest.java:358、EsiApiServiceCharacterPanelTest.java:218、EsiApiServiceMemberTrackingTest.java:236 均 .thenThrow(new EsiException(...))（同步），断言转换为 EsiAccessException。
- 问题：真实 SDK（如 MailApi.addCharacterMail 用 .retrieve().onStatus(..., EsiStatusUtil.dataError())；*Response 变体用 exchangeToMono(handleResponseWithHeaders)）的 4xx/5xx 错误是异步 Mono.error(EsiException)。callSdkMono(Supplier) 只在 sdkCall.get()（仅构建 Mono，不触发 HTTP）处 catch 同步异常，异步错误经 .block() 原样抛出 EsiException，不会转换为 EsiAccessException。列表路径（doListWithClaim:1097）有 .onErrorMap(EsiException.class, this::convertToAccessException)，单对象路径没有——两条路径行为不一致。三个错误路径测试用同步 thenThrow 恰好命中 callSdkMono 的 catch 分支，断言转换为 EsiAccessException，但该分支在生产 HTTP 错误下不可达（mock 剧场）。
- 影响：
  1. 防腐层契约破坏：infrastructure 的 EsiException 越过端口契约泄漏到 application/domain 调用栈（端口 Javadoc 声称转换为 EsiAccessException）。
  2. 跨端点 HTTP 不一致：同一 ESI 403，单对象路径（mail/panel/roles）返回 403 + ESI00403（未转换，EsiException 携带 resultCode），列表路径（industry jobs）经转换后 EsiAccessException.permissionLow() 因工厂未设 resultCode 致 400 + 通用码。APP 侧按码区分授权状态时会踩到不一致。
  3. 测试对 FR-007（发信缺 scope → 需重新授权）关键错误路径给出虚假信心。
  - 注：当前生产用户可见结果尚可接受（403/400 + 友好文案，无泄露），故非 Critical；但属真实契约/一致性/测试保真缺陷。
- 修法（建议一并处理，避免修复引入回归）：
  1. 在 callSdkMono 返回处统一 .onErrorMap(EsiException.class, this::convertToAccessException)（与 doListWithClaim 对齐），使单对象/列表路径转换一致。
  2. 同时修复 EsiAccessException 工厂（permissionLow/authorizationFailure/serverFailure）携带 proper IResultCode（当前 super(message) 致 resultCode=null 致 400），否则第 1 步会把单对象路径现有的 403 退化为 400。此工厂缺陷为 013 遗留，但 022 新复用且与第 1 步耦合，须一起修。
  3. 三个测试改 stub 为 Mono.error(new EsiException(...))（异步），钉住真实生产路径。
- G6 提示：本项为 Should Fix；若用户评估生产影响有限选择豁免，须在签核中记录豁免理由。

### Minor (Nice to Have)

- MIN-1 类级 Javadoc 过期：AppNotificationDetectService.java:34 仍写本类仅实现 detectFuelLow()、T032 将追加工业/技能/清理方法，但工业/技能/清理方法已实现。更新为现状描述。
- MIN-2 refresh_user 集合无界增长（sRemove 未接线）：TokenService.java:154 每次登录/轮换 sAdd sid 并 expire 续期，但 logout/revoke 从不调用 sRemove（端口 CacheGateway.java:119 已定义却无生产消费方）。活跃用户的 refresh_user:<userId> 集合随登录次数单调增长（TTL 不断续期），UserApplicationService.changePassword:174 会遍历并重复撤销早已失效的 sid。建议在 logout/会话撤销处 sRemove 剪枝。近期小规模影响有限，故 Minor。
- MIN-3 fail-closed→0 注释误标：UserApplicationService.java:208 解析失败返回 0（=放行，实为 fail-OPEN），注释却称 fail-closed。行为镜像 LoginRateLimiterService:84-86（一致，可接受），但术语误导。建议更正注释为 fail-open→0（镜像登录限流）或真正 fail-closed（返回 MAX）。
- MIN-4 recipientType 可空绕过校验：SendMailRequest.java:43-44 @Pattern 无 @NotNull，null 通过校验 → 网关发 null type → ESI 400。建议补 @NotNull（或 @NotBlank），在边界 fail-fast。
- MIN-5 WS 异步发送同步块不串行化写：AppWebSocketSessionRegistry.java:78-79 synchronized(session) 内 session.getAsyncRemote().sendText(payload)——async 立即返回 Future，同步块只锁调用不锁完成；同会话快速连续推送（燃料多行循环）可能触发 message-already-in-progress ISE → 该条实时推送丢失（已持久化，可拉取恢复）。镜像 OneBot 先例，故 Minor。建议改 getBasicRemote().sendText 于同步块内，或链式 Future。
- MIN-6 邮件写后不驱逐读缓存：EsiApiService.markCharacterMailRead:2221 标记已读后不清 inbox/labels/detail 缓存，未读状态/正文 read 标志在 TTL 内陈旧（客户端可 forceRefresh）。属可接受最终一致，建议文档提示或写后驱逐对应键。
- MIN-7 deleteExpired 无 gmt_create 独立索引：022_app_notification.sql 仅 idx_user_created(user_id,gmt_create)/idx_dedup(dedup_key,gmt_create)；deleteExpired WHERE gmt_create<cutoff LIMIT 无法用前导列 → 每批全表扫描。每日 3:30 清理，小表影响有限；建议补 idx_gmt_create。
- MIN-8 IMP-A 真实失败分支未测：AppStructureSyncServiceTest.esiFailure_noCleanup stub maxPage=null（生产为防御性死代码，真实失败抛异常）；拉取中途 ESI 抛异常 → 不清理 的活路径无测试。建议补一条 queryCorporationStructures Flux.error → never(removeStale) 用例。
- MIN-9 定时任务日志丢栈：AppNotificationTask/AppStructureSyncTask catch 块 log.error 仅传 e.getMessage() 未传异常对象，丢失堆栈，不利运维定位。建议 log.error 传入异常对象 e。
- MIN-10 时区写法不一致（无功能影响）：AppNotificationDetectService.java:159/209 用 OffsetDateTime.now()（系统默认区），QQ IndustryTask.java:72 用 now(ZoneId.of(+0))。比较均为 instant 级，结果正确；建议统一显式 UTC 以对齐既有风格。

---

## Declined to judge（考虑过但判定超范围）

- EsiAccessException 工厂 resultCode=null 致 400 的纯遗留面：013 起 WalletJournal/WalletTransaction 已用，非 022 引入；仅在 IMP-1 修法中作为耦合项提及，不单独定级。
- ESI_AUTHORIZATION_FAILURE/ESI_SERVER_FAILURE 的 HTTP 映射（400 而非 401/5xx）：GlobalExceptionHandler.resolveHttpStatus 既有映射表决定的全局行为，非 022 新增；不重审。
- OAuth state 固定 eve-helper（常量 CSRF state）：属安全面，security-reviewer 已 APPROVED；不重审。
- RBAC 18 端点与 SQL 精确性、IDOR、改密/解绑守卫安全性：security-reviewer 已 APPROVED（Critical/High/Important/Minor 全 0）；不重审。
- spec 未穷尽的输入/环境：按合理使用者预期评判，已纳入上述定级；未因 spec 沉默而豁免任何真实缺陷。

---

## Recommendations

1. 优先处理 IMP-1：集中式 callSdkMono.onErrorMap + EsiAccessException 工厂补 resultCode + 三测试改异步 stub，一次性消除单对象/列表路径错误契约不一致与 mock 剧场。这是本轮唯一 Should Fix。
2. 合并前顺手清 MIN-1/MIN-3/MIN-4/MIN-9（低成本、纯 correctness/文档/校验）。
3. MIN-2（sRemove 剪枝）建议排入后续小修，避免活跃用户会话索引长期膨胀。
4. 风格收敛（不阻塞）：AuthorizeOAuth.authorizeUrlForServer/JwtAuthorizationTokenFilter 内联全限定 ResultCode/GlobalConstants（名冲突所致）可抽 import 或静态常量；多文件 EOF 缺换行（JobController/CacheGateway/RedisCacheGateway/EveAccountRepository/CharacterApplicationService）统一补。
5. 检测任务运行于 scheduler（无用户上下文）→ roles/skillqueue/industry 走未认证不缓存分支，每角色每轮真调 ESI。当前规模可接受；若绑定角色数增长，关注 ESI error-budget，可考虑为内部检测引入受控缓存。

---

## Assessment

**Ready to merge? With fixes.**

技术结论：022 在 DDD 分层、技术栈冻结、QQ 零变更、并发锁/去重/缓存变体纪律、信息泄露防护上均 PASS，IMP-A 等已闭环修复经独立取证确认正确落地，测试整体钉住真实行为、RED→GREEN 链可信。唯一 Should Fix（IMP-1）是单对象网关路径异步 ESI 错误未转换 + 错误路径测试钉住非生产同步分支，造成防腐层契约破坏与跨端点 HTTP 不一致；生产用户可见结果尚可接受，故非 Critical，但 G6 要求 Important 清零（或用户明确豁免）。建议按 IMP-1 修法集中修复后即可合并；Minor 项可合并前顺手清或排后续。
