-- Location management read paths.

ALTER TABLE wms_physical_inventory
    ADD KEY idx_phys_wh_location (warehouse_id, location_code);

ALTER TABLE wms_location
    ADD KEY idx_location_wh_virtual_rack_column
        (warehouse_id, is_virtual, rack_no, column_no);
