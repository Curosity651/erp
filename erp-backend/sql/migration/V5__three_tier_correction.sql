-- =====================================================================
-- B 三层补正 · 补平台超管层 + 正名 WMS 服务商
-- 三层：平台超管(ROLE_SUPER_ADMIN,非租户,看全部) > WMS服务商(WMS_OPERATOR) > 货主(ERP_USER)
-- 现有 8 个账号=货主(不动)；新建 super_admin；platform_admin 正名为服务商管理员 wms_admin。
-- super_admin/wms_admin 密码均复制 admin（= 你的 admin 登录密码）。
-- 执行：mysql --default-character-set=utf8mb4 -h127.0.0.1 -P3307 -uroot -p123456 erp < erp-backend/sql/migration/V5__three_tier_correction.sql
-- 一次性迁移，勿重复执行。
-- =====================================================================

-- 1) 新增平台超管角色
INSERT INTO sys_role (name, code, type, scope_type, remarks)
SELECT '平台超级管理员', 'ROLE_SUPER_ADMIN', 1, 1, 'B三层补正：平台超管，无租户过滤，看全部'
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE code = 'ROLE_SUPER_ADMIN' AND deleted = 0);

-- 2) 新建平台超管账号 super_admin（密码同 admin），不绑任何租户
INSERT INTO sys_user (username, nickname, password, salt, status, type, organization_id, deleted)
SELECT 'super_admin', '平台超级管理员', password, salt, 1, 1, organization_id, 0
FROM sys_user WHERE username = 'admin' AND deleted = 0 LIMIT 1;
SET @sa_id = LAST_INSERT_ID();

-- 3) 平台超管授 ROLE_SUPER_ADMIN（身份）+ ROLE_ADMIN（全菜单）
INSERT INTO sys_user_role (user_id, role_code) VALUES (@sa_id, 'ROLE_SUPER_ADMIN'), (@sa_id, 'ROLE_ADMIN');

-- 4) tenant2 正名为 WMS 服务商（第一层 3PL）
UPDATE sys_tenant
SET tenant_code = 'wms_provider',
    tenant_name = 'WMS服务商（海外仓）',
    remark      = 'B三层补正：第一层 3PL 服务商'
WHERE id = 2 AND tenant_type = 'WMS_OPERATOR';

-- 5) platform_admin → wms_admin（服务商管理员），密码同 admin；租户绑定(tenant2,is_admin=1)不变
UPDATE sys_user u
JOIN sys_user a ON a.username = 'admin' AND a.deleted = 0
SET u.username = 'wms_admin', u.nickname = 'WMS服务商管理员', u.password = a.password, u.salt = a.salt
WHERE u.username = 'platform_admin' AND u.deleted = 0;

-- ===== 验证 =====
SELECT user_id, username, nickname FROM sys_user WHERE username IN ('super_admin', 'wms_admin', 'admin') ORDER BY user_id;
SELECT ur.user_id, u.username, ur.role_code
FROM sys_user_role ur JOIN sys_user u ON u.user_id = ur.user_id
WHERE u.username IN ('super_admin', 'wms_admin') ORDER BY ur.user_id;
SELECT id, tenant_code, tenant_name, tenant_type, parent_wms_tenant_id FROM sys_tenant ORDER BY id;
SELECT u.username, ut.tenant_id, ut.is_admin
FROM sys_user u LEFT JOIN sys_user_tenant ut ON ut.user_id = u.user_id
WHERE u.username IN ('super_admin', 'wms_admin') ORDER BY u.username;
