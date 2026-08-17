-- =====================================================================
-- 009_wallet_journal_idempotency.sql
-- 用途: 保证钱包流水表 wallet_journal 的幂等唯一键 = PRIMARY(id)
-- 归属: specs/009-asset-wallet-aggregate —— T002 钱包幂等 DDL 迁移
--        US2(钱包流水) 的同步幂等依赖该主键(T013 重复同步不产生重复行)
--
-- 【D2 裁决变更说明】(2026-08-18, 用户批准"先对齐基线")
--   原计划 D2 方案: 为 wallet_journal 加 UNIQUE(id, owner_id),使 insertOrUpdateSelective
--   的 ON DUPLICATE KEY UPDATE 生效。但经实况库(eve_helper@192.168.12.249)实测:
--     * SHOW INDEX 显示该表已存在 PRIMARY(id) 主键;
--     * 数据校验 total=33146, distinct_id=33146, 即 id 全局唯一。
--   由此判定: 幂等唯一键即为现有主键 PRIMARY(id), ON DUPLICATE KEY UPDATE 对主键冲突
--   同样生效, UNIQUE(id, owner_id) 与 PRIMARY(id) 冗余、无额外价值, 故【废弃 UNIQUE 方案】。
--   本脚本的两个动作均以"幂等键 = id 主键"为目标, 仅作现状对齐与兜底, 不新增冗余索引。
--   注: 建表脚本基线 src/SQL/convert/数据库创建脚本.sql 已同步补上 id PRIMARY KEY,
--       消除脚本基线(原 id bigint null)与实况库之间的漂移。
--
-- 执行语义(幂等, 可重复执行):
--   1) 若实况库 id 主键已存在(与基线/192 一致): 本脚本第一段不产生任何 DDL/删除, 直接成功。
--   2) 若某环境(如未同步过此表定义的历史库)id 尚无主键: 第一段删除 id 重复的冗余行,
--      第二段补 id 主键, 使幂等键就位。
--   3) 生产库(ali-eve / 47.96.179.174)在本特性开发期网络不可达, 未远程核实;
--      部署时应先对新库/旧库分别执行并核对 SHOW INDEX, 确保 id 主键存在。
--
-- 迁移风险:
--   1. 若某库 id 存在重复且 date 无法区分先后(同一笔交易被重复插入且时间戳一致),
--      第一段的 date 排序判据会删除 0 行, 第二段 ADD PRIMARY KEY 将失败。
--      此时需人工先核对该环境的重复数据来源, 再手动清理后重试。
--   2. id 为 EVE ESI 钱包流水条目全局唯一 id; 若未来依赖"不同 owner 持有相同 id"的假设
--      (当前不存在此场景), 需先撤 PRIMARY(id) 再改复合键, 属跨域设计变更, 不在此脚本范围。
-- =====================================================================

-- 第一步(兜底去重): 仅当 id 存在重复且无主键时可生效; id 已主键的库此段删除 0 行。
--   按 id 分组保留 date 最小(最早)一条, 删除其余 -- date 为 ESI 交易发生时间。
DELETE w FROM wallet_journal w
JOIN wallet_journal w2
  ON w.id = w2.id AND w.date > w2.date;

-- 第二步(幂等补键): 若 id 尚无主键则补 PRIMARY(id) 作为幂等唯一键; 已存在则忽略报错。
--   MySQL 无 ADD PRIMARY KEY IF NOT EXISTS, 故直接执行并检查返回:
--   若已存在主键, MySQL 返回 Duplicate key name 'PRIMARY' 即表明已就位。
ALTER TABLE wallet_journal ADD PRIMARY KEY (id);