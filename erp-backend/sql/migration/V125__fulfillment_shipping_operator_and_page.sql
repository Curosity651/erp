ALTER TABLE wms_fulfillment_order
  ADD COLUMN shipped_by BIGINT NULL COMMENT 'User who completed shipment' AFTER shipped_time,
  ADD INDEX idx_fulfillment_shipping_page
    (fulfillment_status, warehouse_id, erp_tenant_id, create_time),
  ADD INDEX idx_fulfillment_shipped_by (shipped_by);
