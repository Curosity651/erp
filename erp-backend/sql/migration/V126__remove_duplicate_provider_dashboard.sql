-- The provider dashboard and operator analytics menus rendered the same page.
-- Keep the analytics page and move its API permission under that page.
UPDATE sys_menu
SET parent_id = 900400,
    sort = 1,
    update_time = NOW()
WHERE id = 180501
  AND deleted = 0;

DELETE FROM sys_role_menu
WHERE menu_id = 180500;

UPDATE sys_menu
SET deleted = 180500,
    update_time = NOW()
WHERE id = 180500
  AND deleted = 0;
