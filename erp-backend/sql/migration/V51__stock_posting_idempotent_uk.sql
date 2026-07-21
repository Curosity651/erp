-- M-2：过账幂等 DB 兜底。此前幂等仅靠应用层「先查后插」（无锁），RC 并发下双读 null → 双插双应用库存。
-- 加唯一键 (source_type, source_id, posting_type, erp_tenant_id)：并发第二插被 DB 拦下，服务层捕获后回查返回幂等。
-- 含 erp_tenant_id 维：规避各货主 source_id 序列独立自增时的跨租户撞号（否则 A 货主过账会误命中 B 的记录被跳过）。
-- 说明：历史行 erp_tenant_id 存在 NULL（V22 加列为 NULL），MySQL 唯一键中 NULL 互不相等、不阻断本 ALTER；
--       新过账均带货主维，NULL 仅存于历史/无货主流水（transfer 等，其 source_id 全局唯一），由应用层「先查后插」兜底。
--       上线前已核验：当前无 (source_type, source_id, posting_type, erp_tenant_id) 重复组。
ALTER TABLE wms_stock_posting
    ADD UNIQUE KEY uk_source_posting_tenant (source_type, source_id, posting_type, erp_tenant_id);
