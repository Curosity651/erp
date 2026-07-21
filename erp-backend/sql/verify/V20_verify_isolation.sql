-- =====================================================================
-- V20 校验：三平台身份层隔离（import V20 后执行，只读）
-- 执行：mysql --default-character-set=utf8mb4 -h127.0.0.1 -P3307 -uroot -p123456 erp < erp-backend/sql/verify/V20_verify_isolation.sql
-- =====================================================================

-- 1) 海外仓平台单实例：应有 id=-1 / OVERSEAS_PLATFORM
SELECT id, tenant_code, tenant_name, tenant_type FROM sys_tenant WHERE id = -1;

-- 2) sys_user 归属分布（-1=平台 user30；1=货主1；2/3=服务商；4=货主4；NULL=未绑定兜底)
SELECT tenant_id, GROUP_CONCAT(user_id ORDER BY user_id) users, COUNT(*) cnt
FROM sys_user WHERE deleted = 0 GROUP BY tenant_id ORDER BY tenant_id;

-- 3) 角色：ROLE_SUPER_ADMIN 应已不存在；ROLE_OVERSEAS_PLATFORM_ADMIN(tenant -1) 应存在
SELECT id, name, code, tenant_id FROM sys_role WHERE code IN ('ROLE_SUPER_ADMIN','ROLE_OVERSEAS_PLATFORM_ADMIN');

-- 4) 各实例角色菜单数（重授后）：平台/货主/服务商
SELECT role_code, COUNT(*) menu_cnt FROM sys_role_menu
WHERE role_code IN ('ROLE_OVERSEAS_PLATFORM_ADMIN','ROLE_ADMIN_T1','ROLE_ADMIN_T2','ROLE_ADMIN_T3','ROLE_ADMIN_T4')
GROUP BY role_code ORDER BY role_code;

-- 5) 越权/范围检查：
--   货主T1：应含用户100100/角色100200/组织100700，不含字典100500/配置100400/菜单权限100800/平台顶级
--   服务商T2：应含用户100100/角色100200/组织100700，不含字典/配置/菜单权限
SELECT 'T1用户100100' k, COUNT(*) v FROM sys_role_menu WHERE role_code='ROLE_ADMIN_T1' AND menu_id=100100
UNION ALL SELECT 'T1组织100700', COUNT(*) FROM sys_role_menu WHERE role_code='ROLE_ADMIN_T1' AND menu_id=100700
UNION ALL SELECT 'T1不含字典100500', COUNT(*) FROM sys_role_menu WHERE role_code='ROLE_ADMIN_T1' AND menu_id=100500
UNION ALL SELECT 'T1不含配置100400', COUNT(*) FROM sys_role_menu WHERE role_code='ROLE_ADMIN_T1' AND menu_id=100400
UNION ALL SELECT 'T1不含菜单权限100800', COUNT(*) FROM sys_role_menu WHERE role_code='ROLE_ADMIN_T1' AND menu_id=100800
UNION ALL SELECT 'T2用户100100', COUNT(*) FROM sys_role_menu WHERE role_code='ROLE_ADMIN_T2' AND menu_id=100100
UNION ALL SELECT 'T2组织100700', COUNT(*) FROM sys_role_menu WHERE role_code='ROLE_ADMIN_T2' AND menu_id=100700
UNION ALL SELECT 'T2不含配置100400', COUNT(*) FROM sys_role_menu WHERE role_code='ROLE_ADMIN_T2' AND menu_id=100400
UNION ALL SELECT '平台含菜单权限100800', COUNT(*) FROM sys_role_menu WHERE role_code='ROLE_OVERSEAS_PLATFORM_ADMIN' AND menu_id=100800
UNION ALL SELECT '平台含字典100500', COUNT(*) FROM sys_role_menu WHERE role_code='ROLE_OVERSEAS_PLATFORM_ADMIN' AND menu_id=100500;
-- 期望：含的=1，不含的=0

-- 6) 平台越权检查：货主/服务商角色不应含任何平台顶级(170400/170500/170600/170700)
SELECT role_code, COUNT(*) leak FROM sys_role_menu
WHERE role_code IN ('ROLE_ADMIN_T1','ROLE_ADMIN_T2','ROLE_ADMIN_T3','ROLE_ADMIN_T4')
  AND menu_id IN (170400,170500,170600,170700,100800,100500,100400)
GROUP BY role_code;
-- 期望：无结果(0 行)

-- 7) user30 角色（期望仅 ROLE_OVERSEAS_PLATFORM_ADMIN）
SELECT user_id, GROUP_CONCAT(role_code) roles FROM sys_user_role WHERE user_id=30 GROUP BY user_id;
