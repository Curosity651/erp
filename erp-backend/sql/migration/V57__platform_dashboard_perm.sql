-- E28：平台数据分析看板(900300)此前 controller 无 @PreAuthorize，仅 service 断言 OVERSEAS_PLATFORM；
-- 跨身份已被断言挡住，但平台内无该菜单的子账号仍可直调。补按钮型权限码 platform:dashboard:read，
-- 配合 controller 的 @PreAuthorize('platform:dashboard:read')，与兄弟看板(180501 operator-dashboard)口径一致。
-- 幂等：sys_menu 固定 ID + ON DUPLICATE KEY UPDATE；sys_role_menu 靠唯一键(role_code,menu_id) + INSERT IGNORE。

-- 1) 在 平台数据分析 900300 下建按钮型权限码(type=2)
INSERT INTO sys_menu (id, parent_id, title, permission, path, target_type, uri, sort, keep_alive, hidden, type, deleted)
VALUES
  (900301, 900300, '查看', 'platform:dashboard:read', NULL, 1, '', 1, 0, 0, 2, 0)
ON DUPLICATE KEY UPDATE
  parent_id=VALUES(parent_id), title=VALUES(title), permission=VALUES(permission),
  path=VALUES(path), target_type=VALUES(target_type), uri=VALUES(uri), type=VALUES(type), deleted=0;

-- 2) 授权：凡当前拥有 900300 看板菜单的角色(平台超管等)一并获得该读权限码，保持与看板可见性一致
INSERT IGNORE INTO sys_role_menu (role_code, menu_id)
SELECT DISTINCT role_code, 900301 FROM sys_role_menu WHERE menu_id = 900300;
