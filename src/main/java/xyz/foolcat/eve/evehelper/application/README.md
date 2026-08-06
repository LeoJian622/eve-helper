# Application Layer (应用层)

应用层负责协调领域对象来完成用户的业务用例。

## 目录结构

```
application/
├── service/         # 应用服务（用例实现，声明事务边界）
├── dto/             # 数据传输对象
│   ├── request/     # 入参 DTO（含 BlueprintsQuery 等查询条件对象）
│   └── response/    # 响应 DTO
└── assembler/       # 组装器（领域对象 ↔ DTO，MapStruct 编译期生成）
```

> **依赖约束**: 本层可依赖 `domain` 与 `shared`，不得 import `infrastructure`
> 或 `interfaces` 的任何类型。PO↔领域的转换属基础设施职责，见
> `infrastructure/assembler`。
>
> **历史说明**: 早期曾引入 CQRS（`command/`、`query/` 及 CommandBus/QueryBus），
> 因项目规模未达到收益临界点已全部移除，查询直接由应用服务编排。

## 职责

- 实现业务用例
- 协调领域对象
- 处理事务边界
- 转换DTO和领域对象 