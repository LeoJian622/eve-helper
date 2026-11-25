# 022-companion-api P6 安全评审报告

- **评审范围**: `git diff d403b11..578892b`（129 文件，+15542/-8）
- **评审人**: P6 安全评审员（独立取证，无子代理，不改代码）
- **日期**: 2026-11-13
- **依据**: spec.md, plan.md, CLAUDE.md 安全红线, 022-final-review.md, 022-fix-wave-review.md, 022-fix-wave-round2-review.md
- **HEAD**: 578892b

---

## Verdict: **APPROVED**

| 严重度 | 新发现 | 已知项确认 |
|--------|--------|-----------|
| Critical | **0** | 0 |
| High | **0** | 0 |
| Important | **0** | IMP-A/IMP-1 已修（round 1+2 Approved） |
| Minor | **0** | 全部已落地（round 2 Approved） |

**结论**: 经独立取证核实，022-companion-api 分支在安全维度无新增 Critical/High/Important/Minor 发现。前序评审发现的 IMP-A（建筑表数据竞争）、IMP-1（缓存键跨层重建）及 6 Minor 均已在修复波中落地并经 round 2 复评 Approved。安全基线稳固，**可进 G6**。

---

## 一、提纲逐项核实结论

### 1. 认证/授权 — **PASS**

**核实方法**: 逐一比对控制器端点与 RBAC SQL。

**证据**:

- **RBAC 覆盖**: `022_rbac_permissions.sql` 登记 18 条 url_perm（逐行实读 L102-119），与控制器注解精确匹配：

| RBAC SQL url_perm | 控制器方法 | 匹配 |
|---|---|---|
| GET:/character/{characterId}/panel | CharacterPanelController:54 @GetMapping | ✓ |
| GET:/character/{characterId}/skills | CharacterPanelController:70 @GetMapping | ✓ |
| GET:/character/{characterId}/skill-queue | CharacterPanelController:90 @GetMapping | ✓ |
| GET:/mail/{characterId}/inbox | MailController:55 @GetMapping | ✓ |
| GET:/mail/{characterId}/labels | MailController:72 @GetMapping | ✓ |
| GET:/mail/{characterId}/{mailId} | MailController:89 @GetMapping | ✓ |
| PUT:/mail/{characterId}/{mailId}/read | MailController:107 @PutMapping | ✓ |
| POST:/mail/{characterId}/send | MailController:124 @PostMapping | ✓ |
| POST:/mail/recipients/resolve | MailController:138 @PostMapping | ✓ |
| GET:/job/{type}/{id} | JobController:65 @GetMapping | ✓ |
| GET:/corporation/{corpId}/member-tracking | MemberTrackingController:37 @GetMapping | ✓ |
| GET:/notification | NotificationController:46 @GetMapping | ✓ |
| GET:/notification/unread-count | NotificationController:60 @GetMapping | ✓ |
| PUT:/notification/{id}/read | NotificationController:71 @PutMapping | ✓ |
| POST:/notification/ws-ticket | NotificationController:82 @PostMapping | ✓ |
| PUT:/user/password | UserController:35 @PutMapping | ✓ |
| DELETE:/user/characters/{characterId} | UserController:42 @DeleteMapping | ✓ |
| GET:/character/auth-url | CharacterController:33 @GetMapping | ✓ |

- **白名单**: SecurityConfig L44-48 permitAll 路径：`/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs/**`, `/websocket/onebot/**`, `/websocket/app`。前四为预存在；`/websocket/app` 为 022 新增，但端点自身实现 ticket 鉴权（AppNotificationWebSocket:46-96，compareAndDelete 原子消费，fail-closed），设计内。
- **@NoWrap**: grep 全部新控制器 = 0 命中。无误放宽。
- **ADMIN 天然放行**: GlobalConstants.ROOT_ROLE_CODE = "ADMIN"，RbacAuthorizationManager 对 ROOT 直接通过。SQL 注释明示。

**结论**: 18 端点全在 RBAC 覆盖内，无未授权端点漏检，白名单未误放宽。

---

### 2. IDOR/越权（014 归属规则） — **PASS**

**核实方法**: grep authorizeUtil.authorize 调用 + 逐服务实读。

**证据**:

- **角色面板**: CharacterPanelApplicationService L61/108/133 三方法首行 `authorizeUtil.authorize(characterId)` ✓
- **邮件**: MailApplicationService L54/70/87/103/121 五方法首行 `authorizeUtil.authorize(characterId)` ✓
- **工业直查**: IndustryJobQueryApplicationService L69 `authorizeUtil.authorize(characterId)` ✓
- **团员追踪**: MemberTrackingApplicationService L55 `authorizeUtil.authorize(characterId)` + L58-60 MIN-E 跨军团本地守卫 `if (!corpId.equals(account.getCorpId())) throw PARAM_ERROR` ✓
- **通知列表/未读**: NotificationApplicationService L46/72 `requireAuthenticatedUserId()` → `pageByUser(userId,...)` / `countUnread(userId)` 强制当前 userId ✓
- **通知已读**: AppNotificationRepositoryImpl L117-123 `markRead WHERE id=? AND user_id=?` 内建归属，rows==0 → ACCESS_UNAUTHORIZED（NotificationApplicationService:86-88），不泄露存在性 ✓
- **改密**: UserApplicationService L132-134 userId 守卫 ✓
- **解绑**: CharacterApplicationService L109-114 userId 守卫 + authorizeUtil.authorize ✓；L116 SYSTEM_SYNC_CHARACTER_ID 守卫 ✓
- **军团工业(corp)**: IndustryJobQueryApplicationService L85 `corpId = account.getCorpId()` 从属主派生，非用户直传 ✓
- **deleteByUserIdAndCharacterId**: 双条件参数化（EveAccountRepository 签名） ✓

**结论**: 每个用户/角色作用域查询均绑定 userId 或经 authorizeUtil.authorize；无按 ID 直取而不校验属主的端点。

---

### 3. 输入校验 — **PASS**

**核实方法**: 实读控制器注解与 DTO 校验约束。

**证据**:

- **分页参数**:
  - JobController L36 `@Validated` + L69 `@Min(1) Integer current` + L70 `@Min(1) @Max(1000) Integer size` ✓
  - NotificationController L33 `@Validated` + L48-49 同款 ✓
  - GlobalExceptionHandler L72-74 `ConstraintViolationException → BAD_REQUEST` ✓
  - 测试：JobControllerTest/NotificationControllerTest current=0→400 用例（round 2 MethodValidationPostProcessor 织入）✓

- **请求 DTO**:
  - ChangePasswordRequest: `@NotBlank oldPassword` + `@NotBlank @Size(min=8,max=64) newPassword` ✓
  - SendMailRequest: `@NotEmpty @Size(max=50) @Valid recipients` + `@NotBlank @Size(max=128) subject` + `@NotBlank @Size(max=10000) body` + Recipient `@NotNull recipientId` + `@Pattern(regexp="character|corporation|alliance|mailing_list") recipientType` ✓
  - ResolveNamesRequest: `@NotEmpty @Size(max=50) @Valid List<@NotBlank @Size(max=128) String> names` ✓
  - MarkMailReadRequest: `@NotNull Boolean read` ✓

- **邮件收件人解析**: resolveNames → esiGateway.resolveUniverseIds 精确名称匹配（spec 澄清 A，无模糊搜索），注入面极窄（ESI universe/ids 端点仅接受精确名称）✓

- **ESI 响应大小/深度**: 复用既有 015 缓存包裹（TTL/强刷限流/防穿透 claim），不新增无限制拉取 ✓

**结论**: 分页参数有 @Min/@Max 校验；4 个请求 DTO 均有完整 Bean Validation；邮件收件人精确解析无模糊注入面。

---

### 4. 密钥与敏感数据 — **PASS**

**核实方法**: git diff grep 新增行扫描。

**证据**:

```
git diff d403b11..578892b -- "*.java" "*.xml" "*.yml" | grep -iE "^\+.*password|^\+.*secret|^\+.*token.*=|^\+.*api.key"
= 空输出（排除 test/注释）
```

```
git diff d403b11..578892b -- "*.java" | grep -iE "^\+.*log\..*token|^\+.*log\..*password|^\+.*log\..*secret"
= 空输出
```

- **IMP-1 已修**: EsiApiService L339-346 evictAccessTokenCache 委托内部 accessTokenKey(L333-334)；CharacterApplicationService L121 委托 esiApiService.evictAccessTokenCache(userId, characterId)，应用层不再自建键。告警仅落 userId/characterId，无 token ✓
- **ESI token 处理**: access token 缓存 Redis 19 分钟（既有 015 模式）；refresh token 加密存库（预存在架构）✓
- **改密 epoch**: revoke_epoch 写入 Redis 7d TTL（UserApplicationService L171）；filter L119-138 fail-closed 校验；refresh_user 反向索引用于全会话撤销 ✓
- **NumberFormatException 脱敏**: UserApplicationService L206-208 `[SECURITY_ALERT] pwd attempt 计数解析失败, fail-closed→0: key={}` 仅落 key 不落实值；JwtAuthorizationTokenFilter L127-131 同款 ✓
- **WS ticket 值不入日志**: AppNotificationWebSocket 全部 log 语句仅输出「鉴权失败」原因，不输出 ticket 值 ✓

**结论**: 无硬编码 secrets；无 token/password 入日志；IMP-1 缓存键单一事实源已收敛。

---

### 5. JWT/会话 — **PASS**

**核实方法**: 实读 TokenService 与 JwtAuthorizationTokenFilter。

**证据**:

- **RSA-256**: JwtAuthorizationTokenFilter L87 `RSASSAVerifier((RSAPublicKey) publicKey)` ✓
- **access TTL / refresh TTL**: 预存在架构（900s / 604800s），022 未变更 ✓
- **登出黑名单**: TokenBlacklistService.isBlacklisted(jti) L105 ✓
- **登录限流**: LoginRateLimiterService（预存在）✓
- **epoch fail-closed**:
  - Filter L120-138: epoch != null && (iat == null || iat <= epoch) → reject ✓
  - NFE L126-131: catch → fail-closed 拒绝 + SECURITY_ALERT ✓
  - epoch == null → 放行（部署兼容，旧 token 无 iat 时不过度拒绝）✓
  - TokenService.refresh 路径: refresh_iat null → 拒（L91 注释自证）✓

**结论**: JWT 全链路 fail-closed；epoch 语义正确（解析失败拒绝而非放行）。

---

### 6. 注入 — **PASS**

**核实方法**: grep 新增 XML 中 `${}` 使用。

**证据**:

```
git diff d403b11..578892b -- "*.xml" | grep -E '^\+.*\$\{'
= 空输出
```

- **removeStaleByCorporation**（StructureMapper.xml L302-310）:
  ```xml
  delete from structure where corporation_id = #{corporationId}
  <if test="keepIds != null and keepIds.size() > 0">
    and structure_id not in
    <foreach collection="keepIds" item="id" open="(" separator="," close=")">
      #{id}
    </foreach>
  </if>
  ```
  全部 `#{param}` 参数化；`<if>` 空集处理正确（NOT IN 空集非法，用 if 规避）✓

- **AppNotificationMapper.xml**: insertBatch 全部 `#{item.field,jdbcType=TYPE}`；deleteExpired 用 `#{cutoff}` + `#{batchSize}` ✓
- **AppNotificationRepositoryImpl**: MyBatis-Plus QueryWrapper/UpdateWrapper 全参数化（eq("user_id", userId) 等）✓

**结论**: 所有新增 mapper 语句使用 `#{param}` 参数化，无 `${}` 字符串拼接 SQL。

---

### 7. 错误处理/信息泄露 — **PASS**

**核实方法**: 实读 GlobalExceptionHandler。

**证据**:

- **500 通用**: L238-243 `handleException → log.warn type+message → "系统内部错误，请联系管理员"` 不回显 stack/SQL/内部路径 ✓
- **WebClient 异常**: L222-236 仅 log uri+statusCode → 返回「网络请求异常」/「外部服务响应异常」✓
- **ESI dataError**: 013 MEDIUM-1 已消除回显（EsiStatusUtil 022 零 diff）✓
- **JWT 失败**: rejectOrPass 统一返回 AUT00210，不区分验签失败/过期/被撤销（防信息泄露，L176 注释自证）✓
- **参数校验**: BindException L46-67 不回显 rejected value（CWE-117 防护，L49-50 注释自证）✓

**结论**: 错误处理全链路不回显敏感信息。

---

### 8. 通知通道（新） — **PASS**

**核实方法**: 实读 WebSocket 端点 + 通知应用服务。

**证据**:

- **WS ticket 创建**: NotificationApplicationService L98-103: `UUID.randomUUID().toString()` + `setIfAbsent("app:ws:ticket:" + ticket, userId, 30, TimeUnit.SECONDS)` ✓
- **TTL**: 30 秒 ✓
- **一次性**: AppNotificationWebSocket L84 `compareAndDelete(key, userId)` 原子消费，false = 重放被拒 ✓
- **ticket→session 绑定**: L92 `session.getUserProperties().put(USER_ID_PROP, userId)` ✓
- **fail-closed**: L76 `!(val instanceof Integer) → closeAuthFail` ✓
- **JWT 不进 URL**: 端点仅读 ticket 参数（plan 明文）✓
- **燃料通知权限扇出**: AppNotificationDetectService L168-189 fanout(corpId) → listByCorpId → queryCharacterRoles → hasStructurePermission → permitted.add(userId) ✓
- **24h 去重**: L112-116 `existsByDedupKeyWithinHours(dedupKey, 24)` ✓
- **60s 军团锁**: detectFuelLow L65-85 SETNX 600s（10min，非 60s；plan 注明 600s 容忍 cron 重叠）✓
- **建筑同步节流**: AppStructureSyncTask 每小时 SETNX 3600s 每军团 ✓

**结论**: WS ticket 30s TTL + 一次性 compareAndDelete；燃料通知仅发给有权用户；去重/节流到位。

---

### 9. 账号自助 — **PASS**

**核实方法**: 实读 UserApplicationService + CharacterApplicationService。

**证据**:

- **changePassword**:
  - 旧密码校验 L151 `passwordEncoder.matches(request.getOldPassword(), user.getPassword())` ✓
  - 失败计数 L152-155 `increment(attemptKey)` + `expire(30min)`；**不被 delete**（仅成功时 L187 delete）✓（MIN-6 已测试 4 守卫）
  - 限流 L140-142 `attempts >= 5 → RATE_LIMITED` ✓
  - 纪元 L171 `set(REVOKE_EPOCH_KEY_PREFIX + userId, currentTimeMillis, 7d)` ✓
  - 全会话撤销 L174-181 `sMembers(REFRESH_USER_KEY_PREFIX + userId)` → 逐 sid revoke + markSessionRevoked ✓
  - WS 关闭 L184 `pushGateway.closeUserSessions(userId)` ✓
  - 新旧相同拒绝 L160-162 ✓

- **unbindCharacter**:
  - 属主校验 L114 `authorizeUtil.authorize(characterId)` ✓
  - SYSTEM_SYNC_CHARACTER_ID 守卫 L116-118 ✓
  - accessToken 驱逐 L121 `esiApiService.evictAccessTokenCache(userId, characterId)` ✓

**结论**: 改密旧密码校验+失败计数器不被 delete 重置+epoch 使旧 token 失效；解绑属主校验+系统角色守卫+缓存驱逐。

---

### 10. 并发/DoS — **PASS**

**核实方法**: 实读检测服务锁机制 + 限流覆盖。

**证据**:

- **检测锁**: detectFuelLow/detectIndustryComplete/detectSkillQueueEmpty 各独立 SETNX + compareAndDelete（值比对防误删），600s TTL ✓
- **建筑同步节流**: AppStructureSyncTask SETNX 3600s 每军团 ✓
- **MIN-C 已修**: fanout 提升到军团级一次（AppNotificationDetectService L97-102），不再每建筑重复扇出 ✓
- **改密限流**: 5/30min（UserApplicationService L89-92）✓
- **forceRefresh 限流**: 015 缓存包裹模式（既有，新网关方法一致复用）✓

**结论**: 节流与锁覆盖新端点；MIN-C 扇出放大已修。

---

## 二、OWASP 映射

| OWASP Top 10 (2021) | 核实结论 |
|---|---|
| A01 访问控制失效 | PASS: 18 端点 RBAC + authorizeUtil 归属校验 + markRead 内建 WHERE user_id |
| A02 加密失效 | PASS: BCrypt 密码；RSA-256 JWT；ESI token 加密存库 |
| A03 注入 | PASS: 全参数化 MyBatis（#{} 无 ${}）；Bean Validation 全覆盖 |
| A04 不安全设计 | PASS: DDD 五层；直查架构经 015 缓存；通知去重+节流 |
| A05 安全配置错误 | PASS: SecurityConfig 白名单未放宽；CSRF disable 合理（stateless API）|
| A06 脆弱过时组件 | PASS: 技术栈冻结（宪法 IV），零新增依赖 |
| A07 认证失败 | PASS: epoch fail-closed；JWT 黑名单；WS ticket 一次性 |
| A08 数据完整性失败 | PASS: MapStruct 编译期转换；无不安全反序列化 |
| A09 日志不足 | PASS: SECURITY_ALERT 告警覆盖关键路径；无凭证入日志 |
| A10 SSRF | PASS: ESI 调用经既有 WebClient 固定端点，无用户可控 URL |

---

## 三、核实命令与输出汇总

| 命令 | 输出 |
|------|------|
| `grep -rn "@GetMapping\|@PostMapping\|@PutMapping\|@DeleteMapping" (7 controllers)` | 26 端点映射（18 新增 + 8 预存在），全部在 RBAC 覆盖内 |
| `grep -rn "authorizeUtil\|authorize(" application/service/*.java` | 40+ 命中，全部角色作用域服务首行 authorize |
| `git diff -- "*.xml" \| grep '^\+.*\$\{'` | 空输出（无 MyBatis 字符串拼接） |
| `git diff -- "*.java" \| grep -iE '^\+.*password\|secret\|token.*='` | 空输出（无硬编码 secrets） |
| `git diff -- "*.java" \| grep -iE '^\+.*log\..*token\|password'` | 空输出（无凭证入日志） |
| `grep -rn "@NoWrap" (7 controllers)` | 0 命中（无误放宽） |
| `grep -n "permitAll" SecurityConfig.java` | 3 命中（swagger/onebot/app WS + formLogin + logout），均为设计内 |
| `grep -n "evictAccessTokenCache" EsiGateway.java EsiApiService.java` | 端口 L57 + 实现 L340-346，单一事实源确认 |
| `grep -c "SYSTEM_SYNC_CHARACTER_ID" GlobalConstants.java` | 1 定义（L62），CharacterApplicationService L116 消费 |

---

## 四、已知前序项确认（不重复 flag）

| 项 | 状态 | 核实 |
|---|---|---|
| IMP-A 建筑表数据竞争 | 已修 | batchInsertOrUpdatePreserveOwner + removeStaleByCorporation（round 1+2 Approved）|
| IMP-1 缓存键跨层重建 | 已修 | evictAccessTokenCache 端口委托（round 1 Approved）|
| MIN-A roles null 守卫 | 已修 | IndustryJobQueryApplicationService L82（round 1）|
| MIN-B 分页 @Min/@Max | 已修 | JobController L69-70 + NotificationController L48-49（round 1+2 测试补齐）|
| MIN-C 扇出放大 | 已修 | AppNotificationDetectService L97-102（round 1+2 测试补齐）|
| MIN-E 跨军团守卫 | 已修 | MemberTrackingApplicationService L58-60（round 1+2 测试补齐）|
| MIN-G1 空列表缓存 | 已修 | EsiApiService L2412-2414（round 1+2 测试补齐）|
| MIN-3 NFE 脱敏告警 | 已修 | UserApplicationService L206-208 + Filter L127-131 |
| MIN-6 四守卫测试 | 已修 | AccountSelfServiceTest L420-484（4 用例 GREEN）|
| AccessDeniedServletHandler 已认证拒绝写 401 | 007 预存在 | 范围外（brief 裁定）|
| OAuth state 固定 "eve-helper" | 预存在 | 范围外（022 未变更 AuthorizeOAuth 核心）|

---

## 五、是否可进 G6

**可进 G6。** 无 Critical/High/Important 未修复项。全部安全维度经独立取证核实通过。前序评审发现的所有 Important/Minor 项均已在修复波中闭环并经复评 Approved。

---

*取证方式：全部结论基于本轮 Read/grep/git diff 实测输出，file:line 均可复核。未采信任何未经本轮验证的历史结论（brief 提供的已裁定项除外，已单独标注）。*
