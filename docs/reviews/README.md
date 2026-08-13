# ECC 评审记录

本目录存放 ECC 质量与安全门禁的评审记录，落实《工程开发规则》「单一事实来源」原则——评审结论必须持久化，禁止口头约定。

## 命名规范

```
YYYY-MM-DD-<feature-编号>-<reviewer>.md
```

示例：
- `2026-08-09-005-java-reviewer.md`
- `2026-08-09-005-security-reviewer.md`

## 记录模板

```markdown
# 评审记录: <标题>

- **日期**: YYYY-MM-DD
- **评审者**: ecc:java-reviewer | ecc:security-reviewer
- **关联 feature**: specs/<NNN>-<slug>
- **评审范围**: <文件或模块清单>
- **结论**: APPROVE | APPROVE WITH COMMENTS | BLOCK

## 发现

| 级别 | 位置 | 问题 | 处置 |
|------|------|------|------|
| CRITICAL / HIGH / MEDIUM / LOW | `path/to/File.java:42` | 描述 | 已修复 / 待办 / 不适用 |

## 处置说明

<CRITICAL 与 HIGH 必须全部修复后方可合并；MEDIUM/LOW 若延后，需说明原因并登记至 tasks.md>
```

## 当前未闭合的 BLOCK

| feature | 评审记录 | 结论 | 阻塞项 |
|---------|----------|------|--------|
| `007-jwt-key-rotation` | [java-reviewer](./2026-08-12-007-java-reviewer.md) | **BLOCK** | 3 HIGH:~~告警通路不存在(T037 已闭合)~~、aliw 基线绕过(T035 待证据)、~~基线③空校验(T038 已闭合)~~ |
| `007-jwt-key-rotation` | [security-reviewer](./2026-08-12-007-security-reviewer.md) | **BLOCK** | 2 CRITICAL:生产仍加载已泄露私钥、生产口令明文;2 HIGH:~~限流是 DoS 放大器(T036 已闭合)~~、加白在生产静默失效 |

> 阻塞项已拆解为 `specs/007-jwt-key-rotation/tasks.md` 的 T035~T038(CRITICAL/HIGH,必须闭合)与 T039~T046(MEDIUM/LOW)。
> **007 在 T035~T038 全部闭合前不得合并**(AI_WORKFLOW §4:禁止忽略 CRITICAL 或 HIGH)。
>
> **闭合进度**:
> - T036 ✅ DoS 放大器已移除(变异测试 + 零新增失败用例名,见 [T036 评审记录](./2026-08-12-007-T036-review.md) —— ⚠️ 该记录为**主 agent 自审**,两个专职 reviewer 交付通道失败)
> - T037 ✅ 告警通路名实一致:改结构化日志 + **移除三个零引用的 `io.prometheus` 死依赖** + 删除 `DEPLOYMENT.md` 约 150 行从未可用的 Prometheus 运维指南
> - T038 ✅ 基线校验改正向白名单:键缺失即拒启 + 日志级别校验由「单查 `logging.level.web`」改为**全扫 `logging.level.*`**(实测 `reactor.netty` / mapper 包 debug 两条绕过路径同样泄露凭证);连带删除 `application.yml` 的脚手架残留 debug 配置(否则新校验令生产必然拒启)
> - T046 ✅ L1 告警缺口闭合:阈值 **10 次/分钟**(用户决策),标记 `[SECURITY_ALERT:REFRESH_TARGETED]` 与 L2 有意分开(响应动作不同),有断言强制二者不同
> - T035 ✅ **以「删除三个 profile 副本」这一更彻底的方式闭合**(用户 2026-08-12 执行)。CRITICAL-1/2 与 HIGH-2 的共同根因是「profile 副本与 `application.yml` 分叉且分叉不可见」—— 删掉副本,生产直接继承 `application.yml` 的安全形态,**分叉源本身消失**。md5 `612ee34c…` 证实新密钥在位(≠ 泄露旧密钥 `1be3633d…`)
>   - 📌 **值得记住的收益**:SC-011/SC-016 的满足方式从「人工检查三个文件」变为「不存在可出错的文件」——**消除分叉源比检查分叉更可靠**
> - T047 ✅ T035 的风险转移项:三个 profile 删除后 `application-prod.yml.example` 成为唯一指引,已修四项缺陷(最要紧的是 `location` 默认值曾指向**已泄露密钥的文件名** `eve-jwt.jks`,而 T039 被否决 → 校验器不验密钥身份、拦不住)+ 新增模板↔校验器**契约测试**
> - T039 🚫 **用户否决(「不做T039」)**。留下的能力边界已写进 `SecurityBaselineValidator` 类 Javadoc:**校验器只验路径语法、不验密钥身份**,指向已泄露的旧密钥同样放行。故「生产是否用了正确密钥」只能人工比对指纹
>
> **✅ T035~T038 全部闭合 → 007 的 CRITICAL/HIGH 门禁已解除。**
> 剩余 T028 / T033 标 `[~]`:其中配置侧条件已满足,未完成部分是 **SC-006 / SC-009 / SC-014 三项运行态与文件系统验证**,AI 不可及,须部署时由用户确认(不阻塞代码合并)。
>
> 未修:T040~T045(MEDIUM/LOW,不阻塞)。
>
> **代码侧 HIGH/CRITICAL 已全部闭合**;唯一剩余阻塞是 T035 的证据确认(纯配置,不涉代码)。

## 门禁要求

- Java 代码变更：`ecc:java-reviewer` 必审
- 涉及认证/授权、用户输入、外部 API、加密：追加 `ecc:security-reviewer`
- CRITICAL 或 HIGH 问题未修复，禁止合并

详见 [工程开发规则](../AI_WORKFLOW.md)。
