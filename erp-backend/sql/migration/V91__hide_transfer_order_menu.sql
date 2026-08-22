-- Cross-warehouse transfer management is reserved for later redesign.
-- Keep routes, permissions and historical data intact, but hide the menu entry.
UPDATE sys_menu
SET hidden = 1
WHERE id = 160700
  AND deleted = 0;
