# Infrastructure Layer (基础设施层)

基础设施层提供技术实现，支持其他层的功能。

## 目录结构

```
infrastructure/
├── persistence/     # 持久化实现
│   ├── repository/  # 仓储实现
│   ├── mapper/      # MyBatis映射器
│   ├── entity/      # 数据库实体
│   └── converter/   # 数据转换器
├── external/        # 外部服务集成
│   ├── esi/         # EVE ESI API集成
│   ├── onebot/      # OneBot集成
│   └── client/      # 外部API客户端
├── config/          # 配置类
│   ├── database/    # 数据库配置
│   ├── security/    # 安全配置
│   ├── redis/       # Redis配置
│   └── async/       # 异步配置
├── cache/           # 缓存实现
├── message/         # 消息队列
├── file/            # 文件处理
└── util/            # 工具类
```

## 职责

- 实现数据持久化
- 集成外部服务
- 提供技术基础设施
- 处理跨切面关注点 