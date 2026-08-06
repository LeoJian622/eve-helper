# Interfaces Layer (接口层)

接口层负责处理用户交互和外部系统集成。

## 目录结构

```
interfaces/
└── web/                 # Web 接口
    ├── controller/      # REST 控制器（Assets/Auth/Blueprints/Character/
    │                    #   Job/MarketGroup/Mining/User）
    └── advice/          # GlobalExceptionHandler 全局异常处理
                         #   + @NoWrap 响应包装豁免标记
```

> **返回类型约定**: 控制器统一返回 `Result<T>` 信封。`T` 使用 `application/dto/response`
> 下的响应 DTO，或 `domain/model/vo` 下的领域读模型（被多层共享的查询结果）。
> 本层不含独立 VO 包。

## 职责

- 处理HTTP请求和响应
- 实现RESTful API
- 处理WebSocket连接
- 提供用户界面
- 数据验证和转换 