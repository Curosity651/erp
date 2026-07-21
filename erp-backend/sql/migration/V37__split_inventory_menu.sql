-- =====================================================================
-- 货主端菜单拆分：新建一级菜单「库存管理」，移入 5 个库存相关页
--   决策(用户)：仓储管理(160000) 条目过多，将纯库存类页面独立成一级菜单。
--   保留在 仓储管理 的是单据类（采购/物流/入库/出库/自定义单）；
--   移入 库存管理(165000) 的是：库存总览161000/库存明细161200/库存记录161100/
--   库存预测161500/库存配置161400（其下 type=2 权限按钮随父项一并迁移）。
--
--   路由影响：路由 = 父path + 子path，父 path 由 wms 变为 inventory，
--   故 /wms/inventory-* 变为 /inventory/inventory-*（前端跳转引用同步已改）。
--   组件加载走 sys_menu.uri（不受路由前缀影响），页面解析不变。
--
--   可见性：前端 menu-filter.ts 已把 'inventory' 加入货主可见顶级；
--   新建货主 provisioning 走 TenantRoleMapper.selectAllowedMenuIds，
--   已把 165000 加入 ERP_USER 递归根集，故未来货主也能拿到本菜单。
-- 执行：mysql --default-character-set=utf8mb4 -h127.0.0.1 -P3307 -uroot -p123456 erp < erp-backend/sql/migration/V37__split_inventory_menu.sql
-- 一次性迁移，勿重复执行。
-- =====================================================================

-- ---------- 1. 新建一级菜单「库存管理」(type=0 目录) ----------
INSERT INTO sys_menu (id, parent_id, title, icon, path, target_type, uri, sort, keep_alive, hidden, type)
VALUES (165000, 0, '库存管理', 'database', 'inventory', 1, NULL, 8, 0, 0, 0);

-- ---------- 2. 5 个库存页移入 165000（子权限按钮随父自动迁移）----------
UPDATE sys_menu SET parent_id = 165000, sort = 1 WHERE id = 161000; -- 库存总览
UPDATE sys_menu SET parent_id = 165000, sort = 2 WHERE id = 161200; -- 库存明细
UPDATE sys_menu SET parent_id = 165000, sort = 3 WHERE id = 161100; -- 库存记录
UPDATE sys_menu SET parent_id = 165000, sort = 4 WHERE id = 161500; -- 库存预测
UPDATE sys_menu SET parent_id = 165000, sort = 5 WHERE id = 161400; -- 库存配置

-- ---------- 3. 授权：凡持有「库存总览」的实例角色，补授新父菜单 165000 ----------
-- （即当前全部货主实例角色 ROLE_ADMIN_T*，自动覆盖，不硬编码具体租户）
INSERT INTO sys_role_menu (role_code, menu_id)
SELECT DISTINCT role_code, 165000 FROM sys_role_menu WHERE menu_id = 161000;

-- ===== 验证 =====
SELECT id, parent_id, title, path, sort FROM sys_menu
 WHERE deleted = 0 AND (id = 165000 OR parent_id = 165000) ORDER BY sort;
