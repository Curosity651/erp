ALTER TABLE wms_pallet
    ADD COLUMN manual_full TINYINT NOT NULL DEFAULT 0
        COMMENT 'Operator explicitly marked pallet full' AFTER whole_pallet_eligible;

CREATE TABLE wms_putaway_receipt_line (
    id BIGINT NOT NULL AUTO_INCREMENT,
    inbound_order_id BIGINT NOT NULL,
    pallet_id BIGINT NULL,
    pallet_no VARCHAR(64) NULL,
    slot_code VARCHAR(64) NOT NULL,
    sku_code VARCHAR(100) NOT NULL,
    quality VARCHAR(20) NOT NULL DEFAULT 'GOOD',
    quantity INT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_putaway_receipt_line (
        inbound_order_id, pallet_id, sku_code, quality
    ),
    KEY idx_putaway_receipt_inbound (inbound_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='Immutable quantity snapshot for completed inbound putaway receipts';

-- Backfill a best-effort snapshot for existing completed orders.
INSERT INTO wms_putaway_receipt_line (
    inbound_order_id, pallet_id, pallet_no, slot_code,
    sku_code, quality, quantity, create_time
)
SELECT
    item.inbound_order_id,
    pallet.id,
    pallet.pallet_no,
    pallet.slot_code,
    inventory.sku_code,
    inventory.quality,
    SUM(inventory.quantity),
    COALESCE(inbound_order.putaway_time, inbound_order.update_time, CURRENT_TIMESTAMP)
FROM wms_physical_inventory inventory
INNER JOIN wms_pallet pallet ON pallet.id = inventory.pallet_id
INNER JOIN wms_purchase_inbound_order_item item ON item.id = inventory.inbound_item_id
INNER JOIN wms_purchase_inbound_order inbound_order ON inbound_order.id = item.inbound_order_id
WHERE inbound_order.order_status = 'COMPLETED'
  AND inventory.quantity > 0
GROUP BY
    item.inbound_order_id,
    pallet.id,
    pallet.pallet_no,
    pallet.slot_code,
    inventory.sku_code,
    inventory.quality,
    inbound_order.putaway_time,
    inbound_order.update_time;
