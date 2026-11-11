# Data Model — 022 伴侣 APP 后端支撑端点（第一期）

> 依据：spec.md Key Entities + research.md R1–R15。唯一新表 `app_notification`；既有表零 schema 变更（014 归属不动）。
> 跨层读模型＝`domain/model/vo` record（DDD 跨层类型规则）；仅接口层出参＝`application/dto/response`。

## 1. 持久化实体：AppNotification（APP 通知）

### 1.1 DDL（`src/main/resources/db/migration/022_app_notification.sql`，部署手动执行）

```sql
-- 022 伴侣 APP 通知表。幂等：CREATE TABLE IF NOT EXISTS。
-- 归属：user_id = 接收者系统用户（sys_user.id）；燃料事件按权限扇出，一事件多行（每接收者一行，dedup_key 相同）。
CREATE TABLE IF NOT EXISTS app_notification (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id       INT          NOT NULL COMMENT '接收者系统用户ID(sys_user.id)',
  event_type    VARCHAR(32)  NOT NULL COMMENT 'INDUSTRY_COMPLETE/FUEL_LOW/SKILL_QUEUE_EMPTY',
  dedup_key     VARCHAR(128) NOT NULL COMMENT '去重键=事件+对象;窗口由查询判定(见1.3)',
  character_id  INT          NULL COMMENT '相关EVE角色ID(燃料事件为NULL)',
  corp_id       INT          NULL COMMENT '相关军团ID(燃料事件)',
  title         VARCHAR(128) NOT NULL COMMENT '通知标题',
  content       VARCHAR(512) NOT NULL COMMENT '通知正文(纯文本)',
  is_read       TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '已读标记',
  gmt_create    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  gmt_modified  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (id),
  KEY idx_user_created (user_id, gmt_create),
  KEY idx_dedup (dedup_key, gmt_create)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='APP通知表(022)';
```

### 1.2 字段与校验

| 字段 | 规则 |
|------|------|
| user_id | 必填；列表/已读操作仅限本人（查询强制 `user_id = UserUtil.getUserId()`，标记已读 `WHERE id=? AND user_id=?`） |
| event_type | 枚举三值（`shared/enums` 新增 `AppNotificationEventType`）；非法值不入库 |
| dedup_key | 必填；格式见 1.3；由检测器生成，不接受外部输入 |
| title/content | 检测器模板生成（纯文本，含对象名与时间）；长度上限 128/512，超长截断 |
| is_read | 默认 false；`PUT /notification/{id}/read` 仅本人可改 |

### 1.3 去重键与窗口（FR-017 / SC-007）

| eventType | dedup_key | 类别 | 去重窗口 | 判定查询 |
|-----------|-----------|------|----------|----------|
| INDUSTRY_COMPLETE | `INDUSTRY_COMPLETE:{jobId}` | 一次性 | 保留期内至多一次 | `idx_dedup` 上存在性查询（不限时间） |
| FUEL_LOW | `FUEL_LOW:{structureId}` | 持续性 | 24h 至多一次 | `dedup_key=? AND gmt_create > NOW()-INTERVAL 24 HOUR` 存在性 |
| SKILL_QUEUE_EMPTY | `SKILL_QUEUE_EMPTY:{characterId}` | 持续性 | 24h 至多一次 | 同上 |

- 燃料扇出：同一 structureId 的多接收者行共享 dedup_key；存在性查询**按 key 全局判定**（任一接收者行在窗口内 → 本轮整体跳过），保证「每建筑每 24h 判定一次」。
- 竞态容忍：DB 去重为权威；检测任务另持 Redis 运行锁 `app:notify:lock:{eventType}`（research R13）。
- 保留期：30 天（spec Assumptions）；每日清理 `DELETE WHERE gmt_create < NOW()-INTERVAL 30 DAY`（分批 LIMIT，避免长事务）。

### 1.4 状态迁移

`未读(is_read=0) → 已读(is_read=1)`（仅本人标记）；`过期(>30d) → 删除`（清理任务）。无其他状态。

### 1.5 仓储接口（`domain/repository/system/AppNotificationRepository.java`）

```java
public interface AppNotificationRepository {
    int insertBatch(List<AppNotification> notifications);
    boolean existsByDedupKey(String dedupKey);                       // 一次性事件
    boolean existsByDedupKeyWithinHours(String dedupKey, int hours); // 持续性事件(24)
    PageResult<AppNotification> pageByUser(Integer userId, Boolean unreadOnly, PageQuery pageQuery);
    long countUnread(Integer userId);
    int markRead(Long id, Integer userId);   // WHERE id=? AND user_id=?（归属内建）
    int deleteExpired(int retentionDays, int batchSize);
}
```

领域实体 `domain/model/entity/system/AppNotification.java` 继承 BaseEntity；PO `AppNotificationPO`（`@TableName("app_notification")`）+ MapStruct `AppNotificationPoConverter`（po2Domain/domain2Po 含 List 重载，仿 StructurePoConverter）。

## 2. 既有实体：操作新增，schema 零变更

| 实体 | 新操作 | 说明 |
|------|--------|------|
| EveAccount（eve_account） | `listByCorpId(corpId)`；`deleteByUserIdAndCharacterId(userId, characterId)` | 解绑＝删行（character_id 全局唯一）；守卫：TaskConstant.CHARACTER_ID 不可解绑（research R14） |
| SysUser（sys_user） | 更新 password（BCrypt） | 改密流程见 research R10；无新列 |
| Structure（structure） | 复用 `batchInsertOrUpdate`/`selectFuelExpiresList` | APP 建筑同步写入 userId=同步角色属主（014 语义）；QQ 同步路径不动 |

## 3. 领域读模型（`domain/model/vo`，record，非持久化）

> API 出参形状以 contracts/rest-api.md 为准；VO 为网关→应用层载体。工业订单/建筑复用既有端口返回类型，不新建。

| record | 关键字段 | 来源 ESI |
|--------|----------|----------|
| `CharacterPanelInfo` | characterId, name, corporationId, allianceId, birthday, securityStatus, online, lastLogin, lastLogout, solarSystemId, stationId, shipTypeId, shipName | characters/{id} + online + location + ship 组合（4 调用，网关层各自缓存）；shipName 经 eve 静态库既有物品名称查询解析 |
| `CharacterSkillsInfo` | totalSp, unallocatedSp, attributes(intelligence/memory/charisma/perception/willpower/bonusRemaps), skills[{skillId, trainedSkillLevel, activeSkillLevel, skillpointsInSkill}] | characters/{id}/skills + attributes |
| `SkillQueueInfo` | queue[{skillId, finishedLevel, trainingStartSp, skillpointsInSkill, startDate, endDate, position}], queueEndsAt（末条 endDate；空队列→null） | characters/{id}/skillqueue |
| `MailHeaderPage` | mails[{mailId, from, subject, timestamp, isRead, labels}]（≤50）, nextLastMailId（末条 mailId；不足 50→null） | characters/{id}/mail（lastMailId 游标） |
| `MailDetailInfo` | mailId, from, recipients[{recipientId, recipientType}], subject, timestamp, body（原文）, isRead | characters/{id}/mail/{mailId} |
| `MailLabelsInfo` | labels[{labelId, name, unreadCount}], totalUnread | characters/{id}/mail/labels（单调用双数据） |
| `MemberTrackingInfo` | members[{characterId, locationId, shipTypeId, logonDate, logoffDate}], trackedAt | corporations/{id}/membertracking（Director） |
| `RecipientResolution` | name, id, type(character/corporation/alliance), resolved | universe/ids（公开，pub 缓存） |
| `CharacterRolesInfo` | rolesAtHq/rolesAtBase/rolesAtOther（List<String>） | characters/{id}/roles；建筑权限＝Director/Station_Manager 出现于任一集合；Director 追踪权限＝Director（精确集合 P4 对照 ESI structures 鉴权文档以 fixtures 固化） |

技能名称（skillName）：技能队列/列表中的名称经 eve 静态库既有名称查询解析（同 shipName 路径）。

## 4. Redis 键清单

### 4.1 新增

| 键模式 | 值 | TTL | 写/读方 |
|--------|-----|-----|---------|
| `app:ws:ticket:{uuid}` | userId | 30s | NotificationApplicationService 创建（setIfAbsent）/ WS onOpen 消费（get+compareAndDelete） |
| `auth:user:revoke_epoch:{userId}` | 改密时刻毫秒 | 7d | 改密流程写 / JwtAuthorizationTokenFilter + TokenService.refresh 读 |
| `refresh_user:{userId}` | Set<sid> | 7d（随写续期） | TokenService.generateTokenPair / 改密流程遍历 |
| `refresh_iat:{sid}` | 会话创建毫秒 | 7d | TokenService 写 / refresh 校验读 |
| `app:struct:sync:{corpId}` | "1" | 3600s | AppStructureSyncTask SETNX 节流（FR-028） |
| `app:notify:lock:{eventType}` | 运行锁 UUID | 600s | 检测任务 SETNX + compareAndDelete 释放 |
| `pwd:attempt:{userId}` | 失败计数 | 30min | 改密限流（5 次锁定，仿 login:attempt） |

### 4.2 复用（零改动）

`esi:resp:{ds}:{userId|pub}:{uri}:{page}`（015）、`esi_access_token:{userId}:{characterId}`（19min）、`esi_refresh_lock:{characterId}`、`refresh_token/refresh_session/refresh_owner/session_revoked`、`auth:token:blacklist:{jti}`、`system:perm_roles_rule:url:`（启动重建）。

## 5. 通知生成数据流

```text
AppStructureSyncTask(hourly, 错开整点)
  └─ 逐军团: SETNX app:struct:sync:{corpId} → 选有建筑权限的绑定角色
       → AppStructureSyncApplicationService: authorizeInternal(userId,cid)
       → EsiGateway.queryCorporationStructures(缓存) → StructureRepository.batchInsertOrUpdate

AppNotificationTask
  ├─ 燃料(cron 18/22 点, 复用 QQ): 逐军团 selectFuelExpiresList(24,corpId) − 势力铁壁
  │    → 逐建筑 dedup(FUEL_LOW:{structureId},24h) → listByCorpId → queryCharacterRoles(缓存)
  │    → 筛 Director/Station_Manager 属主 → 每接收者一行 insertBatch → 逐用户 WS 推送
  ├─ 工业(cron 复用 QQ noticeJobComplete0): 逐绑定角色 authorizeInternal
  │    → queryCharacterIndustryJobs(缓存) → (active∧endDate≤now+24h)∨(delivered∧endDate≥now−24h)
  │    → dedup(INDUSTRY_COMPLETE:{jobId},一次性) → insertBatch(属主) → WS 推送
  ├─ 技能(hourly): 逐绑定角色 querySkillQueue(缓存) → 空
  │    → dedup(SKILL_QUEUE_EMPTY:{characterId},24h) → insertBatch(属主) → WS 推送
  └─ 清理(daily): deleteExpired(30d, 分批)
```

逐角色错误隔离：授权失效/ESI 错误 → 跳过该角色 + 结构化日志，不中断本轮。在线判定＝WS 注册表含该 userId；离线仅入库（重连后 GET /notification 补齐）。

## 6. 请求 DTO 校验（`application/dto/request`，Bean Validation）

| DTO | 约束 |
|-----|------|
| `ChangePasswordRequest` | oldPassword @NotBlank；newPassword @NotBlank @Size(min=8,max=64)；新旧不得相同（服务层校验） |
| `SendMailRequest` | recipients @NotEmpty @Size(max=50)，元素 recipientId @NotNull + recipientType 枚举(character/corporation/alliance/mailing_list)；subject @NotBlank @Size(max=128)；body @NotBlank @Size(max=10000)（本 API 契约上限，保守值；ESI 侧错误仍走友好映射） |
| `ResolveNamesRequest` | names @NotEmpty @Size(max=50)，元素 @NotBlank @Size(max=128) |
| `MarkMailReadRequest` | read @NotNull（Boolean） |
| 通知列表查询 | 继承 PageQuery（current≥1，size≤1000）+ unreadOnly 可选 |

响应 DTO（`application/dto/response`）：逐端点 record，形状＝contracts/rest-api.md；分页统一 `PageResult<T>`；**data 不使用 Boolean**（Result.java:32-38 翻转规则，research R15）。