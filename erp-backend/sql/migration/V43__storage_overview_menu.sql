-- 服务商运营「货架库存(只读)」→ 重命名为「仓储概览」，并新增只读权限码授予服务商。
-- 页面组件由前端 COMPONENT_OVERRIDES['rack-inventory'] 接管（菜单 path 不变），故此处不改 uri。
UPDATE sys_menu SET title = '仓储概览' WHERE id = 180400;

-- 只读权限按钮：供后端 @PreAuthorize('wms:rack-inventory:read')。仅挂在服务商专属子树(180000)下，
-- 货主/平台的 selectAllowedMenuIds 不含 180000 → 不会拿到此码，天然不越权。
INSERT INTO sys_menu (id, parent_id, title, permission, type, sort, hidden, deleted, create_time)
SELECT 180401, 180400, '仓储概览查询', 'wms:rack-inventory:read', 2, 1, 0, 0, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu m WHERE m.id = 180401);

-- 授予现有 WMS 服务商管理员角色（未来新开通的服务商由 selectAllowedMenuIds 递归 180000 子树自动包含 180401）。
INSERT INTO sys_role_menu (role_code, menu_id)
SELECT r.code, 180401
FROM sys_role r
JOIN sys_tenant t ON t.id = r.tenant_id
WHERE t.tenant_type = 'WMS_OPERATOR'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_code = r.code AND rm.menu_id = 180401
  );
