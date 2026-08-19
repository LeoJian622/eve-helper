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