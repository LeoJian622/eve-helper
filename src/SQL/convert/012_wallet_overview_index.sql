-- =====================================================================
-- 012_wallet_overview_index.sql
-- 用途: 为钱包总览聚合查询补复合索引,优化大语料下 owner(+division)+date 区间的扫描。
--   wallet_journal 现有索引: PRIMARY(id)、character_wallet_journal_owner_id_index(owner_id)、
--   idx_owner_div(owner_id, division)。缺少 date 参与,钱包总览的 5 条聚合查询
--   (selectOverviewAggregate/Categories/Trend/DivisionBalances/DivisionFlow)按 owner(+division)
--   过滤并常带 date 区间,owner 单列索引只能锁 owner 行再内存扫 date。
-- 归属: specs/012-wallet-overview —— 12 final-review Minor#1 follow-up(复合索引)
-- 关联端点: GET:/wallet/overview/{cid}、GET:/wallet/overview/corp/{corpId}
-- 幂等: 用 information_schema 判 INDEX_NAME 是否存在,不存在才 CREATE,可重复执行。
-- 执行: 在 eve_helper 运行库执行本脚本即生效;零锁表风险链入正常窗口即可(建索引非 instant 但表读频率低)。
-- =====================================================================

SET @idx_exists := (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'wallet_journal'
    AND INDEX_NAME = 'idx_owner_div_date'
);

SET @ddl := IF(@idx_exists = 0,
  'CREATE INDEX idx_owner_div_date ON wallet_journal (owner_id, division, `date`)',
  'SELECT ''idx_owner_div_date already exists, skip'' AS info');

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;