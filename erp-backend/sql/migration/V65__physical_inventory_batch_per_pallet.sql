-- A physical batch is stored on a pallet after the three-level slot migration.
-- The same inbound item may be split across pallets on different levels of one base location.
ALTER TABLE wms_physical_inventory
    DROP INDEX uk_batch,
    ADD UNIQUE KEY uk_batch (
        wms_tenant_id,
        erp_tenant_id,
        warehouse_id,
        sku_code,
        inbound_item_id,
        location_code,
        pallet_id
    );
