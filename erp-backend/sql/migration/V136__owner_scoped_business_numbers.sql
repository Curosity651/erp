-- Owner-managed business numbers are unique within an ERP owner, not globally.

ALTER TABLE wms_logistics_provider
    DROP INDEX ux_provider_code,
    ADD UNIQUE KEY ux_owner_provider_code (erp_tenant_id, provider_code, deleted);

ALTER TABLE wms_purchase_order
    DROP INDEX ux_order_no,
    ADD UNIQUE KEY ux_owner_order_no (erp_tenant_id, order_no, deleted);

ALTER TABLE wms_shipping_order
    DROP INDEX uk_shipping_no,
    ADD UNIQUE KEY uk_owner_shipping_no (erp_tenant_id, shipping_no, deleted);
