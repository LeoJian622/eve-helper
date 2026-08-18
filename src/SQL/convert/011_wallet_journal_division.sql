-- 011_wallet_journal_division.sql
-- 为 wallet_journal 表加 division 列,支持军团多分账(1-7)与角色(0)区分。
-- 现有数据全是军团 division=1 流水,显式回填。

ALTER TABLE wallet_journal ADD COLUMN division INT;
UPDATE wallet_journal SET division = 1 WHERE division IS NULL;
CREATE INDEX idx_owner_div ON wallet_journal(owner_id, division);
