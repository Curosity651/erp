-- 海外仓平台(OVERSEAS_PLATFORM)系统管理新增「项目组管理」(100900)与「岗位管理」(101000)。
-- 给平台管理员角色 ROLE_OVERSEAS_PLATFORM_ADMIN 补授两菜单容器及其全部按钮权限(查询/新增/修改/删除)。
-- 与 menu-filter.ts(PLATFORM_SYSTEM_MODULE_IDS)、TenantRoleMapper.xml(selectAllowedMenuIds/OVERSEAS_PLATFORM)保持一致。
-- 幂等:NOT EXISTS 防重复插入。
INSERT INTO sys_role_menu (role_code, menu_id)
SELECT 'ROLE_OVERSEAS_PLATFORM_ADMIN', m.id
FROM sys_menu m
WHERE m.id IN (100900,100901,100902,100903,100904,101000,101001,101002,101003,101004)
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm
    WHERE rm.role_code = 'ROLE_OVERSEAS_PLATFORM_ADMIN' AND rm.menu_id = m.id
  );
