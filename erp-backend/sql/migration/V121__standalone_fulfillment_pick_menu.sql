-- Add a standalone picking-task page between order shelving and outbound signout.
INSERT INTO sys_menu (
    id, parent_id, title, icon, permission, path, target_type, uri, sort,
    keep_alive, hidden, type, remarks, deleted, create_by, update_by,
    create_time, update_time
)
SELECT
    170507, parent_id, CONVERT(0xE68BA3E8B4A7E4BBBBE58AA1 USING utf8mb4),
    icon, permission, 'fulfillment-picking', target_type,
    'platform/fulfillment-picking/FulfillmentPickingPage', 4,
    keep_alive, 0, type, remarks, 0, create_by, update_by, NOW(), NOW()
FROM sys_menu
WHERE id = 170503 AND deleted = 0
ON DUPLICATE KEY UPDATE
    parent_id = VALUES(parent_id), title = VALUES(title), path = VALUES(path),
    uri = VALUES(uri), sort = VALUES(sort), hidden = 0, deleted = 0,
    update_time = NOW();

UPDATE sys_menu SET sort = 5, update_time = NOW()
WHERE id = 170503 AND deleted = 0;

INSERT INTO sys_role_menu (role_code, menu_id)
SELECT role_code, 170507
FROM sys_role_menu
WHERE menu_id = 170503
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_menu existing
      WHERE existing.role_code = sys_role_menu.role_code
        AND existing.menu_id = 170507
  );
