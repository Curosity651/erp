-- V23 废弃 sys_user_tenant（is_admin 已在 V22 并入 sys_user.is_admin；租户归属由 sys_user.tenant_id 承担）。
-- 执行：mysql --default-character-set=utf8mb4 -h127.0.0.1 -P3307 -uroot -p123456 erp < erp-backend/sql/migration/V23__drop_sys_user_tenant.sql
DROP TABLE IF EXISTS sys_user_tenant;
