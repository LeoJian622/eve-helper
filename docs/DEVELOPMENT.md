# 开发指南

本文档提供 EVE Helper 项目的开发工作流程、工具使用和最佳实践。

## 📋 目录

- [开发环境设置](#开发环境设置)
- [项目结构](#项目结构)
- [开发工作流](#开发工作流)
- [Maven命令参考](#maven命令参考)
- [代码规范](#代码规范)
- [调试技巧](#调试技巧)

## 🛠️ 开发环境设置

### 前置要求

- **JDK**: 11 或更高版本
- **Maven**: 3.6+
- **MySQL**: 8.0+
- **Redis**: 5.0+
- **IDE**: IntelliJ IDEA (推荐) 或 Eclipse

### 1. 克隆项目

```bash
git clone https://github.com/your-org/eve-helper.git
cd eve-helper
```

### 2. 配置环境变量

```bash
# 复制环境变量模板
cp .env.example .env

# 编辑 .env 文件,填入开发环境配置
vim .env
```

详细的环境变量配置请参考 [环境变量文档](./ENVIRONMENT.md)。

### 3. 初始化数据库

```bash
# 创建数据库
mysql -u root -p << EOF
CREATE DATABASE IF NOT EXISTS eve_helper CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS eve CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
EOF

# 导入数据库结构 (如果有SQL文件)
mysql -u root -p eve_helper < db/schema.sql
mysql -u root -p eve < db/eve_schema.sql
```

### 4. 安装依赖

```bash
mvn clean install
```

### 5. 启动应用

```bash
# 使用开发环境配置启动
mvn spring-boot:run -Dspring.profiles.active=dev

# 或者使用IDE运行 EveHelperApplication.java
```

### 6. 验证安装

访问以下URL验证应用是否正常运行:

- **应用首页**: http://localhost:9999
- **API文档**: http://localhost:9999/swagger-ui.html
- **健康检查**: http://localhost:9999/actuator/health

## 📁 项目结构

EVE Helper 采用 **领域驱动设计 (DDD)** 架构:

```
eve-helper/
├── src/
│   ├── main/
│   │   ├── java/xyz/foolcat/eve/evehelper/
│   │   │   ├── application/          # 应用层 - 应用服务和DTO
│   │   │   │   ├── assembler/        # DTO转换器
│   │   │   │   └── service/          # 应用服务
│   │   │   ├── domain/               # 领域层 - 核心业务逻辑
│   │   │   │   ├── model/            # 领域模型
│   │   │   │   ├── repository/       # 仓储接口
│   │   │   │   └── service/          # 领域服务
│   │   │   ├── infrastructure/       # 基础设施层 - 技术实现
│   │   │   │   ├── external/         # 外部服务集成
│   │   │   │   │   ├── esi/          # EVE ESI API
│   │   │   │   │   └── onebot/       # OneBot协议
│   │   │   │   ├── persistence/      # 数据持久化
│   │   │   │   └── util/             # 工具类
│   │   │   ├── interfaces/           # 接口层 - 对外接口
│   │   │   │   └── web/              # Web控制器
│   │   │   └── shared/               # 共享层 - 通用组件
│   │   └── resources/
│   │       ├── application.yml       # 主配置文件
│   │       ├── application-dev.yml   # 开发环境配置
│   │       └── application-test.yml  # 测试环境配置
│   └── test/                         # 测试代码
│       ├── java/                     # 单元测试和集成测试
│       └── resources/                # 测试资源
├── docs/                             # 项目文档
├── pom.xml                           # Maven配置
├── .env.example                      # 环境变量模板
└── README.md                         # 项目说明
```

### DDD架构说明

#### 1. 接口层 (interfaces)
- **职责**: 处理HTTP请求,参数验证,响应格式化
- **组件**: Controller, DTO, Request/Response对象
- **示例**: `CharacterController`, `BlueprintsController`

#### 2. 应用层 (application)
- **职责**: 编排业务流程,协调领域对象
- **组件**: Application Service, Assembler (DTO转换)
- **示例**: `UserService`, `UserConverter`

#### 3. 领域层 (domain)
- **职责**: 核心业务逻辑,业务规则
- **组件**: Entity, Value Object, Domain Service, Repository Interface
- **示例**: `SysUserService`, `BlueprintsService`

#### 4. 基础设施层 (infrastructure)
- **职责**: 技术实现,外部服务集成
- **组件**: Repository Implementation, External API Client, Util
- **示例**: `EsiApiService`, `BotDispatcher`

#### 5. 共享层 (shared)
- **职责**: 跨层共享的通用组件
- **组件**: Common Utils, Constants, Exceptions

## 🔄 开发工作流

### 1. 创建新功能分支

```bash
# 从main分支创建功能分支
git checkout main
git pull origin main
git checkout -b feature/your-feature-name
```

### 2. 开发流程

#### 步骤 1: 定义接口 (interfaces层)

```java
@RestController
@RequestMapping("/api/v1/market")
public class MarketController {

    @GetMapping("/orders")
    public ResponseEntity<List<OrderDTO>> getOrders() {
        // TODO: 实现
    }
}
```

#### 步骤 2: 创建应用服务 (application层)

```java
@Service
public class MarketApplicationService {

    private final MarketDomainService marketDomainService;
    private final OrderAssembler orderAssembler;

    public List<OrderDTO> getOrders() {
        List<Order> orders = marketDomainService.findAllOrders();
        return orderAssembler.toDTO(orders);
    }
}
```

#### 步骤 3: 实现领域逻辑 (domain层)

```java
@Service
public class MarketDomainService {

    private final OrderRepository orderRepository;

    public List<Order> findAllOrders() {
        return orderRepository.findAll();
    }
}
```

#### 步骤 4: 实现基础设施 (infrastructure层)

```java
@Repository
public class OrderRepositoryImpl implements OrderRepository {

    @Autowired
    private OrderMapper orderMapper;

    @Override
    public List<Order> findAll() {
        return orderMapper.selectList(null);
    }
}
```

### 3. 编写测试

```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=MarketControllerTest

# 运行测试并生成覆盖率报告
mvn test jacoco:report
```

### 4. 代码审查

```bash
# 提交代码前检查
mvn clean verify

# 格式化代码
mvn spotless:apply

# 静态代码分析
mvn spotbugs:check
```

### 5. 提交代码

```bash
# 添加变更
git add .

# 提交 (使用规范的提交信息)
git commit -m "feat: 添加市场订单查询功能"

# 推送到远程
git push origin feature/your-feature-name
```

### 6. 创建Pull Request

1. 在GitHub上创建Pull Request
2. 填写PR描述,说明变更内容
3. 等待代码审查
4. 根据反馈修改代码
5. 合并到main分支

## 📦 Maven命令参考

### 基础命令

| 命令 | 描述 |
|------|------|
| `mvn clean` | 清理构建目录 |
| `mvn compile` | 编译源代码 |
| `mvn test` | 运行测试 |
| `mvn package` | 打包应用 (生成JAR) |
| `mvn install` | 安装到本地仓库 |
| `mvn verify` | 运行所有检查 |

### Spring Boot命令

| 命令 | 描述 |
|------|------|
| `mvn spring-boot:run` | 启动应用 |
| `mvn spring-boot:run -Dspring.profiles.active=dev` | 使用dev配置启动 |
| `mvn spring-boot:run -Ddebug` | 调试模式启动 |

### 测试命令

| 命令 | 描述 |
|------|------|
| `mvn test` | 运行所有测试 |
| `mvn test -Dtest=ClassName` | 运行特定测试类 |
| `mvn test -Dtest=ClassName#methodName` | 运行特定测试方法 |
| `mvn test -DskipTests` | 跳过测试 |

### 依赖管理

| 命令 | 描述 |
|------|------|
| `mvn dependency:tree` | 查看依赖树 |
| `mvn dependency:analyze` | 分析依赖 |
| `mvn versions:display-dependency-updates` | 检查依赖更新 |

### 代码质量

| 命令 | 描述 |
|------|------|
| `mvn spotless:check` | 检查代码格式 |
| `mvn spotless:apply` | 自动格式化代码 |
| `mvn spotbugs:check` | 静态代码分析 |

## 📝 代码规范

### Java代码规范

遵循 [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html):

- **缩进**: 4个空格
- **行宽**: 120字符
- **命名**:
  - 类名: `PascalCase`
  - 方法名: `camelCase`
  - 常量: `UPPER_SNAKE_CASE`
  - 包名: `lowercase`

### 提交信息规范

遵循 [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <subject>

<body>

<footer>
```

**类型 (type)**:
- `feat`: 新功能
- `fix`: 修复bug
- `docs`: 文档更新
- `style`: 代码格式调整
- `refactor`: 重构
- `test`: 测试相关
- `chore`: 构建/工具相关

**示例**:
```
feat(market): 添加市场订单查询API

- 实现订单列表查询
- 添加订单详情查询
- 添加单元测试

Closes #123
```

### 分支命名规范

- `feature/功能名称`: 新功能开发
- `bugfix/问题描述`: Bug修复
- `hotfix/紧急修复`: 生产环境紧急修复
- `refactor/重构内容`: 代码重构
- `docs/文档更新`: 文档更新

## 🐛 调试技巧

### 1. 使用IDE调试

**IntelliJ IDEA**:
1. 在代码行号处点击设置断点
2. 点击 Debug 按钮启动应用
3. 使用调试工具栏控制执行流程

### 2. 远程调试

```bash
# 启动应用时启用远程调试
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"

# 在IDE中配置远程调试连接到 localhost:5005
```

### 3. 日志调试

```yaml
# application-dev.yml
logging:
  level:
    root: INFO
    xyz.foolcat.eve.evehelper: DEBUG
    org.springframework.web: DEBUG
    org.hibernate.SQL: DEBUG
```

### 4. 使用Actuator监控

```bash
# 查看应用健康状态
curl http://localhost:9999/actuator/health

# 查看应用信息
curl http://localhost:9999/actuator/info

# 查看所有端点
curl http://localhost:9999/actuator
```

### 5. 数据库查询调试

```yaml
# 打印SQL语句
mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

## 🔧 常见问题

### 问题1: 编译失败 - Lombok注解不生效

**解决方法**:
1. 确保IDE安装了Lombok插件
2. 启用注解处理: Settings → Build → Compiler → Annotation Processors → Enable annotation processing

### 问题2: 测试失败 - 数据库连接错误

**解决方法**:
1. 检查测试环境配置: `src/test/resources/application-test.yml`
2. 确保测试数据库已创建
3. 验证环境变量是否正确设置

### 问题3: 应用启动失败 - 端口被占用

**解决方法**:
```bash
# 查找占用端口的进程
netstat -ano | findstr :9999  # Windows
lsof -i :9999                 # Linux/Mac

# 终止进程或修改端口配置
```

### 问题4: MapStruct映射不生效

**解决方法**:
1. 确保Maven编译时启用了注解处理器
2. 清理并重新构建: `mvn clean compile`
3. 检查MapStruct版本兼容性

## 📚 相关文档

- [环境变量配置](./ENVIRONMENT.md)
- [测试指南](./TESTING.md)
- [部署手册](./DEPLOYMENT.md)
- [API文档](./API.md)
- [DDD架构迁移](../DDD_ARCHITECTURE_MIGRATION.md)

## 🆘 获取帮助

- **文档**: 查看 `docs/` 目录下的文档
- **Issues**: [GitHub Issues](https://github.com/your-org/eve-helper/issues)
- **Wiki**: [项目Wiki](https://github.com/your-org/eve-helper/wiki)
- **团队沟通**: Slack #eve-helper频道

---

**最后更新**: 2026-02-01
**维护者**: EVE Helper Team
