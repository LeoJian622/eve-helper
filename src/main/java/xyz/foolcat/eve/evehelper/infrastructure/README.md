# Infrastructure Layer (基础设施层)

基础设施层提供技术实现,实现领域层定义的接口与出站端口,支持其他层的功能。

## 目录结构

```
infrastructure/
├── assembler/       # 映射转换器
│   ├── persistence/ # PO ↔ 领域对象
│   └── esi/         # ESI 响应 ↔ 领域对象
├── cache/           # CacheGateway 适配器(Redis 实现,如 RedisCacheGateway)
├── config/          # 配置类
│   ├── druid/       # 多数据源(Druid)
│   ├── mybatis/     # MyBatis Plus
│   ├── security/    # Spring Security / JWT / RBAC
│   ├── AsyncConfiguration.java        # 异步线程池
│   ├── SchedulerConfig.java           # 定时任务调度
│   ├── RedisConfig.java               # Redis
│   ├── SpringDocConfig.java           # OpenAPI 文档
│   └── InitPermissionRolesCache.java  # 权限角色缓存初始化
├── external/        # 外部服务集成
│   ├── esi/         # EVE ESI API(OAuth2 PKCE,30+ API 类)
│   └── onebot/      # OneBot 协议集成
├── persistence/     # 持久化实现
│   ├── entity/      # PO(数据库实体)
│   ├── mapper/      # MyBatis 映射器
│   └── repository/  # 仓储实现(实现 domain/repository 接口)
├── service/         # 基础设施相关服务实现
│   └── thread/      # 线程相关服务
└── util/            # 工具类(定时任务)
    ├── IndustryTask.java   # 工业任务
    ├── MiningTask.java     # 采矿任务
    ├── StructTask.java     # 建筑任务
    ├── WalletTask.java     # 钱包任务
    └── TaskConstant.java   # 任务常量
```

> **依赖方向**: 本层依赖 `domain`(实现其接口与端口)与 `shared`,不得被 `domain` 反向依赖。
> PO ↔ 领域转换属本层职责,经 `assembler/persistence` 完成;领域 ↔ DTO 的转换属应用层,见
> `application/assembler`。

## 职责

- 实现数据持久化(PO + MyBatis Plus + 仓储实现)
- 集成外部服务(ESI API、OneBot)
- 实现领域层出站端口(EsiGateway、CacheGateway)
- 提供技术基础设施配置(数据源、安全、缓存、异步、定时任务)

---

**最后更新**: 2026-08-07
