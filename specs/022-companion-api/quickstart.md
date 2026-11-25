# Quickstart — 022 验证指南

> 验证本特性各用户故事的最小路径。测试命令遵循仓库实际通道（Maven；`@SpringBootTest` 依赖内网 MySQL/Redis，见 research.md）。
> 所有 ID 为合成示例。

## 前置

```bash
mvn clean package -DskipTests   # 构建
mvn test                        # 全量测试（基线对照见 memory: test-baseline-volatility）
```

## US1 角色面板

```bash
mvn test -Dtest=CharacterPanelApplicationServiceTest
mvn test -Dtest=CharacterPanelControllerTest
```

手动：登录取 JWT → `GET /character/90000001/panel` → 断言含 online/location/ship；`GET .../skill-queue` → 队列条目含 endDate 与 queueEndsAt；空队列角色 → `queue:[]`。

## US2 邮件

```bash
mvn test -Dtest=MailApplicationServiceTest
mvn test -Dtest=MailControllerTest
```

手动：`GET /mail/90000001/inbox` → 分页+未读标识；`GET /mail/90000001/1001` → 正文；`PUT .../1001/read`；`POST /mail/recipients/resolve {"names":["合成角色名"]}` → id/type；`POST /mail/90000001/send` → 发送成功。缺发信权限角色 → 发信返回重新授权提示、读信正常。

## US3 工业查询

```bash
mvn test -Dtest=IndustryJobQueryApplicationServiceTest,JobControllerTest
```

手动：`GET /job/char/90000001?status=active` → 仅 active 订单；缓存有效期内二次调用不触发 ESI（日志验证）；现有 `POST /job/char/90000001/true` 行为不变。

## US4 通知通道

```bash
mvn test -Dtest=NotificationServiceTest
mvn test -Dtest=NotificationEventDetectorTest
mvn test -Dtest=AppWebSocketHandlerTest
```

手动：触发三类事件（测试桩）→ `GET /notification` 可见；WebSocket 客户端在线 → 5s 内收到 NOTIFICATION 帧；同一事件 24h 内重复判定 → 不重复；持续条件 >24h → 新提醒；`PUT /notification/1/read` → 已读持久化；QQ 推送回归（现有 StructTask/IndustryTask 测试全绿）。

负载（SC-003）：模拟 300 个 WS 连接（军团全体成员规模）→ 触发事件 → 推送 p95 ≤5s、无连接降级；通知列表分页查询走缓存/DB 路径 <200ms p95。

## US5 团员追踪

```bash
mvn test -Dtest=MemberTrackingApplicationServiceTest
```

手动：Director 角色 `GET /corporation/98000001/member-tracking?characterId=90000001` → 团员列表；非 Director → 拒绝；两用户先后查询 → 缓存隔离（日志键含 userId）。

## US6 账号自助

```bash
mvn test -Dtest=AccountSelfServiceTest
```

手动：`PUT /user/password`（正确旧密码）→ 成功，旧 refresh token 失效；错误旧密码 → 拒绝；`DELETE /user/characters/90000001` → 解绑，该角色通知停止；他人角色 → 拒绝。`GET /character/auth-url` → 返回含所需 scope 的 URL。

## 安全回归（SC-006）

- 跨属主：用户 A 访问用户 B 角色的 panel/mail/job → 全部拒绝
- 燃料扇出边界：无建筑权限用户不得收到 FUEL_LOW
- 改密/解绑越权：操作他人资源 → 拒绝
