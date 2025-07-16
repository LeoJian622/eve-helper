# Domain Layer (领域层)

领域层是DDD架构的核心，包含业务逻辑和领域模型。

## 目录结构

```
domain/
├── model/           # 领域模型（实体、值对象、聚合根）
│   ├── entity/      # 实体
│   ├── valueobject/ # 值对象
│   └── aggregate/   # 聚合根
├── repository/      # 仓储接口（定义数据访问契约）
├── service/         # 领域服务（跨实体的业务逻辑）
├── event/           # 领域事件
│   ├── DomainEvent.java
│   └── handler/     # 事件处理器
└── specification/   # 规格模式（复杂查询条件）
```

## 职责

- 定义业务实体和值对象
- 实现核心业务逻辑
- 定义仓储接口
- 处理领域事件
- 维护业务规则和约束 