-- =====================================================================
-- B1 · 双层租户定性 + 平台超管账号
-- 决策：现有用户=货主(ERP_USER)；新建海外仓平台(WMS_OPERATOR)；平台屏蔽货主 OMS。
-- 执行：mysql --default-character-set=utf8mb4 -h127.0.0.1 -P3307 -uroot -p123456 erp < erp-backend/sql/migration/V4__tenant_role_foundation.sql
-- 一次性迁移，勿重复执行。
-- =====================================================================

-- 1) 清理 A2/A3 隔离测试遗留探针（原假 tenant_id=2，现 id=2 将被真平台租户占用，必须先删）
DELETE FROM sku        WHERE sku_code         LIKE '%-T2VERIFY';
DELETE FROM erp_order  WHERE platform_order_id LIKE '%-T2VERIFY';

-- 2) 新建第一层：海外仓平台（WMS_OPERATOR）。当前 sys_tenant 仅 id=1，本条得 id=2。
INSERT INTO sys_tenant (tenant_code, tenant_name, tenant_type, parent_wms_tenant_id, status, remark)
VALUES ('platform', '海外仓平台', 'WMS_OPERATOR', NULL, 1, 'B1 新建：第一层服务商，管物理仓储+开货主+计费');
SET @ops_tenant_id = LAST_INSERT_ID();

-- 3) tenant1 重定性为第二层货主，挂到平台下（历史 OMS 数据原地归属此货主）
UPDATE sys_tenant
SET tenant_type          = 'ERP_USER',
    tenant_code          = 'default_erp',
    tenant_name          = '自营货主（历史数据）',
    parent_wms_tenant_id = @ops_tenant_id,
    remark               = 'B1：原 default_wms 重定性为货主'
WHERE id = 1;

-- 4) 新建平台超管账号 platform_admin，复制 admin 的 bcrypt 密码（登录密码 = 你的 admin 密码）
INSERT INTO sys_user (username, nickname, password, salt, status, type, organization_id, deleted)
SELECT 'platform_admin', '平台超级管理员', password, salt, 1, 1, organization_id, 0
FROM sys_user WHERE username = 'admin' AND deleted = 0 LIMIT 1;
SET @ops_user_id = LAST_INSERT_ID();

-- 5) 平台超管绑定到平台租户，标记租户管理员
INSERT INTO sys_user_tenant (user_id, tenant_id, is_admin) VALUES (@ops_user_id, @ops_tenant_id, 1);

-- 6) 平台超管授 ROLE_ADMIN（沿用现有全功能角色；ERP/WMS 菜单细分留 B2）
INSERT INTO sys_user_role (user_id, role_code) VALUES (@ops_user_id, 'ROLE_ADMIN');

-- 7) 现有 admin(user_id=1) 标记为货主管理员
UPDATE sys_user_tenant SET is_admin = 1 WHERE user_id = 1 AND tenant_id = 1;

-- ===== 验证 =====
SELECT id, tenant_code, tenant_name, tenant_type, parent_wms_tenant_id FROM sys_tenant ORDER BY id;
SELECT user_id, username, nickname FROM sys_user WHERE username = 'platform_admin';
SELECT CONCAT('platform_oms_sku=', COUNT(*)) AS r FROM sku       WHERE tenant_id = @ops_tenant_id; -- 期望 0
SELECT CONCAT('platform_oms_ord=', COUNT(*)) AS r FROM erp_order WHERE tenant_id = @ops_tenant_id; -- 期望 0
