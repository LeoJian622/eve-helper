# EVE Helper DDD架构迁移指南

## 概述

本指南将帮助您将现有的EVE Helper项目从传统的分层架构迁移到领域驱动设计(DDD)架构。

## 当前架构分析

当前项目采用传统的三层架构：
- Controller层：处理HTTP请求
- Service层：业务逻辑
- Mapper层：数据访问

## 目标DDD架构

```
src/main/java/xyz/foolcat/eve/evehelper/
├── domain/          # 领域层 - 核心业务逻辑
├── application/     # 应用层 - 用例协调
├── infrastructure/  # 基础设施层 - 技术实现
├── interfaces/      # 接口层 - 用户交互
└── shared/          # 共享层 - 通用组件
```

## 迁移步骤

### 第一步：创建新的目录结构

1. 创建DDD分层目录
2. 在每个层下创建相应的子目录
3. 保持现有代码不变，逐步迁移

### 第二步：领域层迁移

#### 2.1 识别领域模型
- **实体(Entity)**: 有唯一标识的对象
  - User (用户)
  - Character (角色)
  - Blueprint (蓝图)
  - Asset (资产)
  - MarketOrder (市场订单)

- **值对象(Value Object)**: 不可变的对象
  - Money (金额)
  - ISK (游戏货币)
  - ItemQuantity (物品数量)
  - Location (位置)

- **聚合根(Aggregate Root)**: 管理聚合内实体的根实体
  - UserAggregate (用户聚合)
  - CharacterAggregate (角色聚合)
  - BlueprintAggregate (蓝图聚合)

#### 2.2 迁移现有实体
```java
// 从 domain/eve/ 和 domain/system/ 迁移到 domain/model/entity/
```

### 第三步：应用层迁移

#### 3.1 创建应用服务
```java
// 将现有的service层业务逻辑迁移到application/service/
// 应用服务负责协调领域对象完成用例
```

#### 3.2 创建DTO
```java
// 将现有的dto和vo迁移到application/dto/
// 按命令、查询、响应分类
```

### 第四步：基础设施层迁移

#### 4.1 持久化实现
```java
// 将mapper和数据库相关代码迁移到infrastructure/persistence/
```

#### 4.2 外部服务集成
```java
// 将esi相关代码迁移到infrastructure/external/esi/
// 将onebot相关代码迁移到infrastructure/external/onebot/
```

#### 4.3 配置迁移
```java
// 将config相关代码迁移到infrastructure/config/
```

### 第五步：接口层迁移

#### 5.1 Web接口
```java
// 将controller迁移到interfaces/web/controller/
// 将filter迁移到interfaces/web/filter/
```

#### 5.2 WebSocket接口
```java
// 将onebot websocket相关代码迁移到interfaces/websocket/
```

### 第六步：共享层迁移

#### 6.1 通用组件
```java
// 将common、enums、exception等迁移到shared/
```

## 具体迁移映射

### 当前目录 → DDD目录

| 当前目录 | DDD目录 | 说明 |
|---------|---------|------|
| `domain/eve/` | `domain/model/entity/` | EVE游戏相关实体 |
| `domain/system/` | `domain/model/entity/` | 系统相关实体 |
| `service/` | `application/service/` | 应用服务 |
| `service/esi/` | `infrastructure/external/esi/` | ESI外部服务 |
| `service/eve/` | `domain/service/` | 领域服务 |
| `service/system/` | `application/service/` | 应用服务 |
| `controller/` | `interfaces/web/controller/` | Web控制器 |
| `mapper/` | `infrastructure/persistence/mapper/` | 数据访问 |
| `dto/` | `application/dto/` | 数据传输对象 |
| `vo/` | `interfaces/vo/` | 视图对象 |
| `esi/` | `infrastructure/external/esi/` | ESI集成 |
| `onebot/` | `infrastructure/external/onebot/` | OneBot集成 |
| `config/` | `infrastructure/config/` | 配置类 |
| `common/` | `shared/` | 共享组件 |
| `enums/` | `shared/kernel/enums/` | 枚举定义 |
| `exception/` | `shared/kernel/exception/` | 异常定义 |
| `util/` | `shared/util/` | 工具类 |

## 迁移优先级

1. **高优先级**: 核心业务逻辑迁移到领域层
2. **中优先级**: 应用服务和DTO迁移
3. **低优先级**: 基础设施和接口层迁移

## 注意事项

1. 保持向后兼容性
2. 逐步迁移，不要一次性重构
3. 确保测试覆盖
4. 更新包名和导入语句
5. 维护依赖关系

## 完成后的优势

1. **清晰的业务边界**: 领域模型清晰表达业务概念
2. **更好的可维护性**: 分层明确，职责清晰
3. **更强的可扩展性**: 易于添加新功能
4. **更好的测试性**: 各层可以独立测试
5. **团队协作**: 不同角色可以专注于不同层 