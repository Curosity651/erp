-- Contract-created rack assignments must belong to one contract rack only.
ALTER TABLE wms_contract_rack
    ADD UNIQUE KEY uk_contract_rack_assignment (rack_assignment_id);

-- The warehouse deposit is fixed per service contract, independent of rack count.
UPDATE wms_service_contract
SET warehouse_deposit = 60000.00
WHERE warehouse_deposit <> 60000.00;
