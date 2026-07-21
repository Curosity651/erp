-- WMS 服务商(WMS_OPERATOR)系统管理不再保留「组织架构」(菜单100700),仅保留角色管理(100200)/系统用户(100100)。
-- 撤销所有 WMS_OPERATOR 租户下角色对 100700 的授权(含租户管理员角色及其自建角色)。
-- 与 menu-filter.ts(WMS_SYSTEM_MODULE_IDS)、TenantRoleMapper.xml(selectAllowedMenuIds/WMS_OPERATOR)保持一致。
DELETE rm FROM sys_role_menu rm
JOIN sys_role r ON r.code = rm.role_code
JOIN sys_tenant t ON t.id = r.tenant_id
WHERE t.tenant_type = 'WMS_OPERATOR'
  AND rm.menu_id = 100700;
