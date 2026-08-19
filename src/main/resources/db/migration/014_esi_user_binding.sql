-- =====================================================================
-- 014_esi_user_binding.sql — ESI 业务数据 × 系统用户强关联 (特性轨 Phase 2 / T001)
--
-- 目标:为 8 张 ESI 业务表新增系统用户归属列 user_id BIGINT NULL,
--       对齐 eve_account.user_id (Long),软引(无 FK 约束)。
-- user_id 语义:同步者系统用户 ID;NULL 表示存量/未归属(迁移前)。
--
-- market_order(公共行情)明确排除,不新增 user_id。
--
-- 注:
--   * column 位置 AFTER 各表现有归属性列。
--   * observer 表真实列名为 mis-spell 的 croporation_id(仓库既有映射保持不动),
--     AFTER 以实际列名定位,避免 ALTER 报 Unknown column。
--   * 本脚本由用户在部署时手动执行;幂等(IF NOT EXISTS)能力在 T026 追加。
-- =====================================================================

ALTER TABLE `assets`
    ADD COLUMN `user_id` BIGINT NULL COMMENT '同步者系统用户ID' AFTER `owner_id`;

ALTER TABLE `blueprints`
    ADD COLUMN `user_id` BIGINT NULL COMMENT '同步者系统用户ID' AFTER `owner_id`;

ALTER TABLE `mining_detail`
    ADD COLUMN `user_id` BIGINT NULL COMMENT '同步者系统用户ID' AFTER `character_id`;

ALTER TABLE `wallet_journal`
    ADD COLUMN `user_id` BIGINT NULL COMMENT '同步者系统用户ID' AFTER `division`;

ALTER TABLE `wallet_transaction`
    ADD COLUMN `user_id` BIGINT NULL COMMENT '同步者系统用户ID' AFTER `division`;

ALTER TABLE `industry_job`
    ADD COLUMN `user_id` BIGINT NULL COMMENT '同步者系统用户ID' AFTER `corporation_id`;

ALTER TABLE `observer`
    ADD COLUMN `user_id` BIGINT NULL COMMENT '同步者系统用户ID' AFTER `croporation_id`;

ALTER TABLE `structure`
    ADD COLUMN `user_id` BIGINT NULL COMMENT '同步者系统用户ID' AFTER `corporation_id`;

-- =====================================================================
-- 存量迁移 (US4 / T024)
--
-- 目标:一次性把历史存量行归属到对应系统用户 ——
--   人物行 → 其角色属主(eve_account.user_id)
--   军团行 → 管理域(ROOT admin)userId
--   孤儿行 → 管理域兜底(未匹配到角色/军团的剩余行)
-- 行数 0 丢失:全部为 UPDATE——不删除、不插入,仅改写 user_id。
--
-- 【部署占位 '?'】
--   下方所有 '?' 为用户部署时的占位,需替换为 ROOT admin 的 user_id。
--   建议一键替换(例如 admin userId=1):
--     sed -i 's/user_id = ?/user_id = 1/g' 014_esi_user_binding.sql
--   person(join) 段取 eve_account.user_id 回填,无需占位。
--
-- 【幂等标注】
--   全部 UPDATE 均带 WHERE user_id IS NULL:已归属/已兜底的行重跑不再改写,天然幂等可重跑。
--   顶层 ADD COLUMN 为 ALTER,MySQL 无 ADD COLUMN IF NOT EXISTS;
--   重复执行前须先 CHECK information_schema.columns 确认列已存在(或手动确保),否则报 Duplicate column。
--
-- 【注意事项】
--   * observer 表真实列名为 mis-spell 的 croporation_id,join 以实际列名定位。
--   * 同一 owner_id 若同时命中 character_id 与 corp_id(EVE ID 空间几乎不重叠),
--     人物段 UPDATE 先执行、军团段 UPDATE 后者覆盖,属多重属主取其一,plan 已裁决不阻塞。
-- =====================================================================

-- ── 1. 人物行 → 角色属主(join eve_account 按 character_id 取 e.user_id) ──
UPDATE assets            a JOIN eve_account e ON e.character_id = a.owner_id      SET a.user_id = e.user_id WHERE a.user_id IS NULL;
UPDATE blueprints        b JOIN eve_account e ON e.character_id = b.owner_id      SET b.user_id = e.user_id WHERE b.user_id IS NULL;
UPDATE mining_detail     m JOIN eve_account e ON e.character_id = m.character_id  SET m.user_id = e.user_id WHERE m.user_id IS NULL;
UPDATE wallet_journal    w JOIN eve_account e ON e.character_id = w.owner_id      SET w.user_id = e.user_id WHERE w.user_id IS NULL;
UPDATE wallet_transaction t JOIN eve_account e ON e.character_id = t.owner_id AND t.owner_type = 'character'  SET t.user_id = e.user_id WHERE t.user_id IS NULL;

-- ── 2. 军团行 → 管理域(ROOT admin userId,部署占位 '?';join eve_account 按 corp_id 限定到系统已登记军团,再 SET 常量 admin) ──
UPDATE structure         s JOIN eve_account e ON e.corp_id = s.corporation_id     SET s.user_id = ? WHERE s.user_id IS NULL;
UPDATE industry_job      i JOIN eve_account e ON e.corp_id = i.corporation_id     SET i.user_id = ? WHERE i.user_id IS NULL;
UPDATE observer          o JOIN eve_account e ON e.corp_id = o.croporation_id     SET o.user_id = ? WHERE o.user_id IS NULL;
UPDATE wallet_journal    w JOIN eve_account e ON e.corp_id = w.owner_id            SET w.user_id = ? WHERE w.user_id IS NULL;
UPDATE wallet_transaction t JOIN eve_account e ON e.corp_id = t.owner_id AND t.owner_type = 'corporation' SET t.user_id = ? WHERE t.user_id IS NULL;

-- ── 3. 孤儿行兜底(迁移后仍 user_id IS NULL 的未匹配行,归管理域 admin) ──
UPDATE assets            SET user_id = ? WHERE user_id IS NULL;
UPDATE blueprints        SET user_id = ? WHERE user_id IS NULL;
UPDATE mining_detail     SET user_id = ? WHERE user_id IS NULL;
UPDATE industry_job      SET user_id = ? WHERE user_id IS NULL;
UPDATE observer          SET user_id = ? WHERE user_id IS NULL;
UPDATE structure         SET user_id = ? WHERE user_id IS NULL;
UPDATE wallet_journal    SET user_id = ? WHERE user_id IS NULL;
UPDATE wallet_transaction SET user_id = ? WHERE user_id IS NULL;