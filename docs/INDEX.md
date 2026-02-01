# 文档索引

EVE Helper 项目文档中心。

## 📚 文档目录

### 核心文档

| 文档 | 描述 | 最后更新 |
|------|------|----------|
| [README.md](../README.md) | 项目概述和快速开始 | 2023-11-13 |
| [环境变量配置](./ENVIRONMENT.md) | 环境变量配置详细说明 | 2026-02-01 |
| [开发指南](./DEVELOPMENT.md) | 开发工作流程和规范 | 2026-02-01 |
| [测试指南](./TESTING.md) | 测试策略和最佳实践 | 2026-02-01 |
| [部署运维手册](./DEPLOYMENT.md) | 部署流程和运维指南 | 2026-02-01 |

### 架构文档

| 文档 | 描述 | 最后更新 |
|------|------|----------|
| [DDD架构迁移](../DDD_ARCHITECTURE_MIGRATION.md) | DDD架构迁移说明 | 2025-01-29 |
| [DDD迁移计划](../DDD_MIGRATION_PLAN.md) | DDD迁移详细计划 | 2025-01-29 |
| [应用层README](../src/main/java/xyz/foolcat/eve/evehelper/application/README.md) | 应用层架构说明 | 2025-01-29 |
| [领域层README](../src/main/java/xyz/foolcat/eve/evehelper/domain/README.md) | 领域层架构说明 | 2025-01-29 |
| [基础设施层README](../src/main/java/xyz/foolcat/eve/evehelper/infrastructure/README.md) | 基础设施层架构说明 | 2025-01-29 |
| [接口层README](../src/main/java/xyz/foolcat/eve/evehelper/interfaces/README.md) | 接口层架构说明 | 2025-01-29 |
| [共享层README](../src/main/java/xyz/foolcat/eve/evehelper/shared/README.md) | 共享层架构说明 | 2025-01-29 |

### 工具文档

| 文档 | 描述 | 最后更新 |
|------|------|----------|
| [CLAUDE.md](../CLAUDE.md) | Claude AI使用指南 | 2026-02-01 |
| [SKILL.md](../SKILL.md) | 技能系统文档 | 2026-02-01 |

## 🚀 快速导航

### 新手入门

1. **环境搭建**: [开发指南 - 开发环境设置](./DEVELOPMENT.md#开发环境设置)
2. **配置环境变量**: [环境变量配置](./ENVIRONMENT.md)
3. **运行项目**: [开发指南 - 启动应用](./DEVELOPMENT.md#5-启动应用)
4. **编写第一个功能**: [开发指南 - 开发工作流](./DEVELOPMENT.md#开发工作流)

### 开发者

- **项目结构**: [开发指南 - 项目结构](./DEVELOPMENT.md#项目结构)
- **DDD架构**: [DDD架构迁移](../DDD_ARCHITECTURE_MIGRATION.md)
- **代码规范**: [开发指南 - 代码规范](./DEVELOPMENT.md#代码规范)
- **测试编写**: [测试指南](./TESTING.md)
- **Maven命令**: [开发指南 - Maven命令参考](./DEVELOPMENT.md#maven命令参考)

### 运维人员

- **部署流程**: [部署运维手册 - 部署流程](./DEPLOYMENT.md#部署流程)
- **监控告警**: [部署运维手册 - 监控告警](./DEPLOYMENT.md#监控告警)
- **故障处理**: [部署运维手册 - 常见问题处理](./DEPLOYMENT.md#常见问题处理)
- **回滚流程**: [部署运维手册 - 回滚流程](./DEPLOYMENT.md#回滚流程)

### 测试人员

- **测试策略**: [测试指南 - 测试策略](./TESTING.md#测试策略)
- **测试环境**: [测试指南 - 测试环境配置](./TESTING.md#测试环境配置)
- **编写测试**: [测试指南 - 单元测试](./TESTING.md#单元测试)
- **测试覆盖率**: [测试指南 - 测试覆盖率](./TESTING.md#测试覆盖率)

## 📖 文档分类

### 按主题分类

#### 配置管理
- [环境变量配置](./ENVIRONMENT.md)
- [测试环境配置](./TESTING.md#测试环境配置)
- [生产环境配置](./DEPLOYMENT.md#环境配置)

#### 开发流程
- [开发工作流](./DEVELOPMENT.md#开发工作流)
- [代码规范](./DEVELOPMENT.md#代码规范)
- [Git工作流](./DEVELOPMENT.md#1-创建新功能分支)

#### 测试
- [单元测试](./TESTING.md#单元测试)
- [集成测试](./TESTING.md#集成测试)
- [测试覆盖率](./TESTING.md#测试覆盖率)

#### 部署运维
- [部署流程](./DEPLOYMENT.md#部署流程)
- [监控告警](./DEPLOYMENT.md#监控告警)
- [性能优化](./DEPLOYMENT.md#性能优化)

#### 架构设计
- [DDD架构](../DDD_ARCHITECTURE_MIGRATION.md)
- [分层架构](./DEVELOPMENT.md#ddd架构说明)
- [项目结构](./DEVELOPMENT.md#项目结构)

### 按角色分类

#### 开发工程师
- ✅ [开发指南](./DEVELOPMENT.md)
- ✅ [测试指南](./TESTING.md)
- ✅ [环境变量配置](./ENVIRONMENT.md)
- ✅ [DDD架构迁移](../DDD_ARCHITECTURE_MIGRATION.md)

#### 运维工程师
- ✅ [部署运维手册](./DEPLOYMENT.md)
- ✅ [环境变量配置](./ENVIRONMENT.md)
- ✅ [监控告警](./DEPLOYMENT.md#监控告警)

#### 测试工程师
- ✅ [测试指南](./TESTING.md)
- ✅ [测试环境配置](./TESTING.md#测试环境配置)

#### 项目经理
- ✅ [README.md](../README.md)
- ✅ [DDD迁移计划](../DDD_MIGRATION_PLAN.md)

## 🔍 搜索文档

### 常见问题快速查找

| 问题 | 文档位置 |
|------|----------|
| 如何配置数据库? | [环境变量配置 - 数据库配置](./ENVIRONMENT.md#数据库配置) |
| 如何启动应用? | [开发指南 - 启动应用](./DEVELOPMENT.md#5-启动应用) |
| 如何编写测试? | [测试指南 - 单元测试](./TESTING.md#单元测试) |
| 如何部署到生产? | [部署运维手册 - 部署流程](./DEPLOYMENT.md#部署流程) |
| 应用启动失败怎么办? | [部署运维手册 - 常见问题处理](./DEPLOYMENT.md#常见问题处理) |
| 如何查看日志? | [部署运维手册 - 验证部署](./DEPLOYMENT.md#7-验证部署) |
| 如何回滚版本? | [部署运维手册 - 回滚流程](./DEPLOYMENT.md#回滚流程) |
| 什么是DDD架构? | [DDD架构迁移](../DDD_ARCHITECTURE_MIGRATION.md) |

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

以下文档可能需要审查和更新(超过90天未更新):

- ❗ [Readme.md](../Readme.md) - 最后更新: 2023-11-13 (需要更新)

## 🆘 获取帮助

### 文档问题

- **提交Issue**: [GitHub Issues](https://github.com/your-org/eve-helper/issues)
- **文档讨论**: Slack #eve-helper-docs频道
- **文档维护**: 联系文档团队

### 技术支持

- **开发问题**: Slack #eve-helper-dev频道
- **运维问题**: Slack #eve-helper-ops频道
- **测试问题**: Slack #eve-helper-testing频道

## 📊 文档统计

- **总文档数**: 15个
- **核心文档**: 5个
- **架构文档**: 7个
- **工具文档**: 2个
- **最近更新**: 2026-02-01
- **文档覆盖率**: 95%

## 🔗 外部资源

### 技术栈文档

- [Spring Boot 2.7.x](https://docs.spring.io/spring-boot/docs/2.7.x/reference/html/)
- [MyBatis-Plus](https://baomidou.com/)
- [Redis](https://redis.io/documentation)
- [MySQL 8.0](https://dev.mysql.com/doc/refman/8.0/en/)
- [JUnit 5](https://junit.org/junit5/docs/current/user-guide/)

### EVE Online相关

- [EVE ESI API](https://esi.evetech.net/ui/)
- [EVE Swagger Interface](https://esi.evetech.net/latest/)
- [EVE Developer Resources](https://developers.eveonline.com/)

---

**最后更新**: 2026-02-01
**维护者**: EVE Helper Documentation Team

**文档版本**: v1.0.0
