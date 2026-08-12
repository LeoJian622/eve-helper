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
| `007-jwt-key-rotation` | [java-reviewer](./2026-08-12-007-java-reviewer.md) | **BLOCK** | 3 HIGH:~~告警通路不存在(T037 已闭合)~~、aliw 基线绕过、基线③空校验 |
| `007-jwt-key-rotation` | [security-reviewer](./2026-08-12-007-security-reviewer.md) | **BLOCK** | 2 CRITICAL:生产仍加载已泄露私钥、生产口令明文;2 HIGH:~~限流是 DoS 放大器(T036 已闭合)~~、加白在生产静默失效 |

> 阻塞项已拆解为 `specs/007-jwt-key-rotation/tasks.md` 的 T035~T038(CRITICAL/HIGH,必须闭合)与 T039~T046(MEDIUM/LOW)。
> **007 在 T035~T038 全部闭合前不得合并**(AI_WORKFLOW §4:禁止忽略 CRITICAL 或 HIGH)。
>
> **闭合进度**:
> - T036 ✅ DoS 放大器已移除(变异测试 + 零新增失败用例名,见 [T036 评审记录](./2026-08-12-007-T036-review.md) —— ⚠️ 该记录为**主 agent 自审**,两个专职 reviewer 交付通道失败)
> - T037 ✅ 告警通路名实一致:改结构化日志 + **移除三个零引用的 `io.prometheus` 死依赖** + 删除 `DEPLOYMENT.md` 约 150 行从未可用的 Prometheus 运维指南
> - T035 ❌ / T038 ❌ 未闭合

## 门禁要求

- Java 代码变更：`ecc:java-reviewer` 必审
- 涉及认证/授权、用户输入、外部 API、加密：追加 `ecc:security-reviewer`
- CRITICAL 或 HIGH 问题未修复，禁止合并

详见 [工程开发规则](../AI_WORKFLOW.md)。
