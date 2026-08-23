-- Split order shelving from the fulfillment workbench. Use UTF-8 byte
-- literals so Windows mysql clients cannot corrupt the Chinese title.
INSERT INTO sys_menu (
    id, parent_id, title, icon, permission, path, target_type, uri, sort,
    keep_alive, hidden, type, remarks, deleted, create_by, update_by,
    create_time, update_time
)
SELECT
    170506, parent_id, CONVERT(0xE8AEA2E58D95E4B88BE69EB6 USING utf8mb4),
    icon, permission, 'fulfillment-shelf', target_type,
    'platform/fulfillment-shelf/FulfillmentShelfPage', 3,
    keep_alive, 0, type, remarks, 0, create_by, update_by, NOW(), NOW()
FROM sys_menu
WHERE id = 170503 AND deleted = 0
ON DUPLICATE KEY UPDATE
    parent_id = VALUES(parent_id),
    title = VALUES(title),
    permission = VALUES(permission),
    path = VALUES(path),
    uri = VALUES(uri),
    sort = VALUES(sort),
    hidden = 0,
    deleted = 0,
    update_time = NOW();

UPDATE sys_menu
SET sort = 4, update_time = NOW()
WHERE id = 170503 AND deleted = 0;

-- Give every role that can access the original workbench access to the new
-- order shelving page as well.
INSERT INTO sys_role_menu (role_code, menu_id)
SELECT role_code, 170506
FROM sys_role_menu
WHERE menu_id = 170503
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_menu existing
      WHERE existing.role_code = sys_role_menu.role_code
        AND existing.menu_id = 170506
  );
