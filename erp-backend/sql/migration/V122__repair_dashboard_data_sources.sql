-- Activate the real overseas-platform dashboard. The statistical mapper changes in this
-- release use the current fulfillment and physical-inventory tables instead of retired data.
UPDATE sys_menu
SET uri = 'dashboard/platform/index',
    update_time = NOW()
WHERE id = 900300
  AND uri <> 'dashboard/platform/index';
