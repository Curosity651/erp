-- Prefix inbound numbers with the owner name snapshot so printed numbers are
-- globally recognizable, and keep numbers permanently unique for auditability.
ALTER TABLE wms_purchase_inbound_order
    MODIFY COLUMN inbound_no VARCHAR(100) NOT NULL COMMENT 'Owner-prefixed inbound number';

UPDATE wms_purchase_inbound_order pio
INNER JOIN sys_tenant owner ON owner.id = pio.erp_tenant_id
SET pio.inbound_no = CONCAT(
        UPPER(REPLACE(TRIM(owner.tenant_name), ' ', '_')),
        '-',
        pio.inbound_no
    );

ALTER TABLE wms_purchase_inbound_order
    DROP INDEX uk_inbound_no,
    ADD UNIQUE KEY uk_inbound_no (inbound_no);
