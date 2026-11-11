# 任务质量清单: 022 伴侣 APP 后端支撑端点（G3 门禁）

**Purpose**: 对 tasks.md 作为「实现需求文档」的质量做单元测试——粒度/TDD 顺序/签名一致性/FR 覆盖/安全与零侵入约束（用户指定焦点）
**Created**: 2026-11-12
**Feature**: [spec.md](../spec.md) · [plan.md](../plan.md) · [tasks.md](../tasks.md)
**Depth**: 正式门禁（G3）｜**Audience**: 评审者（用户批准前核对）
**核验方式**: 逐项对照 spec/plan/data-model/contracts/research/quickstart 与代码库实测签名；`[x]`＝通过（证据见文末核验记录）

## 任务粒度与可执行性

- [x] CHK001 每个任务是否具备唯一 ID、复选框与精确文件路径（无「某处/相关文件」式指代）？[Completeness, tasks.md Format]
- [x] CHK002 每个任务的交付物是否为携带自身测试循环的最小单元（评审者可以独立否决其一而不影响邻任务）？[Clarity, writing-plans 粒度标准]
- [x] CHK003 每个任务的 Interfaces 块是否给出 Consumes/Produces 的精确签名（名称/参数/返回类型），执行者无需补做设计决策？[Completeness, tasks.md 各任务]
- [x] CHK004 任务文本是否不含未决占位符（TBD/TODO/「酌情」「以实际为准」「等」式未定指代）？[Ambiguity, Gap]
- [x] CHK005 [P] 标记是否仅授予不同文件且无未完成依赖的任务（共享文件 EsiGateway/EsiApiService/UserController 跨任务不并行）？[Consistency, tasks.md Dependencies]

## TDD 顺序与验收质量

- [x] CHK006 每个故事的测试任务是否先于实现任务，且明文要求 RED 证据后才写生产代码？[Consistency, 工作流铁律 1]
- [x] CHK007 测试用例是否逐条覆盖 spec 各故事的 Acceptance Scenarios 并可追溯（用例名↔场景编号）？[Coverage, Spec §User Stories]
- [x] CHK008 每个任务是否给出可执行的验证命令与预期结果（RED→GREEN 两态）？[Measurability, tasks.md AC]

## 签名与工件一致性

- [x] CHK009 跨任务复用的端口签名是否完全一致（queryCharacterRoles 于 T022/T029/T038；AppNotificationPushGateway 于 T025/T031/T041；listAll/listByCorpId 于 T028/T030/T031/T032）？[Consistency]
- [x] CHK010 VO record 字段是否与 data-model §3 及 P3 补充注记一致（网关子读模型 vs 应用聚合视图的命名区分：CharacterSkillList vs CharacterSkillsInfo）？[Consistency, data-model §3]
- [x] CHK011 AppNotificationRepository 七签名是否与 data-model §1.5 逐字一致？[Consistency, data-model §1.5]
- [x] CHK012 测试类名是否与 quickstart.md 命令一致？[Consistency, quickstart.md]
- [x] CHK013 文件路径是否与代码库实际位置一致（EsiApiService=gateway/esi；Filter=config/security/filter；EveAccount 三件套；SQL 范例=012）？[Consistency, P3 校正]

## FR / SC / 澄清覆盖

- [x] CHK014 28 个 FR 是否每个至少有一个任务归属（含横切 FR-025/026 的落点）？[Coverage, Spec §Requirements]
- [x] CHK015 10 个 SC 是否均有验证路径（任务 AC 或 quickstart 手动项）？[Coverage, Spec §Success Criteria]
- [x] CHK016 6 项用户澄清是否全部落进任务语义（24h 重复/精确解析/直查不落库/权限扇出/同步≤1h/手动 code）？[Consistency, Spec §Clarifications]

## 安全需求质量

- [x] CHK017 全部新端点的属主校验要求是否明确，且区分请求线程（authorize）与定时线程（authorizeInternal）两种身份路径？[Coverage, Spec FR-008/019/023]
- [x] CHK018 改密撤销纪元的 fail-closed 语义是否定义为可测试矩阵（含「无标记时旧 token 无 iat 照常通过」的部署兼容分支）？[Clarity, research R10]
- [x] CHK019 WS ticket 的安全属性（一次性/30s/JWT 不进 URL/失败即关闭）是否量化并归属到任务测试？[Clarity, research R11]
- [x] CHK020 燃料扇出权限边界（无建筑权限用户零通知）是否定义为可客观验证的需求？[Measurability, Spec FR-027/SC-006]
- [x] CHK021 解绑守卫（系统同步角色不可解绑）与解绑后 fail-closed 行为是否明确成文？[Completeness, research R14]
- [x] CHK022 限流阈值（改密 5 次/30min）与全部输入校验上限（recipients≤50/subject≤128/body≤10000/names≤50/密码 8-64）是否量化？[Clarity, data-model §6]

## 零侵入与架构约束

- [x] CHK023 QQ 零变更约束是否枚举到文件级（StructTask/IndustryTask/WalletTask/StructureService/OneBot WebSocket）并归属回归任务？[Consistency, Spec FR-018]
- [x] CHK024 014 归属不变是否体现在同步写入语义（userId=同步角色属主）与解绑后历史数据保留要求？[Consistency, research R13/R14]
- [x] CHK025 零新增依赖约束是否在 Global Constraints 声明且与宪法 IV 栈冻结一致（WS 用既有 starter）？[Consistency, 宪法 IV]
- [x] CHK026 DDD 依赖方向要求（application/domain 零 infrastructure import；任务→领域服务；WS 推送经领域端口；auth-url 经端口）是否明确且与 plan P3 校正一致？[Consistency, plan §Structure Decision]

## 依赖、假设与部署

- [x] CHK027 故事间依赖（T029 共用取先执行者、T025→US6 改密）与推荐执行顺序是否明确无环？[Completeness, tasks.md Dependencies]
- [x] CHK028 测试环境前置（T024 前测试库执行 DDL；@SpringBootTest 内网 MySQL/Redis 依赖与归因规则）是否成文？[Dependency, research R12]
- [x] CHK029 部署步骤（手动执行 DDL+RBAC → 重启刷新权限缓存）是否归属任务并注记原因？[Completeness, research R9]
- [x] CHK030 去重窗口语义（一次性事件保留期内至多一次 vs 持续条件 24h；燃料按 dedup_key 全局判定）是否在需求层面无歧义？[Clarity, Spec FR-017]

---

## 核验记录（2026-11-12，逐项证据）

| 项 | 证据 |
|----|------|
| CHK001 | 48 任务全部含 `- [ ] T0NN` + Files 精确路径；T028/T035 两处模糊指代已在本轮修正为实测路径/类名 |
| CHK002 | 每任务独立 Files+AC+测试循环；网关按端点组切分（T009/T010/T011），仓储/服务/控制器分离 |
| CHK003 | 16 端口方法、3 缓存 Set 方法、7 仓储签名、推送端口 3 方法、应用服务方法全部给出精确签名；SDK 消费签名经本轮 grep 实测（CharacterApi:49/296、LocationApi:46/69/92、SkillApi:48/72/96、MailApi:55/80/133/162/189、CorporationApi:446、UniverseApi:321） |
| CHK004 | 全文扫描无 TBD/TODO；「以实际为准」「等」式指代已清零（T028→三件套实测路径；T035→四个实测测试类） |
| CHK005 | [P] 仅 14 处，均不同文件；EsiGateway/EsiApiService 任务链（T009→T010→T011、T016→T017→T018、T029、T037、T043）无 [P]；Dependencies 节明文共享文件规则 |
| CHK006 | US1/2/3/5/6 独立 RED 测试任务（T007/008、T014/015、T021、T036、T039）先于实现；US4 每任务 AC 首项即 RED→GREEN；Global Constraints 2 铁律 |
| CHK007 | AC 内显式引用 spec 场景号（US1-3/4/5、US2-4/5/7、US3-2/3、US5-2/3、US6-1/2/3/4）；US4 九场景分布 T025/T027/T031/T032/T034/T035 |
| CHK008 | 每任务 AC 含 `mvn test -Dtest=…` 命令与 RED/GREEN 两态要求 |
| CHK009 | queryCharacterRoles(CharacterRolesInfo, Integer, String) 三处一致；推送端口三方法 T025/T031/T041 一致；listAll/listByCorpId 四处一致 |
| CHK010 | 网关 CharacterSkillList（含嵌套 SkillItem）vs 应用 CharacterSkillsInfo（合并 attributes）命名区分已写入 data-model P3 注记与 T011/T012 |
| CHK011 | T024 代码块与 data-model §1.5 逐字比对一致（含 markRead 双条件注释） |
| CHK012 | quickstart 四处类名已对齐 tasks（CharacterPanelApplicationServiceTest/MailApplicationServiceTest/IndustryJobQueryApplicationServiceTest+JobControllerTest/MemberTrackingApplicationServiceTest）；其余类名原生一致 |
| CHK013 | 本轮实测：EsiApiService 在 infrastructure/gateway/esi/；Filter 在 config/security/filter/；EveAccount 三件套路径确认；RBAC 范例 012_rbac_permissions.sql 存在（021 无端点 SQL） |
| CHK014 | FR-001~003→T007-013；004~008→T014-020；009/010→T021/022；011~013→T036-038；014→T031-033；015→T024/027；016→T025/026/031；017→T031/032/034；018→T035+约束3；019→T024/027/031；020→T027；021→T005/006/041；022→T040/042；023→T041/042；024→T043；025→T002/047；026→约束7+网关 AC；027→T031/034；028→T030 |
| CHK015 | SC-001→T048/quickstart；002→T035；003→T035/T048；004→T045；005→各故事测试+T048；006→T044；007→T034；008→T035；009→US6 测试；010→US2 链路+contracts 回信注记 |
| CHK016 | 24h 重复→T032/T034；精确解析→T018；直查不落库→T021/022/T037；扇出→T031；≤1h→T030 SETNX 3600；手动 code→T043+contracts §6 |
| CHK017 | 约束 4 双路径签名+行号；各应用服务任务 Consumes AuthorizeUtil；检测任务用 authorizeInternal |
| CHK018 | T006 四例矩阵×2 路径（含 noMarker_legacyTokenWithoutIat_passes）；Review Focus 1 |
| CHK019 | T026 AC 五例（valid/missing/expired/replayed/onClose）+30s TTL+「JWT 不进 URL」明文 |
| CHK020 | T031 fuel_fanout_onlyPermittedUsers + T034 fanout_userLosesPermission + T044 矩阵 |
| CHK021 | T042 守卫 TaskConstant.CHARACTER_ID(2112818290) + unbind_subsequentCharacterEndpoint_failsClosed |
| CHK022 | T041 限流 5/30min；T015/T019 校验上限逐字＝data-model §6；ChangePasswordRequest 8-64 |
| CHK023 | 约束 3 五类文件名 + T035 四个回归测试类 + git diff --stat 证据要求 |
| CHK024 | T030 userId=属主（014 语义）；T042 历史数据保留明文 |
| CHK025 | 约束 1 零新增依赖；plan Post-Design Re-Check 依赖行一致 |
| CHK026 | 约束 5 + plan P3 校正②③④ + T025 端口/T043 端口任务一致 |
| CHK027 | Dependencies 节三条例外+推荐顺序图，无环（T029 取先执行者规则消解 US3/US4 互依赖） |
| CHK028 | 约束 10 + T024 前置粗体注记 |
| CHK029 | T003 重启注释 AC + T047 终审 + Notes 部署行 |
| CHK030 | T024 existsByDedupKey_ignoresTimeWindow/boundary24h；T031 全局判定粗体；T032 一次性语义；data-model §1.3 一致 |

**结果**: 30/30 通过（CHK004/CHK012/CHK013 发现的漂移已在本轮修正后复验）。
