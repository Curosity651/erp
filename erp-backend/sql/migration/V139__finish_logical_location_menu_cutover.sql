-- Complete the menu metadata cutover so active pages no longer depend on frontend route overrides.
UPDATE sys_menu
SET uri = 'platform/return-ops/ReturnQcPage'
WHERE id = 170505;

UPDATE sys_menu
SET title = '仓储概览', uri = 'wms/storage-overview/index'
WHERE id = 180400;

-- Custom return orders were removed from the current ERP inbound workflow.
-- Keep historical permissions and records readable, but do not expose a creation entry.
UPDATE sys_menu
SET hidden = 1
WHERE id = 162004;
