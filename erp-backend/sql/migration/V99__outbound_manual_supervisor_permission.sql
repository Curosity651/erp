INSERT INTO sys_menu (
    id, parent_id, title, icon, permission, path, target_type, uri,
    sort, keep_alive, hidden, type, remarks, deleted
)
SELECT
    170509, 170503, '出库手工登记', NULL, 'wms:outbound-exec:supervise', NULL, 1, '',
    99, 0, 0, 2, '允许绕过扫码进行手工拣货、分货和打包登记', 0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu
    WHERE permission = 'wms:outbound-exec:supervise' AND deleted = 0
);

INSERT INTO sys_role_menu (role_code, menu_id)
SELECT 'ROLE_OVERSEAS_PLATFORM_ADMIN', id
FROM sys_menu m
WHERE m.permission = 'wms:outbound-exec:supervise' AND m.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_menu
      WHERE role_code = 'ROLE_OVERSEAS_PLATFORM_ADMIN'
        AND menu_id = m.id
  );
