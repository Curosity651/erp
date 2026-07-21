-- =====================================================================
-- V20 三平台身份层彻底隔离（系统管理 RBAC 阶段2）
--
-- 目标：货主 / WMS服务商 / 海外仓平台 = 三个相互隔离的后台平台（共用一套代码/库），
--   身份/管理层（用户、角色、组织）彻底按实例隔离；字典/配置收归海外仓平台（方案A）。
--   废弃"平台超管/SUPER_ADMIN"表述，第三身份统一为「海外仓平台 OVERSEAS_PLATFORM」。
--
-- 关键设计：海外仓平台做成"单实例"，其 sys_tenant.id 直接 = -1（= 既有 BLOCK 哨兵）。
--   这样平台请求上下文仍是 -1 → 业务层"看全部货主数据"的逻辑（收货/上架/SKU认货/库存）完全不变；
--   同时 admin 表（sys_user/sys_role/sys_organization）按 tenant_id=-1 scope 到平台自己。一举两得，无需改业务层。
--
-- 身份允许的系统子模块（与 menu-filter.ts / TenantRoleMapper.xml 一致）：
--   货主         : 用户100100/角色100200/组织100700/店铺101100/项目组100900/岗位101000
--   服务商       : 用户100100/角色100200/组织100700
--   海外仓平台   : 用户100100/角色100200/组织100700/字典100500/配置100400/菜单权限100800
--   （字典/配置/菜单权限仅海外仓平台；用户/组织/角色按实例隔离）
--
-- 执行：mysql --default-character-set=utf8mb4 -h127.0.0.1 -P3307 -uroot -p123456 erp < erp-backend/sql/migration/V20__platform_instance_and_user_org_isolation.sql
-- 一次性迁移，勿重复执行。
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) sys_user / sys_organization 增加 tenant_id（纳入租户行级白名单后据此按实例隔离）
-- ---------------------------------------------------------------------
ALTER TABLE sys_user
  ADD COLUMN tenant_id BIGINT NULL COMMENT '所属实例/租户：-1=海外仓平台，>0=服务商/货主；NULL=未绑定(运行时兜底屏蔽)' AFTER type;
ALTER TABLE sys_organization
  ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '所属实例/租户 id' AFTER id;

-- 回填用户归属：按现有 sys_user_tenant 绑定
UPDATE sys_user u
  JOIN sys_user_tenant t ON u.user_id = t.user_id
  SET u.tenant_id = t.tenant_id;
-- 现有组织均为历史货主1所有
UPDATE sys_organization SET tenant_id = 1;

-- ---------------------------------------------------------------------
-- 2) 海外仓平台做成单实例：sys_tenant.id = -1（= BLOCK 哨兵，保业务层"看全部"逻辑不变）
-- ---------------------------------------------------------------------
INSERT INTO sys_tenant (id, tenant_code, tenant_name, tenant_type, status, deleted)
VALUES (-1, 'OVERSEAS_PLATFORM', '海外仓平台', 'OVERSEAS_PLATFORM', 1, 0);

-- 平台管理员 user30 归属海外仓平台实例(-1)；之前无 sys_user_tenant 绑定，补上(仅 is_admin 用)
UPDATE sys_user SET tenant_id = -1 WHERE user_id = 30;
INSERT INTO sys_user_tenant (user_id, tenant_id, is_admin, create_time)
VALUES (30, -1, 1, NOW());

-- ---------------------------------------------------------------------
-- 3) 平台管理员角色：以 ROLE_OVERSEAS_PLATFORM_ADMIN 取代 ROLE_SUPER_ADMIN（彻底改名）
-- ---------------------------------------------------------------------
INSERT INTO sys_role (name, code, tenant_id, type, scope_type, remarks, create_time)
VALUES ('管理员', 'ROLE_OVERSEAS_PLATFORM_ADMIN', -1, 1, 1, '海外仓平台管理员（V20 三平台隔离）', NOW());

-- 平台菜单集（客户/仓库/作业/平台财务/日志/个人页 + 系统{用户/角色/组织/字典/配置/菜单权限} + 数据分析900300 + 容器）
INSERT INTO sys_role_menu (role_code, menu_id)
WITH RECURSIVE inc AS (
    SELECT id FROM sys_menu WHERE id IN (170700,170400,170500,170600,110000,10028,
        100100,100200,100700,100500,100400,100800, 900300)
    UNION ALL SELECT m.id FROM sys_menu m JOIN inc ON m.parent_id = inc.id
)
SELECT 'ROLE_OVERSEAS_PLATFORM_ADMIN', id FROM inc
UNION SELECT 'ROLE_OVERSEAS_PLATFORM_ADMIN', 100000
UNION SELECT 'ROLE_OVERSEAS_PLATFORM_ADMIN', 900000;

-- user30 改派：去 ROLE_SUPER_ADMIN，加 ROLE_OVERSEAS_PLATFORM_ADMIN
DELETE FROM sys_user_role WHERE user_id = 30 AND role_code = 'ROLE_SUPER_ADMIN';
INSERT INTO sys_user_role (user_id, role_code) VALUES (30, 'ROLE_OVERSEAS_PLATFORM_ADMIN');

-- 删除旧 ROLE_SUPER_ADMIN（已无用户引用）
DELETE FROM sys_role_menu WHERE role_code = 'ROLE_SUPER_ADMIN';
DELETE FROM sys_role WHERE code = 'ROLE_SUPER_ADMIN';

-- ---------------------------------------------------------------------
-- 4) 按新菜单集重授各租户实例角色 role_menu（货主去字典/配置；服务商补用户/组织）
-- ---------------------------------------------------------------------
DELETE FROM sys_role_menu WHERE role_code IN ('ROLE_ADMIN_T1','ROLE_ADMIN_T2','ROLE_ADMIN_T3','ROLE_ADMIN_T4');

-- 4.1 货主集（T1、T4）
INSERT INTO sys_role_menu (role_code, menu_id)
WITH RECURSIVE inc AS (
    SELECT id FROM sys_menu WHERE id IN (130000,140000,150000,160000,110000,10028,
        100100,100200,100700,101100,100900,101000, 900100,900200)
    UNION ALL SELECT m.id FROM sys_menu m JOIN inc ON m.parent_id = inc.id
)
SELECT 'ROLE_ADMIN_T1', id FROM inc UNION SELECT 'ROLE_ADMIN_T1',100000 UNION SELECT 'ROLE_ADMIN_T1',900000;

INSERT INTO sys_role_menu (role_code, menu_id)
WITH RECURSIVE inc AS (
    SELECT id FROM sys_menu WHERE id IN (130000,140000,150000,160000,110000,10028,
        100100,100200,100700,101100,100900,101000, 900100,900200)
    UNION ALL SELECT m.id FROM sys_menu m JOIN inc ON m.parent_id = inc.id
)
SELECT 'ROLE_ADMIN_T4', id FROM inc UNION SELECT 'ROLE_ADMIN_T4',100000 UNION SELECT 'ROLE_ADMIN_T4',900000;

-- 4.2 服务商集（T2、T3）
INSERT INTO sys_role_menu (role_code, menu_id)
WITH RECURSIVE inc AS (
    SELECT id FROM sys_menu WHERE id IN (180000,10028,100100,100200,100700,900400)
    UNION ALL SELECT m.id FROM sys_menu m JOIN inc ON m.parent_id = inc.id
)
SELECT 'ROLE_ADMIN_T2', id FROM inc UNION SELECT 'ROLE_ADMIN_T2',100000 UNION SELECT 'ROLE_ADMIN_T2',900000;

INSERT INTO sys_role_menu (role_code, menu_id)
WITH RECURSIVE inc AS (
    SELECT id FROM sys_menu WHERE id IN (180000,10028,100100,100200,100700,900400)
    UNION ALL SELECT m.id FROM sys_menu m JOIN inc ON m.parent_id = inc.id
)
SELECT 'ROLE_ADMIN_T3', id FROM inc UNION SELECT 'ROLE_ADMIN_T3',100000 UNION SELECT 'ROLE_ADMIN_T3',900000;

-- 说明：sys_user.username 已有全局唯一索引 uk_username_deleted，跨实例重名在 DB 层已阻断（登录按用户名全局解析）。
