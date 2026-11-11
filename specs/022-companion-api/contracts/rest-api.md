# REST API Contracts — 022 伴侣 APP 后端支撑端点（第一期）

> APP 前端项目的接口契约依据。信封/分页/错误语义沿用现有 `shared/kernel/base` 与 `Result<T>` 定义（精确字段见 research.md）。
> 所有端点需 JWT Bearer 认证（除白名单）+ RBAC `METHOD:PATH` 登记；直查端点支持可选 `forceRefresh`（015 限流）。
> 示例值均为合成数据。

## 1. 角色面板（US1 / FR-001~003）

| 方法 | 路径 | 参数 | 响应 data | 说明 |
|------|------|------|-----------|------|
| GET | `/character/{characterId}/panel` | forceRefresh? | `{characterId, name, corporationId, allianceId, birthday, securityStatus, online, lastLogin, lastLogout, location:{solarSystemId, stationId, structureId}, ship:{typeId, name}}` | 基础信息+在线+位置+舰船聚合；直查 ESI 组合；建筑内位置以 structureId 表示（stationId 为 null） |
| GET | `/character/{characterId}/skills` | forceRefresh? | `{totalSp, unallocatedSp, attributes:{intelligence, memory, charisma, perception, willpower, bonusRemaps}, skills:[{skillId, trainedSkillLevel, activeSkillLevel, skillpointsInSkill}]}` | 技能列表+属性 |
| GET | `/character/{characterId}/skill-queue` | forceRefresh? | `{queue:[{skillId, skillName, finishedLevel, trainingStartSp, skillpointsInSkill, endDate, position}], queueEndsAt}` | 队列空→`queue:[]`；`queueEndsAt` 为最后条目 endDate |

授权缺失/失效：沿用 013/021 授权状态语义（可区分提示），HTTP 200 + Result 错误码或 403 友好映射（与现有 ESI 端点一致）。

## 2. 邮件（US2 / FR-004~008）

| 方法 | 路径 | 参数 | 响应 data | 说明 |
|------|------|------|-----------|------|
| GET | `/mail/{characterId}/inbox` | lastMailId?, forceRefresh? | `{mails:[{mailId, from, subject, timestamp, isRead, labels}](≤50), nextLastMailId}` | 收件箱游标分页（ESI 原生 50 条/页 + lastMailId，research R4）；未读总数走 labels 端点 |
| GET | `/mail/{characterId}/labels` | forceRefresh? | `{labels:[{labelId, name, unreadCount}], totalUnread}` | 未读数 |
| GET | `/mail/{characterId}/{mailId}` | forceRefresh? | `{mailId, from, recipients, subject, timestamp, body, isRead}` | 正文原文返回（渲染安全归前端） |
| PUT | `/mail/{characterId}/{mailId}/read` | `{read: true}` | 无 | 标记已读/未读 |
| POST | `/mail/{characterId}/send` | `{recipients:[{recipientId, recipientType}], subject, body}` | `{mailId}` | recipientType: character/corporation/alliance/mailing_list；发信权限缺失→「需重新授权」提示 |
| POST | `/mail/recipients/resolve` | `{names:["角色名","军团名"]}` | `{results:[{name, id, type, resolved}]}` | 精确名称解析（公开接口，不消耗角色授权）；未解析项 resolved=false |

回信：前端复用 send 端点（recipient=原发件人，subject=Re: 原标题）——后端不提供专用 reply 端点（YAGNI）。

## 3. 工业订单查询（US3 / FR-009~010）

| 方法 | 路径 | 参数 | 响应 data | 说明 |
|------|------|------|-----------|------|
| GET | `/job/{type}/{id}` | status?, page?, size?, forceRefresh? | `{jobs:[{jobId, activityId, status, productTypeId, runs, startDate, endDate, installerId, facilityId}], total}` | type=char/corp（与现有 POST 同步端点同构）；直查 ESI；status 筛选内存过滤；corp 需相应军团权限 |

现有 `POST /job/{type}/{id}/{complete}`（QQ 侧同步）保持不变。

## 4. 团员追踪（US5 / FR-011~013）

| 方法 | 路径 | 参数 | 响应 data | 说明 |
|------|------|------|-----------|------|
| GET | `/corporation/{corpId}/member-tracking` | characterId（必填，Director 角色）, forceRefresh? | `{members:[{characterId, online, locationId, shipTypeId, logonDate, logoffDate}], trackedAt}` | 直查 ESI membertracking，不落库；缓存按用户隔离；无 Director 权限→拒绝；online 由服务端派生（logonDate > logoffDate） |

## 5. APP 通知（US4 / FR-014~020, 027~028）

| 方法 | 路径 | 参数 | 响应 data | 说明 |
|------|------|------|-----------|------|
| GET | `/notification` | current?, size?, unreadOnly? | `PageResult`：`{records:[{id, eventType, title, content, characterId, corpId, isRead, gmtCreate}], total, current, size, pages}` | 当前用户通知列表（归属隔离）；分页参数沿用 PageQuery（current≥1，size≤1000） |
| GET | `/notification/unread-count` | 无 | `{count}` | APP 角标 |
| PUT | `/notification/{id}/read` | 无 | 无 | 标记已读（仅本人通知） |
| POST | `/notification/ws-ticket` | 无 | `{ticket, expiresInSeconds:30}` | 换取 WS 一次性连接票据（JWT 认证；票据 30s 有效、一次性消费） |

eventType: `INDUSTRY_COMPLETE` / `FUEL_LOW` / `SKILL_QUEUE_EMPTY`。
燃料通知受众：该军团中绑定角色具备建筑接口权限的用户（权限扇出，FR-027）。

### WebSocket 实时通道

- 端点：`/websocket/app`（新建，与 OneBot `/websocket/onebot/**` 完全隔离）
- 握手认证：**一次性 ticket**（research R11）——先 `POST /notification/ws-ticket`（JWT）取 ticket，再连 `/websocket/app?ticket=<ticket>`；ticket 30s 有效、原子一次性消费，失败即关闭；**JWT 不进 URL**
- 服务端推送信封：`{type:"NOTIFICATION", data:{id, eventType, title, content, characterId, corpId, gmtCreate}}`
- 多设备：同一用户多连接均收到推送（注册表按 userId 扇出）
- 心跳：服务端空闲超时 90s（maxIdleTimeout）；客户端建议 ≤30s 应用层 ping；超时断开后重连需重新换取 ticket
- 断线重连：客户端重连后调 `GET /notification` 补齐（通知已持久化，不丢）；ticket 过期需重新换取
- 会话失效：改密成功后服务端主动关闭该用户全部 APP WS 连接

## 6. 账号自助与授权入口（US6 / FR-021~024）

| 方法 | 路径 | 参数 | 响应 data | 说明 |
|------|------|------|-----------|------|
| PUT | `/user/password` | `{oldPassword, newPassword}` | 无 | 验证旧密码；成功后该用户全部会话失效；限流 |
| DELETE | `/user/characters/{characterId}` | 无 | 无 | 解绑本人角色；停止该角色同步与通知；历史数据按现有归属规则保留 |
| GET | `/character/auth-url` | server?（serenity/tranquility，默认＝部署 datasource） | `{authorizeUrl, state}` | 系统生成含本特性所需权限集的授权 URL（SsoScopes.ALL）；state 固定 `eve-helper`（既有 AGENT 常量）；server 与部署 datasource 不一致→友好错误（单服务器配置，P3 注记）；用户浏览器授权后手动复制 code 回 APP，走现有 `POST /character/{code}` |

## 7. 错误语义（横切 / FR-025~026）

- ESI 错误：统一友好映射（现有 dataError 模式），不回显原始错误体
- 授权缺失：可区分状态 + 重新授权提示（021 scopes 语义），不连带阻塞其他能力
- 越权：403 友好提示，不泄露资源存在性
- 限流：429 + 友好提示（改密、forceRefresh）
