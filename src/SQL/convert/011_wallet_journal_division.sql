-- 011_wallet_journal_division.sql
-- 为 wallet_journal 表加 division 列,支持军团多分账(1-7)与角色(0)区分。
-- NOT NULL DEFAULT 0 使 ADD COLUMN 为 instant DDL(MySQL 8.0+),不锁表。
-- 之后 UPDATE 将已有军团数据回填为 division=1。

ALTER TABLE wallet_journal ADD COLUMN division INT NOT NULL DEFAULT 0;
UPDATE wallet_journal SET division = 1 WHERE division = 0;
CREATE INDEX idx_owner_div ON wallet_journal(owner_id, division);
