# EVE Helper 功能清单

> 全项目功能盘点，按业务域整理。
> 依据：14 个 REST 控制器、33 个 ESI API 集成类、4 个定时任务、OneBot 分发器及 `specs/` 特性档案。
> 生成日期：2026-11-11（基于 `springboot3.0` 分支，021 合并后）。

## 1. 账户与安全

- **用户注册 / 登录**：`POST /user` 注册；Spring Security formLogin 登录（含登录限流）
- **JWT 会话体系**：RSA-256 签名；access 900s / refresh 604800s；refresh token 存 Redis；`POST /auth/tokens` 刷新、`DELETE /auth/sessions` 登出（黑名单机制）；已完成密钥轮换（007）
- **RBAC 权限控制**：`RbacAuthorizationManager` 按 `METHOD:PATH` 鉴权，URL-权限-角色映射缓存 Redis，白名单可配置
- **用户角色列表**：`GET /user/{userId}` 返回绑定的 EVE 角色及 **ESI 授权详情 scopes**（021，null=未解析 / []=无权限的语义区分）

## 2. ESI 授权管理（EVE Online 官方 API）

- **OAuth2 绑定**：Authorization Code + PKCE，`POST /character/{code}` 完成角色授权绑定
- **双服务器支持**：宁静（Tranquility）与晨曦（Serenity）
- **Token 生命周期**：refresh token 加密存库；access token 缓存 Redis 19 分钟；刷新锁防并发争用
- **accessToken 查询**：`GET /character/{characterId}/access-token`（006）
- **授权状态与 403 治理**：人物授权唯一基准，ESI 数据 403 友好映射（003/013）
- **ESI 客户端去重**（020）

## 3. 钱包体系（人物 + 军团）

- **钱包流水 journal**：`/wallet/journal` 人物/军团同步 + 分页倒序查询；军团全 7 个 division 分账（010/011）
- **交易流水 transaction**：`/wallet/transaction` 人物/军团同步 + 查询（from_id 游标、division 失败隔离、批量 upsert）
- **钱包总览**：`/wallet/overview/{cid}` 与 `/corp/{cid}`——余额/收支/类目/趋势/分账分布聚合，支持时间范围（012）；军团总览支持人物入口（017）
- **余额直查**：`/wallet/balance` 由 ESI 实时直查人物+军团余额（016）；**按用户横向聚合**所有角色余额，四态 status + 60s 结果缓存（018）

## 4. 资产与蓝图

- **资产同步/查询**：`POST /assets/{cid}/sync`、`GET /assets/{cid}`
- **资产聚合概况**：`GET /assets/aggregate` 按角色聚合件数/价值/类目数（009）
- **蓝图查询**：`GET /blueprints/{id}`

## 5. 军团设施与工业

- **建筑查询**（005）：`/structures/{corpId}` 下 6 个端点——建筑列表、详情、燃料、服务、统计、增强 timers
- **工业项目**：`POST /job/{type}/{id}/{complete}` 工业项目操作；ESI IndustryApi 集成
- **采矿台账**：`POST /mining/{characterId}/{observerId}` 军团卫星矿观测台账同步

## 6. 市场数据

- **市场分组树**：`GET /market/group/{parent}`（基于 eve 静态数据库）
- **市场订单**：`MarketOrderService` + 专用异步线程池同步订单数据，支撑 Jita 价格查询

## 7. QQ 机器人（OneBot）与主动通知

- **WebSocket 接入**：`/websocket/onebot/**`，`BotDispatcher` 指令分发：
  - `.jita` — Jita 市场价格查询
  - `.tax` — 税率查询
  - `.struct` — 建筑查询（限定授权 QQ 用户）
  - `.admAddType` — 管理员添加物品类型
- **定时任务通知**（4 个 Task）：
  - `StructTask`：每小时更新建筑信息；每日 18/22 点通知 **24 小时内燃料耗尽**的建筑
  - `IndustryTask`：每 5 分钟检查工业项目；分时段推送**项目完成**通知
  - `MiningTask`：每日/每周一 19 点通知**卫星矿可开采时间**
  - `WalletTask`：每 15 天更新钱包记录

## 8. 平台级能力（横切）

- **ESI 响应缓存**（015）：按响应头 TTL 命中跳过真调、缓存键用户隔离（防 IDOR）、forceRefresh 强刷限流、防并发穿透
- **数据用户归属**（014）：8 张表 user_id 双锁模型——人物数据属主共享、军团数据同步者私有、ROOT 全量
- **多数据源**：`eve`（游戏静态数据，只读）+ `eve_helper`（运行时数据），Druid 连接池
- **统一 API 规范**：`Result<T>` 响应信封、全局异常处理（`@NoWrap` 例外）、SpringDoc OpenAPI（Swagger UI）
- **DDD 五层架构**：已完成分层纯度治理（004），32 个 assembler 做层间转换

## 附录：功能演进索引（specs/）

| 编号 | 特性 | 主题 |
|------|------|------|
| 003 | esi-auth-status | ESI 授权状态 |
| 004 | ddd-layering-purity | DDD 分层纯度治理 |
| 005 | structure-query-api | 军团建筑查询 API |
| 006 | character-access-token-api | 人物 accessToken 查询 |
| 007 | jwt-key-rotation | JWT 密钥轮换 |
| 008 | security-review-followup | 安全评审遗留修复 |
| 009 | asset-wallet-aggregate | 资产聚合 + 钱包流水 |
| 010 | wallet-transactions | 钱包交易流水 |
| 011 | corporation-wallet-journal | 军团钱包流水 |
| 012 | wallet-overview | 钱包总览 |
| 013 | esi-char-auth-403 | ESI 人物授权 + 403 治理 |
| 014 | esi-data-user-binding | ESI 数据用户归属 |
| 015 | esi-response-caching | ESI 响应缓存 |
| 016 | wallet-balance-endpoint | 钱包余额直查 |
| 017 | corp-overview-character-entry | 军团总览人物入口 |
| 018 | wallet-balance-by-user | 按用户聚合余额 |
| 020 | esi-client-dedup | ESI 客户端去重 |
| 021 | esi-auth-scopes | ESI 授权详情 scopes |

> 各特性详细流程见 `docs/knowledge/` 模块文档；规格/计划/任务工件见 `specs/<NNN>-<feature>/`。

## 备注

ESI 集成层共 33 个 API 类（含 Contracts、Mail、KillMail、Fleet、PI、主权战争等），
其中大部分目前仅作为底层能力存在，尚未全部开放为 REST 端点——已暴露的业务面即上述 1~7 节。

---

**最后更新**: 2026-11-11
**维护者**: EVE Helper Documentation Team
