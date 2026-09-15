-- =====================================================================
-- 修复：货主/服务商租户管理员打开「角色管理→授权」时 grant-list 返回 500。
--   根因：RBAC 阶段2(V19/V20) 给货主/服务商角色去掉了「菜单权限」100800 子树，
--         而 BallCat 原生 GET /system/menu/grant-list 的方法级 @Authorize 要求
--         hasPermission('system:menu:read')（= 菜单权限查询 100801）→ 无此权限抛
--         AccessDenied → 全局处理器包成 500「系统异常，请联系管理员」。
--   修复：给「有角色管理(100200) 却无菜单权限查询(100801)」的租户管理员角色补授
--         100800(容器,授权树结构完整) + 100801(system:menu:read 读权限)。
--         **只授读**，不授新增/修改/删除(100802-4)——sys_menu 是全局共享表，
--         货主/服务商不得改动。菜单权限「页面」仍被前端 menu-filter 隐藏(不进侧边栏)。
--   配套：TenantRoleMapper.xml 已把 100800/100801 加入 ERP_USER/WMS_OPERATOR 允许集，
--         使新开租户自动带上、且授权树按身份过滤(MenuGrantScopeAdvice)时保留该节点。
-- 执行：mysql --default-character-set=utf8mb4 -h127.0.0.1 -P3307 -uroot -p123456 erp < erp-backend/sql/migration/V21__grant_menu_read_for_role_authorize.sql
-- 一次性迁移，勿重复执行（内含 NOT EXISTS 幂等保护）。
-- =====================================================================

-- 100801：system:menu:read（授权接口实际要求的权限）
INSERT INTO sys_role_menu (role_code, menu_id)
SELECT DISTINCT a.role_code, 100801
FROM sys_role_menu a
WHERE a.menu_id = 100200
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu b WHERE b.role_code = a.role_code AND b.menu_id = 100801);

-- 100800：菜单权限容器（仅为授权树父节点结构完整，无权限码）
INSERT INTO sys_role_menu (role_code, menu_id)
SELECT DISTINCT a.role_code, 100800
FROM sys_role_menu a
WHERE a.menu_id = 100801
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu b WHERE b.role_code = a.role_code AND b.menu_id = 100800);
