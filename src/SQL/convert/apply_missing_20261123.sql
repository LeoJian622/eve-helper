-- =====================================================================
-- apply_missing_20261123.sql
--
-- Production database: eve_helper
-- Scope:
--   1. Create wallet_transaction when missing (010)
--   2. Add user_id to the 8 ESI business tables when missing (014)
--   3. Add wallet_journal.idx_owner_div_date when missing (012)
--   4. Add wallet_journal.idx_owner_div and structure.corporation index when missing
--   5. Backfill historical user_id values
--
-- Preconditions:
--   * ROOT administrator user_id is 1. Replace 1 below if production differs.
--   * eve_account contains the character/corporation ownership mapping.
--   * Back up the production database before executing.
--
-- This script intentionally does not add wallet_journal PRIMARY KEY.
-- Run 009_wallet_journal_unique.sql only after checking duplicates and backing up.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. wallet_transaction (010 + the user_id column required by 014)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wallet_transaction (
  id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '自增主键(不依赖 ESI id)',
  owner_type     VARCHAR(32)  NOT NULL COMMENT '所有者类型:character=人物,corporation=军团',
  owner_id       BIGINT       NOT NULL COMMENT '所有者ID(人物 ID 或军团 ID)',
  division       INT          NOT NULL COMMENT '钱包 division:人物=0,军团 1..7',
  user_id        BIGINT       NULL COMMENT '同步者系统用户ID',
  transaction_id BIGINT       NOT NULL COMMENT 'ESI 交易ID',
  `date`         DATETIME     NOT NULL COMMENT '交易发生时间',
  type_id        INT          NULL COMMENT '物品类型ID',
  quantity       INT          NULL COMMENT '数量',
  unit_price     DOUBLE       NULL COMMENT '单价',
  client_id      INT          NULL COMMENT '对手方客户端ID',
  location_id    BIGINT       NULL COMMENT '下单位置ID',
  is_buy         TINYINT(1)   NULL COMMENT '是否为买单',
  is_personal    TINYINT(1)   NULL COMMENT '是否个人交易',
  journal_ref_id BIGINT       NULL COMMENT '关联钱包流水条目ID',
  PRIMARY KEY (id),
  UNIQUE KEY uk_owner_div_tx (owner_type, owner_id, division, transaction_id),
  KEY idx_date (`date`),
  KEY idx_owner (owner_type, owner_id, division)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='钱包交易记录表(人物/军团钱包交易流水)';

-- ---------------------------------------------------------------------
-- 2. Add user_id to all tables in 014 when missing
-- ---------------------------------------------------------------------
SET @ddl := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'assets' AND COLUMN_NAME = 'user_id') = 0,
  'ALTER TABLE `assets` ADD COLUMN `user_id` BIGINT NULL COMMENT ''同步者系统用户ID'' AFTER `owner_id`',
  'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'blueprints' AND COLUMN_NAME = 'user_id') = 0,
  'ALTER TABLE `blueprints` ADD COLUMN `user_id` BIGINT NULL COMMENT ''同步者系统用户ID'' AFTER `owner_id`',
  'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mining_detail' AND COLUMN_NAME = 'user_id') = 0,
  'ALTER TABLE `mining_detail` ADD COLUMN `user_id` BIGINT NULL COMMENT ''同步者系统用户ID'' AFTER `character_id`',
  'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wallet_journal' AND COLUMN_NAME = 'user_id') = 0,
  'ALTER TABLE `wallet_journal` ADD COLUMN `user_id` BIGINT NULL COMMENT ''同步者系统用户ID'' AFTER `division`',
  'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wallet_transaction' AND COLUMN_NAME = 'user_id') = 0,
  'ALTER TABLE `wallet_transaction` ADD COLUMN `user_id` BIGINT NULL COMMENT ''同步者系统用户ID'' AFTER `division`',
  'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'industry_job' AND COLUMN_NAME = 'user_id') = 0,
  'ALTER TABLE `industry_job` ADD COLUMN `user_id` BIGINT NULL COMMENT ''同步者系统用户ID'' AFTER `corporation_id`',
  'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'observer' AND COLUMN_NAME = 'user_id') = 0,
  'ALTER TABLE `observer` ADD COLUMN `user_id` BIGINT NULL COMMENT ''同步者系统用户ID'' AFTER `croporation_id`',
  'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'structure' AND COLUMN_NAME = 'user_id') = 0,
  'ALTER TABLE `structure` ADD COLUMN `user_id` BIGINT NULL COMMENT ''同步者系统用户ID'' AFTER `corporation_id`',
  'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------
-- 3. Missing indexes from 012 and 015
-- ---------------------------------------------------------------------
SET @ddl := IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wallet_journal'
     AND INDEX_NAME = 'idx_owner_div_date') = 0,
  'CREATE INDEX idx_owner_div_date ON wallet_journal (owner_id, division, `date`)',
  'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl := IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wallet_journal'
     AND INDEX_NAME = 'idx_owner_div') = 0,
  'CREATE INDEX idx_owner_div ON wallet_journal (owner_id, division)',
  'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl := IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'structure'
     AND INDEX_NAME = 'idx_structure_corporation_id') = 0,
  'CREATE INDEX idx_structure_corporation_id ON `structure` (`corporation_id`)',
  'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------
-- 4. Backfill historical ownership (014)
--    Replace 1 with the actual ROOT administrator user_id if necessary.
-- ---------------------------------------------------------------------
UPDATE assets a
JOIN eve_account e ON e.character_id = a.owner_id
SET a.user_id = e.user_id
WHERE a.user_id IS NULL;

UPDATE blueprints b
JOIN eve_account e ON e.character_id = b.owner_id
SET b.user_id = e.user_id
WHERE b.user_id IS NULL;

UPDATE mining_detail m
JOIN eve_account e ON e.character_id = m.character_id
SET m.user_id = e.user_id
WHERE m.user_id IS NULL;

UPDATE wallet_journal w
JOIN eve_account e ON e.character_id = w.owner_id
SET w.user_id = e.user_id
WHERE w.user_id IS NULL;

UPDATE wallet_transaction t
JOIN eve_account e ON e.character_id = t.owner_id
SET t.user_id = e.user_id
WHERE t.user_id IS NULL AND t.owner_type = 'character';

UPDATE structure s
JOIN eve_account e ON e.corp_id = s.corporation_id
SET s.user_id = 1
WHERE s.user_id IS NULL;

UPDATE industry_job i
JOIN eve_account e ON e.corp_id = i.corporation_id
SET i.user_id = 1
WHERE i.user_id IS NULL;

UPDATE observer o
JOIN eve_account e ON e.corp_id = o.croporation_id
SET o.user_id = 1
WHERE o.user_id IS NULL;

UPDATE wallet_journal w
JOIN eve_account e ON e.corp_id = w.owner_id
SET w.user_id = 1
WHERE w.user_id IS NULL;

UPDATE wallet_transaction t
JOIN eve_account e ON e.corp_id = t.owner_id
SET t.user_id = 1
WHERE t.user_id IS NULL AND t.owner_type = 'corporation';

UPDATE assets SET user_id = 1 WHERE user_id IS NULL;
UPDATE blueprints SET user_id = 1 WHERE user_id IS NULL;
UPDATE mining_detail SET user_id = 1 WHERE user_id IS NULL;
UPDATE industry_job SET user_id = 1 WHERE user_id IS NULL;
UPDATE observer SET user_id = 1 WHERE user_id IS NULL;
UPDATE structure SET user_id = 1 WHERE user_id IS NULL;
UPDATE wallet_journal SET user_id = 1 WHERE user_id IS NULL;
UPDATE wallet_transaction SET user_id = 1 WHERE user_id IS NULL;

-- ---------------------------------------------------------------------
-- 5. Verification
-- ---------------------------------------------------------------------
SELECT expected.table_name,
       IF(actual.column_name IS NULL, 'MISSING', 'OK') AS status
FROM (
    SELECT 'assets' AS table_name
    UNION ALL SELECT 'blueprints'
    UNION ALL SELECT 'mining_detail'
    UNION ALL SELECT 'wallet_journal'
    UNION ALL SELECT 'wallet_transaction'
    UNION ALL SELECT 'industry_job'
    UNION ALL SELECT 'observer'
    UNION ALL SELECT 'structure'
) expected
LEFT JOIN information_schema.COLUMNS actual
  ON actual.TABLE_SCHEMA = DATABASE()
 AND actual.TABLE_NAME = expected.table_name
 AND actual.COLUMN_NAME = 'user_id';

SELECT 'wallet_journal.PRIMARY' AS item,
       IF(COUNT(*) > 0, 'OK', 'MISSING') AS status
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'wallet_journal'
  AND INDEX_NAME = 'PRIMARY';
