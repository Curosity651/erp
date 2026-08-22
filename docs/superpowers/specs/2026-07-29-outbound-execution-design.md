# Overseas Warehouse Outbound Execution Design

## Goal

Keep the current sales outbound, reservation, package, platform-document, billing, and shipment structure while making warehouse execution accurate enough for normal multi-owner operations.

## Supported Picking Modes

- `SINGLE`: one sales outbound order per task.
- `WAVE`: multiple compatible outbound orders grouped by warehouse, owner, and source type.
- `PALLET_DIRECT`: an outbound demand that consumes a complete eligible pallet. The pallet is scanned once and its task lines are completed together.

Each task may contain `WHOLE_PALLET` and `PIECE` line strategies. Zone relay picking is intentionally deferred.

## State Flow

Sales outbound:

`DRAFT -> CONFIRMED -> PICKING -> PICKED -> PACKED -> SHIPPED`

Picking task:

`PICKING -> SORTING -> COMPLETED`

Exception flow:

`PICKING -> EXCEPTION -> PICKING`

If picked goods must be put back:

`EXCEPTION -> RETURNING -> CANCELLED`

The outbound order becomes `BACKORDER` only after every already-picked item has been scanned back to its original pallet/slot.

## Scan Rules

- Whole-pallet lines require the expected pallet code. Quantity is the complete remaining pallet allocation and cannot be manually changed.
- Piece-pick lines require the expected full slot or pallet first, followed by the internal warehouse SKU or product barcode.
- A pallet code can never substitute for a product scan on a piece-pick line.
- Manual registration is available only to users with `wms:outbound-exec:supervise`; a reason and remark are mandatory.
- Every scan event stores the actual logged-in operator, separately from the assigned picker or entered packer.

## Internal SKU

Warehouse pages and documents display `warehouseSkuCode`. Database inventory and outbound items continue storing the original ERP SKU. Scan resolution accepts internal SKU, original SKU, and registered barcode.

## Sorting And Packing

- A wave with more than one platform package receives one sort-slot code per platform order package.
- Sorting scans are recorded against the selected package and cannot exceed its expected quantity.
- Packing repeats product verification per platform package.
- Warehouse-print mode requires generated platform labels; owner-provided mode requires explicit document confirmation.
- Required Ozon handover documents must be ready or externally confirmed before shipment.

## Shipment

Shipment requires every package packed, required documents ready, positive weight, a logistics channel, and consistent reservations. The existing `PACKED -> SHIPPED` CAS remains the idempotency boundary. Inventory deduction, reservation release, pallet refresh, billing, and ERP order completion stay in one transaction.

## Operational UI

The current list and drawer layout is retained. The picking drawer becomes a continuous scan work surface:

- task summary and progress;
- current location, full slot, pallet, internal SKU, and remaining quantity;
- scan controls;
- naturally sorted line table;
- exception and return-to-stock controls;
- package sort slots after physical picking.

The pack/ship page remains separate and operates per platform package.

## Compatibility

Existing completed tasks and packages remain readable. New fields use safe defaults. No historical inventory quantity or outbound allocation is rewritten.

