-- =====================================================================
-- C2 · 货架分配（平台超管把仓库某几排分配给 WMS 服务商）
-- 平台级合同流水：无 deleted（结束用 effective_to）；审计沿用 create_by/create_time/update_time。
-- 执行：mysql --default-character-set=utf8mb4 -h127.0.0.1 -P3307 -uroot -p123456 erp < erp-backend/sql/migration/V9__rack_assignment.sql
-- 一次性迁移，勿重复执行。
-- =====================================================================

CREATE TABLE IF NOT EXISTS wms_rack_assignment (
  id                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  wms_tenant_id     BIGINT        NOT NULL COMMENT '被分配的 WMS 服务商 tenant_id',
  warehouse_id      BIGINT        NOT NULL COMMENT '所在仓库',
  rack_no           VARCHAR(20)   NOT NULL COMMENT '货架排号(每排一条)',
  monthly_fee       DECIMAL(12,2) NOT NULL COMMENT '该排月租金(CNY)',
  effective_from    DATE          NOT NULL COMMENT '分配生效日期',
  effective_to      DATE          DEFAULT NULL COMMENT '分配结束日期(NULL=当前有效)',
  contract_file_url VARCHAR(500)  DEFAULT NULL COMMENT '签署合同扫描件',
  remark            VARCHAR(500)  DEFAULT NULL COMMENT '备注',
  create_by         BIGINT        DEFAULT NULL COMMENT '操作超管 user_id',
  create_time       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_wms_tenant (wms_tenant_id, effective_from),
  KEY idx_wh (warehouse_id),
  UNIQUE KEY uk_rack_period (rack_no, warehouse_id, effective_from)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='货架分配记录(超管分配给WMS服务商)';

-- 菜单：货架分配（挂在 平台管理 170000 下，仅平台超管前端可见）
INSERT INTO sys_menu (id, parent_id, title, icon, path, target_type, uri, sort, keep_alive, hidden, type)
VALUES (170200, 170000, '货架分配', 'build', 'rack-assignment', 1, 'platform/rack-assignment', 2, 0, 0, 1);

INSERT INTO sys_role_menu (role_code, menu_id) VALUES ('ROLE_ADMIN', 170200);

-- ===== 验证 =====
SELECT COUNT(*) rack_tbl FROM information_schema.tables WHERE table_schema='erp' AND table_name='wms_rack_assignment';
SELECT id, parent_id, title, path, uri FROM sys_menu WHERE id = 170200;
