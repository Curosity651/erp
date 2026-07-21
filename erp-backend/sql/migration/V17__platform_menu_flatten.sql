-- =====================================================================
-- 平台(超管)菜单拍平：去掉「平台管理」三级包裹，改为最多两级。
--   一级：客户管理 / 仓库管理 / 海外仓作业 / 平台财务（仓储管理 160000 本就两级，不动）。
--   纯菜单重组：路由由 uri 决定、与菜单位置无关；无页面/路由/后端改动。
-- 执行：mysql --default-character-set=utf8mb4 -h127.0.0.1 -P3307 -uroot -p123456 erp < erp-backend/sql/migration/V17__platform_menu_flatten.sql
-- 一次性迁移，勿重复执行。
-- =====================================================================

-- ---------- 1. 仓库管理（复用 170400「仓库建模」→ 提升为一级 + 改名）----------
UPDATE sys_menu
   SET parent_id = 0, title = '仓库管理', path = 'warehouse-mgmt', icon = 'database', sort = 51
 WHERE id = 170400;

-- 1a. 货架分配（170200）从 平台管理 迁入 仓库管理
UPDATE sys_menu SET parent_id = 170400, sort = 4 WHERE id = 170200;

-- 1b. 仓库管理叶子（160100）改名，避免与一级同名；规整子项排序
UPDATE sys_menu SET title = '仓库信息', sort = 1 WHERE id = 160100;
UPDATE sys_menu SET sort = 2 WHERE id = 161800; -- 库位管理
UPDATE sys_menu SET sort = 3 WHERE id = 161600; -- 区域管理

-- ---------- 2. 海外仓作业（170500 提升为一级）----------
UPDATE sys_menu SET parent_id = 0, icon = 'deployment-unit', sort = 52 WHERE id = 170500;

-- ---------- 3. 平台财务（170600 提升为一级）----------
UPDATE sys_menu SET parent_id = 0, icon = 'account-book', sort = 53 WHERE id = 170600;

-- ---------- 4. 客户管理（新建一级）+ WMS服务商 迁入 ----------
INSERT INTO sys_menu (id, parent_id, title, permission, path, icon, target_type, uri, sort, keep_alive, hidden, type, deleted)
VALUES (170700, 0, '客户管理', NULL, 'customer-mgmt', 'team', 1, NULL, 50, 0, 0, 0, 0);

UPDATE sys_menu SET parent_id = 170700, sort = 1 WHERE id = 170100;

INSERT INTO sys_role_menu (role_code, menu_id) VALUES ('ROLE_ADMIN', 170700);

-- ---------- 5. 删除空壳「平台管理」(170000) ----------
DELETE FROM sys_role_menu WHERE menu_id = 170000;
DELETE FROM sys_menu WHERE id = 170000;
