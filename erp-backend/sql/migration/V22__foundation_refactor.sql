-- =====================================================================
-- V22 底层数据表重构（一次性 schema）。详见 底层数据表重构设计.md。
-- 仅 schema；代码切换分阶段进行。sys_user_tenant 删表留到 P9（代码切换后）。
-- 执行：mysql --default-character-set=utf8mb4 -h127.0.0.1 -P3307 -uroot -p123456 erp < erp-backend/sql/migration/V22__foundation_refactor.sql
-- =====================================================================
SET FOREIGN_KEY_CHECKS = 0;

-- P3: sys_user 加 is_admin（实例管理员标记），从 sys_user_tenant 回填
ALTER TABLE sys_user ADD COLUMN is_admin TINYINT NOT NULL DEFAULT 0 COMMENT '实例管理员 1是/0否';
UPDATE sys_user u JOIN sys_user_tenant t ON t.user_id = u.user_id SET u.is_admin = COALESCE(t.is_admin, 0);

-- P8: sys_file / export_data 纳入 Tier T（tenant_id）
ALTER TABLE sys_file    ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '所属租户', ADD KEY idx_tenant_id (tenant_id);
ALTER TABLE export_data ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '所属租户', ADD KEY idx_tenant_id (tenant_id);

-- P4: 三日志加 tenant_id（写入时填；读取按它过滤）
ALTER TABLE log_login_log     ADD COLUMN tenant_id BIGINT NULL COMMENT '所属租户', ADD KEY idx_tenant_id (tenant_id);
ALTER TABLE log_operation_log ADD COLUMN tenant_id BIGINT NULL COMMENT '所属租户', ADD KEY idx_tenant_id (tenant_id);
ALTER TABLE log_access_log    ADD COLUMN tenant_id BIGINT NULL COMMENT '所属租户', ADD KEY idx_tenant_id (tenant_id);

-- P5: region_inventory 加 erp_tenant_id（单维），重建唯一键
ALTER TABLE wms_region_inventory ADD COLUMN erp_tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '归属货主 tenant_id';
ALTER TABLE wms_region_inventory DROP INDEX uk_region_sku, ADD UNIQUE KEY uk_tenant_region_sku (erp_tenant_id, region_id, sku_code);

-- P7: 平台作业明细 + 过账 加双维（溯源 + 货主级/仓库级双重过账）；建单/过账时显式写入
ALTER TABLE wms_adjustment_order_item ADD COLUMN erp_tenant_id BIGINT NULL COMMENT '货主维(溯源)', ADD COLUMN wms_tenant_id BIGINT NULL COMMENT '服务商维(溯源)';
ALTER TABLE wms_stocktake_order_item  ADD COLUMN erp_tenant_id BIGINT NULL COMMENT '货主维(溯源)', ADD COLUMN wms_tenant_id BIGINT NULL COMMENT '服务商维(溯源)';
ALTER TABLE wms_transfer_order_item   ADD COLUMN erp_tenant_id BIGINT NULL COMMENT '货主维(溯源)', ADD COLUMN wms_tenant_id BIGINT NULL COMMENT '服务商维(溯源)';
ALTER TABLE wms_stock_posting         ADD COLUMN erp_tenant_id BIGINT NULL COMMENT '货主维', ADD COLUMN wms_tenant_id BIGINT NULL COMMENT '服务商维', ADD KEY idx_erp_tenant (erp_tenant_id), ADD KEY idx_wms_tenant (wms_tenant_id);
ALTER TABLE wms_stock_posting_item    ADD COLUMN erp_tenant_id BIGINT NULL COMMENT '货主维', ADD COLUMN wms_tenant_id BIGINT NULL COMMENT '服务商维';

-- P4/P9: 删垃圾备份表
DROP TABLE IF EXISTS erp_order_bak_20260227;

SET FOREIGN_KEY_CHECKS = 1;
