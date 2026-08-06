# Domain Layer (领域层)

领域层是DDD架构的核心，包含业务逻辑和领域模型。

## 目录结构

```
domain/
├── model/           # 领域模型
│   ├── entity/      # 实体（eve: 游戏静态数据 / system: 运行时数据）
│   └── vo/          # 领域读模型（跨层共享的查询结果载体，如 BlueprintsDTO、TokenResult）
├── repository/      # 仓储接口（定义数据访问契约，由 infrastructure 实现）
│   ├── eve/
│   └── system/
├── port/            # 出站端口（依赖倒置，由 infrastructure 提供适配器）
│   ├── esi/         # EsiGateway —— ESI 外部 API 防腐层
│   └── cache/       # CacheGateway —— 缓存访问抽象
├── service/         # 领域服务（跨实体的业务逻辑）
│   ├── esi/
│   ├── eve/
│   ├── system/
│   ├── security/    # Token、黑名单、登录限流
│   └── thread/      # 异步领域操作
└── util/            # 领域工具
```

> **纯净性约束**: 本层不得 import `application`、`infrastructure`、`interfaces` 的任何类型，
> 仅可依赖 `shared`。外部资源（ESI、Redis）一律经 `port/` 下的端口接口访问。

## 职责

- 定义业务实体和领域读模型
- 实现核心业务逻辑
- 定义仓储接口与出站端口（依赖倒置）
- 维护业务规则和约束