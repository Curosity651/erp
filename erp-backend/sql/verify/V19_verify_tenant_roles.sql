-- =====================================================================
-- V19 校验：角色租户化结果核对（import V19 后执行，全部为只读）
-- 执行：mysql --default-character-set=utf8mb4 -h127.0.0.1 -P3307 -uroot -p123456 erp < erp-backend/sql/verify/V19_verify_tenant_roles.sql
-- =====================================================================

-- 1) sys_role 的 tenant_id 分布（期望：平台角色=-1；克隆角色 T1=1/T4=4/T2=2/T3=3；历史业务角色=1）
SELECT id, name, code, tenant_id, type FROM sys_role ORDER BY tenant_id, code;

-- 2) 各角色菜单数（期望 货主 T1/T4=161；服务商 T2/T3=26；平台 ROLE_SUPER_ADMIN=80；旧 ROLE_ADMIN 仍 222 但已休眠）
SELECT role_code, COUNT(*) menu_cnt
FROM sys_role_menu
WHERE role_code IN ('ROLE_ADMIN_T1','ROLE_ADMIN_T2','ROLE_ADMIN_T3','ROLE_ADMIN_T4','ROLE_SUPER_ADMIN','ROLE_ADMIN')
GROUP BY role_code ORDER BY role_code;

-- 3) 越权检查（期望全部为 0）：货主/服务商角色不应含 菜单权限100800、任何平台顶级(170400/170500/170600/170700)
SELECT role_code, COUNT(*) leak_cnt
FROM sys_role_menu
WHERE role_code IN ('ROLE_ADMIN_T1','ROLE_ADMIN_T2','ROLE_ADMIN_T3','ROLE_ADMIN_T4')
  AND menu_id IN (100800,170400,170500,170600,170700)
GROUP BY role_code;
-- 上面无结果(0 行) = 无泄漏；若有行则说明越权菜单被授出

-- 3b) 货主角色应仍含 角色管理100200（货主可管理自己的角色）；服务商应含 100200+100400
SELECT 'T1含角色管理100200' k, COUNT(*) v FROM sys_role_menu WHERE role_code='ROLE_ADMIN_T1' AND menu_id=100200
UNION ALL SELECT 'T2含角色100200', COUNT(*) FROM sys_role_menu WHERE role_code='ROLE_ADMIN_T2' AND menu_id=100200
UNION ALL SELECT 'T2含配置100400', COUNT(*) FROM sys_role_menu WHERE role_code='ROLE_ADMIN_T2' AND menu_id=100400
UNION ALL SELECT 'T1不含菜单权限100800', COUNT(*) FROM sys_role_menu WHERE role_code='ROLE_ADMIN_T1' AND menu_id=100800;
-- 期望：前三行 v=1，最后一行 v=0

-- 4) 用户改派（期望：T1=user1,18,19,20,21,22,23；T4=32；T2=29；T3=31；ROLE_ADMIN=0 个用户）
SELECT role_code, GROUP_CONCAT(user_id ORDER BY user_id) users, COUNT(*) cnt
FROM sys_user_role
WHERE role_code IN ('ROLE_ADMIN','ROLE_ADMIN_T1','ROLE_ADMIN_T2','ROLE_ADMIN_T3','ROLE_ADMIN_T4','ROLE_SUPER_ADMIN')
GROUP BY role_code ORDER BY role_code;

-- 5) 平台超管 user30 角色（期望仅 ROLE_SUPER_ADMIN）；孤儿 27/28 应无任何 ROLE_ADMIN
SELECT user_id, GROUP_CONCAT(role_code) roles FROM sys_user_role WHERE user_id IN (30,27,28) GROUP BY user_id;
