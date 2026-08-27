-- =====================================================================
-- 015_structure_corporation_id_index.sql
-- structure 表补充 corporation_id 索引 (特性 014 遗留)
--
-- 背景:StructureMapper.xml 的 6 个查询均以 corporation_id 为等值过滤条件
--   (selectStructuresWithNames / selectFuelExpiresList / selectFuelExpiresListWithNames
--    / selectSummary / selectTimers / selectFuelExpiresList),缺少索引会全表扫描。
--
-- 数据源:eve_helper(运行时库,structure 表在 mapper/system/ 包下)
--
-- 幂等:MySQL 不支持 CREATE INDEX IF NOT EXISTS,重复执行前请确认索引是否已存在:
--   SHOW INDEX FROM structure WHERE Key_name = 'idx_structure_corporation_id';
-- =====================================================================

CREATE INDEX idx_structure_corporation_id ON `structure` (`corporation_id`);
