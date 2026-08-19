# 文档索引

EVE Helper 项目文档中心。

## 📚 文档目录

### 核心文档

| 文档 | 描述 | 最后更新 |
|------|------|----------|
| [Readme.md](../Readme.md) | 项目概述、快速开始、ESI 授权 scope 清单 | 2026-08-06 |
| [环境变量配置](./ENVIRONMENT.md) | 环境变量配置详细说明 | 2026-08-07 |
| [开发指南](./DEVELOPMENT.md) | 开发工作流程和规范(含测试编写与命令) | 2026-08-06 |
| [部署运维手册](./DEPLOYMENT.md) | 部署流程和运维指南 | 2026-08-07 |

### 架构文档

| 文档 | 描述 | 最后更新 |
|------|------|----------|
| [应用层README](../src/main/java/xyz/foolcat/eve/evehelper/application/README.md) | 应用层架构说明 | 2025-01-29 |
| [领域层README](../src/main/java/xyz/foolcat/eve/evehelper/domain/README.md) | 领域层架构说明(实体/读模型/仓储/端口) | 2026-08-06 |
| [基础设施层README](../src/main/java/xyz/foolcat/eve/evehelper/infrastructure/README.md) | 基础设施层架构说明 | 2026-08-07 |
| [接口层README](../src/main/java/xyz/foolcat/eve/evehelper/interfaces/README.md) | 接口层架构说明(控制器/异常处理) | 2026-08-06 |
| [共享层README](../src/main/java/xyz/foolcat/eve/evehelper/shared/README.md) | 共享层架构说明 | 2026-08-07 |

### 工具文档

| 文档 | 描述 | 最后更新 |
|------|------|----------|
| [CLAUDE.md](../CLAUDE.md) | Claude Code 项目指南(架构/约束/流程速查) | 2026-08-09 |
| [AGENTS.md](../AGENTS.md) | 跨 AI 工具入口指针 | 2026-08-09 |
| [开发工作流（权威）](./workflow/DEVELOPMENT-WORKFLOW.md) | 三系统统一开发流程（强制，任务分轨 + 门禁） | 2026-08-13 |
| [阶段详细步骤](./workflow/PHASE-DETAILS.md) | P0–P7 各阶段步骤、门禁与证据要求 | 2026-08-13 |
| [工具分工矩阵](./workflow/TOOL-MAP.md) | Spec Kit / Superpowers / ECC 职责矩阵与裁决 | 2026-08-13 |
| [项目现状与约定](./project/PROJECT-STATE.md) | 项目现状与 Spec Kit 约定 | 2026-08-13 |
| [ECC 评审记录](./reviews/README.md) | 评审记录归档目录与模板 | 2026-08-09 |
| [功能模块资料库](./knowledge/README.md) | 功能模块流程文档约定与模板（每模块一份） | 2026-08-17 |
| [登录全生命周期](knowledge/login-lifecycle.md) | 注册/登录/请求认证/刷新/登出全链路与 Token 体系 | 2026-08-14 |
| [ESI 授权绑定](knowledge/esi-authorization-binding.md) | OAuth 绑定/accessToken 刷新与锁/授权状态判定 | 2026-08-17 |
| [RBAC 权限体系](knowledge/rbac-authorization.md) | 角色权限数据模型/规则缓存/请求鉴权 | 2026-08-17 |
| [资产多角色聚合](knowledge/asset-aggregate-query.md) | 游戏资产按角色聚合概况(件数/价值/类目数)查询流程 | 2026-08-18 |
| [人物钱包流水](knowledge/wallet-journal-character.md) | 人物钱包 journal 同步与分页倒序查询流程 | 2026-08-18 |
| [钱包交易流水](knowledge/wallet-transaction.md) | 人物/军团钱包交易同步(from_id游标+division失败隔离)与分页倒序查询流程 | 2026-08-18 |
| [军团钱包流水](knowledge/corporation-wallet-journal.md) | 军团钱包 journal 全 division 同步(page-based+M4批量+失败隔离)与分账分页查询流程 | 2026-08-18 |
| [钱包总览](knowledge/wallet-overview.md) | 人物/军团钱包总览聚合(余额/收支/类目/趋势/分账分布)与时间范围/安全/RBAC | 2026-08-19 |

## 🚀 快速导航

### 新手入门

1. **环境搭建**: [开发指南 - 开发环境设置](./DEVELOPMENT.md#开发环境设置)
2. **配置环境变量**: [环境变量配置](./ENVIRONMENT.md)
3. **运行项目**: [开发指南 - 启动应用](./DEVELOPMENT.md#5-启动应用)
4. **编写第一个功能**: [开发指南 - 开发工作流](./DEVELOPMENT.md#开发工作流)

### 开发者

- **项目结构**: [开发指南 - 项目结构](./DEVELOPMENT.md#项目结构)
- **DDD架构**: [开发指南 - DDD架构说明](./DEVELOPMENT.md#ddd架构说明)
- **代码规范**: [开发指南 - 代码规范](./DEVELOPMENT.md#代码规范)
- **测试编写**: [开发指南 - 编写测试](./DEVELOPMENT.md#3-编写测试)
- **Maven命令**: [开发指南 - Maven命令参考](./DEVELOPMENT.md#maven命令参考)

### 运维人员

- **部署流程**: [部署运维手册 - 部署流程](./DEPLOYMENT.md#部署流程)
- **监控告警**: [部署运维手册 - 监控告警](./DEPLOYMENT.md#监控告警)
- **故障处理**: [部署运维手册 - 常见问题处理](./DEPLOYMENT.md#常见问题处理)
- **回滚流程**: [部署运维手册 - 回滚流程](./DEPLOYMENT.md#回滚流程)

### 测试人员

- **测试流程**: [开发工作流（权威） - 特性轨阶段总览](./workflow/DEVELOPMENT-WORKFLOW.md#特性轨阶段总览)
- **编写测试**: [开发指南 - 编写测试](./DEVELOPMENT.md#3-编写测试)
- **测试命令**: [开发指南 - 测试命令](./DEVELOPMENT.md#测试命令)

## 📖 文档分类

### 按主题分类

#### 配置管理
- [环境变量配置](./ENVIRONMENT.md)
- [生产环境配置](./DEPLOYMENT.md#环境配置)

#### 开发流程
- [开发工作流](./DEVELOPMENT.md#开发工作流)
- [代码规范](./DEVELOPMENT.md#代码规范)
- [Git工作流](./DEVELOPMENT.md#1-创建新功能分支)

#### 测试
- [编写测试](./DEVELOPMENT.md#3-编写测试)
- [测试命令](./DEVELOPMENT.md#测试命令)
- [TDD 流程](./workflow/DEVELOPMENT-WORKFLOW.md#铁律)

#### 部署运维
- [部署流程](./DEPLOYMENT.md#部署流程)
- [监控告警](./DEPLOYMENT.md#监控告警)
- [性能优化](./DEPLOYMENT.md#性能优化)

#### 架构设计
- [DDD架构](./DEVELOPMENT.md#ddd架构说明)
- [分层架构](./DEVELOPMENT.md#ddd架构说明)
- [项目结构](./DEVELOPMENT.md#项目结构)

### 按角色分类

#### 开发工程师
- ✅ [开发指南](./DEVELOPMENT.md)
- ✅ [环境变量配置](./ENVIRONMENT.md)
- ✅ [开发工作流（权威）](./workflow/DEVELOPMENT-WORKFLOW.md)

#### 运维工程师
- ✅ [部署运维手册](./DEPLOYMENT.md)
- ✅ [环境变量配置](./ENVIRONMENT.md)
- ✅ [监控告警](./DEPLOYMENT.md#监控告警)

#### 测试工程师
- ✅ [开发指南 - 编写测试](./DEVELOPMENT.md#3-编写测试)
- ✅ [开发工作流（权威）](./workflow/DEVELOPMENT-WORKFLOW.md)

#### 项目经理
- ✅ [Readme.md](../Readme.md)
- ✅ [CLAUDE.md](../CLAUDE.md)

## 🔍 搜索文档

### 常见问题快速查找

| 问题 | 文档位置 |
|------|----------|
| 如何配置数据库? | [环境变量配置 - 数据库配置](./ENVIRONMENT.md#数据库配置) |
| 如何启动应用? | [开发指南 - 启动应用](./DEVELOPMENT.md#5-启动应用) |
| 如何编写测试? | [开发指南 - 编写测试](./DEVELOPMENT.md#3-编写测试) |
| 如何部署到生产? | [部署运维手册 - 部署流程](./DEPLOYMENT.md#部署流程) |
| 应用启动失败怎么办? | [部署运维手册 - 常见问题处理](./DEPLOYMENT.md#常见问题处理) |
| 如何查看日志? | [部署运维手册 - 验证部署](./DEPLOYMENT.md#7-验证部署) |
| 如何回滚版本? | [部署运维手册 - 回滚流程](./DEPLOYMENT.md#回滚流程) |
| 什么是DDD架构? | [开发指南 - DDD架构说明](./DEVELOPMENT.md#ddd架构说明) |

## 📝 文档维护

### 文档更新规范

1. **更新频率**: 每次重大变更后更新相关文档
2. **版本标记**: 在文档底部标注最后更新日期
3. **审核流程**: 文档变更需要经过Code Review
4. **格式规范**: 使用Markdown格式,遵循统一的文档结构

### 文档贡献指南

1. 在 `docs/` 目录下创建或修改文档
2. 使用清晰的标题和目录结构
3. 添加代码示例和截图(如需要)
4. 更新本索引文件
5. 提交PR并请求审核

### 需要更新的文档

当前无超过 90 天未更新的文档。

## 🆘 获取帮助

- **提交 Issue**: [GitHub Issues](https://github.com/LeoJian622/eve-helper/issues)
- **仓库地址**: https://github.com/LeoJian622/eve-helper

## 📊 文档统计

- **docs/ 目录**: INDEX、DEVELOPMENT、DEPLOYMENT、ENVIRONMENT + workflow/(DEVELOPMENT-WORKFLOW、PHASE-DETAILS、TOOL-MAP)+ project/(PROJECT-STATE)+ archive/(已废弃 AI_WORKFLOW、superpowers plans/specs、superpowers-sdd)+ reviews/(ECC 评审记录)
- **分层架构 README**: 5 个(application/domain/infrastructure/interfaces/shared)
- **根目录**: Readme.md、CLAUDE.md、AGENTS.md
- **最近更新**: 2026-08-13

## 🔗 外部资源

### 技术栈文档

- [Spring Boot 3.5.x](https://docs.spring.io/spring-boot/reference/)
- [MyBatis-Plus](https://baomidou.com/)
- [Redis](https://redis.io/documentation)
- [MySQL](https://dev.mysql.com/doc/)
- [JUnit 5](https://junit.org/junit5/docs/current/user-guide/)

### EVE Online相关

- [EVE ESI API](https://esi.evetech.net/ui/)
- [EVE Swagger Interface](https://esi.evetech.net/latest/)
- [EVE Developer Resources](https://developers.eveonline.com/)

---

**最后更新**: 2026-08-13
**维护者**: EVE Helper Documentation Team

**文档版本**: v1.2.0
