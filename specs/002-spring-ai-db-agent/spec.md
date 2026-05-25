# Feature Specification: Spring AI 数据库查询 Agent

**Feature Branch**: `002-spring-ai-db-agent`  
**Created**: 2026-05-19  
**Status**: Draft  
**Input**: User description: "我要引入spring ai ailibaba 1.1.2.0 来开发一个查询展现数据库的agent"

## Clarifications

### Session 2026-05-19

- Q: 数据最大返回条数限制? → A: 不超过50条
- Q: 数据库操作安全限制? → A: 禁止任何会破坏原有数据的操作
- Q: 超过50条数据如何处理? → A: 只返回前面50条数据
- Q: 使用哪个数据源存储查询历史? → A: 使用声明为systemd的数据源存储查询历史记录
- Q: 集成哪个AI大模型? → A: 使用spring-ai-alibaba-starter-dashscope 1.1.2.0集成通义千问大模型

### Session 2026-05-21

- Q: 多轮对话记忆如何实现? → A: 使用 Spring AI Alibaba 框架自带的 Chat Memory 记忆管理能力，而非自定义实现

## User Scenarios & Testing *(mandatory)*

### User Story 1 - 自然语言查询数据库 (Priority: P1)

用户通过自然语言提问,系统自动转换为 SQL 查询并执行,最终以友好的格式展现查询结果。

**Why this priority**: 这是核心功能,直接提供用户价值。没有这个功能,整个 Agent 就没有存在的意义。

**Independent Test**: 可以通过发送一个自然语言问题,验证系统能够返回正确的数据库查询结果。

**Acceptance Scenarios**:

1. **Given** 系统已连接到 EVE 数据库, **When** 用户提问"查询所有 MarketOrder 的数量", **Then** 系统返回正确的订单总数。
2. **Given** 用户的问题涉及多个表关联, **When** 用户提问"查询某个用户的所有资产", **Then** 系统生成正确的 JOIN 查询并返回结果。
3. **Given** 用户的问题包含筛选条件, **When** 用户提问"查询价格超过 100万 ISK 的订单", **Then** 系统生成带有 WHERE 条件的正确查询。
4. **Given** 查询结果超过 50 条, **When** 系统返回数据时, **Then** 系统只返回前面50条数据。

---

### User Story 2 - 查询结果可视化展示 (Priority: P2)

系统支持以表格、图表等多种形式展示查询结果,帮助用户更好地理解数据。

**Why this priority**: 数据展现是用户体验的重要组成部分,能够显著提升数据的可读性和价值。

**Independent Test**: 可以通过执行一个查询,验证系统能够以表格或图表形式正确展示结果。

**Acceptance Scenarios**:

1. **Given** 查询返回多条记录, **When** 结果展示时, **Then** 系统以清晰的表格形式呈现数据。
2. **Given** 查询包含可统计的数据, **When** 用户选择图表展示, **Then** 系统生成对应的柱状图或饼图。

---

### User Story 3 - 查询历史和上下文管理 (Priority: P3)

系统保存用户的查询历史,并支持基于上下文的连续对话。

**Why this priority**: 提升用户体验,支持复杂的探索式查询。

**Independent Test**: 可以通过连续提问多个相关问题,验证系统能够理解上下文并正确回答。

**Acceptance Scenarios**:

1. **Given** 用户已经执行过一次查询, **When** 用户提问"再列出前10条", **Then** 系统理解上下文并返回正确的结果。
2. **Given** 用户需要查看之前的查询, **When** 用户请求历史记录, **Then** 系统显示完整的查询历史。

---

### Edge Cases

- 当用户的问题无法理解或无法生成有效 SQL 时,系统如何友好提示?
- 当查询返回超过 50 条数据时,系统只返回前面50条数据
- 当 SQL 查询包含危险操作(DELETE、DROP、ALTER、TRUNCATE、UPDATE 无 WHERE、INSERT 等)时,系统立即拦截不执行
- 当数据库连接超时或查询执行超时时,系统如何处理?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: 系统 MUST 支持用户通过自然语言(中文)提问查询数据库
- **FR-002**: 系统 MUST 将自然语言自动转换为合法的 SQL 查询语句
- **FR-003**: 系统 MUST 执行生成的 SQL 查询并返回结果
- **FR-004**: 系统 MUST 以友好的表格格式展示查询结果
- **FR-005**: 系统 MUST 拦截并禁止执行任何会破坏原有数据的 SQL 操作(DELETE、DROP、ALTER、TRUNCATE、UPDATE 无 WHERE、INSERT 等)
- **FR-006**: 系统 MUST 支持查询执行超时保护
- **FR-007**: 系统 MUST 保存用户的查询历史记录
- **FR-008**: 系统 MUST 支持基于对话上下文的连续查询
- **FR-009**: 系统 MUST 提供 REST API 接口供前端调用
- **FR-010**: 系统 MUST 集成 Spring Security 进行访问控制保护
- **FR-011**: 系统 MUST 限制单次查询返回结果数量,超过50条时只返回前面50条数据

### Key Entities

- **Database Query Agent**: 处理自然语言到 SQL 的转换,执行查询并管理对话上下文
- **Query History**: 记录用户的查询历史,包括问题、生成的 SQL、执行时间、结果数量
- **Database Schema Context**: 数据库表结构、字段、关系的元数据,用于辅助 AI 生成正确的 SQL
- **Security Policy**: SQL 安全规则,定义允许和禁止的操作类型,严格限制为只读操作

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 用户可以在 30 秒内完成从提问到看到查询结果的完整流程
- **SC-002**: 自然语言转 SQL 的准确率达到 90% 以上(针对常见业务查询场景)
- **SC-003**: 95% 的查询在 10 秒内完成并返回结果
- **SC-004**: 系统 100% 拦截配置的危险 SQL 操作,禁止任何数据破坏操作
- **SC-005**: 用户可以查看至少最近 100 条查询历史记录
- **SC-006**: 系统正确理解上下文关联查询的比例达到 85% 以上
- **SC-007**: 单次查询超过50条结果时,系统只返回前面50条数据

## Assumptions

- 目标用户是熟悉 EVE Online 游戏数据的玩家和运营人员
- 数据库采用双数据源架构: eve 数据源(存储游戏静态数据)和 systemd 数据源(存储应用运行时数据)
- 查询历史记录存储在 systemd 数据源中
- 现有 Spring Boot 架构和安全机制可以复用到新功能上
- 用户通过现有认证机制登录后使用本功能
- 仅支持只读查询操作(SELECT),绝对禁止任何修改数据的操作
- 初始版本支持中文自然语言查询
- 查询结果默认以表格形式展示,图表展示为可选增强功能
- 集成通义千问大模型,使用 spring-ai-alibaba-starter-dashscope 1.1.2.0
- 多轮对话记忆使用 Spring AI Alibaba 框架自带的 Chat Memory 管理能力（MessageChatMemory）
- 单次查询超过50条结果时,只返回前面50条数据
