-- Pallet management describes the warehouse's physical storage structure,
-- so keep it with warehouse and location configuration rather than daily operations.
UPDATE sys_menu
SET parent_id = 170400,
    sort = 5
WHERE id = 170512;
