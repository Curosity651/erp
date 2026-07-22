-- Marketplace orders are demand records and must not produce WMS stock flows.
-- V67 removed the legacy postings and reset the region projection, but existing
-- stock-flow rows are independent audit rows and therefore require cleanup too.

DELETE FROM wms_stock_flow
WHERE source_type = 'ORDER'
  AND posting_type IN ('SALES_RESERVE', 'SALES_RELEASE');
