# Shared Layer (共享层)

共享层包含被多个层共同使用的通用组件。

## 目录结构

```
shared/
├── kernel/          # 核心组件
│   ├── base/        # 基础类
│   ├── constant/    # 常量定义
│   ├── enums/       # 枚举定义
│   └── exception/   # 异常定义
├── result/          # 统一返回结果
├── security/        # 安全相关
├── validation/      # 验证组件
└── util/            # 通用工具类
```

## 职责

- 提供通用基础类
- 定义常量和枚举
- 统一异常处理
- 提供工具方法
- 安全相关组件 