-- Normalize configurable warehouse defaults and UTF-8 labels.
-- This is idempotent and also repairs installations where a console import lost Chinese text.

UPDATE wms_warehouse
SET pallet_levels = 6,
    pallet_positions_per_level = 3,
    allow_cross_owner_mix = 0
WHERE warehouse_type = 'OWN' AND deleted = 0;

UPDATE wms_fee_rate_card
SET fee_name = CASE fee_code
        WHEN 'INBOUND_CBM' THEN '入库操作费'
        WHEN 'AFTER_HOURS_SURCHARGE' THEN '客户原因加班附加费'
        WHEN 'OUTBOUND_FULL_PALLET' THEN '整托出库费'
        WHEN 'OUTBOUND_LARGE_BOX' THEN '大箱出库费'
        WHEN 'OUTBOUND_SMALL_ITEM' THEN '小件散货出库费'
        ELSE fee_name
    END,
    remark = CASE fee_code
        WHEN 'INBOUND_CBM' THEN '收货、理货、上架'
        WHEN 'AFTER_HOURS_SURCHARGE' THEN '基础操作费的50%'
        WHEN 'OUTBOUND_FULL_PALLET' THEN '完整LPN离库'
        WHEN 'OUTBOUND_LARGE_BOX' THEN '一箱一计费件'
        WHEN 'OUTBOUND_SMALL_ITEM' THEN '按件计费'
        ELSE remark
    END
WHERE wms_tenant_id = 0
  AND fee_code IN (
      'INBOUND_CBM',
      'AFTER_HOURS_SURCHARGE',
      'OUTBOUND_FULL_PALLET',
      'OUTBOUND_LARGE_BOX',
      'OUTBOUND_SMALL_ITEM'
  );

UPDATE sys_menu
SET title = '合同资金'
WHERE id = 170602;
