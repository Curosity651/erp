-- Marketplace order synchronization no longer owns WMS inventory reservations.
-- Remove legacy ORDER postings and reset their region-level reservation projection.

DELETE spi
FROM wms_stock_posting_item spi
INNER JOIN wms_stock_posting sp ON sp.id = spi.posting_id
WHERE sp.source_type = 'ORDER'
  AND sp.posting_type IN ('SALES_RESERVE', 'SALES_RELEASE');

DELETE FROM wms_stock_posting
WHERE source_type = 'ORDER'
  AND posting_type IN ('SALES_RESERVE', 'SALES_RELEASE');

UPDATE wms_region_inventory
SET reserved_quantity = 0,
    update_time = CURRENT_TIMESTAMP
WHERE reserved_quantity <> 0;
