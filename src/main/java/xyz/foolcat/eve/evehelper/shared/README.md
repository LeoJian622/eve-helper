# Shared Layer (共享层)

共享层包含被多个层共同使用的通用组件,不依赖任何业务层。

## 目录结构

```
shared/
├── kernel/          # 核心组件
│   ├── base/        # 基础类(BaseEntity、PageResult、PageQuery)
│   ├── config/      # 配置属性
│   ├── constants/   # 常量定义
│   ├── enums/       # 枚举定义
│   └── exception/   # 异常(EveHelperException 等)
├── result/          # 统一返回结果
│   ├── IResultCode.java
│   ├── Result.java       # Result<T> 信封
│   └── ResultCode.java   # 结果码
└── util/            # 通用工具类
    ├── PageResultUtil.java       # 分页工具
    ├── ResponseUtils.java        # 响应工具
    └── SensitiveDataMasker.java  # 敏感数据脱敏
```

> **纯净性**: 本层不依赖 `application`/`domain`/`infrastructure`/`interfaces` 任何业务层;所有业务层均可依赖本层。

## 职责

- 提供通用基础类(BaseEntity、分页对象 PageResult/PageQuery)
- 定义常量、枚举与异常(EveHelperException)
- 统一返回结果封装(Result<T> / ResultCode / IResultCode)
- 提供通用工具方法(分页、响应、敏感数据脱敏)

---

**最后更新**: 2026-08-07
