-- =====================================================================
-- V21 货主单据数据级隔离 + 新开通管理员补齐功能权限
--
-- Bug1：以下货主业务表此前无任何租户列，货主之间互相可见。本次加 erp_tenant_id（回填存量为货主1），
--   配合后端 WmsOwnerDataPermissionHandler 对其 SELECT/UPDATE/DELETE 自动注入 erp_tenant_id 范围：
--   货主只见自己、WMS服务商只见名下货主(只读)、海外仓平台看全部(不过滤→平台收货/上架回写不受影响)。
--   新建时由 MyMetaObjectHandler 自动盖章 erp_tenant_id=当前货主。
--     wms_logistics_provider 物流商 / wms_purchase_order 采购单 / wms_shipping_order 物流单 /
--     wms_sales_outbound_order 销售·自定义出库 / wms_return_inbound_order 退货·自定义退货 / wms_inventory_config 库存配置。
--   （明细表 *_item 不加列，查询经父单 id 透传隔离；已隔离的入库单/库存/批次/流水不在本次范围。）
--
-- Bug2：新开通的货主/服务商管理员应是"本平台全部功能权限"管理员，但其页面依赖的共享只读端点
--   （仓库 options /wms/warehouse、区域 /wms/region）要求 wms:warehouse:read(160101)/wms:region:read(161601)，
--   而租户管理员角色没有这两码 → 子请求 403（admin 走超管旁路未暴露）。本次给租户管理员补这两码（仅补权限，
--   菜单仍被 menu-filter 按顶级隐藏，不显示平台菜单）；并补建 product:sku:export 按钮(130405)修复 SKU 导出。
--
-- 执行：mysql --default-character-set=utf8mb4 -h127.0.0.1 -P3307 -uroot -p123456 erp < erp-backend/sql/migration/V21__erp_owner_isolation_and_admin_perms.sql
-- 一次性迁移，勿重复执行。
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) Bug1：6 张货主业务表加 erp_tenant_id（NOT NULL DEFAULT 1 → 存量自动回填货主1）+ 索引
-- ---------------------------------------------------------------------
ALTER TABLE wms_logistics_provider  ADD COLUMN erp_tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '归属货主 tenant_id' AFTER id, ADD KEY idx_erp_tenant (erp_tenant_id);
ALTER TABLE wms_purchase_order       ADD COLUMN erp_tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '归属货主 tenant_id' AFTER id, ADD KEY idx_erp_tenant (erp_tenant_id);
ALTER TABLE wms_shipping_order       ADD COLUMN erp_tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '归属货主 tenant_id' AFTER id, ADD KEY idx_erp_tenant (erp_tenant_id);
ALTER TABLE wms_sales_outbound_order ADD COLUMN erp_tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '归属货主 tenant_id' AFTER id, ADD KEY idx_erp_tenant (erp_tenant_id);
ALTER TABLE wms_return_inbound_order ADD COLUMN erp_tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '归属货主 tenant_id' AFTER id, ADD KEY idx_erp_tenant (erp_tenant_id);
ALTER TABLE wms_inventory_config     ADD COLUMN erp_tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '归属货主 tenant_id' AFTER id, ADD KEY idx_erp_tenant (erp_tenant_id);

-- ---------------------------------------------------------------------
-- 2) Bug2：补建 product:sku:export 按钮菜单（挂 SKU 管理 130400 下）
-- ---------------------------------------------------------------------
INSERT INTO sys_menu (id, parent_id, title, permission, type, sort, deleted, create_time)
VALUES (130405, 130400, 'SKU管理表导出', 'product:sku:export', 2, 5, 0, NOW());

-- ---------------------------------------------------------------------
-- 3) Bug2：给租户管理员角色补授共享只读码 + 货主补 sku:export
--    160101=wms:warehouse:read，161601=wms:region:read（货主/服务商页面下拉依赖）
-- ---------------------------------------------------------------------
INSERT INTO sys_role_menu (role_code, menu_id)
SELECT r.code, m.menu_id
FROM (SELECT 'ROLE_ADMIN_T1' code UNION ALL SELECT 'ROLE_ADMIN_T2' UNION ALL SELECT 'ROLE_ADMIN_T3' UNION ALL SELECT 'ROLE_ADMIN_T4') r
JOIN (SELECT 160101 menu_id UNION ALL SELECT 161601) m
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_code=r.code AND x.menu_id=m.menu_id);

-- sku:export 仅授货主（T1/T4；服务商无商品菜单）
INSERT INTO sys_role_menu (role_code, menu_id)
SELECT r.code, 130405
FROM (SELECT 'ROLE_ADMIN_T1' code UNION ALL SELECT 'ROLE_ADMIN_T4') r
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_code=r.code AND x.menu_id=130405);

-- 说明：新开通租户由 TenantRoleMapper.selectAllowedMenuIds 自动纳入 160101/161601（货主/服务商分支）与
--      130405（在 product 树内，货主分支整树递归自动包含）。
