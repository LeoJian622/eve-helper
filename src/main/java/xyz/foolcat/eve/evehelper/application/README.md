# Application Layer (应用层)

应用层负责协调领域对象来完成用户的业务用例。

## 目录结构

```
application/
├── service/         # 应用服务（用例实现）
│   ├── command/     # 命令处理
│   └── query/       # 查询处理
├── dto/             # 数据传输对象
│   ├── command/     # 命令DTO
│   ├── query/       # 查询DTO
│   └── response/    # 响应DTO
├── assembler/       # 组装器（DTO与领域对象转换）
├── event/           # 应用事件
│   └── handler/     # 应用事件处理器
└── validator/       # 应用层验证器
```

## 职责

- 实现业务用例
- 协调领域对象
- 处理事务边界
- 转换DTO和领域对象
- 处理应用事件 