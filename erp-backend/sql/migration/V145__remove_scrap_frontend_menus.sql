-- Remove the standalone scrap-order UI from both the overseas platform and ERP owner portals.
DELETE FROM sys_role_menu
WHERE menu_id IN (160900, 160901, 160902, 160903, 160904, 160905, 160920, 160921);

DELETE FROM sys_menu
WHERE id IN (160901, 160902, 160903, 160904, 160905, 160921, 160900, 160920);
