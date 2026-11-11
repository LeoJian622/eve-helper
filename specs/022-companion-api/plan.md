# Implementation Plan: 全功能 EVE 伴侣 APP 后端支撑端点（第一期）

**Branch**: `022-companion-api` | **Date**: 2026-11-12 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/022-companion-api/spec.md`

## Summary

六组能力：角色面板（技能/队列/属性/在线/位置/舰船）、邮件收发（含收件人精确名称解析）、工业订单查询、军团团员追踪、APP 通知通道（三事件 + 持久化 + 实时送达）、账号自助（改密/解绑）+ 系统授权入口。

技术路线（用户澄清 + 建造者评审定稿）：**直查架构**——角色面板/工业订单/团员追踪均按绑定角色直查 ESI，经 015 响应缓存（016 先例），不新建多租户同步层；通知通道 = 事件判定（复用 QQ 侧判定逻辑，工业/技能改为多租户直查判定）+ 通知表持久化（去重键：事件 + 对象 + 24h 窗口）+ WebSocket 实时推送；燃料通知按权限扇出（军团内有建筑权限的用户），建筑同步每军团每小时至多一次；QQ 推送与 014 归属规则零变更。

## Technical Context

**Language/Version**: Java 17
**Primary Dependencies**: Spring Boot 3.5.14（Web/Security/Validation/Cache/Data Redis/WebSocket/Actuator）、WebFlux + Reactor Netty（ESI 客户端）、MyBatis Plus 3.5.15、Druid 1.2.24、MySQL Connector/J 9.6.0、Lombok 1.18.40、MapStruct 1.6.3、Hutool 5.8.44、SpringDoc 2.8.16、Nimbus JOSE JWT 10.0.2——全部冻结栈，**零新增依赖**
**Storage**: MySQL `eve_helper`（仅新增 app_notification 表）、MySQL `eve`（静态数据只读，舰船/技能名称解析）、Redis（015 响应缓存 / 会话；新增：WS ticket、改密撤销纪元+会话索引、建筑同步节流、检测运行锁、改密限流——键清单见 data-model.md §4）
**Testing**: JUnit 5 + Mockito + AssertJ；`@SpringBootTest` 走 test profile（依赖内网 MySQL/Redis）；Maven（`mvn test -Dtest=Class#method`）
**Target Platform**: Linux server（生产）/ Windows（开发）；客户端为另立跨平台 APP 项目（安卓+iOS）
**Project Type**: web-service（DDD 五层单体）
**Performance Goals**: 缓存命中路径 <200ms p95（宪法 I）；ESI 直查 miss 路径 ≤5s 外部超时；通知实时送达 ≤5s（SC-002）；面板/列表呈现 ≤1s（SC-001）
**Constraints**: 技术栈冻结（宪法 IV）；QQ 推送零行为变更（FR-018）；014 归属规则不变；授权 code 本轮手动复制回 APP；单实例部署（通知去重须容忍 cron 重叠）
**Scale/Scope**: 军团成员数百人 × 每人 1–N 绑定角色；6 用户故事 / 28 FR；通知量约每日数百条

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**宪法 v1.6.0 逐条检查（初检，Phase 0 前）**：

| 原则 | 状态 | 说明 |
|------|------|------|
| I. REST API First / DDD / Result<T> | ✅ PASS | 新能力全部 REST 暴露；五层依赖规则不变；统一 Result<T> 信封 |
| I. TDD Red-Green-Refactor | ✅ PASS | P4 每任务 TDD 铁律；无先失败测试不写生产代码 |
| I. API 性能 200ms p95 | ⚠️ 口径解释 | "standard requests"＝缓存命中/DB 路径 <200ms p95；ESI 直查 miss 路径受 5s 外部超时约束（宪法明文允许），与 016 直查先例一致；SC-001 的 1s 为用户面呈现口径。见 Complexity Tracking |
| II. 可观测性 / 安全 | ✅ PASS | 结构化日志；改密端点限流（auth 端点要求）；新端点 RBAC 登记；token 不落日志 |
| III. 覆盖率 ≥80% / YAGNI | ✅ PASS | SC-004 ≥80%；不建 speculative 抽象（通知通道只服务三事件，不做通用事件总线） |
| IV. 技术栈冻结 | ✅ PASS | 零新增依赖；WebSocket 用既有 spring-boot-starter-websocket |
| V. Spec-First | ✅ PASS | G1 已批准（f2f4940）；本 plan 为 P2 工件 |
| V.1 特性分支 | ✅ PASS | 022-companion-api 自 springboot3.0 切出，主干零直接提交 |
| V.1 实现后评审 | ✅ 计划 | P6：ecc:java-reviewer 必须 + 安全评审（涉 auth/用户输入/外部 API；按会话经验 security-reviewer 代理易卡死，改用 general-purpose + 详尽提纲并记录） |

### API Performance Gates
- 缓存命中/DB 路径 <200ms p95；ESI miss 路径 ≤5s 超时 + 015 缓存防穿透
- DB 查询：通知列表分页 + (user_id, created_at) 索引；扇出批量插入，无 N+1
- 外部依赖：ESI 调用走既有 WebClient 超时与错误映射（015）；不引入新熔断库（栈冻结），复用既有 EsiException 降级路径

### Test Coverage Gates
- 单元测试 ≥80%（新代码）；集成测试覆盖 6 用户故事全部验收场景（SC-005）
- 安全测试：跨属主访问、燃料通知权限扇出边界、改密/解绑越权（SC-006）
- 性能：缓存命中路径抽样验证；通知送达时延测试（SC-002）

### Post-Design Re-Check（Phase 1 完成后复检）

- 依赖：零新增——WS 用既有 spring-boot-starter-websocket（raw JSR-356）；CacheGateway +3 Set 方法、EsiGateway +~13 方法均为端口纯增量（021 模式），实现复用既有 RedisTemplate/WebClient。✅
- 性能：200ms 口径不变（缓存命中/DB 路径）；通知列表走 idx_user_created，扇出 insertBatch 无 N+1；建筑同步节流 SETNX 每军团≤1/h。✅
- 安全：WS ticket 一次性 30s（JWT 不进 URL）；改密纪元 fail-closed；解绑守卫系统角色；RBAC 全端点登记；错误不回显原始体。✅
- 数据：仅 1 新表（双索引）；既有表零变更；014/QQ 零侵入（新任务/新服务，不改 StructTask/IndustryTask/StructureService）。✅
- 结论：无新违规，Complexity Tracking 无需新增条目。

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

> `[+]` 新增 / `[M]` 修改（纯增量，既有签名与行为不动）。基础包 `xyz.foolcat.eve.evehelper`。

```text
src/main/java/xyz/foolcat/eve/evehelper/
├── domain/
│   ├── model/entity/system/AppNotification.java                  [+] 通知实体(extends BaseEntity)
│   ├── model/vo/                                                 [+] 9 个读模型 record(data-model §3)
│   │   CharacterPanelInfo / CharacterSkillsInfo / SkillQueueInfo /
│   │   MailHeaderPage / MailDetailInfo / MailLabelsInfo /
│   │   MemberTrackingInfo / RecipientResolution / CharacterRolesInfo
│   ├── port/esi/EsiGateway.java                                  [M] +~13 方法(R1;既有签名不动)
│   ├── port/cache/CacheGateway.java                              [M] +sAdd/sMembers/sRemove(R10)
│   ├── repository/system/AppNotificationRepository.java          [+] 签名见 data-model §1.5
│   ├── repository/system/EveAccountRepository.java               [M] +listByCorpId/+deleteByUserIdAndCharacterId
│   └── service/security/TokenService.java                        [M] +iat claim/+refresh_iat/+refresh_user 索引/+refresh 纪元校验
├── application/
│   ├── service/CharacterPanelApplicationService.java             [+] panel/skills/skill-queue 聚合(顺序组合,网关层各自缓存)
│   ├── service/MailApplicationService.java                       [+] inbox/body/read/labels/send/resolve
│   ├── service/IndustryJobQueryApplicationService.java           [+] char/corp 直查+status 内存过滤+分页
│   ├── service/MemberTrackingApplicationService.java             [+] Director 校验(roles)+直查
│   ├── service/NotificationApplicationService.java               [+] 列表/未读数/已读/ws-ticket/生成+推送
│   ├── service/AppStructureSyncApplicationService.java           [+] 每军团建筑同步(R13;不动 StructureService)
│   ├── service/UserApplicationService.java                       [M] +changePassword(R10 流程)
│   ├── service/CharacterApplicationService.java                  [M] +unbindCharacter/+getAuthorizeUrl
│   ├── dto/request/                                              [+] ChangePassword/SendMail/ResolveNames/MarkMailRead(校验见 data-model §6)
│   └── dto/response/                                             [+] 逐端点 record(contracts 形状)
├── infrastructure/
│   ├── external/esi/EsiApiService.java                           [M] 新端口方法实现(015 缓存包裹,R2/R3)
│   ├── external/appws/AppNotificationWebSocket.java              [+] @ServerEndpoint("/websocket/app") 薄适配器
│   ├── external/appws/AppWebSocketSessionRegistry.java           [+] userId→Set<Session> 多设备注册表+推送
│   ├── persistence/entity/system/AppNotificationPO.java          [+]
│   ├── persistence/mapper/system/AppNotificationMapper.java      [+]
│   ├── persistence/repository/system/AppNotificationRepositoryImpl.java [+]
│   ├── persistence/assembler/persistence/AppNotificationPoConverter.java [+] MapStruct
│   ├── assembler/esi/                                            [+] 新 VO 对应 ESI 响应转换器(按需)
│   ├── config/security/JwtAuthorizationTokenFilter.java          [M] +撤销纪元校验(R10)
│   ├── config/security/SecurityConfig.java                       [M] /websocket/app permitAll(端点 ticket 自鉴权)
│   ├── util/AppStructureSyncTask.java                            [+] hourly 每军团节流同步
│   └── util/AppNotificationTask.java                             [+] 工业/燃料/技能检测+30d 清理(调用模式仿 StructTask→服务)
├── interfaces/web/controller/
│   ├── CharacterPanelController.java                             [+] /character/{id}/panel|skills|skill-queue
│   ├── MailController.java                                       [+] /mail/**
│   ├── JobController.java                                        [M] +GET /job/{type}/{id}(POST 同步不动)
│   ├── MemberTrackingController.java                             [+] /corporation/{corpId}/member-tracking
│   ├── NotificationController.java                               [+] /notification/**(含 ws-ticket)
│   ├── UserController.java                                       [M] +PUT /user/password +DELETE /user/characters/{cid}
│   └── CharacterController.java                                  [M] +GET /character/auth-url(POST /{code} 不动)
└── shared/                                                       零改动(Result/PageQuery/PageResult 沿用;+AppNotificationEventType 枚举)

src/main/resources/
├── mappers/system/AppNotificationMapper.xml                      [+]
└── db/migration/022_app_notification.sql                         [+] DDL(部署手动执行)

src/SQL/convert/
├── 022_rbac_permissions.sql                                      [+] ~19 端点权限(delete-then-insert 事务)
└── deploy_all_pending.sql                                        [M] 并入 022 段(执行后须重启刷新权限缓存,R9)

src/test/java/…                                                   镜像主包结构;测试类清单见 quickstart.md
```

**Structure Decision**: 单项目 DDD 五层单体，全部落位于既有包结构；唯一新包 `infrastructure/external/appws`（APP WS 通道，与 onebot 完全隔离）。依赖方向合规：任务→应用服务（仿既有 StructTask→StructureService 模式）；应用层经 EsiGateway/CacheGateway/Repository 端口访问外部；WS 端点薄适配器→Spring 管理注册表（规避 @Profile("!test") 容器依赖，保证可单测）。不建多租户同步层、不建通用事件总线（YAGNI，用户澄清直查架构）。

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| 200ms p95 口径解释（非违规，备案） | ESI 直查 miss 路径天然 >200ms；宪法允许 5s 外部超时，"standard requests" 解释为缓存命中/DB 路径（016 先例已合并） | 强制全路径 <200ms 须把全部数据预同步入库＝多租户同步层，已被用户澄清明确否决（直查架构） |
