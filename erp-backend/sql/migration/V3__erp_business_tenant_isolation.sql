-- =====================================================================
-- A3 · 其余 ERP/OMS 业务表接入租户隔离
-- 范围：货主业务表（订单/店铺/贴标/统计辅助/WB财务）。
-- 不含：wms_* 物理表（C/D 阶段加 erp_tenant_id）、exchange_rate/sys_file（全局共享）。
-- 唯一键策略：键起始列已是"跨租户唯一的父ID"(shop_id/batch_id)则保留；
--             键基于会跨租户重复的业务码/外部ID则重建为 (tenant_id, ...)。
-- 执行：mysql --default-character-set=utf8mb4 -h127.0.0.1 -P3307 -uroot -p123456 erp < erp-backend/sql/migration/V3__erp_business_tenant_isolation.sql
-- 一次性迁移，勿重复执行。
-- =====================================================================

-- ===== 订单 =====
ALTER TABLE erp_order
  ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID' AFTER id,
  ADD INDEX idx_tenant_id (tenant_id),
  DROP INDEX ux_order_platform_order,
  ADD UNIQUE KEY uk_tenant_order_platform (tenant_id, platform, platform_order_id);

ALTER TABLE erp_order_item
  ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID' AFTER id,
  ADD INDEX idx_tenant_id (tenant_id);

-- ===== 贴标批次（batch_id 为跨租户唯一父ID，子表唯一键保留） =====
ALTER TABLE erp_label_batch
  ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID' AFTER id,
  ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE erp_label_batch_file
  ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID' AFTER id,
  ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE erp_label_batch_item
  ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID' AFTER id,
  ADD INDEX idx_tenant_id (tenant_id);

-- ===== 店铺 =====
ALTER TABLE shop
  ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID' AFTER id,
  ADD INDEX idx_tenant_id (tenant_id),
  DROP INDEX ux_shop_platform_platform_shop,
  ADD UNIQUE KEY uk_tenant_shop_platform (tenant_id, platform, platform_shop_id);

-- ===== 系统辅助（业务码跨租户独立） =====
ALTER TABLE project_group
  ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID' AFTER id,
  ADD INDEX idx_tenant_id (tenant_id),
  DROP INDEX code,
  ADD UNIQUE KEY uk_tenant_project_group_code (tenant_id, code);
ALTER TABLE position
  ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID' AFTER id,
  ADD INDEX idx_tenant_id (tenant_id),
  DROP INDEX code,
  ADD UNIQUE KEY uk_tenant_position_code (tenant_id, code);
ALTER TABLE sales_target
  ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID' AFTER id,
  ADD INDEX idx_tenant_id (tenant_id),
  DROP INDEX uk_target,
  ADD UNIQUE KEY uk_tenant_target (tenant_id, target_type, target_year, target_month);

-- ===== 同步游标（shop_id 跨租户唯一，唯一键保留） =====
ALTER TABLE sync_cursor
  ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID' AFTER id,
  ADD INDEX idx_tenant_id (tenant_id);

-- ===== WB 财务 =====
ALTER TABLE wb_report_detail
  ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID' AFTER id,
  ADD INDEX idx_tenant_id (tenant_id),
  DROP INDEX uqx_period_rrdid,
  ADD UNIQUE KEY uk_tenant_period_rrdid (tenant_id, period_type, rrd_id);
ALTER TABLE wb_financial_sync_job
  ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID' AFTER id,
  ADD INDEX idx_tenant_id (tenant_id),
  DROP INDEX uk_job_code,
  ADD UNIQUE KEY uk_tenant_job_code (tenant_id, job_code);
ALTER TABLE wb_financial_sync_task
  ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID' AFTER id,
  ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE wb_financial_sync_page_log
  ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID' AFTER id,
  ADD INDEX idx_tenant_id (tenant_id);

-- ===== WB 业务（wb_office: shop_id 唯一键保留；wb_supply: supply_id 可跨租户重复，重建） =====
ALTER TABLE wb_office
  ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID' AFTER id,
  ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE wb_supply
  ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID' AFTER id,
  ADD INDEX idx_tenant_id (tenant_id),
  DROP INDEX ux_supply_platform_supply,
  ADD UNIQUE KEY uk_tenant_supply_platform (tenant_id, platform, supply_id);
