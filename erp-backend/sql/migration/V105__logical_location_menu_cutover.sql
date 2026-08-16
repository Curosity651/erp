-- Replace the legacy pallet management entry with the logical location inventory view.
-- Keep the menu id and role bindings so existing deployments retain their permissions.
UPDATE sys_menu
SET title = '库位库存',
    icon = 'database',
    path = 'location-inventory',
    uri = 'platform/location-inventory/index',
    update_time = NOW()
WHERE id = 170512
  AND deleted = 0;
