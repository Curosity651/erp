# Package-Centric Packing And Shipping Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the sales-outbound-order packing workspace with a platform-order/package workspace and support independent package shipping with automatic but editable channels.

**Architecture:** Keep sales outbound orders as aggregate demand records and promote `wms_sales_outbound_package` to the packing and shipping execution unit. Track consumed outbound allocations so package shipping deducts only its own SKU quantities, while the existing order-level workflow remains available for custom outbound orders.

**Tech Stack:** Java 8, Spring Boot, MyBatis-Plus, MySQL 8, Vue 3, TypeScript, Ant Design Vue, JUnit 5, Mockito.

## Global Constraints

- One platform order equals one physical package, one shipping label and one packing-list row.
- Sales outbound orders remain source and aggregate records, not primary packing-page rows.
- Shipping channels are automatically selected and may be changed by the operator.
- Slot sorting remains optional.
- Custom outbound orders retain their existing order-level flow.

---

### Task 1: Persist Package Shipping State

**Files:**
- Create: `erp-backend/sql/migration/V100__package_level_shipping.sql`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/entity/WmsSalesOutboundPackage.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/entity/WmsOutboundPickAllocation.java`

**Interfaces:**
- Produces package fields `shipStatus`, `channelCode`, `channelName`, `trackingNo`, `weight`, `shippedBy`, `shippedByName`, `shippedTime`.
- Produces allocation field `shippedQty`.

- [ ] Add a migration with non-null defaults and shipped-order backfill.
- [ ] Add matching entity properties.
- [ ] Apply the migration to the local Docker MySQL database.

### Task 2: Add Package Query Contract

**Files:**
- Create: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/qo/PackShipPackageQO.java`
- Create: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/vo/PackShipPackagePageVO.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/vo/OutboundPackageVO.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/mapper/OutboundShippingMapper.java`
- Modify: `erp-backend/admin/src/main/resources/mapper/wms/OutboundShippingMapper.xml`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/service/OutboundShippingService.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/controller/OutboundShippingController.java`

**Interfaces:**
- Produces `GET /wms/outbound-shipping/package-page`.
- Produces `GET /wms/outbound-shipping/package/locate?scanCode=...`.

- [ ] Write a failing service test proving a platform-order scan locates one package.
- [ ] Run the focused test and confirm it fails because the package query is absent.
- [ ] Add mapper queries, VO mapping and controller endpoints.
- [ ] Run the focused test and confirm it passes.

### Task 3: Ship One Package Safely

**Files:**
- Create: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/dto/ShipPackageDTO.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/service/OutboundShippingService.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/service/SalesOutboundPackageService.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/controller/OutboundShippingController.java`
- Test: `erp-backend/admin/src/test/java/com/erp/admin/wms/OutboundShippingServiceTest.java`

**Interfaces:**
- Produces `POST /wms/outbound-shipping/ship-package`.
- Consumes package ID, channel, tracking number and weight.

- [ ] Write failing tests for per-package allocation consumption, different channels and last-package order completion.
- [ ] Run tests and confirm they fail for missing package shipping behavior.
- [ ] Lock package and order, consume FIFO allocation by package SKU, and persist package shipping fields.
- [ ] Advance the master order only when every package is shipped.
- [ ] Run focused tests and confirm they pass.

### Task 4: Build The Package-Centric Frontend

**Files:**
- Modify: `erp-frontend/src/api/wms/outbound-shipping/types.ts`
- Modify: `erp-frontend/src/api/wms/outbound-shipping/index.ts`
- Modify: `erp-frontend/src/views/platform/outbound-ops/PackShipPage.vue`
- Modify: `erp-frontend/src/views/platform/outbound-ops/PackModal.vue`
- Create: `erp-frontend/src/views/platform/outbound-ops/PackageShipModal.vue`

**Interfaces:**
- Consumes package page, locate and ship-package endpoints.
- Keeps the existing `PackModal` package verification operations.

- [ ] Add TypeScript contracts and API functions.
- [ ] Replace the main order table with one package per row.
- [ ] Add a global scanner that opens the located package.
- [ ] Add default channel selection with manual override.
- [ ] Run `pnpm type-check` and resolve all failures.

### Task 5: Regression And Runtime Verification

**Files:**
- Test: focused backend and frontend files above.

**Interfaces:**
- Verifies the complete package-level flow against the running local system.

- [ ] Run focused JUnit tests.
- [ ] Run frontend type checking and production build.
- [ ] Package and restart the Java backend.
- [ ] Use a multi-package test outbound to ship packages with different channels.
- [ ] Confirm only the last package completes the sales outbound order.
- [ ] Verify the browser page lists individual platform orders.

