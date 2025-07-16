# Interfaces Layer (接口层)

接口层负责处理用户交互和外部系统集成。

## 目录结构

```
interfaces/
├── web/             # Web接口
│   ├── controller/  # REST控制器
│   ├── filter/      # 过滤器
│   ├── interceptor/ # 拦截器
│   └── advice/      # 异常处理
├── websocket/       # WebSocket接口
│   ├── handler/     # WebSocket处理器
│   └── config/      # WebSocket配置
├── cli/             # 命令行接口
├── facade/          # 外观模式（简化复杂子系统）
└── vo/              # 视图对象（View Object）
```

## 职责

- 处理HTTP请求和响应
- 实现RESTful API
- 处理WebSocket连接
- 提供用户界面
- 数据验证和转换 