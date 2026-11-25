# Phase 0 Research — 022 伴侣 APP 后端支撑端点

> 来源：ESI 能力盘点子代理（file:line 证据）+ 平台机制调查子代理 + 用户澄清（spec Clarifications）。
> 所有 NEEDS CLARIFICATION 已解决；无未决项。

## R1: ESI 能力覆盖——SDK 全有，端口缺口 ~13 方法

**Decision**: 零新增 SDK 方法。`EsiGateway` 新增约 13 个端口方法 + `EsiApiService` 缓存包裹实现：
角色公开信息、在线、位置、舰船（4）；技能、技能队列、属性（3）；邮件 headers/正文/标记已读/标签未读/发信（5，收件人类型 SDK 已支持 character/corporation/alliance/mailing_list）；团员追踪（1）；角色军团权限 roles（1）；universe/ids 名称解析（1，公开）。
**工业订单与军团建筑端口已存在**（`EsiGateway#queryCharacterIndustryJobs/queryCorporationIndustryJobs/queryCorporationStructures[MaxPage]`，含缓存包裹）——US3 确为「补查询端点 + 应用服务」。

**Rationale**: 盘点证据——CharacterApi.java:49/296、LocationApi.java:46/69/92、SkillApi.java:48/72/96、MailApi.java:55/80/133/162/189、CorporationApi.java:446/469/714、UniverseApi.java:321、IndustryApi.java:58/363；端口 EsiGateway.java:131-156/194-211。
**Alternatives considered**: 新增 SDK 方法——否决（无缺失）；绕过端口直调 SDK——否决（违反 DDD 依赖规则）。

## R2: 端口扩展模式——照抄 021 零侵入组合式

**Decision**: 只在 `EsiGateway` 加方法，不动既有签名；跨层读模型用 `domain/model/vo` record；`EsiApiService` 组合 SDK + 缓存实现；应用服务消费端口。
**Rationale**: 021 先例——`EsiAuthorizationDetail` record（domain/model/vo/EsiAuthorizationDetail.java:25）+ `getAuthorizationDetail`（EsiGateway.java:86，实现 EsiApiService.java:552-558 组合既有方法，契约明文不产生额外上游调用）。
**Alternatives considered**: 新建独立端口接口——否决（021 已验证单端口加方法零侵入，YAGNI）。

## R3: 直查缓存包裹——列表仿工业、聚合仿 016

**Decision**:
- 列表端点（邮件/工业/团员追踪/建筑）仿 `doQueryCharacterIndustryJobs`（EsiApiService.java:790-818）：`cacheKey(ds,userId,uri,page)` → forceRefresh 经 `ForceRefreshLimiter.tryAcquire` → 读缓存 → 未命中 `doListWithClaim`（真调 ResponseEntity 变体 + `EsiCacheTtlExtractor.extract(headers)` 定 TTL + 空 body 回填 `List.of()`）。
- 角色面板聚合（4 个 ESI 调用）：各调用在网关层独立缓存包裹，应用层组合；可选应用层短 TTL 结果缓存（016 `UserWalletBalanceCache` 60s 先例，application/cache/UserWalletBalanceCache.java:61）。
- **更正**：016 balance 端点本身不走 EsiResponseCache（单值直查 + 应用层 60s 结果缓存）；列表缓存正例是工业 jobs。
- 错误链：SDK `EsiStatusUtil.dataError()` → `EsiException` → 网关 `convertToAccessException`（EsiApiService.java:1492-1503）→ `EsiAccessException.authorizationFailure()/permissionLow()/serverFailure()` → 应用层按 IResultCode 分类。

**Rationale**: EsiResponseCache API——cacheKey:53-68（userId=null→pub 段）、getList:75、put:122（ttl null/≤0 不写）、tryClaim:149/releaseClaim:162；TTL 提取 EsiCacheTtlExtractor.java:39-48。
**Alternatives considered**: 全部应用层结果缓存——否决（丢失响应头 TTL 语义与防穿透占位锁）。

## R4: 邮件分页——lastMailId 游标（契约已修正）

**Decision**: ESI mail headers 为 **50 条/页 + lastMailId 游标**（MailApi.java:55），非 page 号。契约 `GET /mail/{cid}/inbox?lastMailId=` 返回 `{mails(≤50), nextLastMailId}`；未读总数走 labels 端点（单次调用同时返回 labels + totalUnreadCount，MailLabelsAndUnreadCountsResponse.java:21-24）。
**Rationale**: 与 ESI 原生语义一致，避免深翻页游标遍历成本；spec FR-004「分页」含游标分页。
**Alternatives considered**: page/size 适配层——否决（深页需遍历游标，O(n) 调用放大）。

## R5: 燃料权限扇出判定——corp job-role，非 ESI scope

**Decision**: 「军团建筑接口权限」＝角色的军团 job-role（Director / Station_Manager 类），经新增端口方法 `queryCharacterRoles`（CharacterApi.java:296，RoleResponse.rolesAtHq/rolesAtBase/rolesAtOther）判定，结果按用户缓存；通知判定时对军团内绑定角色逐一检查。**不用 021 scopes**（scope 是授权声明，不等于军团职务权限）。精确角色集合在实现期对照 ESI 文档核定（structures 端点角色要求），测试用 role fixtures 覆盖边界。
**Rationale**: 建造者评审指出 scope≠job-role；ESI structures 端点按军团角色鉴权。
**Alternatives considered**: 021 scopes 判定——否决（语义不符）；逐用户试调 structures 探权——否决（调用放大 + 错误噪声）。

## R6: 名称解析——公开端点 pub 缓存范式

**Decision**: `UniverseApi#queryUniverseIds`（POST /universe/ids，无 Authorization 头，UniverseApi.java:321）；网关仿 `queryUniverseNames`（EsiApiService.java:996-1015）：userId=null→pub 段、不查 UserUtil、不限流、names 排序确定性编码进键；响应按 characters/corporations/alliances 分组（Name2IdResponse.java:22-41）映射为 resolved 结果。
**Rationale**: 公开数据全局共享缓存，不消耗角色授权（spec 假设）。
**Alternatives considered**: 角色 token 调用——否决（无必要且消耗配额）。

## R7: 通知事件判定——燃料复用 DB 判定，工业/技能多租户直查

**Decision**:
- 燃料：复用 `StructureService.selectFuelExpiresList(24h)`（QQ 判定时机即 StructTask.noticeFuelExpires，StructTask.java:66-92）；建筑同步沿用 `batchInsertOrUpdateFromEsi`（StructureService.java:123-135），每军团每小时至多一次（FR-028，现有 hourly cron 节奏）。
- 工业完成/技能队列空：新增多租户定时判定——遍历绑定角色，经端口直查（缓存包裹），与通知表去重键比对后生成通知。
- QQ 侧 StructTask/IndustryTask/WalletTask 零改动（FR-018）。
**Rationale**: 用户澄清（工业直查、QQ 不动）；燃料判定已有 DB 基础。
**Alternatives considered**: 工业也入库再多租户查询——否决（用户明确选直查）。

## R8: 授权入口——暴露既有 authorizeUrl，手动 code 回路

**Decision**: `GET /character/auth-url` 暴露 `AuthorizeOAuth.authorizeUrl(SsoScopes.ALL)`（AuthorizeOAuth.java:47）；晨曦 PKCE 有意留空（:64-68 注释，既有约束）；code 手动复制回 APP 走现有 `POST /character/{code}`；自动回调需门户外部配置，本轮不做（用户澄清）。
**Rationale**: SsoScopes.ALL 已含全部所需权限（SsoScopes.java:79）；零外部依赖。
**Alternatives considered**: 精简 scope 集——否决（ALL 已验证且避免二次授权摩擦）；自动回调——用户裁定延后。

## R9: RBAC 登记与权限缓存

**Decision**: 新端点经 `src/SQL/convert/022_rbac_permissions.sql` 登记 `sys_permission`（delete-then-insert 事务，幂等），并入 `deploy_all_pending.sql`（009–012 先例）。`url_perm` 用 Ant 变量名 **`{characterId}` 而非 `{uid}`**——`RbacAuthorizationManager`（:110-115）对 `{uid}` 做 principal 等值校验，而 principal 是系统 userId、非 EVE characterId；属主校验由应用层 `AuthorizeUtil.authorize(characterId)` / `AccessGuard` 执行（016 先例）。ADMIN 角色直接放行（`GlobalConstants.ROOT_ROLE_CODE`）。权限缓存仅启动时经 `InitPermissionRolesCache`（CommandLineRunner）重建，**无管理刷新端点 → 部署执行 SQL 后须重启应用**。白名单不新增（022 全部端点需 JWT）；`/websocket/app` 在过滤链 permitAll + 端点内 ticket 自鉴权（仿 onebot 模式，SecurityConfig.java:47）。
**Rationale**: 平台机制报告 §1——RbacAuthorizationManager.java:56-136、SysPermissionService.java:42-80（refreshPermRolesRules 唯一调用方＝启动 runner）、deploy_all_pending.sql:148-170。
**Alternatives considered**: `{uid}` 变量名——否决（userId≠characterId，全员误拒）；新增管理刷新端点——否决（YAGNI，重启是既有部署约定）。

## R10: 改密与全会话失效（现有机制缺失，本轮新建）

**Decision**: `BCryptPasswordEncoder` 已有（SecurityConfig.java:81-84）；**现无 userId→会话索引，无全会话失效机制**（报告 §2 核实）。新建三组 Redis 结构 + JWT iat claim，零 DDL：
1. `TokenService` claims 增加 `.issueTime(new Date())`（现无 iat，TokenService.java:152-160）；`ParsedAccessToken`（:417）随之扩展。
2. `auth:user:revoke_epoch:<userId>`＝改密时刻毫秒（TTL 7d）。`JwtAuthorizationTokenFilter` 在黑名单检查后：标记存在且 token iat 缺失或 ≤ 标记 → 401 AUT00210。
3. `refresh_user:<userId>`（Set<sid>，TTL 7d 随写续期）+ `refresh_iat:<sid>`（会话创建毫秒，TTL 7d），`generateTokenPair` 时写入；**CacheGateway 增补 `sAdd/sMembers/sRemove`**（021 式零侵入端口扩展，RedisCacheGateway 实现）。refresh 路径：纪元标记存在且 refresh_iat 缺失或 ≤ 标记 → 拒绝（fail-closed，覆盖部署时已存在、无索引的旧会话）。
4. 改密流程：限流（`pwd:attempt:<userId>`，5 次/30min，仿 LoginRateLimiterService.java:25-27）→ BCrypt 验旧密 → 更新 `sys_user.password` → 写纪元标记 → 遍历 `refresh_user` 逐 sid 复用 `revokeRefreshTokenBySession` + `markSessionRevoked` → 关闭该 userId 的 APP WS 会话。
旧 token（无 iat）仅在该用户改密后（标记存在时）被拒，正常使用不受影响。
**Rationale**: 报告 §2——refresh_token/refresh_session/refresh_owner/session_revoked 键系（TokenService.java:44-83）；sid 不可枚举、jti 不可枚举。
**Alternatives considered**: jti 黑名单枚举——否决（不可枚举）；DB 会话表——否决（DDL+清理成本，Redis 已是会话权威）；仅靠 access 900s 自然过期——否决（不满足 FR-021「修改后现有会话必须失效」）。

## R11: APP WebSocket——新端点 + 一次性 ticket 鉴权

**Decision**: 新建 `@ServerEndpoint("/websocket/app")`，复用 `ServerEndpointExporter` 注册（WebSocketConfiguration.java:16-27；注意 `@Profile("!test")` → 端点类做薄适配器，逻辑放 Spring 管理的 `AppWebSocketSessionRegistry` + 推送服务，无容器可单测）。**鉴权＝一次性 ticket**：`POST /notification/ws-ticket`（JWT+RBAC）→ UUID ticket，`CacheGateway.setIfAbsent("app:ws:ticket:"+ticket, userId, 30s)` → 连接 `/websocket/app?ticket=` → onOpen `get` + `compareAndDelete`（CacheGateway.java:92，Lua 原子）消费，失败即关闭（fail-closed）。**JWT 不进 URL**（避免日志/代理泄露）。注册表 `ConcurrentHashMap<Integer, CopyOnWriteArraySet<Session>>` 支持多设备扇出；onClose 同步清理（规避 OneBot SESSION_POOL 泄漏先例，WebSocket.java:94-102）；发送 `synchronized(session)` + `getAsyncRemote().sendText`（仿 :120-132）。推送信封 `{type:"NOTIFICATION",data:{...}}`；断线重连 → `GET /notification` 补齐。
**Rationale**: 报告 §3——OneBot 通道为单 bot/共享密钥/JVM 静态态/测试禁用，不可复用为 APP 通道；§7——CacheGateway 原子原语已够用，ticket 无需新端口方法。
**Alternatives considered**: `?token=<JWT>` 直连——否决（token 进 URL 的日志泄露面）；STOMP——否决（既有栈为 raw JSR-356，引入 broker 属新复杂度）。

## R12: 持久化约定——app_notification 三件套

**Decision**: 通知表落 eve_helper；PO/mapper/repo 三件套仿 StructurePO 范例（`persistence/entity/system` + `mapper/system` + `mappers/system/*.xml` + `assembler/persistence` MapStruct）；`AppNotificationPO` 继承 BaseEntity（gmtCreate/gmtModified 由 MyMetaObjectHandler 自动填充）；DDL `db/migration/022_app_notification.sql` + RBAC `022_rbac_permissions.sql` 并入 `deploy_all_pending.sql`，**部署时手动执行**（014 约定）。既有表零 schema 变更（014 归属不动）。
**Rationale**: 报告 §4——SystemDatasourceConfig.java:26-54（mapper 扫描→eve_helper）、014_esi_user_binding.sql:14 手动执行约定、MybatisPlusConfig.java:30-37（分页 maxLimit 1000）。
**Alternatives considered**: 通知只存 Redis——否决（FR-015 持久化 + 30 天保留 + 已读状态）。

## R13: 任务身份、判定复用与建筑同步节流

**Decision**:
- **身份**：定时线程内逐绑定角色 `AuthorizeUtil.authorizeInternal(account.userId, characterId)`（AuthorizeUtil.java:70-90；该方法拒绝请求线程——检测仅在 @Scheduled 线程运行）。逐角色 try/catch 隔离：授权失效跳过+结构化日志，不阻塞其余角色。
- **燃料判定**：读 `StructureRepository.selectFuelExpiresList(24, corpId)`（StructureMapper.xml:422-427，含 `fuel_expires IS NULL`＝已耗尽）；势力铁壁排除 typeIds 47513–47516 镜像 StructTask.java:42-47（不改 QQ 类）；判定 cron 复用 `"0 0 18,22 * * ?"`。
- **建筑同步（FR-028）**：新增 `AppStructureSyncTask` 每小时（与 QQ 整点错开 5 分）→ 逐军团 Redis SETNX `app:struct:sync:{corpId}` TTL 3600 节流 → 选该军团一个有建筑权限的绑定角色 → **新应用服务**组合既有端口 `queryCorporationStructures` + `StructureRepository.batchInsertOrUpdate`（userId=同步角色属主，014 语义）。**不复用 `StructureService.batchInsertOrUpdateFromEsi`**——其内部 `authorizeInternal(SYSTEM_USER_ID,…)`（StructureService.java:116）对成员角色属主校验失败；QQ StructTask 零改动。无权限角色的军团跳过同步（亦无通知受众，与 FR-027 自洽）。
- **工业判定**：逐绑定角色直查 `queryCharacterIndustryJobs`（既有端口+缓存）；谓词镜像 IndustryTask.queryCompeleteAfterHour:69-81（`active && endDate ≤ now+24h`）**＋ `delivered && endDate ≥ now-24h`**（近 24h 内完成；防首轮历史订单刷屏）【建造者细化 FR-014「完成（含 24h 预警）」】；cron 复用 QQ noticeJobComplete0 两条表达式（IndustryTask.java:50-57）。
- **技能队列判定**：`querySkillQueue`（新端口）返回空 → 通知属主；**每小时 cron**（无 QQ 先例，spec Assumptions 授权计划阶段定间隔）【建造者裁定】。
- **去重竞态**：DB 去重为权威 + 每事件类型 Redis SETNX `app:notify:lock:{eventType}` 运行锁（TTL 600s，compareAndDelete 释放）——容忍 cron 重叠（spec Edge Case）；单实例 + @Scheduled 同方法默认串行。
**Rationale**: 报告 §5（TaskConstant 仅 CHARACTER_ID；authorizeInternal 用法 MiningTask.java:71 等）；用户澄清（工业直查、燃料扇出、同步≤1/h/军团、QQ 不动）。
**Alternatives considered**: 改 StructureService 加 userId 参数——否决（触碰 QQ 路径，FR-018）；工业检测每 5 分钟——否决（24h 预警语义无必要，调用放大）。

## R14: 绑定查询与解绑

**Decision**: `EveAccountRepository` 新增 `listByCorpId(Integer corpId)`（燃料扇出+建筑同步选角）与 `deleteByUserIdAndCharacterId(Integer userId, Integer characterId)`（解绑＝删行；`eve_account.character_id` 全局唯一，数据库创建脚本.sql:89-91）。**解绑守卫**：`characterId == TaskConstant.CHARACTER_ID` → 拒绝「系统同步角色不可解绑」【建造者裁定，防 ROOT 误解绑瘫痪 QQ 同步】。解绑后删除 `esi_access_token:{userId}:{characterId}` 缓存；AUT00208 天然 fail-closed（016 先例：绑定移除后角色端点自动失败关闭）；通知检测遍历 eve_account → 解绑角色自动排除（FR-022 停止同步与通知）。历史数据按 014 归属保留。
**Rationale**: 报告 §6——现有 Repository 仅 getAccount(userId,cid)/getAccountList(userId)，无 corp 查询、无删除方法；corp→users 先例仅 014 迁移 SQL join。
**Alternatives considered**: 软删除标记——否决（现表无 deleted 列，加列＝DDL+全查询过滤侵入；删行与唯一约束语义一致）。

## R15: API 信封与错误映射（沿用，零新建）

**Decision**: 控制器显式返回 `Result<T>`（`@NoWrap` 无消费者，不用）；分页 `PageQuery`（current/size≤1000）+ `PageResult`（records/total/current/size/pages）；错误经 GlobalExceptionHandler 既有映射（ACCESS_UNAUTHORIZED→403、ESI00403→403、其余 ESI→400/500，原始错误体不回显）；`Result.success(Boolean.FALSE)` 翻转为 SYS00101（Result.java:32-38）→ **所有端点 data 不用 Boolean**。
**Rationale**: 报告 §7——Result.java:16-58、PageQuery.java:16-43、GlobalExceptionHandler.java:181-212、EsiStatusUtil.dataError():63-81。
