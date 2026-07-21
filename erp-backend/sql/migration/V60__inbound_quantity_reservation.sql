-- Reserve shipping quantities when a purchase inbound order is submitted.
-- Draft inbound orders do not reserve quantities. Receiving converts the
-- reservation into received quantity and releases any short-received balance.

ALTER TABLE wms_shipping_order_item
    ADD COLUMN inbound_reserved_quantity INT NOT NULL DEFAULT 0
        COMMENT 'Quantity reserved by submitted inbound orders'
        AFTER received_quantity;

-- Backfill valid legacy SUBMITTED orders without creating inventory. If legacy
-- submissions already exceed the remaining quantity, cap the reservation at
-- the remaining quantity so the conflicting order is rejected at receiving.
UPDATE wms_shipping_order_item soi
LEFT JOIN (
    SELECT pioi.shipping_order_item_id,
           SUM(pioi.expected_quantity) AS submitted_quantity
    FROM wms_purchase_inbound_order pio
    INNER JOIN wms_purchase_inbound_order_item pioi
        ON pioi.inbound_order_id = pio.id
    WHERE pio.deleted = 0
      AND pio.order_status = 'SUBMITTED'
      AND pio.shipping_order_id IS NOT NULL
    GROUP BY pioi.shipping_order_item_id
) legacy ON legacy.shipping_order_item_id = soi.id
SET soi.inbound_reserved_quantity = LEAST(
    GREATEST(soi.quantity - IFNULL(soi.received_quantity, 0), 0),
    IFNULL(legacy.submitted_quantity, 0)
);

-- A legacy order that is still SUBMITTED after every referenced shipping line
-- has already been fully received is a stale duplicate and cannot be executed.
UPDATE wms_purchase_inbound_order pio
SET pio.order_status = 'CANCELLED',
    pio.update_time = NOW()
WHERE pio.deleted = 0
  AND pio.order_status = 'SUBMITTED'
  AND pio.shipping_order_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM wms_purchase_inbound_order_item pioi
      INNER JOIN wms_shipping_order_item soi
          ON soi.id = pioi.shipping_order_item_id
      WHERE pioi.inbound_order_id = pio.id
        AND soi.quantity > IFNULL(soi.received_quantity, 0)
  );
