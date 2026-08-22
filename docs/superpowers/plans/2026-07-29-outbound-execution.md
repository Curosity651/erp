# Overseas Warehouse Outbound Execution Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement safe single-order, wave, and whole-pallet outbound execution through picking, sorting, packing, and shipment.

**Architecture:** Extend the existing outbound task and package services rather than replacing them. Keep inventory reservation as the source of allocation truth, use scan events for audit, and add return counters to task lines for shortage rollback.

**Tech Stack:** Java 8, Spring Boot, MyBatis-Plus, MySQL 8, Vue 3, TypeScript, Ant Design Vue.

## Global Constraints

- Original ERP SKU remains the database business key.
- Internal warehouse SKU is used for warehouse display and scan resolution.
- Manual registration requires supervisor authority and a mandatory reason.
- Existing completed records must remain readable.
- Shipment remains the only physical inventory deduction point.

---

### Task 1: Schema And Model Compatibility

**Files:**
- Create: `erp-backend/sql/migration/V98__outbound_execution_hardening.sql`
- Modify: outbound task, task-line, package, and sales-outbound entities/VOs/DTOs

- [ ] Add return counters and actual operator fields with safe defaults.
- [ ] Add supervisor manual reason fields to scan DTOs.
- [ ] Add indexes for task status, package status, and active task lookup.
- [ ] Add model tests that load legacy rows with null new fields.

### Task 2: Precise Picking And Pallet Direct Mode

**Files:**
- Modify: `OutboundPickingService.java`
- Modify: `OutboundPickingController.java`
- Test: `OutboundPickingServiceTest.java`

- [ ] Add failing tests proving piece pick rejects pallet-only scans.
- [ ] Add failing tests proving exact slot/pallet mismatch is rejected.
- [ ] Add failing tests proving a complete pallet scan completes all eligible pallet lines.
- [ ] Implement strict scan rules and actual operator resolution.
- [ ] Protect manual endpoints with supervisor authority and mandatory reason.

### Task 3: Shortage Return-To-Stock

**Files:**
- Modify: picking DTOs, service, controller, API types
- Test: `OutboundPickingServiceTest.java`

- [ ] Add failing test proving short-close cannot release partially picked stock.
- [ ] Implement `RETURNING` task state and per-line return counters.
- [ ] Require scans of original slot/pallet and SKU for piece returns.
- [ ] Release reservations and set `BACKORDER` only after return completion.

### Task 4: Sorting, Packing, Documents, And Shipment

**Files:**
- Modify: `SalesOutboundPackageService.java`
- Modify: `OutboundShippingService.java`
- Modify: relevant controllers and DTOs
- Test: package and shipping service tests

- [ ] Add internal SKU and actual operator names to package responses.
- [ ] Require supervisor reason for manual sort/pack registration.
- [ ] Validate all package/document/reservation conditions before shipment CAS.
- [ ] Preserve owner-provided and warehouse-print document modes.

### Task 5: Warehouse Work UI

**Files:**
- Modify: `PickListDrawer.vue`
- Modify: `OutboundPickingPage.vue`
- Modify: `PackModal.vue`
- Modify: outbound API types and functions

- [ ] Replace repeated modal picking with a continuous scan panel.
- [ ] Show full slot, pallet, internal SKU, strategy, and progress.
- [ ] Add pallet-direct confirmation and return-to-stock views.
- [ ] Show manual controls only with supervisor permission.
- [ ] Keep sort slots and per-package packing in the existing visual style.

### Task 6: Verification And Rollout

- [ ] Run backend outbound service tests.
- [ ] Run frontend type-check and production build.
- [ ] Apply V98 to the Docker MySQL database.
- [ ] Verify existing four outbound orders and package counts remain consistent.
- [ ] Restart backend and frontend.
- [ ] Verify ports, logs, and one single-order plus one wave workflow.

