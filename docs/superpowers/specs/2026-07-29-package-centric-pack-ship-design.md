# Platform-Order Packing And Shipping Design

## Goal

Change the overseas warehouse packing and shipping workspace from a sales-outbound-order view to a platform-order/package view.

The first version uses this fixed relationship:

```text
one platform order = one physical package = one shipping label = one list row
```

Splitting one platform order into multiple physical packages is out of scope.

## Responsibility Boundaries

### Sales outbound order

The sales outbound order remains the warehouse demand and traceability container. It owns reservation, picking allocation, warehouse, owner and aggregate progress. Warehouse packers do not operate it directly from the packing page.

### Platform-order package

The package is the unit for:

- sorting position or tote
- item verification
- label printing and label scan verification
- packing completion
- shipping channel, tracking number and weight
- shipping operator and shipping time
- physical stock deduction

### Picking task

Picking remains organized by one or more sales outbound orders. Slot sorting stays optional. Both sorted packages and packages that skipped sorting enter the same package-level packing queue.

## Packing Workspace

The main table contains one row per platform order/package. It shows:

- platform order number
- platform and shop
- owner and WMS operator
- warehouse
- sorting position when used
- SKU kinds and quantity
- label state
- packing state
- shipping state
- shipping channel and tracking number
- operation

The sales outbound number is available only as a secondary source reference.

The page has one global scan input. It can locate a package using:

- sorting position
- platform order number
- package work label

The first version must at least support exact platform order number and exact sorting position.

## Packing Flow

1. Scan the sorting position or platform order number.
2. Open the matching package.
3. Scan and verify all package items.
4. Print or confirm the platform label.
5. Attach the label and scan its platform order number.
6. Complete the package.

A label scan that does not equal the current package platform order number is rejected.

## Shipping Flow

Each packed package can ship independently.

The shipping form automatically selects a default channel:

1. Use the package's previously saved channel when reopening.
2. Otherwise use the outbound logistics product code when it matches an enabled channel.
3. Otherwise use `AUTO`.

The operator may change the channel before shipping.

The package stores its own channel, tracking number, weight, operator and shipping time. Different packages from the same sales outbound order may use different values.

Shipping a package deducts only that package's SKU quantities. Existing outbound FIFO allocations gain a consumed quantity so multiple package shipments cannot deduct the same allocation twice.

When all packages are shipped:

- the sales outbound order becomes `SHIPPED`
- its linked ERP orders are complete
- order-level outbound handling billing is generated once

Before all packages ship, the sales outbound order remains `PACKED` and its aggregate view reports partial progress through its packages.

## Compatibility

Custom outbound orders keep the existing order-level packing and shipping flow.

Existing shipped sales outbound orders are backfilled so their packages are shipped. Existing packed packages remain ready to ship.

## Error Rules

- only packed packages can ship
- a shipped package cannot ship again
- package and outbound ownership must match
- package item quantities must be positive
- remaining FIFO allocation must cover every package SKU
- concurrent package shipping locks both package and outbound records
- the last package alone may advance the outbound order to `SHIPPED`

## Verification

- two packages in one outbound can save different channels
- shipping the first package deducts only its quantities
- the outbound remains `PACKED` after the first package
- shipping the last package changes the outbound to `SHIPPED`
- duplicate package shipping is rejected
- package list filters and scanner locate the expected row
- custom outbound behavior remains unchanged

