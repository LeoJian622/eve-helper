# 评审记录: 008 安全评审遗留修复 — 实现后独立安全复审(SC-006)

- **日期**: 2026-08-13
- **评审者**: ecc:security-reviewer(独立复审,只读)
- **关联 feature**: `specs/008-security-review-followup`
- **评审对象**: 008 实现(commit `afca42b..283d895`,US1 装饰器/US2 handler/US3 卫生项)
- **对照设计**: research.md v2 R1~R9(design security-reviewer 已于 2026-08-13 APPROVE)
- **结论**: **APPROVE — 零 CRITICAL / 零 HIGH**。008 实质闭合目标缺陷。

## 设计决策闭合确认(逐项实测)

| 决策 | 结论 |
|------|------|
| R1 装饰器五点语义 | ✅ 快照传播/执行前后双向清理(finally)/同线程直通/快照副本隔离,单元+端到端测试钉死;跨用户泄漏已防(worker context 任务间不残留) |
| R1 无 fail-open 回归 | ✅ 移除 MODE_INHERITABLETHREADLOCAL 后 AccessGuard/UserUtil 空上下文均 fail-closed;两异步池均装配装饰器且 initialize 前 |
| R2 hide=false + 统一响应 | ✅ 对外不可区分(byte-identical),异常类名仅日志;成对比对测试覆盖 |
| R5 \p{Cc} 免疫 + 脱敏 | ✅ 5 处限流日志 + handler 均 maskUsername,剔除 \p{Cc} 后无 CRLF/控制字符残留 |
| R4 refresh 边界 | ✅ 实际 token 为 UUID.randomUUID,@Pattern(UUID)+@Size(max=64) 正确且不误伤 |
| R6 删死方法 + 契约 | ✅ writeTokenInfo 已删,契约测试钉死真实过滤链 no-store/无 ACAO |

## 发现(全部 MEDIUM/LOW,均预存在或 008 范围外)

| 级别 | 位置 | 问题 | 处置 |
|------|------|------|------|
| MEDIUM-1 | LoginRateLimiterService:25-27,37 | 锁定按 username 单键,无 IP 维度,定向账号锁定 DoS(预存在) | 登记 follow-up spec(IP+username 键 + permitAll 登录端点 IP 限流) |
| MEDIUM-2 | AuthenticationFailureServletHandler:44-51 | Redis 故障时锁定计数不递增 → 暴力破解防护静默失效(记录为 availability-over-lockout 有意取舍) | 008 research 显式记录为接受残余风险 |
| MEDIUM-3 | AuthenticationFailureServletHandler:40 / LoginRateLimiterService:37 | 登录 username 无长度上限 → Redis 键空间内存 DoS(008 已加 refresh 边界但未加 login) | 登记 follow-up(max-length 校验 + 有界/哈希键) |
| LOW-1 | SecurityContextTaskDecorator:34-35 | 快照共享 Authentication 对象(未深拷贝),Spring 约定认证后不可变,风险低 | 记录边界,可选深拷贝 |
| LOW-2 | ResponseUtils:40-41 | writeErrorInfo 错误路径 ACAO:* + no-cache,与成功路径 no-store/无 ACAO 不一致(不可利用) | 可选对齐 |
| LOW-3 | LoginRateLimiterService:84 | 原始 attemptsObj 数值入日志(INCR 写入,非攻击者可控) | 可选脱敏 |
| LOW-4 | GlobalExceptionHandler:124-127,130-134 | ServerException/IllegalArgumentException 仍 echo e.getMessage() 给客户端(008 未改) | 登记 follow-up(同 R9 结构化摘要) |

## 复核要点(预存在的登录限流残余面)

M-1/M-2/M-3 均落在登录限流器(008 R5 已脱敏其日志,但未改其键结构/边界),建议作为独立 follow-up spec:① 锁定键加 IP 维度;② Redis 故障 fail-open 的取舍显式接受;③ login username 长度上限。

## 复审约定

实现 APPROVE;M-1/M-2/M-3 与 L-1/L-2/L-4 登记为后续 polish/follow-up,不阻塞 008 交付。