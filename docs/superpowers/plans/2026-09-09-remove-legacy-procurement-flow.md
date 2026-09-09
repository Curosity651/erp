# 删除旧采购物流链路 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 删除物流商管理、采购单管理、物流单管理和采购入库单四条 ERP 业务链路，同时保留共享入库内核。

**Architecture:** 先移除菜单与前端入口，再从后端控制器向内清理专属依赖。采购入库底层实体、表、Mapper 和共享服务继续为自定义入库、退货及平台入库执行提供能力。

**Tech Stack:** Vue 3、TypeScript、Spring Boot、MyBatis-Plus、MySQL/Flyway、Maven、pnpm

**Spec:** `docs/superpowers/specs/2026-09-09-remove-legacy-procurement-flow-design.md`

## Global Constraints

- 不删除 `wms_purchase_inbound_order` 与 `wms_purchase_inbound_order_item`。
- 不破坏自定义入库、退货入库、海外仓收货和上架。
- 不清空历史业务数据。
- 所有数据库变更必须幂等。

---

### Task 1: 删除菜单、路由与前端页面

**Files:**
- Delete: `erp-frontend/src/views/wms/logistics-provider/**`
- Delete: `erp-frontend/src/views/wms/purchase-order/**`
- Delete: `erp-frontend/src/views/wms/shipping-order/**`
- Delete: `erp-frontend/src/views/wms/purchase-inbound/PurchaseInbound*.vue`
- Modify: `erp-frontend/src/router/constant-routes.ts`
- Create: `erp-backend/sql/migration/V142__remove_legacy_procurement_flow_menus.sql`

- [ ] 删除四个页面入口、专属路由和不再使用的前端 API。
- [ ] 把仍被自定义入库/退货复用的入库状态组件与类型迁到共享目录并更新引用。
- [ ] 运行 `pnpm type-check` 和 `pnpm build`。
- [ ] 提交前端与菜单迁移。

### Task 2: 删除物流商、采购单和物流单后端专属实现

**Files:**
- Delete: `erp-backend/admin/src/main/java/com/erp/admin/wms/**/LogisticsProvider*`
- Delete: `erp-backend/admin/src/main/java/com/erp/admin/wms/**/PurchaseOrder*`
- Delete: `erp-backend/admin/src/main/java/com/erp/admin/wms/**/ShippingOrder*`
- Delete: `erp-backend/admin/src/main/resources/mapper/{purchase,wms}/*Order*.xml`

- [ ] 先用编译错误和引用扫描列出共享消费者。
- [ ] 将入库执行、库存预测和生产测算解除旧服务依赖。
- [ ] 删除专属控制器、服务、模型、转换器和映射器。
- [ ] 运行管理模块测试与编译并提交。

### Task 3: 收紧共享入库内核

**Files:**
- Delete: `erp-backend/admin/src/main/java/com/erp/admin/wms/controller/PurchaseInboundController.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/service/PurchaseInboundService.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/facade/PurchaseInboundFacade.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/controller/{ManualInboundController,CustomReturnController,WmsInboundExecutionController}.java`

- [ ] 删除采购来源专属接口和分支。
- [ ] 保留 MANUAL、CUSTOM_RETURN 及平台收货上架所需共享方法。
- [ ] 增加或调整测试，覆盖自定义入库、退货和平台执行。
- [ ] 运行后端测试并提交。

### Task 4: 全链路回归

- [ ] 扫描源代码，确认不存在四个已删除页面和接口入口。
- [ ] 运行前端国际化检查、类型检查和生产构建。
- [ ] 运行后端管理模块测试与打包。
- [ ] 检查数据库迁移脚本和 Git 工作区，记录剩余兼容层。

