ALTER TABLE shop
  ADD COLUMN default_wms_warehouse_id bigint NULL COMMENT 'default internal WMS warehouse, never a platform warehouse';

ALTER TABLE erp_order
  ADD COLUMN fulfillment_order_id bigint NULL COMMENT 'new one-order-one-package fulfillment id',
  ADD COLUMN wms_warehouse_id bigint NULL COMMENT 'internal WMS warehouse id',
  ADD COLUMN warehouse_fulfillment_status varchar(32) NULL COMMENT 'warehouse internal status';

CREATE INDEX idx_erp_order_fulfillment ON erp_order (fulfillment_order_id);
