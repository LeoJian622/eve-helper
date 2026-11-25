# Tasks: 全功能 EVE 伴侣 APP 后端支撑端点（第一期）

**Input**: Design documents from `specs/022-companion-api/`
**Prerequisites**: plan.md（G2 已批准 + P3 校正）、spec.md（G1 f2f4940）、research.md R1–R15、data-model.md、contracts/rest-api.md、quickstart.md

**Tests**: 强制（工作流 TDD 铁律）。每故事先写失败测试（RED 证据）再实现（GREEN 证据）。
**Organization**: 按用户故事分组；每故事可独立实现与验收。

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 可并行（不同文件、无未完成依赖）
- **[Story]**: US1–US6（Setup/Foundational/Polish 无故事标签）
- 每任务附 **Files**（Create/Modify/Test）、**Interfaces**（Consumes/Produces 精确签名）、**AC**（测试名+验证命令）

## Path Conventions

- `MAIN` = `src/main/java/xyz/foolcat/eve/evehelper`
- `TEST` = `src/test/java/xyz/foolcat/eve/evehelper`（镜像主包）
- 基础包 `xyz.foolcat.eve.evehelper`；SQL/资源路径为仓库相对路径

## Global Constraints（每任务隐含）

1. 技术栈冻结（宪法 IV）：零新增依赖；Java 17 / Boot 3.5.14 / MyBatis Plus 3.5.15。
2. TDD 铁律：无先失败测试不写生产代码；RED/GREEN 均须当前轮次真实输出。测试通道 `mvn test -Dtest=Class#method`（mvnw 不可用时走 IDEA MCP 等效通道，见 memory build-and-test-toolchain-reality）。
3. QQ 零变更（FR-018）：`StructTask`/`IndustryTask`/`WalletTask`/`StructureService`/OneBot `WebSocket` 零 diff。
4. 014 归属不变：请求线程属主校验 `AuthorizeUtil.authorize(characterId)`（MAIN/domain/util/AuthorizeUtil.java:37）；定时线程 `authorizeInternal(userId, characterId)`（:70，拒绝请求线程）。
5. DDD：application/domain 零 infrastructure import；domain 签名只用领域类型；任务→领域服务（StructTask→StructureService 先例）。
6. API：控制器显式 `Result<T>`；**data 不用 Boolean**（Result.java:32-38 翻转）；分页 `PageQuery`（size≤1000）/`PageResult`；错误经 GlobalExceptionHandler 既有映射，不回显 ESI 原始体。
7. ESI 缓存包裹：列表仿 `doQueryCharacterIndustryJobs`（MAIN/infrastructure/gateway/esi/EsiApiService.java:790-818）；单值仿 016 余额；公开数据 userId=null→pub 段（EsiResponseCache.cacheKey:53-68）；forceRefresh 经 `ForceRefreshLimiter.tryAcquire`；TTL 经 `EsiCacheTtlExtractor.extract(headers)`。
8. 每任务完成即小步提交（`feat:/test:/fix:/refactor:/docs:` 前缀 + Co-Authored-By: Claude Code <noreply@anthropic.com>）；不提交用户无关脏文件（Readme.md 等）。
9. 安全：token/密码不落日志；不提交 `.env.*`/`eve-jwt.jks`；参数化查询；校验所有外部输入。
10. `@SpringBootTest` 依赖内网 MySQL/Redis（test profile）；**T024 前须先在测试库手动执行 T001 DDL**。

## Review Focus（规格隐含的高危输入，各归其主任务测试）

1. **部署兼容**：无纪元标记时旧 JWT（无 iat）必须照常通过；标记存在后 iat 缺失/≤标记一律拒绝（fail-closed）→ T006 测试矩阵。
2. **燃料去重全局性**：同一 structureId 任一接收者行在 24h 窗口内 → 本轮整体跳过（非按用户去重）→ T031/T034。
3. **WS ticket 一次性**：重放/过期/缺失 → 立即关闭；多设备扇出；onClose 注册表清理无泄漏 → T025/T026。
4. **检测隔离**：单角色授权失效/ESI 错误不中断本轮；解绑角色自动排除；cron 重叠运行锁 → T032/T033。
5. **发信与越权**：收件人不可解析 → 明确失败不发送；全部新端点跨属主 → 403 → T019/T044。

---

## Phase 1: Setup（部署工件）

**Purpose**: DDL 与 RBAC 登记工件（部署手动执行，014 约定）

- [x] T001 [P] 创建通知表 DDL `src/main/resources/db/migration/022_app_notification.sql`
  - **Files**: Create `src/main/resources/db/migration/022_app_notification.sql`
  - **Interfaces**: Produces 表 `app_notification`（T024 仓储测试与部署依赖）
  - **AC**: 内容逐字＝data-model.md §1.1（CREATE TABLE IF NOT EXISTS 幂等；idx_user_created + idx_dedup 双索引；utf8mb4）；人工核对无占位符

- [x] T002 [P] 创建 RBAC 权限 SQL `src/SQL/convert/022_rbac_permissions.sql`（18 端点）
  - **Files**: Create `src/SQL/convert/022_rbac_permissions.sql`；结构范例 `src/SQL/convert/012_rbac_permissions.sql`
  - **Interfaces**: Consumes contracts/rest-api.md 全部 18 个 REST 端点（§1×3 + §2×6 + §3×1 + §4×1 + §5×4 + §6×3）
  - **AC**: 每端点一行 `METHOD:PATH`；路径变量一律 `{characterId}`/`{corpId}`/`{id}`/`{mailId}`/`{type}`——**禁用 `{uid}`**（R9：principal=userId≠characterId）；delete-then-insert 事务幂等；角色绑定仿 012 先例（普通用户角色可达，ADMIN 天然放行）；逐端点与 contracts 对照清单附提交信息

- [x] T003 并入部署汇总 `src/SQL/convert/deploy_all_pending.sql`（依赖 T001/T002）
  - **Files**: Modify `src/SQL/convert/deploy_all_pending.sql`（追加 022 段，仿 009–012 段式）
  - **AC**: 022 段含 DDL + RBAC 两脚本内容；段首注释注明「执行后须重启应用刷新权限缓存（InitPermissionRolesCache 仅启动重建，R9）」；既有段零改动

**Checkpoint**: 部署工件就绪（生产执行时机＝P7 收尾，测试库执行＝T024 前置）

---

## Phase 2: Foundational（全局认证/会话基础设施）

**Purpose**: JWT iat + 会话索引 + 撤销纪元——触碰全局认证路径，必须先于各故事落地并回归保护（US6 改密的阻塞前置）

**⚠️ CRITICAL**: 本阶段完成前不开始任何用户故事

- [x] T004 [P] CacheGateway 增补 Set 操作 + Redis 实现
  - **Files**: Modify `MAIN/domain/port/cache/CacheGateway.java` · Modify `MAIN/infrastructure/cache/RedisCacheGateway.java` · Test `TEST/infrastructure/cache/RedisCacheGatewaySetTest.java`
  - **Interfaces**: Produces `Long sAdd(String key, Object... values)` / `Set<Object> sMembers(String key)` / `Long sRemove(String key, Object... values)`（T005/T041 消费）；Consumes 既有 `RedisTemplate.opsForSet()`
  - **AC**: 先写测试 `sAdd_returnsAddedCount` / `sMembers_returnsAllMembers` / `sRemove_returnsRemovedCount`（Mockito mock SetOperations）RED→GREEN；既有 CacheGateway 方法签名零改动；`mvn test -Dtest=RedisCacheGatewaySetTest`

- [x] T005 TokenService 补 iat claim + 会话索引（依赖 T004）
  - **Files**: Modify `MAIN/domain/service/security/TokenService.java`（claims :152-160 加 `.issueTime(new Date())`；`ParsedAccessToken` :417 加 iat 字段；`generateTokenPair` :94 写索引） · Test `TEST/domain/service/security/TokenServiceIatTest.java`
  - **Interfaces**: Produces `ParsedAccessToken.iat()`（Long 毫秒，可 null＝旧 token）；Redis 键 `refresh_iat:{sid}`（会话创建毫秒，TTL 7d）+ `refresh_user:{userId}`（Set<sid>，TTL 7d 随写续期，sAdd 后 expire）；Consumes T004 sAdd、既有 `CacheGateway.set/expire`
  - **AC**: 测试 `generateTokenPair_accessTokenContainsIat` / `generateTokenPair_writesRefreshIatAndUserIndex` / `parseAccessToken_exposesIat` / `parseAccessToken_legacyTokenWithoutIat_returnsNullIat` RED→GREEN；既有 TokenService 全部测试回归绿；`mvn test -Dtest=TokenServiceIatTest,TokenServiceTest`

- [x] T006 撤销纪元强制校验（fail-closed）（依赖 T005）
  - **Files**: Modify `MAIN/infrastructure/config/security/filter/JwtAuthorizationTokenFilter.java`（黑名单检查后插入纪元校验） · Modify `MAIN/domain/service/security/TokenService.java`（`refreshAccessTokenWithUser` :327 加纪元校验） · Test `TEST/domain/service/security/EpochRevocationTest.java` · Test `TEST/infrastructure/config/security/filter/JwtAuthorizationTokenFilterEpochTest.java`
  - **Interfaces**: Consumes 键 `auth:user:revoke_epoch:{userId}`（改密时写，T041）；判定逻辑（两处一致）：
    ```java
    Object epoch = cacheGateway.get("auth:user:revoke_epoch:" + userId);
    // epoch == null → 放行（含无 iat 旧 token，部署兼容）
    // epoch != null && (iat == null || iat <= epoch) → 拒绝：filter 401 ResultCode.TOKEN_ACCESS_EXPIRED(AUT00210)；refresh 路径抛认证异常
    ```
  - **AC**: 测试矩阵（Review Focus 1）`noMarker_legacyTokenWithoutIat_passes` / `markerPresent_iatMissing_rejected` / `markerPresent_iatBeforeEpoch_rejected` / `markerPresent_iatAfterEpoch_passes`（filter 与 refresh 各 4 例）RED→GREEN；既有认证/刷新测试回归绿；`mvn test -Dtest=EpochRevocationTest,JwtAuthorizationTokenFilterEpochTest`

**Checkpoint**: 认证基础设施就绪——用户故事可以开始

---

## Phase 3: User Story 1 - 角色面板与技能成长 (Priority: P1) 🎯 MVP

**Goal**: 绑定角色的基础信息/在线/位置/舰船 + 技能列表/队列/属性，直查 ESI + 015 缓存

**Independent Test**: `mvn test -Dtest=CharacterPanel*Test,EsiApiServiceCharacter*Test`；手动按 quickstart US1（panel/skill-queue/空队列/多角色）

### Tests for User Story 1 ⚠️ 先写并确认 FAIL

- [x] T007 [P] [US1] 应用服务失败测试 `TEST/application/service/CharacterPanelApplicationServiceTest.java`
  - **Files**: Test `TEST/application/service/CharacterPanelApplicationServiceTest.java`
  - **Interfaces**: Consumes（预期签名，T012 实现）`CharacterPanelInfo panel(Integer characterId, boolean forceRefresh)` / `CharacterSkillsInfo skills(Integer characterId, boolean forceRefresh)` / `SkillQueueInfo skillQueue(Integer characterId, boolean forceRefresh)`；Mock EsiGateway/AuthorizeUtil/InvTypesService
  - **AC**: 用例 `panel_composesFourGatewayCalls_andResolvesShipName` / `skills_mergesSkillListAndAttributes` / `skillQueue_emptyQueue_returnsEmptyListWithNullQueueEndsAt`（与查询失败可区分，spec US1-3）/ `skillQueue_queueEndsAt_equalsLastEntryEndDate` / `panel_authFailure_propagatesDistinguishableError`（spec US1-4）/ `panel_otherUsersCharacter_rejected`（spec US1-5，AuthorizeUtil 抛异常）；运行确认 RED（类不存在/断言失败）

- [x] T008 [P] [US1] 控制器失败测试 `TEST/interfaces/web/controller/CharacterPanelControllerTest.java`
  - **Files**: Test `TEST/interfaces/web/controller/CharacterPanelControllerTest.java`
  - **Interfaces**: Consumes contracts §1 三端点 `GET /character/{characterId}/panel|skills|skill-queue`（forceRefresh 可选）；MockMvc standaloneSetup + mock 应用服务
  - **AC**: 用例 `panel_returnsResultEnvelope` / `skills_returnsAttributesAndSkillList` / `skillQueue_empty_returnsEmptyArray` / `forceRefresh_passedToService` / `ownershipViolation_returns403`；data 无 Boolean；运行确认 RED

### Implementation for User Story 1

- [x] T009 [US1] 网关：角色公开信息 + 在线状态（端口+实现+VO）
  - **Files**: Modify `MAIN/domain/port/esi/EsiGateway.java` · Modify `MAIN/infrastructure/gateway/esi/EsiApiService.java`（按需构造器注入 CharacterApi/LocationApi） · Create `MAIN/domain/model/vo/CharacterPublicInfo.java` · Create `MAIN/domain/model/vo/CharacterOnlineInfo.java` · Test `TEST/infrastructure/gateway/esi/EsiApiServiceCharacterPanelTest.java`
  - **Interfaces**: Produces（各含 `boolean forceRefresh` 重载，仿既有双签名）：
    `CharacterPublicInfo queryCharacterPublicInfo(Integer characterId)`；`CharacterOnlineInfo queryCharacterOnline(Integer characterId, String accessToken)`
    record：`CharacterPublicInfo(Integer characterId, String name, Integer corporationId, Integer allianceId, OffsetDateTime birthday, Double securityStatus)`；`CharacterOnlineInfo(boolean online, OffsetDateTime lastLogin, OffsetDateTime lastLogout)`
    Consumes SDK `CharacterApi#queryCharacter(Integer, String datasource)`（CharacterApi.java:49，公开无 token）/ `LocationApi#queryCharacterOnline(Integer, String, String)`（LocationApi.java:69）；EsiResponseCache（publicInfo 用 pub 段 userId=null；online 用 userId 段）
  - **AC**: 测试 `publicInfo_usesPubCacheSegment` / `online_cacheHit_noSdkCall` / `online_forceRefresh_bypassesCacheViaLimiter` / `sdkError_convertedToAccessException` RED→GREEN；`mvn test -Dtest=EsiApiServiceCharacterPanelTest`

- [x] T010 [US1] 网关：位置 + 舰船（依赖 T009，同文件顺序执行）
  - **Files**: Modify `MAIN/domain/port/esi/EsiGateway.java` · Modify `MAIN/infrastructure/gateway/esi/EsiApiService.java` · Create `MAIN/domain/model/vo/CharacterLocationInfo.java` · Test 同 T009 测试类追加
  - **Interfaces**: Produces `CharacterLocationInfo queryCharacterLocation(Integer characterId, String accessToken)` / `Integer queryCharacterShipTypeId(Integer characterId, String accessToken)`（各含 forceRefresh 重载）；record `CharacterLocationInfo(Integer solarSystemId, Integer stationId, Long structureId)`；Consumes `LocationApi#queryCharacterLocation`（:46）/ `#queryCharacterShip`（:92）
  - **AC**: 测试 `location_structureIdPopulated_stationIdNull_whenInStructure`（contracts §1 建筑内位置语义）/ `ship_returnsTypeId` / 缓存命中与强刷各 1 例 RED→GREEN

- [x] T011 [US1] 网关：技能列表 + 属性 + 技能队列（依赖 T010）
  - **Files**: Modify `MAIN/domain/port/esi/EsiGateway.java` · Modify `MAIN/infrastructure/gateway/esi/EsiApiService.java` · Create `MAIN/domain/model/vo/CharacterSkillList.java`（含嵌套 `SkillItem`） · Create `MAIN/domain/model/vo/CharacterAttributesInfo.java` · Create `MAIN/domain/model/vo/SkillQueueItem.java` · Test 同 T009 测试类追加
  - **Interfaces**: Produces `CharacterSkillList queryCharacterSkills(Integer characterId, String accessToken)` / `CharacterAttributesInfo queryCharacterAttributes(Integer characterId, String accessToken)` / `List<SkillQueueItem> queryCharacterSkillQueue(Integer characterId, String accessToken)`（各含 forceRefresh 重载）
    record：`CharacterSkillList(Long totalSp, Integer unallocatedSp, List<SkillItem> skills)`，嵌套 `SkillItem(Integer skillId, Integer trainedSkillLevel, Integer activeSkillLevel, Long skillpointsInSkill)`；`CharacterAttributesInfo(Integer intelligence, Integer memory, Integer charisma, Integer perception, Integer willpower, Integer bonusRemaps)`；`SkillQueueItem(Integer skillId, Integer finishedLevel, Long trainingStartSp, Long skillpointsInSkill, OffsetDateTime startDate, OffsetDateTime endDate, Integer position)`
    Consumes `SkillApi#queryCharactersSkills`（:96）/ `#queryCharactersAttributes`（:48）/ `#queryCharactersSkillqueue`（:72，Flux）；空队列回填 `List.of()`（015 空 body 约定）
  - **AC**: 测试 `skills_mapsTotalSpAndSkillItems` / `attributes_mapsFiveAttributesAndBonusRemaps` / `skillQueue_emptyBody_returnsEmptyList` / `skillQueue_mapsEndDateAndPosition` RED→GREEN

- [x] T012 [US1] 应用服务 CharacterPanelApplicationService（依赖 T009–T011；GREEN T007）
  - **Files**: Create `MAIN/application/service/CharacterPanelApplicationService.java` · Create `MAIN/domain/model/vo/CharacterPanelInfo.java` · Create `MAIN/domain/model/vo/CharacterSkillsInfo.java` · Create `MAIN/domain/model/vo/SkillQueueInfo.java`
  - **Interfaces**: Produces T007 预期三方法；record `CharacterPanelInfo(Integer characterId, String name, Integer corporationId, Integer allianceId, OffsetDateTime birthday, Double securityStatus, boolean online, OffsetDateTime lastLogin, OffsetDateTime lastLogout, Integer solarSystemId, Integer stationId, Long structureId, Integer shipTypeId, String shipName)`；`CharacterSkillsInfo(Long totalSp, Integer unallocatedSp, CharacterAttributesInfo attributes, List<CharacterSkillList.SkillItem> skills)`（合并视图，data-model §3）；`SkillQueueInfo(List<SkillQueueItem> queue, OffsetDateTime queueEndsAt)`
    Consumes `AuthorizeUtil.authorize(characterId)` → EveAccount；`EsiGateway.getAccessTokenWithExpiry(characterId, userId)`（EsiGateway.java:55）取 token；`InvTypesService.getNameByTypeIds(List<Integer>)`（InvTypesService.java:51，返回 Map<Integer,String>）解析 shipName
  - **AC**: T007 全部用例 GREEN；组合顺序＝公开信息→在线→位置→舰船（网关层各自缓存，应用层不做结果缓存，YAGNI）；`mvn test -Dtest=CharacterPanelApplicationServiceTest`

- [x] T013 [US1] 控制器 + 响应 DTO（依赖 T012；GREEN T008）
  - **Files**: Create `MAIN/interfaces/web/controller/CharacterPanelController.java` · Create `MAIN/application/dto/response/CharacterPanelResponse.java`（嵌套 Location/Ship record，形状＝contracts §1） · Create `MAIN/application/dto/response/CharacterSkillsResponse.java` · Create `MAIN/application/dto/response/SkillQueueResponse.java`（queue 条目含 skillName，经 InvTypesService 批量解析）
  - **Interfaces**: Consumes T012 三方法；`@RequestMapping("/character")` + `@GetMapping("/{characterId}/panel"|"/{characterId}/skills"|"/{characterId}/skill-queue")`
  - **AC**: T008 全部用例 GREEN；Result 信封、forceRefresh 默认 false；`mvn test -Dtest=CharacterPanelControllerTest,CharacterPanelApplicationServiceTest`；提交含 US1 验收说明

**Checkpoint**: US1 独立可验收（MVP）——面板/技能/队列全链路

---

## Phase 4: User Story 2 - 邮件收发 (Priority: P2)

**Goal**: 收件箱游标分页/读信/标记/未读数/发信/精确名称解析；发信权限缺失可区分提示

**Independent Test**: `mvn test -Dtest=Mail*Test,EsiApiServiceMail*Test`；手动按 quickstart US2（含缺发信权限角色读信正常）

### Tests for User Story 2 ⚠️ 先写并确认 FAIL

- [x] T014 [P] [US2] 应用服务失败测试 `TEST/application/service/MailApplicationServiceTest.java`
  - **Files**: Test `TEST/application/service/MailApplicationServiceTest.java`
  - **Interfaces**: Consumes（预期，T019 实现）`MailHeaderPage inbox(Integer characterId, Integer lastMailId, boolean forceRefresh)` / `MailLabelsInfo labels(Integer characterId, boolean forceRefresh)` / `MailDetailInfo detail(Integer characterId, Integer mailId, boolean forceRefresh)` / `void markRead(Integer characterId, Integer mailId, boolean read)` / `Integer send(Integer characterId, SendMailRequest request)` / `List<RecipientResolution> resolveNames(List<String> names)`
  - **AC**: 用例 `inbox_passesLastMailIdCursor` / `labels_returnsTotalUnread` / `detail_returnsRawBody`（不转换不执行）/ `markRead_delegatesToGateway` / `send_success_returnsMailId` / `send_permissionLow_throwsReauthHint`（EsiAccessException.permissionLow → 可区分错误，读信不受影响，spec US2-4）/ `send_otherUsersCharacter_rejected`（spec US2-5）/ `resolveNames_partialUnresolved_marksResolvedFalse`（spec US2-7）；RED 证据

- [x] T015 [P] [US2] 控制器失败测试 `TEST/interfaces/web/controller/MailControllerTest.java`
  - **Files**: Test `TEST/interfaces/web/controller/MailControllerTest.java`
  - **Interfaces**: Consumes contracts §2 六端点 + data-model §6 校验（recipients≤50、subject≤128、body≤10000、names≤50、元素 @NotBlank）
  - **AC**: 用例 `inbox_cursorParamOptional` / `send_validation_rejectsOversizedSubject` / `send_validation_rejectsEmptyRecipients` / `resolve_validation_rejectsMoreThan50Names` / `markRead_requiresNonNullRead` / `send_response_containsMailId`；RED 证据

### Implementation for User Story 2

- [x] T016 [US2] 网关：邮件读侧 headers/body/labels
  - **Files**: Modify `MAIN/domain/port/esi/EsiGateway.java` · Modify `MAIN/infrastructure/gateway/esi/EsiApiService.java`（注入 MailApi） · Create `MAIN/domain/model/vo/MailHeaderPage.java`（嵌套 MailHeaderItem） · Create `MAIN/domain/model/vo/MailDetailInfo.java` · Create `MAIN/domain/model/vo/MailRecipient.java` · Create `MAIN/domain/model/vo/MailLabelsInfo.java`（嵌套 MailLabel） · Test `TEST/infrastructure/gateway/esi/EsiApiServiceMailTest.java`
  - **Interfaces**: Produces（各含 forceRefresh 重载）：
    `MailHeaderPage queryCharacterMailHeaders(Integer characterId, Integer lastMailId, String accessToken)`；`MailDetailInfo queryCharacterMail(Integer characterId, Integer mailId, String accessToken)`；`MailLabelsInfo queryCharacterMailLabels(Integer characterId, String accessToken)`
    record：`MailHeaderPage(List<MailHeaderItem> mails, Integer nextLastMailId)`，嵌套 `MailHeaderItem(Integer mailId, Integer from, String subject, OffsetDateTime timestamp, Boolean isRead, List<Integer> labels)`；`MailDetailInfo(Integer mailId, Integer from, List<MailRecipient> recipients, String subject, OffsetDateTime timestamp, String body, Boolean isRead)`；`MailRecipient(Integer recipientId, String recipientType)`；`MailLabelsInfo(List<MailLabel> labels, Long totalUnread)`，嵌套 `MailLabel(Integer labelId, String name, Long unreadCount)`
    Consumes `MailApi#queryCharacterMails(Integer, String, List<Integer> labels, Integer lastMailId, String)`（MailApi.java:55，labels 传 null）/ `#queryCharacterMail`（:133）/ `#queryCharacterMailLabels`（:189）；缓存包裹仿工业列表（lastMailId 编入键 page 段）
  - **AC**: 测试 `headers_nextLastMailId_equalsLastMailId_whenFull50` / `headers_nextLastMailId_null_whenLessThan50`（R4 游标语义）/ `labels_mapsTotalUnreadCount` / `body_returnsRawContent` / 缓存命中 1 例 RED→GREEN

- [x] T017 [US2] 网关：邮件写侧 markRead/send（依赖 T016，同文件）
  - **Files**: Modify `MAIN/domain/port/esi/EsiGateway.java` · Modify `MAIN/infrastructure/gateway/esi/EsiApiService.java` · Create `MAIN/domain/model/vo/SendMailCommand.java` · Test 同 T016 追加
  - **Interfaces**: Produces `void markCharacterMailRead(Integer characterId, Integer mailId, boolean read, String accessToken)` / `Integer sendCharacterMail(Integer characterId, SendMailCommand command, String accessToken)`（写操作不做响应缓存、无 forceRefresh）；record `SendMailCommand(List<MailRecipient> recipients, String subject, String body)`
    Consumes `MailApi#updateCharacterMail(Integer, String, Integer, List<Integer> labelIds, Boolean read, String)`（:162，labelIds 传 null）/ `#addCharacterMail(Integer, String, NewMail, String)`（:80，返回 mailId）；recipientType 四值 character/corporation/alliance/mailing_list（SDK NewMail 已支持）
  - **AC**: 测试 `markRead_delegatesReadFlag` / `send_mapsRecipientsAndReturnsMailId` / `send_sdkError_convertedToAccessException`（permissionLow 可区分）RED→GREEN

- [x] T018 [US2] 网关：universe/ids 精确名称解析（依赖 T017，同文件）
  - **Files**: Modify `MAIN/domain/port/esi/EsiGateway.java` · Modify `MAIN/infrastructure/gateway/esi/EsiApiService.java` · Create `MAIN/domain/model/vo/RecipientResolution.java` · Test 同 T016 追加
  - **Interfaces**: Produces `List<RecipientResolution> resolveUniverseIds(List<String> names)`；record `RecipientResolution(String name, Integer id, String type, boolean resolved)`（type=character/corporation/alliance）
    Consumes `UniverseApi#queryUniverseIds(List<String>, String datasource, String language)`（UniverseApi.java:321，公开无 Authorization）；仿 `queryUniverseNames`（EsiApiService.java:996-1015）：pub 段、不查 UserUtil、不限流、**names 排序后确定性编码进缓存键**（R6）；响应按 characters/corporations/alliances 分组映射，未命中项 resolved=false
  - **AC**: 测试 `resolve_pubCacheSegment_noUserContext` / `resolve_sortedNames_deterministicCacheKey` / `resolve_mixedTypes_mapsAllGroups` / `resolve_unknownName_resolvedFalse` RED→GREEN

- [x] T019 [US2] 应用服务 + 请求 DTO（依赖 T016–T018；GREEN T014）
  - **Files**: Create `MAIN/application/service/MailApplicationService.java` · Create `MAIN/application/dto/request/SendMailRequest.java`（嵌套 Recipient record：recipientId @NotNull + recipientType 枚举校验） · Create `MAIN/application/dto/request/ResolveNamesRequest.java` · Create `MAIN/application/dto/request/MarkMailReadRequest.java` · Create `MAIN/application/dto/response/SendMailResponse.java`（Integer mailId） · Create `MAIN/application/dto/response/ResolveNamesResponse.java`（List<RecipientResolution> results）
  - **Interfaces**: Produces T014 预期六方法；DTO 校验＝data-model §6 逐字；Consumes AuthorizeUtil.authorize + getAccessTokenWithExpiry + T016–T018 端口；SendMailRequest→SendMailCommand 映射在应用层
  - **AC**: T014 全部 GREEN；发信权限缺失错误码可区分（不连带阻塞读信）；`mvn test -Dtest=MailApplicationServiceTest`

- [x] T020 [US2] 控制器 MailController（依赖 T019；GREEN T015）
  - **Files**: Create `MAIN/interfaces/web/controller/MailController.java`
  - **Interfaces**: `@RequestMapping("/mail")`；六端点＝contracts §2（inbox/labels/{mailId}/{mailId}/read/send/recipients/resolve）；回信无专用端点（前端复用 send，YAGNI）
  - **AC**: T015 全部 GREEN；`mvn test -Dtest=MailControllerTest,MailApplicationServiceTest`

**Checkpoint**: US2 独立可验收——读信链路与发信链路分别通过

---

## Phase 5: User Story 3 - 工业订单查询 (Priority: P3)

**Goal**: 按绑定角色直查工业订单（端口已存在），状态筛选 + 分页；QQ 侧 POST 同步零变更

**Independent Test**: `mvn test -Dtest=IndustryJobQuery*Test,JobControllerTest`；手动按 quickstart US3（缓存期内二次调用不触发 ESI）

### Tests for User Story 3 ⚠️ 先写并确认 FAIL

- [x] T021 [P] [US3] 应用服务失败测试 `TEST/application/service/IndustryJobQueryApplicationServiceTest.java`
  - **Files**: Test `TEST/application/service/IndustryJobQueryApplicationServiceTest.java`
  - **Interfaces**: Consumes（预期，T022 实现）`IndustryJobPageResponse queryJobs(String type, Integer id, String status, PageQuery pageQuery, boolean forceRefresh)`；Mock EsiGateway 既有 `queryCharacterIndustryJobs(Integer, boolean includeCompleted, String, boolean)`（EsiGateway.java:156）/ `queryCorporationIndustryJobs`（:146）
  - **AC**: 用例 `charJobs_returnsAllStatuses_whenStatusNull` / `charJobs_filtersByStatus`（spec US3-2）/ `charJobs_empty_returnsEmptyListNotError`（spec US3-3）/ `charJobs_pagination_slicesInMemory` / `corpJobs_requiresCorpRole`（无权限拒绝）/ `otherUsersCharacter_rejected`；includeCompleted=true 取全量后内存过滤；RED 证据

### Implementation for User Story 3

- [x] T022 [US3] 应用服务 + JobController GET + DTO（依赖 T021；GREEN）
  - **Files**: Create `MAIN/application/service/IndustryJobQueryApplicationService.java` · Modify `MAIN/interfaces/web/controller/JobController.java`（仅追加 GET，既有 `@PostMapping("/{type}/{id}/{complete}")` :38 零改动） · Create `MAIN/application/dto/response/IndustryJobPageResponse.java`（嵌套 IndustryJobItem record：jobId/activityId/status/productTypeId/runs/startDate/endDate/installerId/facilityId，形状＝contracts §3） · Test `TEST/interfaces/web/controller/JobControllerTest.java`
  - **Interfaces**: Produces T021 预期方法；`@GetMapping("/{type}/{id}")`（type=char/corp，status/page/size/forceRefresh 可选）；Consumes IndustryJob 实体字段（IndustryJob.java：jobId:20/activityId:25/status:130/productTypeId:50/runs:70/startDate:115/endDate:120/installerId:140/facilityId:110）；corp 权限校验经 `queryCharacterRoles`（T029 产出；若 US3 先于 T029 执行则本任务内先实现该端口方法——与 T029 同一签名，二者取先执行者，后执行者跳过端口部分仅补测试）
  - **AC**: T021 全部 GREEN + JobControllerTest `getJobs_returnsResultEnvelope` / `postSync_unchanged`；既有工业相关测试回归绿（FR-018）；`mvn test -Dtest=IndustryJobQueryApplicationServiceTest,JobControllerTest`

**Checkpoint**: US3 独立可验收——直查+筛选+分页，缓存防重复

---

## Phase 6: User Story 4 - APP 通知通道 (Priority: P4)

**Goal**: 三事件判定→去重→持久化→WS 实时送达；燃料权限扇出；建筑同步每军团≤1/h；QQ 零变更

**Independent Test**: `mvn test -Dtest=Notification*Test,AppNotification*Test,AppWebSocket*Test,AppStructureSync*Test`；手动按 quickstart US4（含 300 连接负载与 QQ 回归）

### Implementation for User Story 4（每任务内部 TDD：先失败测试再实现）

- [x] T023 [P] [US4] 通知枚举 + 实体 + PO + MapStruct 转换器
  - **Files**: Create `MAIN/shared/enums/AppNotificationEventType.java`（INDUSTRY_COMPLETE/FUEL_LOW/SKILL_QUEUE_EMPTY） · Create `MAIN/domain/model/entity/system/AppNotification.java`（extends BaseEntity；id Long/userId Integer/eventType String/dedupKey String/characterId Integer/corpId Integer/title String/content String/isRead Boolean） · Create `MAIN/infrastructure/persistence/entity/system/AppNotificationPO.java`（@TableName("app_notification")，extends BaseEntity，gmtCreate/gmtModified 由 MyMetaObjectHandler 填充） · Create `MAIN/infrastructure/persistence/assembler/persistence/AppNotificationPoConverter.java`（MapStruct po2Domain/domain2Po 含 List 重载，仿 StructurePoConverter） · Test `TEST/infrastructure/persistence/assembler/persistence/AppNotificationPoConverterTest.java`
  - **AC**: 测试 `roundtrip_allFieldsPreserved` / `listConversion_preservesOrder` RED→GREEN；枚举非法值不入库（检测服务只用枚举 .name()）

- [x] T024 [US4] 通知仓储三件套（依赖 T023；**前置：测试库已执行 T001 DDL**）
  - **Files**: Create `MAIN/domain/repository/system/AppNotificationRepository.java` · Create `MAIN/infrastructure/persistence/mapper/system/AppNotificationMapper.java` · Create `src/main/resources/mappers/system/AppNotificationMapper.xml` · Create `MAIN/infrastructure/persistence/repository/system/AppNotificationRepositoryImpl.java` · Test `TEST/infrastructure/persistence/repository/system/AppNotificationRepositoryImplTest.java`（@SpringBootTest test profile）
  - **Interfaces**: Produces（data-model §1.5 逐字）：
    ```java
    int insertBatch(List<AppNotification> notifications);
    boolean existsByDedupKey(String dedupKey);
    boolean existsByDedupKeyWithinHours(String dedupKey, int hours);
    PageResult<AppNotification> pageByUser(Integer userId, Boolean unreadOnly, PageQuery pageQuery);
    long countUnread(Integer userId);
    int markRead(Long id, Integer userId);   // WHERE id=? AND user_id=?
    int deleteExpired(int retentionDays, int batchSize);
    ```
    Consumes SystemDatasourceConfig mapper 扫描（eve_helper）；MybatisPlusConfig 分页 maxLimit 1000
  - **AC**: 测试 `insertBatch_persistsAllRows` / `existsByDedupKey_ignoresTimeWindow`（一次性事件语义）/ `existsByDedupKeyWithinHours_boundary24h`（23h59m 内 true、25h 前 false）/ `pageByUser_unreadOnlyFilters` / `pageByUser_isolatedByUserId` / `markRead_otherUsersRow_returnsZero` / `deleteExpired_batchedDelete` RED→GREEN；SQL 全参数化；idx_dedup 命中（EXPLAIN 抽查）

- [x] T025 [P] [US4] WS 推送领域端口 + 会话注册表
  - **Files**: Create `MAIN/domain/port/appws/AppNotificationPushGateway.java` · Create `MAIN/infrastructure/external/appws/AppWebSocketSessionRegistry.java`（@Component，实现端口） · Test `TEST/infrastructure/external/appws/AppWebSocketSessionRegistryTest.java`
  - **Interfaces**: Produces：
    ```java
    public interface AppNotificationPushGateway {
        boolean isOnline(Integer userId);
        void pushToUser(Integer userId, String payload);   // payload=JSON 信封字符串
        void closeUserSessions(Integer userId);
    }
    ```
    注册表 `ConcurrentHashMap<Integer, CopyOnWriteArraySet<Session>>`；`register(Integer userId, Session)` / `unregister(Integer userId, Session)`（onClose 调用，双清理防泄漏，规避 OneBot SESSION_POOL 先例）；发送 `synchronized(session)` + `getAsyncRemote().sendText`（仿 OneBot WebSocket.java:120-132）
  - **AC**: 测试（Review Focus 3）`pushToUser_multiDevice_fanoutAllSessions` / `pushToUser_offlineUser_noOp` / `unregister_removesSession_andEmptySetCleaned` / `closeUserSessions_closesAllAndClears` / `pushToUser_oneSessionFails_othersStillReceive`（mock Session）RED→GREEN

- [x] T026 [US4] WS 端点薄适配器 + ticket 鉴权 + SecurityConfig（依赖 T025）
  - **Files**: Create `MAIN/infrastructure/external/appws/AppNotificationWebSocket.java`（@ServerEndpoint("/websocket/app")，复用既有 ServerEndpointExporter，WebSocketConfiguration.java:16-27 @Profile("!test")） · Modify `MAIN/infrastructure/config/security/SecurityConfig.java`（/websocket/app permitAll，仿 onebot :47） · Test `TEST/infrastructure/external/appws/AppWebSocketHandlerTest.java`
  - **Interfaces**: Consumes `CacheGateway.get` + `compareAndDelete`（CacheGateway.java:92，Lua 原子）；键 `app:ws:ticket:{uuid}`（值 userId，TTL 30s，T027 创建）；onOpen：`get` → null 即关闭；`compareAndDelete(key, userId)` → false 即关闭（一次性消费）；成功 → registry.register；onClose → registry.unregister；`maxIdleTimeout=90000`
  - **AC**: 测试（Review Focus 3）`onOpen_validTicket_registersAndConsumes` / `onOpen_missingTicket_closes` / `onOpen_expiredTicket_closes` / `onOpen_replayedTicket_closes`（compareAndDelete 第二次 false）/ `onClose_unregisters` RED→GREEN；JWT 不进 URL（端点只读 ticket 参数）；SecurityConfig 既有链零回归

- [x] T027 [US4] 通知 REST 用例 + 控制器（依赖 T024/T025；内部先写 NotificationServiceTest/NotificationControllerTest RED）
  - **Files**: Create `MAIN/application/service/NotificationApplicationService.java` · Create `MAIN/interfaces/web/controller/NotificationController.java` · Create `MAIN/application/dto/response/NotificationItem.java`（id/eventType/title/content/characterId/corpId/isRead/gmtCreate） · Create `MAIN/application/dto/response/WsTicketResponse.java`（ticket/expiresInSeconds） · Create `MAIN/application/dto/response/UnreadCountResponse.java`（count） · Test `TEST/application/service/NotificationServiceTest.java` · Test `TEST/interfaces/web/controller/NotificationControllerTest.java`
  - **Interfaces**: Produces `PageResult<NotificationItem> page(Boolean unreadOnly, PageQuery pageQuery)` / `UnreadCountResponse unreadCount()` / `void markRead(Long id)` / `WsTicketResponse createWsTicket()`；userId 一律 `UserUtil.getUserId()`（UserUtil.java:27）；ticket＝UUID，`setIfAbsent("app:ws:ticket:"+ticket, userId, 30, SECONDS)`
  - **AC**: 测试 `page_onlyOwnNotifications` / `unreadCount_ownOnly` / `markRead_otherUsersNotification_rejected`（markRead 返回 0 → 友好错误不泄露存在性）/ `wsTicket_storedWith30sTtl` / `wsTicket_uniquePerCall`；控制器四端点＝contracts §5；RED→GREEN；`mvn test -Dtest=NotificationServiceTest,NotificationControllerTest`

- [x] T028 [US4] EveAccountRepository 查询扩展（检测/扇出/同步选角共用）
  - **Files**: Modify `MAIN/domain/repository/system/EveAccountRepository.java` · Modify `MAIN/infrastructure/persistence/repository/system/EveAccountRepositoryImpl.java` · Modify `MAIN/infrastructure/persistence/mapper/system/EveAccountMapper.java` · Modify `src/main/resources/mappers/system/EveAccountMapper.xml` · Test `TEST/infrastructure/persistence/repository/system/EveAccountRepositoryQueryTest.java`
  - **Interfaces**: Produces `List<EveAccount> listAll()`（检测遍历全部绑定；军团枚举＝stream map corpId distinct）/ `List<EveAccount> listByCorpId(Integer corpId)`（燃料扇出+同步选角）；EveAccount 含 corpId/userId/characterId 字段（EveAccount.java:30-75）
  - **AC**: 测试 `listAll_returnsAllBindings` / `listByCorpId_filtersByCorp` / `listByCorpId_unknownCorp_emptyList` RED→GREEN；既有方法零改动

- [x] T029 [US4] 网关：角色军团权限 roles（US5 与燃料扇出共用）
  - **Files**: Modify `MAIN/domain/port/esi/EsiGateway.java` · Modify `MAIN/infrastructure/gateway/esi/EsiApiService.java` · Create `MAIN/domain/model/vo/CharacterRolesInfo.java` · Test `TEST/infrastructure/gateway/esi/EsiApiServiceRolesTest.java`
  - **Interfaces**: Produces `CharacterRolesInfo queryCharacterRoles(Integer characterId, String accessToken)`（含 forceRefresh 重载）；record `CharacterRolesInfo(List<String> rolesAtHq, List<String> rolesAtBase, List<String> rolesAtOther)`；Consumes `CharacterApi#queryCharacterRoles(Integer, String, String)`（CharacterApi.java:296，RoleResponse）；缓存按 userId 段
  - **AC**: 测试 `roles_mapsThreeCollections` / `roles_cacheHit_noSdkCall` / `roles_nullCollections_normalizedToEmptyList` RED→GREEN；建筑权限判定常量（Director/Station_Manager 出现于任一集合）与 Director 追踪权限精确集合在本任务以 role fixtures 固化（对照 ESI structures/membertracking 鉴权文档，R5）

- [x] T030 [US4] 建筑同步领域服务 + 节流任务（依赖 T028/T029）
  - **Files**: Create `MAIN/domain/service/system/AppStructureSyncService.java` · Create `MAIN/infrastructure/util/AppStructureSyncTask.java` · Test `TEST/domain/service/system/AppStructureSyncServiceTest.java` · Test `TEST/infrastructure/util/AppStructureSyncTaskTest.java`
  - **Interfaces**: Produces `void syncAllCorps()`；流程：listAll distinct corpId → 逐军团 `setIfAbsent("app:struct:sync:"+corpId, "1", 3600, SECONDS)`（false→跳过）→ listByCorpId 选一个 roles 含建筑权限的角色 → `AuthorizeUtil.authorizeInternal(account.getUserId(), characterId)` → getAccessTokenWithExpiry → `queryCorporationStructuresMaxPage` + 逐页 `queryCorporationStructures(corpId, "zh", page, token)` → `StructureRepository.batchInsertOrUpdate(list)`（userId=同步角色属主，014 语义）；无权限角色的军团跳过；任务 `@Scheduled(cron = "0 5 0/1 * * ?")`（hourly 错开 QQ 整点 5 分）
  - **AC**: 测试 `sync_throttled_secondCallWithinHour_skips`（SETNX false）/ `sync_noPermittedCharacter_skipsCorp` / `sync_writesWithOwnerUserId` / `sync_oneCorpFails_othersContinue` / `task_delegatesToService` RED→GREEN；**不复用 StructureService.batchInsertOrUpdateFromEsi**（内部 SYSTEM 身份，R13）；StructureService/StructTask 零 diff

- [x] T031 [US4] 检测领域服务：燃料事件 + 权限扇出（依赖 T024/T025/T028/T029）
  - **Files**: Create `MAIN/domain/service/system/AppNotificationDetectService.java` · Test `TEST/domain/service/system/AppNotificationDetectServiceFuelTest.java`
  - **Interfaces**: Produces `void detectFuelLow()`；流程：运行锁 `setIfAbsent("app:notify:lock:FUEL_LOW", uuid, 600, SECONDS)`（finally compareAndDelete 释放）→ listAll distinct corpId → 逐军团 `StructureRepository.selectFuelExpiresList(24, corpId)`（StructureRepository.java:40，含 fuel_expires IS NULL）→ 排除势力铁壁 typeIds 47513–47516（镜像 StructTask.java:42-47，不改 QQ 类）→ 逐建筑 `existsByDedupKeyWithinHours("FUEL_LOW:"+structureId, 24)`（**全局判定，任一接收者行在窗口→整体跳过**）→ listByCorpId → 逐绑定角色 queryCharacterRoles 筛 Director/Station_Manager → 每接收者一行（dedup_key 相同）insertBatch → 逐用户 `pushToUser`（在线判定 isOnline；离线仅入库）
  - **AC**: 测试（Review Focus 2）`fuel_globalDedup_anyRecipientRowInWindow_skipsWholeRound` / `fuel_fanout_onlyPermittedUsers`（无权限用户零行，SC-006）/ `fuel_excludesFactionCitadels`（47513–47516）/ `fuel_nullFuelExpires_treatedAsExhausted` / `fuel_offlineUser_persistedNotPushed` / `fuel_lockHeld_skipsRun` RED→GREEN；title/content 模板纯文本≤128/512 超长截断

- [x] T032 [US4] 检测领域服务：工业 + 技能队列 + 隔离（依赖 T031，同文件）
  - **Files**: Modify `MAIN/domain/service/system/AppNotificationDetectService.java` · Test `TEST/domain/service/system/AppNotificationDetectServiceIndustrySkillTest.java`
  - **Interfaces**: Produces `void detectIndustryComplete()` / `void detectSkillQueueEmpty()` / `int cleanupExpired()`；工业谓词（R13 建造者细化，防首轮历史刷屏）：
    ```java
    boolean hit = ("active".equals(job.getStatus()) && !job.getEndDate().isAfter(now.plusHours(24)))
               || ("delivered".equals(job.getStatus()) && !job.getEndDate().isBefore(now.minusHours(24)));
    ```
    逐绑定角色（listAll）authorizeInternal → queryCharacterIndustryJobs(cid, true, token)（既有端口）→ 逐订单 `existsByDedupKey("INDUSTRY_COMPLETE:"+jobId)`（一次性，不限时间）→ insertBatch（属主）+ pushToUser；技能：queryCharacterSkillQueue 空 → `existsByDedupKeyWithinHours("SKILL_QUEUE_EMPTY:"+characterId, 24)` → 同上；cleanupExpired＝`deleteExpired(30, 500)` 循环至 0；各方法独立运行锁（eventType 键）
  - **AC**: 测试（Review Focus 4）`industry_activeWithin24h_notifies` / `industry_deliveredOlderThan24h_skipped` / `industry_deliveredWithin24h_notifies` / `industry_oneShotDedup_everWithinRetention` / `skill_emptyQueue_notifies` / `skill_nonEmptyQueue_noNotification` / `skill_24hRepeat_afterWindow` / `detect_oneCharacterAuthFails_othersContinue` / `detect_unboundCharacter_excluded`（listAll 天然排除）/ `cleanup_batchesUntilZero` RED→GREEN

- [x] T033 [US4] 检测定时任务 AppNotificationTask（依赖 T030/T032）
  - **Files**: Create `MAIN/infrastructure/util/AppNotificationTask.java` · Test `TEST/infrastructure/util/AppNotificationTaskTest.java`
  - **Interfaces**: Consumes AppNotificationDetectService 四方法；cron：燃料 `@Scheduled(cron = "0 0 18,22 * * ?")`（复用 QQ StructTask.java:65）；工业双 cron `"0 0 18,20-23 ? * 1-5"` + `"0 0 8-18,20-23 ? * 0,6"`（复用 QQ IndustryTask.java:50-51）；技能 `@Scheduled(cron = "0 10 0/1 * * ?")`（hourly，建造者裁定）；清理 `@Scheduled(cron = "0 30 3 * * ?")`（daily）
  - **AC**: 测试 `task_fuel_delegatesToDetectFuelLow` 等四委托用例（mock 服务）+ cron 表达式注解断言（反射读 @Scheduled 值＝上述精确字符串）RED→GREEN；QQ 任务类零 diff

- [x] T034 [US4] 去重与扇出场景套件（SC-007/SC-006 边界固化；依赖 T032）
  - **Files**: Test `TEST/domain/service/system/NotificationEventDetectorTest.java`（quickstart 指定类名；纯单元 + fixtures，不依赖 DB）
  - **Interfaces**: Consumes T031/T032 服务 + mock 仓储/端口
  - **AC**: 场景矩阵 `sameEvent_within24h_noDuplicate` / `persistentCondition_after24h_newReminder` / `persistentCondition_cleared_stops`（条件解除后不再产生）/ `industryComplete_notifiedExactlyOnce_acrossRounds` / `fanout_userLosesPermission_noFurtherFuelNotifications`（spec Edge Case）/ `multiCharacterUser_notificationsIsolatedPerCharacter`；全部 GREEN；覆盖率计入 SC-004

- [x] T035 [US4] 负载与 QQ 回归验证（SC-003/SC-008；依赖 T026/T033）
  - **Files**: Test `TEST/infrastructure/external/appws/AppWebSocketLoadTest.java` · 验证记录附提交信息
  - **Interfaces**: Consumes T025 注册表（300 mock Session 扇出）
  - **AC**: `load_300Sessions_fanoutCompletes`（300 连接注册→单事件推送→全部收到，耗时断言 p95≤5s 量级，无连接降级）；通知列表查询路径 <200ms 抽样（T024 分页测试计时）；QQ 回归：`mvn test -Dtest=StructTaskTest,IndustryTaskTest,StructureServiceTest,StructureServiceBatchSyncTest`（四类均已存在于 src/test）全绿 + `git diff --stat` 证明 QQ 五类零变更；真实网络级 300 连接压测按 quickstart 手动项在 P5 执行并记录

**Checkpoint**: US4 独立可验收——三事件生成/去重/列表/实时送达/已读，QQ 零变化

---

## Phase 7: User Story 5 - 军团团员追踪 (Priority: P5)

**Goal**: Director 角色直查团员在线/位置/舰船，不落库，缓存按用户隔离

**Independent Test**: `mvn test -Dtest=MemberTracking*Test,EsiApiServiceMemberTrackingTest`；手动按 quickstart US5

### Tests for User Story 5 ⚠️ 先写并确认 FAIL

- [x] T036 [P] [US5] 应用服务失败测试 `TEST/application/service/MemberTrackingApplicationServiceTest.java`
  - **Files**: Test `TEST/application/service/MemberTrackingApplicationServiceTest.java`
  - **Interfaces**: Consumes（预期，T038 实现）`MemberTrackingInfo track(Integer corpId, Integer characterId, boolean forceRefresh)`；Mock EsiGateway.queryCharacterRoles（T029）/ queryCorporationMemberTracking（T037）/ AuthorizeUtil
  - **AC**: 用例 `track_directorRole_returnsMembers` / `track_nonDirector_rejectedWithReason`（spec US5-2）/ `track_onlineDerived_logonAfterLogoff`（online=true）与 `track_offlineDerived_logoffAfterLogon` / `track_otherUsersCharacter_rejected` / `track_cacheKeyContainsUserId`（隔离，spec US5-3，验证网关调用 token 按用户获取）；RED 证据

### Implementation for User Story 5

- [x] T037 [US5] 网关：团员追踪 membertracking
  - **Files**: Modify `MAIN/domain/port/esi/EsiGateway.java` · Modify `MAIN/infrastructure/gateway/esi/EsiApiService.java` · Create `MAIN/domain/model/vo/MemberTrackingItem.java` · Test `TEST/infrastructure/gateway/esi/EsiApiServiceMemberTrackingTest.java`
  - **Interfaces**: Produces `List<MemberTrackingItem> queryCorporationMemberTracking(Integer corporationId, String accessToken)`（含 forceRefresh 重载）；record `MemberTrackingItem(Integer characterId, Long locationId, Integer shipTypeId, OffsetDateTime logonDate, OffsetDateTime logoffDate)`；Consumes `CorporationApi#queryCorporationMembersTracking(Integer, String, String)`（CorporationApi.java:446，Flux）；缓存 userId 段（按用户隔离）
  - **AC**: 测试 `tracking_mapsAllFields` / `tracking_cacheIsolatedByUserId` / `tracking_permissionLow_convertedToAccessException` RED→GREEN

- [x] T038 [US5] 应用服务 + 控制器 + 聚合 VO（依赖 T036/T037/T029；GREEN）
  - **Files**: Create `MAIN/application/service/MemberTrackingApplicationService.java` · Create `MAIN/interfaces/web/controller/MemberTrackingController.java` · Create `MAIN/domain/model/vo/MemberTrackingInfo.java`（嵌套 MemberTrackingMember：characterId/online/locationId/shipTypeId/logonDate/logoffDate；trackedAt OffsetDateTime） · Test `TEST/interfaces/web/controller/MemberTrackingControllerTest.java`
  - **Interfaces**: Produces T036 预期方法；Director 校验＝queryCharacterRoles 三集合含 "Director"（T029 fixtures 固化的精确集合）；online 派生＝`logonDate != null && (logoffDate == null || logonDate.isAfter(logoffDate))`；`@GetMapping("/corporation/{corpId}/member-tracking")`（characterId 必填参数）
  - **AC**: T036 全部 GREEN + 控制器 `tracking_returnsResultEnvelope` / `tracking_missingCharacterId_400`；`mvn test -Dtest=MemberTrackingApplicationServiceTest,MemberTrackingControllerTest`

**Checkpoint**: US5 独立可验收——权限校验+直查+缓存隔离

---

## Phase 8: User Story 6 - 账号自助 (Priority: P6)

**Goal**: 改密（验旧密+限流+全会话失效+WS 关闭）、解绑（守卫+停同步通知）、授权入口

**Independent Test**: `mvn test -Dtest=AccountSelfServiceTest,UserApplicationServicePasswordTest,CharacterApplicationServiceUnbindTest`；手动按 quickstart US6

### Tests for User Story 6 ⚠️ 先写并确认 FAIL

- [x] T039 [P] [US6] 自助服务失败测试 `TEST/application/service/AccountSelfServiceTest.java`
  - **Files**: Test `TEST/application/service/AccountSelfServiceTest.java`
  - **Interfaces**: Consumes（预期）`UserApplicationService.changePassword(ChangePasswordRequest)`（T041）/ `CharacterApplicationService.unbindCharacter(Integer characterId)`（T042）/ `CharacterApplicationService.getAuthorizeUrl(String server)`（T043）
  - **AC**: 用例 `changePassword_correctOld_succeeds_andRevokesAllSessions`（纪元写入+逐 sid 撤销+WS 关闭，spec US6-1）/ `changePassword_wrongOld_rejected`（spec US6-2）/ `changePassword_rateLimited_after5Failures` / `changePassword_sameAsOld_rejected` / `unbind_ownCharacter_deletesBinding`（spec US6-3）/ `unbind_otherUsersCharacter_rejected`（spec US6-4）/ `unbind_systemCharacter_rejected`（TaskConstant.CHARACTER_ID 守卫）/ `authUrl_returnsUrlWithScopes` / `authUrl_serverMismatch_friendlyError`；RED 证据

### Implementation for User Story 6

- [x] T040 [US6] EveAccountRepository 删除方法
  - **Files**: Modify `MAIN/domain/repository/system/EveAccountRepository.java` + Impl/Mapper · Test `TEST/infrastructure/persistence/repository/system/EveAccountRepositoryDeleteTest.java`
  - **Interfaces**: Produces `int deleteByUserIdAndCharacterId(Integer userId, Integer characterId)`（WHERE 双条件，归属内建）
  - **AC**: 测试 `delete_ownBinding_removesRow` / `delete_otherUsersBinding_returnsZero` RED→GREEN

- [x] T041 [US6] 改密全链路（依赖 T004–T006/T025/T039；GREEN）
  - **Files**: Modify `MAIN/application/service/UserApplicationService.java`（+changePassword，构造器追加依赖） · Create `MAIN/application/dto/request/ChangePasswordRequest.java`（oldPassword @NotBlank；newPassword @NotBlank @Size(min=8,max=64)） · Modify `MAIN/interfaces/web/controller/UserController.java`（+`@PutMapping("/password")`，既有端点零改动） · Test `TEST/application/service/UserApplicationServicePasswordTest.java` · Test `TEST/interfaces/web/controller/UserControllerPasswordTest.java`
  - **Interfaces**: Produces `void changePassword(ChangePasswordRequest request)`；流程（R10 四步）：① 限流 `pwd:attempt:{userId}`（increment+expire 30min，≥5 拒绝，仿 LoginRateLimiterService.java:25-27 模式）→ ② `SysUserRepository.queryById(userId)`（SysUserRepository.java:22）+ `BCryptPasswordEncoder.matches` 验旧密（失败 increment 计数）→ ③ 新旧相同拒绝；`insertOrUpdateSelective` 更新 password（BCrypt encode）→ ④ 写 `auth:user:revoke_epoch:{userId}`=now 毫秒（TTL 7d）→ `sMembers("refresh_user:"+userId)` 逐 sid `TokenService.revokeRefreshTokenBySession(sid)`（:210）+ `markSessionRevoked(sid)`（:276）→ `AppNotificationPushGateway.closeUserSessions(userId)` → 清计数
  - **AC**: T039 改密用例 GREEN + `changePassword_epochWrittenWith7dTtl` / `changePassword_allIndexedSessionsRevoked` / `changePassword_wsSessionsClosed` / `changePassword_oldAccessTokenRejectedAfterEpoch`（联动 T006）；控制器 `putPassword_validation_rejectsShortNewPassword`；`mvn test -Dtest=UserApplicationServicePasswordTest,UserControllerPasswordTest`

- [x] T042 [US6] 解绑角色（依赖 T040/T039；GREEN）
  - **Files**: Modify `MAIN/application/service/CharacterApplicationService.java`（+unbindCharacter） · Modify `MAIN/interfaces/web/controller/UserController.java`（+`@DeleteMapping("/characters/{characterId}")`） · Test `TEST/application/service/CharacterApplicationServiceUnbindTest.java`
  - **Interfaces**: Produces `void unbindCharacter(Integer characterId)`；流程：AuthorizeUtil.authorize(characterId) 属主校验 → 守卫 `characterId.equals(TaskConstant.CHARACTER_ID)`（2112818290）→ 拒绝「系统同步角色不可解绑」→ `deleteByUserIdAndCharacterId(userId, characterId)` → `CacheGateway.delete("esi_access_token:"+userId+":"+characterId)`；解绑后 AUT00208 天然 fail-closed（016 先例）；检测遍历 listAll 自动排除（FR-022）；历史数据按 014 保留
  - **AC**: T039 解绑用例 GREEN + `unbind_deletesAccessTokenCache` / `unbind_subsequentCharacterEndpoint_failsClosed`；`mvn test -Dtest=CharacterApplicationServiceUnbindTest`

- [x] T043 [US6] 授权入口 auth-url（依赖 T039；GREEN）
  - **Files**: Modify `MAIN/domain/port/esi/EsiGateway.java` · Modify `MAIN/infrastructure/gateway/esi/EsiApiService.java` · Modify `MAIN/application/service/CharacterApplicationService.java`（+getAuthorizeUrl） · Modify `MAIN/interfaces/web/controller/CharacterController.java`（+`@GetMapping("/auth-url")`，既有 `@PostMapping("/{code}")` :33 零改动） · Create `MAIN/application/dto/response/AuthorizeUrlResponse.java`（authorizeUrl/state） · Test `TEST/application/service/CharacterApplicationServiceAuthUrlTest.java`
  - **Interfaces**: Produces 端口 `String buildAuthorizeUrl(String server)`（第 16 方法）；实现：server 非空且 ≠ `esiClientProperties.getDatasource()`（EsiClientProperties.java:52，默认 serenity）→ 抛 EveHelperException 友好错误；否则 `AuthorizeOAuth.authorizeUrl(SsoScopes.ALL)`（AuthorizeOAuth.java:47，SsoScopes.java:79）；应用层 `AuthorizeUrlResponse getAuthorizeUrl(String server)`（state 固定 "eve-helper"＝AuthorizeOAuth.AGENT）；晨曦 PKCE 有意留空（既有约束，不改）
  - **AC**: T039 authUrl 用例 GREEN + `authUrl_containsAllRequiredScopes` / `authUrl_defaultServer_usesConfiguredDatasource`；`mvn test -Dtest=CharacterApplicationServiceAuthUrlTest`

**Checkpoint**: US6 独立可验收——改密全会话失效、解绑守卫、授权入口

---

## Phase 9: Polish & Cross-Cutting Concerns

- [x] T044 [P] 安全回归矩阵（SC-006，Review Focus 5）
  - **Files**: Test `TEST/interfaces/web/controller/SecurityRegressionTest.java`
  - **AC**: 跨属主矩阵——用户 A 访问用户 B 角色的 panel/skills/skill-queue/mail 六端点/job/member-tracking → 全部 403/友好拒绝；燃料扇出边界（无权限用户零通知，复用 T034 fixtures）；改密/解绑越权拒绝；未认证访问全部 18 端点 → 401；`mvn test -Dtest=SecurityRegressionTest` 全绿

- [x] T045 覆盖率达标（SC-004 ≥80%）
  - **Files**: 按缺口补测试（新代码范围）
  - **AC**: `ecc:test-coverage`（JaCoCo）报告新代码行覆盖 ≥80%；缺口清单→补测试→复测，输出真实报告；豁免项（如纯配置类）记录理由

- [x] T046 [P] 知识文档（仓库约定）
  - **Files**: Create `docs/knowledge/022-companion-api.md` · Modify `docs/INDEX.md`
  - **AC**: 覆盖六能力流程/通知判定与去重/WS ticket 时序/改密会话失效机制/部署步骤（DDL+RBAC+重启）；INDEX 登记；中文

- [x] T047 RBAC SQL 终审 + 部署核对
  - **Files**: Modify `src/SQL/convert/022_rbac_permissions.sql`（如与实现漂移则修正） · Modify `src/SQL/convert/deploy_all_pending.sql`（同步）
  - **AC**: 18 端点逐条对照实际控制器映射（方法+路径+变量名）；无遗漏/无多余；重启要求注释在位

- [x] T048 quickstart 全量验证 + P5 准备
  - **Files**: 验证记录（提交信息/会话输出）
  - **AC**: `mvn clean package -DskipTests` exit 0；`mvn test` 全量——按 memory test-baseline-volatility 逐用例 diff 分离 Failures/Errors（勿用固定阈值）；quickstart 六故事手动路径逐项执行记录；SC-003 真实 300 连接压测（手动项）；产出 VERIFICATION REPORT 草案供 P5

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup（P1）**: 无依赖，立即开始（T001/T002 并行 → T003）
- **Foundational（P2）**: T004 → T005 → T006 顺序（同文件）；**阻塞全部故事**
- **US1–US6**: 均依赖 Foundational 完成；故事间原则独立，例外：
  - US3 的 corp 权限校验消费 T029（US4）——T022 已注明先执行者实现端口
  - US5 消费 T029（roles）——建议 US4 的 T029 先于 US5，或 US5 内先行实现（同签名）
  - US6 改密消费 T025（closeUserSessions）——US6 须在 T025 后
- **Polish**: 依赖全部故事完成（T046/T047 可提前并行）

### User Story Dependencies（推荐顺序）

```text
Foundational → US1(MVP) → US2 → US3 → US4 → US5 → US6 → Polish
                                      └─ T029 可提前至 US3 前（corp 权限共用）
```

### Within Each User Story

- 测试先写并确认 RED → 网关（端口+实现+VO）→ 应用服务 → 控制器 → 故事验收
- 同文件任务顺序执行（EsiGateway/EsiApiService 为全故事共享文件，**跨故事不标 [P]**）

### Parallel Opportunities

- T001 ∥ T002；T004 ∥（T001/T002）
- 各故事测试任务 [P] 可与前一故事实现并行编写
- T023 ∥ T025 ∥ T028（US4 内不同文件）；T044 ∥ T046
- 不同故事可由不同执行者并行（共享文件 EsiGateway/EsiApiService 须协调合并顺序）

## Parallel Example: User Story 1

```bash
# 先并行写两个失败测试：
Task: "T007 CharacterPanelApplicationServiceTest（RED）"
Task: "T008 CharacterPanelControllerTest（RED）"
# 网关三任务顺序（同文件）：T009 → T010 → T011
# 然后 T012（GREEN T007）→ T013（GREEN T008）
```

## Implementation Strategy

### MVP First（US1）

1. Phase 1 Setup + Phase 2 Foundational（认证基础先行，回归保护）
2. Phase 3 US1 → **STOP & VALIDATE**：面板/技能/队列独立验收
3. 可交付演示（APP 第一屏）

### Incremental Delivery

US1 → US2（邮件）→ US3（工业）→ US4（通知，最大增量）→ US5（管理视角）→ US6（自助）→ Polish；每故事验收不破坏前序（QQ 回归每故事末抽查）。

### Execution Mode（P4 启动时向用户展示二选一）

- `superpowers:subagent-driven-development`（默认推荐）：每任务全新子代理 + 两阶段评审
- `superpowers:executing-plans`：本会话分批 + 检查点

## Notes

- 每任务 RED/GREEN 证据 + 小步提交；任务级评审（ecc:java-reviewer）Critical/Important 修完才进下一任务
- 共享文件（EsiGateway/EsiApiService/UserController）追加式修改，既有签名与行为零改动
- 测试类名与 quickstart.md 对齐；@SpringBootTest 用例依赖内网 DB（不可达时按 memory 对照复跑归因，勿误判回归）
- 部署（P7）：测试库/生产库手动执行 022 DDL + RBAC → **重启应用**（权限缓存仅启动重建）
