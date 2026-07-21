-- =====================================================================
-- V19 角色租户化（系统管理 RBAC 改造 · 阶段1）
--
-- 背景：sys_role 原为全局共享表，所有租户管理员共用同一个 ROLE_ADMIN（含 222 个菜单、
--   system:role:* / system:menu:* 等平台权限），导致货主/服务商能查看/改动全局角色、
--   能给角色勾选海外仓平台的菜单（越权授权）。本次改为「每租户独立角色」：
--     - sys_role 增 tenant_id（平台=-1，货主/服务商=各自 tenant id）；
--     - 配合后端把 sys_role 加入租户行级白名单，原生角色管理自动按 tenant_id 隔离；
--     - 每个租户克隆其类型应有的管理员角色，菜单集裁到本类型允许范围（堵住越权权限）；
--     - 平台菜单集授予 ROLE_SUPER_ADMIN（因菜单纯由 role_menu 并集决定，无超管旁路，
--       去掉平台超管的 ROLE_ADMIN 前必须先把平台菜单授给 ROLE_SUPER_ADMIN）。
--
-- 身份允许的菜单集（与前端 menu-filter.ts 对齐）：
--   货主 ERP_USER   : 商品130000/订单140000/财务150000/仓储160000/日志110000/个人页10028
--                     + 系统100000(去掉菜单权限100800) + 数据分析900000(仅900100/900200)
--   服务商 WMS_OPER. : 服务商运营180000/个人页10028 + 系统100000(仅角色100200/配置100400)
--                     + 数据分析900000(仅900400)
--   平台 SUPER_ADMIN : 客户170700/仓库170400/作业170500/平台财务170600/日志110000/个人页10028
--                     + 系统100000(菜单权限100800/角色100200/配置100400) + 数据分析900000(仅900300)
--
-- 数据现状(执行前)：
--   租户1 自营货主(ERP_USER)  管理员/成员: user 1,18,19,20,21,22,23
--   租户4 a(ERP_USER)        管理员: user 32
--   租户2 WMS服务商(WMS_OPER) 管理员: user 29
--   租户3 weqwe(WMS_OPER)     管理员: user 31
--   平台超管(无租户)          user 30 (ROLE_SUPER_ADMIN + ROLE_ADMIN)
--   孤儿(无租户绑定)          user 27,28 (仅 ROLE_ADMIN，疑似测试残留 → 本次清除)
--
-- 执行：mysql --default-character-set=utf8mb4 -h127.0.0.1 -P3307 -uroot -p123456 erp < erp-backend/sql/migration/V19__tenant_scoped_roles.sql
-- 一次性迁移，勿重复执行（含 ALTER / INSERT，重复会报错或脏数据）。
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) sys_role 增加 tenant_id 列（平台 -1；货主/服务商 = 各自 tenant id）
-- ---------------------------------------------------------------------
ALTER TABLE sys_role
  ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '所属租户：-1=平台，>0=货主/服务商租户 id' AFTER code;

-- ---------------------------------------------------------------------
-- 2) 存量角色打标
--    平台角色 → -1；租户1 历史业务角色（财务/业务/物流经理）→ 1
-- ---------------------------------------------------------------------
UPDATE sys_role SET tenant_id = -1 WHERE code IN ('ROLE_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_ALL');
UPDATE sys_role SET tenant_id = 1  WHERE code IN ('ROLE_MANFER', 'ROLE_MANGER', 'ROLE_WLMANGER');

-- ---------------------------------------------------------------------
-- 3) 克隆每个租户的管理员角色（type=1 系统角色，租户内不可删；scope_type=1）
--    code = ROLE_ADMIN_T{tenantId}
-- ---------------------------------------------------------------------
INSERT INTO sys_role (name, code, tenant_id, type, scope_type, remarks, create_time)
VALUES ('管理员', 'ROLE_ADMIN_T1', 1, 1, 1, '租户1 自营货主管理员（V19 租户化）', NOW()),
       ('管理员', 'ROLE_ADMIN_T4', 4, 1, 1, '租户4 货主管理员（V19 租户化）',      NOW()),
       ('管理员', 'ROLE_ADMIN_T2', 2, 1, 1, '租户2 服务商管理员（V19 租户化）',    NOW()),
       ('管理员', 'ROLE_ADMIN_T3', 3, 1, 1, '租户3 服务商管理员（V19 租户化）',    NOW());

-- ---------------------------------------------------------------------
-- 4) 授 role_menu —— 按身份类型递归取菜单子树
-- ---------------------------------------------------------------------

-- 4.1 货主集（T1、T4）：从 商品/订单/财务/仓储/日志/个人页/系统/数据分析 顶级递归，
--     再剔除 菜单权限(100800)/平台数据分析(900300)/运营数据分析(900400) 这三棵子树。
INSERT INTO sys_role_menu (role_code, menu_id)
WITH RECURSIVE inc AS (
    SELECT id FROM sys_menu WHERE id IN (130000,140000,150000,160000,110000,10028,100000,900000)
    UNION ALL
    SELECT m.id FROM sys_menu m JOIN inc ON m.parent_id = inc.id
),
exc AS (
    SELECT id FROM sys_menu WHERE id IN (100800,900300,900400)
    UNION ALL
    SELECT m.id FROM sys_menu m JOIN exc ON m.parent_id = exc.id
)
SELECT 'ROLE_ADMIN_T1', id FROM inc WHERE id NOT IN (SELECT id FROM exc);

INSERT INTO sys_role_menu (role_code, menu_id)
WITH RECURSIVE inc AS (
    SELECT id FROM sys_menu WHERE id IN (130000,140000,150000,160000,110000,10028,100000,900000)
    UNION ALL
    SELECT m.id FROM sys_menu m JOIN inc ON m.parent_id = inc.id
),
exc AS (
    SELECT id FROM sys_menu WHERE id IN (100800,900300,900400)
    UNION ALL
    SELECT m.id FROM sys_menu m JOIN exc ON m.parent_id = exc.id
)
SELECT 'ROLE_ADMIN_T4', id FROM inc WHERE id NOT IN (SELECT id FROM exc);

-- 4.2 服务商集（T2、T3）：服务商运营180000/个人页10028/角色100200/配置100400/运营数据分析900400 递归
--     + 系统100000、数据分析900000 两个容器节点（用于左树显示父级）。
INSERT INTO sys_role_menu (role_code, menu_id)
WITH RECURSIVE inc AS (
    SELECT id FROM sys_menu WHERE id IN (180000,10028,100200,100400,900400)
    UNION ALL
    SELECT m.id FROM sys_menu m JOIN inc ON m.parent_id = inc.id
)
SELECT 'ROLE_ADMIN_T2', id FROM inc
UNION SELECT 'ROLE_ADMIN_T2', 100000
UNION SELECT 'ROLE_ADMIN_T2', 900000;

INSERT INTO sys_role_menu (role_code, menu_id)
WITH RECURSIVE inc AS (
    SELECT id FROM sys_menu WHERE id IN (180000,10028,100200,100400,900400)
    UNION ALL
    SELECT m.id FROM sys_menu m JOIN inc ON m.parent_id = inc.id
)
SELECT 'ROLE_ADMIN_T3', id FROM inc
UNION SELECT 'ROLE_ADMIN_T3', 100000
UNION SELECT 'ROLE_ADMIN_T3', 900000;

-- 4.3 平台集 → ROLE_SUPER_ADMIN（原本 0 行 role_menu，靠 ROLE_ADMIN 才看得到菜单；
--     去掉 user30 的 ROLE_ADMIN 前必须先补齐）。
--     客户/仓库/作业/平台财务/日志/个人页/菜单权限100800/角色100200/配置100400/平台数据分析900300 递归
--     + 系统100000、数据分析900000 容器。
INSERT INTO sys_role_menu (role_code, menu_id)
WITH RECURSIVE inc AS (
    SELECT id FROM sys_menu WHERE id IN (170700,170400,170500,170600,110000,10028,100800,100200,100400,900300)
    UNION ALL
    SELECT m.id FROM sys_menu m JOIN inc ON m.parent_id = inc.id
)
SELECT 'ROLE_SUPER_ADMIN', id FROM inc
UNION SELECT 'ROLE_SUPER_ADMIN', 100000
UNION SELECT 'ROLE_SUPER_ADMIN', 900000;

-- ---------------------------------------------------------------------
-- 5) 改派用户角色：把租户成员从共享 ROLE_ADMIN 切到各自的租户角色
-- ---------------------------------------------------------------------
-- 租户1（含历史超管 user1，按决定保留为货主1管理员）
UPDATE sys_user_role SET role_code = 'ROLE_ADMIN_T1'
  WHERE role_code = 'ROLE_ADMIN' AND user_id IN (1,18,19,20,21,22,23);
-- 租户4 货主
UPDATE sys_user_role SET role_code = 'ROLE_ADMIN_T4' WHERE role_code = 'ROLE_ADMIN' AND user_id = 32;
-- 租户2 服务商
UPDATE sys_user_role SET role_code = 'ROLE_ADMIN_T2' WHERE role_code = 'ROLE_ADMIN' AND user_id = 29;
-- 租户3 服务商
UPDATE sys_user_role SET role_code = 'ROLE_ADMIN_T3' WHERE role_code = 'ROLE_ADMIN' AND user_id = 31;

-- 平台超管 user30：去掉 ROLE_ADMIN，仅留 ROLE_SUPER_ADMIN（已在 4.3 补齐菜单）
DELETE FROM sys_user_role WHERE role_code = 'ROLE_ADMIN' AND user_id = 30;

-- 孤儿账号 user27/28（无租户绑定、疑似测试残留）：清除其 ROLE_ADMIN
DELETE FROM sys_user_role WHERE role_code = 'ROLE_ADMIN' AND user_id IN (27,28);

-- 说明：执行后共享 ROLE_ADMIN(tenant_id=-1) 不再绑定任何用户，其 222 个 role_menu 保留但已休眠；
--      作为历史模板留存，不删除，避免影响潜在引用。
