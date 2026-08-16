SET FOREIGN_KEY_CHECKS = 0;
START TRANSACTION;

DELETE FROM wms_fulfillment_pick_task_line;
DELETE FROM wms_fulfillment_pick_task_order;
DELETE FROM wms_fulfillment_pick_task;
DELETE FROM wms_fulfillment_platform_action;
DELETE FROM wms_inventory_reservation;
DELETE FROM wms_fulfillment_item;
DELETE FROM wms_fulfillment_order;
DELETE FROM wms_location_inventory;
DELETE FROM wms_putaway_receipt_line;
DELETE FROM wms_location_transfer_item;
DELETE FROM wms_location_transfer_order;
DELETE FROM wms_outbound_scan_event;
DELETE FROM wms_outbound_pick_task_line;
DELETE FROM wms_outbound_pick_task_order;
DELETE FROM wms_outbound_pick_task;
DELETE FROM wms_outbound_pick_allocation;
DELETE FROM wms_sales_outbound_package;
DELETE FROM wms_sales_outbound_order_item;
DELETE FROM wms_sales_outbound_order;
DELETE FROM wms_sort_slot;
DELETE FROM wms_pallet_operation_log;
DELETE FROM wms_physical_inventory;
DELETE FROM wms_pallet;
DELETE FROM wms_adjustment_order_item;
DELETE FROM wms_adjustment_order;
DELETE FROM wms_stocktake_location_task;
DELETE FROM wms_stocktake_order_item;
DELETE FROM wms_stocktake_order;
DELETE FROM wms_return_qc_item;
DELETE FROM wms_return_inbound_order;
DELETE FROM wms_transfer_order_item;
DELETE FROM wms_transfer_order;
DELETE FROM wms_purchase_inbound_order_item;
DELETE FROM wms_purchase_inbound_order;
DELETE FROM wms_shipping_order_adjust;
DELETE FROM wms_shipping_order_item;
DELETE FROM wms_shipping_order;
DELETE FROM wms_purchase_order_qc_item;
DELETE FROM wms_purchase_order_file;
DELETE FROM wms_purchase_order_item;
DELETE FROM wms_purchase_order;
DELETE FROM wms_stock_posting_item;
DELETE FROM wms_stock_posting;
DELETE FROM wms_stock_flow;
DELETE FROM wms_region_inventory;
DELETE FROM wms_inventory;
DELETE FROM wms_fbo_inventory_snapshot;
DELETE FROM wms_fbo_sync_log;
DELETE FROM wms_billing_record;
DELETE FROM wms_client_billing_record;
DELETE FROM wms_monthly_bill;
DELETE FROM wms_rack_assignment;
DELETE FROM wms_location_slot;

UPDATE erp_order
SET fulfillment_order_id = NULL,
    wms_warehouse_id = NULL,
    warehouse_fulfillment_status = NULL,
    locked = 0,
    outbound_status = 'NONE';

COMMIT;
SET FOREIGN_KEY_CHECKS = 1;
