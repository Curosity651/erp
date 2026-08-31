# 库存管理六功能新库存内核修复实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. If the user explicitly requests subagents, superpowers:subagent-driven-development may be used instead.

**Goal:** 将库存总览、库存明细、库存记录、库存预测、库存配置和发货生产测算完全切换到新逻辑库位库存内核，并使所有新库存变更可审计、可核对、可回滚。

**Architecture:** `wms_location_inventory` 是仓内库存唯一事实源；新增 `wms_inventory_event` 和 `wms_inventory_event_line` 作为事件账本。货主库存查询通过独立的 `OwnerInventoryQueryService` 聚合，FBO、在途和采购未发货继续读取各自业务事实表。六个功能不读写旧库存表，不建立新旧双写。

**Tech Stack:** Java 8、Spring Boot 2.7、MyBatis-Plus、MySQL 8、JUnit 5、Mockito、Vue 3、TypeScript、Ant Design Vue、ExcelJS、Vitest、pnpm。

**Spec:** `docs/superpowers/specs/2026-08-26-inventory-management-new-core-design.md`

## Global Constraints

- 不修改 `wms_location_inventory` 的 SSOT 地位。
- `available = quantity - reserved_quantity`，下游禁止再次扣预占。
- FBO、在途和采购未发货不得写入仓内库存表。
- 库存更新和事件账本必须在同一事务提交或回滚。
- 新业务写入品质只允许 `GOOD/DEFECTIVE`；旧输入 `DAMAGED` 仅在边界映射。
- 第一批上线不删除旧表；旧表只保留离线回查，不参与六个功能。
- 当前工作区已有大量非本任务改动。实施者不得格式化、暂存、提交或覆盖无关文件。
- 迁移文件暂定 `V135`。开始实施前若已存在其他 `V135`，必须暂停并统一重编号，禁止重复版本上线。
- 每个任务先写失败测试，再写最小实现；每个任务验证通过后才进入下一任务。

---

### Task 0: 建立实施检查点与依赖基线

**Files:**
- Create: `docs/audits/2026-08-26-inventory-new-core-preflight.md`
- Create: `erp-backend/admin/src/test/java/com/erp/admin/wms/NewInventoryFeatureDependencyGuardTest.java`

**Interfaces:**
- Produces: 本任务实施前的 Git、迁移版本、数据库数量和旧表依赖清单。
- Produces: 六个功能不得重新依赖旧库存表的静态守卫测试。

- [ ] **Step 1: 记录当前 Git 状态，不自动清理工作区。**

  Run:

  ```powershell
  git status --short
  git diff --name-only
  git ls-files --others --exclude-standard
  ```

  Expected: 输出被保存到预检文档；若本计划涉及的文件正在被其他任务修改，暂停实施并先协调，不使用 `reset` 或 `checkout`。

- [ ] **Step 2: 确认迁移版本。**

  Run:

  ```powershell
  Get-ChildItem erp-backend/sql/migration -File | Sort-Object Name | Select-Object -Last 20 -ExpandProperty Name
  ```

  Expected: 最高版本仍为 `V134__repair_provider_settlement_menu_encoding.sql`，不存在 `V135`。

- [ ] **Step 3: 保存数据库切换前数量基线。**

  Run in MySQL:

  ```sql
  SELECT erp_tenant_id,
         COUNT(*) AS row_count,
         SUM(quantity) AS physical_quantity,
         SUM(reserved_quantity) AS reserved_quantity,
         SUM(quantity - reserved_quantity) AS available_quantity
  FROM wms_location_inventory
  WHERE deleted = 0
  GROUP BY erp_tenant_id;

  SELECT tenant_id, COUNT(*) AS row_count, SUM(quantity) AS fbo_quantity,
         MAX(synced_at) AS last_synced_at
  FROM wms_fbo_inventory_snapshot
  GROUP BY tenant_id;

  SELECT COUNT(*) AS legacy_inventory_rows FROM wms_inventory;
  SELECT COUNT(*) AS legacy_flow_rows FROM wms_stock_flow;
  SELECT COUNT(*) AS legacy_posting_rows FROM wms_stock_posting;
  ```

  Expected for current owner tenant 6: location rows `6`、physical `24`、reserved `4`、available `20`；FBO quantity `12`。

- [ ] **Step 4: 编写守卫测试并先确认失败。**

  守卫扫描以下运行时文件及其 Mapper XML：

  ```text
  InventoryController
  OwnerInventoryQueryService
  RegionStockDataProvider
  InventoryForecastFacade
  InventoryAlertTask
  ShipProdCalcFacade
  StockFlowController
  StockPostingQueryController
  ```

  禁止出现：

  ```text
  InventoryMapper
  InventoryService
  wms_inventory
  wms_stock_flow
  wms_stock_posting
  wms_physical_inventory
  ```

  Run:

  ```powershell
  Set-Location erp-backend
  mvn -pl admin -Dtest=NewInventoryFeatureDependencyGuardTest test
  ```

  Expected: 当前实现仍依赖旧服务/旧表，测试失败。

- [ ] **Step 5: 提交仅包含预检和守卫测试的变更。**

  ```powershell
  git add docs/audits/2026-08-26-inventory-new-core-preflight.md erp-backend/admin/src/test/java/com/erp/admin/wms/NewInventoryFeatureDependencyGuardTest.java
  git commit -m "test: guard new inventory feature dependencies"
  ```

---

### Task 1: 新增事件账本和品质约束

**Files:**
- Create: `erp-backend/sql/migration/V135__inventory_event_ledger_and_quality.sql`
- Create: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/entity/InventoryEvent.java`
- Create: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/entity/InventoryEventLine.java`
- Create: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/enums/LocationInventoryQuality.java`
- Create: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/enums/InventoryEventType.java`
- Create: `erp-backend/admin/src/main/java/com/erp/admin/wms/mapper/InventoryEventMapper.java`
- Create: `erp-backend/admin/src/main/java/com/erp/admin/wms/mapper/InventoryEventLineMapper.java`
- Create: `erp-backend/admin/src/test/java/com/erp/admin/wms/InventoryEventSchemaTest.java`

**Interfaces:**
- Produces: `wms_inventory_event`、`wms_inventory_event_line`。
- Produces: `LocationInventoryQuality.normalize(String)`，将 `DAMAGED` 映射为 `DEFECTIVE`，拒绝其他值。
- Produces: 唯一幂等约束 `(tenant_id, idempotency_key)`。

- [ ] **Step 1: 写 schema 失败测试。**

  断言迁移包含：事件头、事件行、两个唯一键、货主/仓库/来源索引、品质归一 SQL、新库存读取索引。

- [ ] **Step 2: 运行测试确认失败。**

  ```powershell
  Set-Location erp-backend
  mvn -pl admin -Dtest=InventoryEventSchemaTest test
  ```

- [ ] **Step 3: 创建事件表。**

  迁移中的核心约束必须等价于：

  ```sql
  UNIQUE KEY uk_inventory_event_idempotency (tenant_id, idempotency_key),
  UNIQUE KEY uk_inventory_event_line_no (event_id, line_no),
  CHECK (quality IN ('GOOD', 'DEFECTIVE')),
  CHECK (after_quantity >= 0),
  CHECK (after_reserved >= 0 AND after_reserved <= after_quantity)
  ```

  事件行使用有符号 `quantity_delta/reserved_delta`；前后值均为非负整数。

- [ ] **Step 4: 在 V135 中归一新库存品质并修正数据库字段注释。**

  `V135` 执行：

  ```sql
  UPDATE wms_location_inventory
  SET quality = 'DEFECTIVE'
  WHERE UPPER(quality) = 'DAMAGED';

  ALTER TABLE wms_location_inventory
    MODIFY COLUMN quality VARCHAR(32) NOT NULL DEFAULT 'GOOD'
      COMMENT 'GOOD or DEFECTIVE',
    ADD CONSTRAINT chk_location_inventory_quality
      CHECK (quality IN ('GOOD', 'DEFECTIVE'));
  ```

  禁止修改已经执行过的 `V103__logical_location_inventory_core.sql`，避免 Flyway checksum 变化。

- [ ] **Step 5: 添加实体、枚举和 Mapper。**

  `LocationInventoryQuality.normalize` 行为：

  ```java
  GOOD       -> GOOD
  DEFECTIVE  -> DEFECTIVE
  DAMAGED    -> DEFECTIVE
  null/blank/other -> IllegalArgumentException
  ```

- [ ] **Step 6: 运行 schema 与枚举测试。**

  ```powershell
  Set-Location erp-backend
  mvn -pl admin -Dtest=InventoryEventSchemaTest,LocationInventorySchemaTest test
  ```

- [ ] **Step 7: 提交。**

  ```powershell
  git add erp-backend/sql/migration/V135__inventory_event_ledger_and_quality.sql erp-backend/admin/src/main/java/com/erp/admin/wms/model/entity/InventoryEvent.java erp-backend/admin/src/main/java/com/erp/admin/wms/model/entity/InventoryEventLine.java erp-backend/admin/src/main/java/com/erp/admin/wms/model/enums/LocationInventoryQuality.java erp-backend/admin/src/main/java/com/erp/admin/wms/model/enums/InventoryEventType.java erp-backend/admin/src/main/java/com/erp/admin/wms/mapper/InventoryEventMapper.java erp-backend/admin/src/main/java/com/erp/admin/wms/mapper/InventoryEventLineMapper.java erp-backend/admin/src/test/java/com/erp/admin/wms/InventoryEventSchemaTest.java
  git commit -m "feat: add logical inventory event ledger"
  ```

---

### Task 2: 建立原子库存变更与事件写入服务

**Files:**
- Create: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/dto/InventoryMutationContext.java`
- Create: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/dto/InventoryMutationLine.java`
- Create: `erp-backend/admin/src/main/java/com/erp/admin/wms/service/InventoryEventService.java`
- Create: `erp-backend/admin/src/test/java/com/erp/admin/wms/InventoryEventServiceTest.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/service/LocationInventoryService.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/mapper/WmsLocationInventoryMapper.java`
- Modify: `erp-backend/admin/src/main/resources/mapper/wms/WmsLocationInventoryMapper.xml`
- Modify: `erp-backend/admin/src/test/java/com/erp/admin/wms/LocationInventoryServiceTest.java`

**Interfaces:**
- Consumes: `InventoryMutationContext { eventType, sourceType, sourceId, sourceNo, reason, operatorId, operatorName, idempotencyKey }`。
- Produces: `InventoryEventService.append(context, headerDimensions, lines)`。
- Guarantees: 同一幂等键不会再次改变库存。

- [ ] **Step 1: 为 `increase/decrease` 编写失败测试。**

  覆盖新行创建、已有行增加、可用不足回滚、品质归一、事件前后值正确、事件保存失败时库存回滚。

- [ ] **Step 2: 为 `reserve/release/ship` 编写失败测试。**

  核心断言：

  ```text
  reserve: quantityDelta=0,  reservedDelta=+N
  release: quantityDelta=0,  reservedDelta=-N
  ship:    quantityDelta=-N, reservedDelta=-N
  ```

- [ ] **Step 3: 为 `move/stocktake/scrap` 编写失败测试。**

  `MOVE` 必须一头两行且净 `quantity_delta=0`；盘盈/盘亏分别产生正负实物变化；报废最终减少实物和预留。

- [ ] **Step 4: 运行测试确认当前签名和事件能力不足。**

  ```powershell
  Set-Location erp-backend
  mvn -pl admin -Dtest=LocationInventoryServiceTest,InventoryEventServiceTest test
  ```

- [ ] **Step 5: 实现幂等事件服务。**

  处理顺序：先按 `(tenantId, idempotencyKey)` 查询已存在事件；存在则返回；不存在时由调用方锁库存并更新，再保存事件头/行。捕获唯一键冲突时必须抛出并回滚当前事务，不允许吞掉冲突后继续提交库存。

- [ ] **Step 6: 修改 `LocationInventoryService` 的所有写方法。**

  新签名必须要求上下文，不保留无上下文的 public 写入重载：

  ```java
  increase(key, quantity, context)
  decrease(key, quantity, context)
  reserve(request, context)
  release(fulfillmentOrderId, context)
  ship(fulfillmentOrderId, context)
  move(sourceInventoryId, targetLocationId, quantity, context)
  adjustCountedQuantity(inventoryId, countedQuantity, context)
  reserveInventory(inventoryId, quantity, context)
  releaseInventory(inventoryId, quantity, context)
  scrapReservedInventory(inventoryId, quantity, context)
  ```

- [ ] **Step 7: 修复 Mapper 条件更新。**

  所有更新同时限制 `deleted = 0`；品质写入前统一归一；任何更新行数不是 1 都抛错。

- [ ] **Step 8: 运行测试。**

  ```powershell
  Set-Location erp-backend
  mvn -pl admin -Dtest=LocationInventoryServiceTest,InventoryEventServiceTest,LogicalInventoryOperationRulesTest test
  ```

- [ ] **Step 9: 提交。**

  ```powershell
  git add erp-backend/admin/src/main/java/com/erp/admin/wms/model/dto/InventoryMutationContext.java erp-backend/admin/src/main/java/com/erp/admin/wms/model/dto/InventoryMutationLine.java erp-backend/admin/src/main/java/com/erp/admin/wms/service/InventoryEventService.java erp-backend/admin/src/main/java/com/erp/admin/wms/service/LocationInventoryService.java erp-backend/admin/src/main/java/com/erp/admin/wms/mapper/WmsLocationInventoryMapper.java erp-backend/admin/src/main/resources/mapper/wms/WmsLocationInventoryMapper.xml erp-backend/admin/src/test/java/com/erp/admin/wms/InventoryEventServiceTest.java erp-backend/admin/src/test/java/com/erp/admin/wms/LocationInventoryServiceTest.java
  git commit -m "feat: write inventory mutations to event ledger"
  ```

---

### Task 3: 将全部新库存业务写入口接入事件上下文

**Files:**
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/service/LogicalInboundPutawayService.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/service/LogicalLocationTransferService.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/service/FulfillmentReservationService.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/service/FulfillmentShippingService.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/service/FulfillmentStatusSyncService.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/service/StocktakeService.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/service/ReturnQcService.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/service/AdjustmentService.java`
- Modify: `erp-backend/admin/src/test/java/com/erp/admin/wms/LogicalInboundPutawayServiceTest.java`
- Modify: `erp-backend/admin/src/test/java/com/erp/admin/wms/LogicalLocationTransferServiceTest.java`
- Modify: `erp-backend/admin/src/test/java/com/erp/admin/wms/FulfillmentReservationServiceTest.java`
- Modify: `erp-backend/admin/src/test/java/com/erp/admin/wms/FulfillmentShippingServiceTest.java`
- Modify: `erp-backend/admin/src/test/java/com/erp/admin/wms/FulfillmentStatusSyncServiceTest.java`
- Modify: `erp-backend/admin/src/test/java/com/erp/admin/wms/ReturnQcServiceTest.java`
- Create: `erp-backend/admin/src/test/java/com/erp/admin/wms/StocktakeInventoryEventTest.java`
- Create: `erp-backend/admin/src/test/java/com/erp/admin/wms/AdjustmentInventoryEventTest.java`
- Create: `erp-backend/admin/src/test/java/com/erp/admin/wms/InventoryMutationEntryPointTest.java`

**Interfaces:**
- Produces: 每一个新库存写入口都有来源单据和稳定幂等键。

- [ ] **Step 1: 编写入口守卫测试。**

  扫描所有 `LocationInventoryService` 调用，禁止调用没有 `InventoryMutationContext` 的签名，并验证八类业务事件类型映射。

- [ ] **Step 2: 运行定向测试确认编译/守卫失败。**

  ```powershell
  Set-Location erp-backend
  mvn -pl admin -Dtest=InventoryMutationEntryPointTest,FulfillmentReservationServiceTest,FulfillmentShippingServiceTest,FulfillmentStatusSyncServiceTest,ReturnQcServiceTest test
  ```

- [ ] **Step 3: 接入上架、退货和移库。**

  映射：

  ```text
  LogicalInboundPutawayService -> INBOUND_PUTAWAY
  ReturnQcService              -> RETURN_PUTAWAY
  LogicalLocationTransferService -> MOVE
  ```

  移库原因和操作人必须取调整单/请求真实值，不使用固定空字符串。

- [ ] **Step 4: 接入履约预占、释放和签出。**

  映射：

  ```text
  FulfillmentReservationService -> RESERVE
  FulfillmentStatusSyncService   -> RELEASE
  FulfillmentShippingService     -> SHIP
  ```

  重复取消、重复签出继续保持业务幂等，并且只存在一条库存事件。

- [ ] **Step 5: 接入盘点和报废。**

  盘点按差额产生 `STOCKTAKE_GAIN/LOSS`；报废的预留、取消预留、最终报废分别产生 `SCRAP_RESERVE/SCRAP_RELEASE/SCRAP`。

- [ ] **Step 6: 运行入口和业务测试。**

  ```powershell
  Set-Location erp-backend
  mvn -pl admin -Dtest=InventoryMutationEntryPointTest,LocationInventoryServiceTest,FulfillmentReservationServiceTest,FulfillmentShippingServiceTest,FulfillmentStatusSyncServiceTest,ReturnQcServiceTest,StocktakeCreationModeTest test
  ```

- [ ] **Step 7: 提交。**

  ```powershell
  git add erp-backend/admin/src/main/java/com/erp/admin/wms/service/LogicalInboundPutawayService.java erp-backend/admin/src/main/java/com/erp/admin/wms/service/LogicalLocationTransferService.java erp-backend/admin/src/main/java/com/erp/admin/wms/service/FulfillmentReservationService.java erp-backend/admin/src/main/java/com/erp/admin/wms/service/FulfillmentShippingService.java erp-backend/admin/src/main/java/com/erp/admin/wms/service/FulfillmentStatusSyncService.java erp-backend/admin/src/main/java/com/erp/admin/wms/service/StocktakeService.java erp-backend/admin/src/main/java/com/erp/admin/wms/service/ReturnQcService.java erp-backend/admin/src/main/java/com/erp/admin/wms/service/AdjustmentService.java erp-backend/admin/src/test/java/com/erp/admin/wms/LogicalInboundPutawayServiceTest.java erp-backend/admin/src/test/java/com/erp/admin/wms/LogicalLocationTransferServiceTest.java erp-backend/admin/src/test/java/com/erp/admin/wms/FulfillmentReservationServiceTest.java erp-backend/admin/src/test/java/com/erp/admin/wms/FulfillmentShippingServiceTest.java erp-backend/admin/src/test/java/com/erp/admin/wms/FulfillmentStatusSyncServiceTest.java erp-backend/admin/src/test/java/com/erp/admin/wms/ReturnQcServiceTest.java erp-backend/admin/src/test/java/com/erp/admin/wms/StocktakeInventoryEventTest.java erp-backend/admin/src/test/java/com/erp/admin/wms/AdjustmentInventoryEventTest.java erp-backend/admin/src/test/java/com/erp/admin/wms/InventoryMutationEntryPointTest.java
  git commit -m "feat: audit all logical inventory mutations"
  ```

  Before commit: 运行 `git diff --cached --name-only`，确认仅包含上述文件；如上述文件本身已有他人改动，先按补丁块复核，不覆盖。

---

### Task 4: 建立货主库存统一查询层

**Files:**
- Create: `erp-backend/admin/src/main/java/com/erp/admin/wms/mapper/OwnerInventoryQueryMapper.java`
- Create: `erp-backend/admin/src/main/resources/mapper/wms/OwnerInventoryQueryMapper.xml`
- Create: `erp-backend/admin/src/main/java/com/erp/admin/wms/service/OwnerInventoryQueryService.java`
- Create: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/dto/OwnerInventoryAggregateDTO.java`
- Create: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/dto/OwnerInventoryLocationDTO.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/dto/RegionSkuStockDTO.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/qo/InventoryQO.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/vo/InventorySummaryVO.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/vo/InventoryPageVO.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/vo/InventoryDetailVO.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/vo/SkuSummaryVO.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/vo/WarehouseSummaryVO.java`
- Modify: `erp-backend/admin/src/main/resources/mapper/wms/AssetFinanceMapper.xml`
- Create: `erp-backend/admin/src/test/java/com/erp/admin/wms/OwnerInventoryQueryMapperSqlTest.java`
- Create: `erp-backend/admin/src/test/java/com/erp/admin/wms/OwnerInventoryQueryServiceTest.java`
- Create: `erp-backend/admin/src/test/java/com/erp/admin/wms/InventoryQualityCompatibilitySqlTest.java`

**Interfaces:**
- Produces: 明确的 `goodPhysical/available/reserved/defective/physical` 聚合。
- Produces: 库位粒度的库存明细分页。

- [ ] **Step 1: 编写 SQL 失败测试。**

  断言新 Mapper：

  - 只从 `wms_location_inventory` 读取仓内库存；
  - 显式过滤 `erp_tenant_id`、`deleted=0` 和有效自有仓；
  - `available` 只计算一次 `quantity - reserved_quantity`；
  - `DEFECTIVE` 不进入可用；
  - 仓库数量受货主/访问范围限制；
  - 不出现任何旧库存表名。

- [ ] **Step 2: 编写服务失败测试。**

  覆盖平台、服务商、货主三类作用域；空库存返回全零；库位详情不能越权；SKU 汇总正确合并 FBO，但 FBO 不进入海外仓可用。

- [ ] **Step 3: 运行测试确认失败。**

  ```powershell
  Set-Location erp-backend
  mvn -pl admin -Dtest=OwnerInventoryQueryMapperSqlTest,OwnerInventoryQueryServiceTest test
  ```

- [ ] **Step 4: 实现 Mapper SQL。**

  关键表达式统一为：

  ```sql
  SUM(CASE WHEN li.quality = 'GOOD' THEN li.quantity ELSE 0 END)
  SUM(CASE WHEN li.quality = 'GOOD' THEN li.reserved_quantity ELSE 0 END)
  SUM(CASE WHEN li.quality = 'GOOD' THEN li.quantity - li.reserved_quantity ELSE 0 END)
  SUM(CASE WHEN li.quality = 'DEFECTIVE' THEN li.quantity ELSE 0 END)
  ```

- [ ] **Step 5: 实现服务和展示数据批量填充。**

  仓库、区域、库位和 SKU 展示信息批量加载；分页由 MyBatis 完成。

  同时把 `AssetFinanceMapper.xml` 的不良品判断改为 `quality = 'DEFECTIVE'`，禁止用 `quality <> 'DAMAGED'` 判断良品；这样 V135 品质归一后资产页不会把不良品误计为可用。

- [ ] **Step 6: 运行测试。**

  ```powershell
  Set-Location erp-backend
  mvn -pl admin -Dtest=OwnerInventoryQueryMapperSqlTest,OwnerInventoryQueryServiceTest,LocationInventoryQueryServiceTest,InventoryQualityCompatibilitySqlTest test
  ```

- [ ] **Step 7: 提交。**

  ```powershell
  git add erp-backend/admin/src/main/java/com/erp/admin/wms/mapper/OwnerInventoryQueryMapper.java erp-backend/admin/src/main/resources/mapper/wms/OwnerInventoryQueryMapper.xml erp-backend/admin/src/main/java/com/erp/admin/wms/service/OwnerInventoryQueryService.java erp-backend/admin/src/main/java/com/erp/admin/wms/model/dto/OwnerInventoryAggregateDTO.java erp-backend/admin/src/main/java/com/erp/admin/wms/model/dto/OwnerInventoryLocationDTO.java erp-backend/admin/src/main/java/com/erp/admin/wms/model/dto/RegionSkuStockDTO.java erp-backend/admin/src/main/java/com/erp/admin/wms/model/qo/InventoryQO.java erp-backend/admin/src/main/java/com/erp/admin/wms/model/vo/InventorySummaryVO.java erp-backend/admin/src/main/java/com/erp/admin/wms/model/vo/InventoryPageVO.java erp-backend/admin/src/main/java/com/erp/admin/wms/model/vo/InventoryDetailVO.java erp-backend/admin/src/main/java/com/erp/admin/wms/model/vo/SkuSummaryVO.java erp-backend/admin/src/main/java/com/erp/admin/wms/model/vo/WarehouseSummaryVO.java erp-backend/admin/src/main/resources/mapper/wms/AssetFinanceMapper.xml erp-backend/admin/src/test/java/com/erp/admin/wms/OwnerInventoryQueryMapperSqlTest.java erp-backend/admin/src/test/java/com/erp/admin/wms/OwnerInventoryQueryServiceTest.java erp-backend/admin/src/test/java/com/erp/admin/wms/InventoryQualityCompatibilitySqlTest.java
  git commit -m "feat: add owner logical inventory read model"
  ```

---

### Task 5: 切换库存总览和库存明细后端

**Files:**
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/controller/InventoryController.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/service/RegionInventoryService.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/service/RegionStockDataProvider.java`
- Create: `erp-backend/admin/src/test/java/com/erp/admin/wms/InventoryControllerContractTest.java`
- Modify: `erp-backend/admin/src/test/java/com/erp/admin/wms/RegionInventoryServiceTest.java`

**Interfaces:**
- Preserves: 现有 `/wms/inventory/*` 路径。
- Changes: `/page` 返回库位粒度；`/detail` 返回库位组成和新事件摘要。

- [ ] **Step 1: 编写接口契约失败测试。**

  断言所有总览/明细接口由 `OwnerInventoryQueryService` 提供，控制器不再注入 `InventoryService`。

- [ ] **Step 2: 改造控制器和区域统计服务。**

  `/by-operator` 若仍有使用方，改为新查询层的服务商作用域聚合；若前端和菜单无调用，标记废弃并移除，不能继续返回旧 `Inventory` 实体。

- [ ] **Step 3: 改造 `RegionStockDataProvider`。**

  只通过 `OwnerInventoryQueryService` 获取仓内聚合；移除 `InventoryMapper` 依赖。方法返回的 `available` 已扣预占。

- [ ] **Step 4: 运行后端测试和依赖守卫。**

  ```powershell
  Set-Location erp-backend
  mvn -pl admin -Dtest=InventoryControllerContractTest,OwnerInventoryQueryServiceTest,RegionInventoryServiceTest,NewInventoryFeatureDependencyGuardTest test
  ```

- [ ] **Step 5: 提交。**

  ```powershell
  git add erp-backend/admin/src/main/java/com/erp/admin/wms/controller/InventoryController.java erp-backend/admin/src/main/java/com/erp/admin/wms/service/RegionInventoryService.java erp-backend/admin/src/main/java/com/erp/admin/wms/service/RegionStockDataProvider.java erp-backend/admin/src/test/java/com/erp/admin/wms/InventoryControllerContractTest.java erp-backend/admin/src/test/java/com/erp/admin/wms/RegionInventoryServiceTest.java
  git commit -m "fix: switch inventory overview to location inventory"
  ```

---

### Task 6: 改造库存总览和库存明细前端

**Files:**
- Modify: `erp-frontend/src/api/wms/inventory/types.ts`
- Modify: `erp-frontend/src/api/wms/inventory/index.ts`
- Modify: `erp-frontend/src/views/wms/inventory/InventoryOverviewPage.vue`
- Modify: `erp-frontend/src/views/wms/inventory/InventoryDetailPage.vue`
- Modify: `erp-frontend/src/views/wms/inventory/components/StatisticPanel.vue`
- Modify: `erp-frontend/src/views/wms/inventory/components/WarehouseOverviewTable.vue`
- Modify: `erp-frontend/src/views/wms/inventory/components/SkuOverviewTable.vue`
- Modify: `erp-frontend/src/views/wms/inventory/components/RegionOverviewTable.vue`
- Modify: `erp-frontend/src/views/wms/inventory/components/InventoryDetailSearch.vue`
- Modify: `erp-frontend/src/views/wms/inventory/components/StockFlowDrawer.vue`
- Create: `erp-frontend/src/views/wms/inventory/inventory-quantity.ts`
- Create: `erp-frontend/src/views/wms/inventory/inventory-quantity.vitest.ts`
- Create: `erp-frontend/src/views/wms/inventory/inventory-export.ts`
- Create: `erp-frontend/src/views/wms/inventory/inventory-export.vitest.ts`

**Interfaces:**
- Consumes: 新数量字段、库位字段和品质字段。
- Produces: 可测试的数量展示与 Excel 导出。

- [ ] **Step 1: 编写数量展示失败测试。**

  当前基线输入 `physical=24, reserved=4, available=20, fbo=12` 时，断言页面不会显示 `available=16`，海外仓和 FBO 分列。

- [ ] **Step 2: 编写导出失败测试。**

  导出列至少包含区域、仓库、库位、SKU、品质、实物、预占、可用、更新时间；禁止保留“开发中”提示。

- [ ] **Step 3: 运行 Vitest 确认失败。**

  ```powershell
  Set-Location erp-frontend
  pnpm exec vitest run src/views/wms/inventory/inventory-quantity.vitest.ts src/views/wms/inventory/inventory-export.vitest.ts
  ```

- [ ] **Step 4: 更新类型、总览和明细。**

  所有旧字段兼容映射只放在 API 适配层，页面内部统一使用新语义字段。品质展示 `GOOD=良品`、`DEFECTIVE=不良品`。

- [ ] **Step 5: 实现 Excel 导出。**

  使用已有 `exceljs`，导出当前筛选条件下的全部后端结果；超过单次接口上限时后端新增专用导出接口，不循环猜测页数。

- [ ] **Step 6: 运行前端验证。**

  ```powershell
  Set-Location erp-frontend
  pnpm exec vitest run src/views/wms/inventory/inventory-quantity.vitest.ts src/views/wms/inventory/inventory-export.vitest.ts
  pnpm type-check
  ```

- [ ] **Step 7: 提交。**

  ```powershell
  git add erp-frontend/src/api/wms/inventory/types.ts erp-frontend/src/api/wms/inventory/index.ts erp-frontend/src/views/wms/inventory/InventoryOverviewPage.vue erp-frontend/src/views/wms/inventory/InventoryDetailPage.vue erp-frontend/src/views/wms/inventory/components/StatisticPanel.vue erp-frontend/src/views/wms/inventory/components/WarehouseOverviewTable.vue erp-frontend/src/views/wms/inventory/components/SkuOverviewTable.vue erp-frontend/src/views/wms/inventory/components/RegionOverviewTable.vue erp-frontend/src/views/wms/inventory/components/InventoryDetailSearch.vue erp-frontend/src/views/wms/inventory/components/StockFlowDrawer.vue erp-frontend/src/views/wms/inventory/inventory-quantity.ts erp-frontend/src/views/wms/inventory/inventory-quantity.vitest.ts erp-frontend/src/views/wms/inventory/inventory-export.ts erp-frontend/src/views/wms/inventory/inventory-export.vitest.ts
  git commit -m "fix: show logical location inventory in owner pages"
  ```

---

### Task 7: 将库存记录切换到新事件账本

**Files:**
- Create: `erp-backend/admin/src/main/java/com/erp/admin/wms/mapper/InventoryEventQueryMapper.java`
- Create: `erp-backend/admin/src/main/resources/mapper/wms/InventoryEventQueryMapper.xml`
- Create: `erp-backend/admin/src/main/java/com/erp/admin/wms/service/InventoryEventQueryService.java`
- Create: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/vo/InventoryEventPageVO.java`
- Create: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/vo/InventoryEventDetailVO.java`
- Create: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/vo/InventoryEventLineVO.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/controller/StockFlowController.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/controller/StockPostingQueryController.java`
- Create: `erp-backend/admin/src/test/java/com/erp/admin/wms/InventoryEventQueryMapperSqlTest.java`
- Create: `erp-backend/admin/src/test/java/com/erp/admin/wms/InventoryEventQueryServiceTest.java`
- Modify: `erp-frontend/src/api/wms/stock-flow/index.ts`
- Modify: `erp-frontend/src/api/wms/stock-flow/types.ts`
- Modify: `erp-frontend/src/api/wms/stock-posting/index.ts`
- Modify: `erp-frontend/src/api/wms/stock-posting/types.ts`
- Modify: `erp-frontend/src/views/wms/stock-record/StockRecordPage.vue`
- Modify: `erp-frontend/src/views/wms/stock-record/views/FlowListView.vue`
- Modify: `erp-frontend/src/views/wms/stock-record/views/OrderListView.vue`
- Modify: `erp-frontend/src/views/wms/stock-record/components/StockPostingDetailDrawer.vue`
- Modify: `erp-frontend/src/views/wms/stock-record/components/TodaySummaryCards.vue`
- Create: `erp-frontend/src/views/wms/stock-record/inventory-event-export.ts`
- Create: `erp-frontend/src/views/wms/stock-record/inventory-event-export.vitest.ts`

**Interfaces:**
- Preserves: `/wms/stock-flow/*`、`/wms/stock-posting/*` 和权限码。
- Changes: 底层查询新事件表，返回库位、品质、实物变化和预占变化。

- [ ] **Step 1: 写事件查询 SQL 失败测试。**

  断言分页、详情、今日统计和趋势均查询新事件表；MOVE 不计入出入库统计；查询按货主作用域过滤。

- [ ] **Step 2: 写服务权限和汇总失败测试。**

  覆盖越权详情返回“记录不存在”、事件头/行分页、移库一头两行、预占/释放/签出统计。

- [ ] **Step 3: 运行后端测试确认失败。**

  ```powershell
  Set-Location erp-backend
  mvn -pl admin -Dtest=InventoryEventQueryMapperSqlTest,InventoryEventQueryServiceTest,NewInventoryFeatureDependencyGuardTest test
  ```

- [ ] **Step 4: 实现新事件查询并切换两个控制器。**

  控制器不再注入 `StockFlowService` 或 `StockPostingQueryService`。旧路径只作为 API 兼容层，不再代表旧表。

- [ ] **Step 5: 编写前端导出和字段格式测试。**

  流水导出包含源/目标库位、品质、实物变化、预占变化；操作单导出包含事件号、来源单据、操作人和原因。

- [ ] **Step 6: 改造库存记录页面。**

  移除两个 `console.log` 导出占位；MOVE 的明细同时显示源库位和目标库位；数量变化保留正负号。

- [ ] **Step 7: 运行前后端验证。**

  ```powershell
  Set-Location erp-backend
  mvn -pl admin -Dtest=InventoryEventQueryMapperSqlTest,InventoryEventQueryServiceTest,NewInventoryFeatureDependencyGuardTest test
  Set-Location ..\erp-frontend
  pnpm exec vitest run src/views/wms/stock-record/inventory-event-export.vitest.ts
  pnpm type-check
  ```

- [ ] **Step 8: 提交。**

  ```powershell
  git add erp-backend/admin/src/main/java/com/erp/admin/wms/mapper/InventoryEventQueryMapper.java erp-backend/admin/src/main/resources/mapper/wms/InventoryEventQueryMapper.xml erp-backend/admin/src/main/java/com/erp/admin/wms/service/InventoryEventQueryService.java erp-backend/admin/src/main/java/com/erp/admin/wms/model/vo/InventoryEventPageVO.java erp-backend/admin/src/main/java/com/erp/admin/wms/model/vo/InventoryEventDetailVO.java erp-backend/admin/src/main/java/com/erp/admin/wms/model/vo/InventoryEventLineVO.java erp-backend/admin/src/main/java/com/erp/admin/wms/controller/StockFlowController.java erp-backend/admin/src/main/java/com/erp/admin/wms/controller/StockPostingQueryController.java erp-backend/admin/src/test/java/com/erp/admin/wms/InventoryEventQueryMapperSqlTest.java erp-backend/admin/src/test/java/com/erp/admin/wms/InventoryEventQueryServiceTest.java erp-frontend/src/api/wms/stock-flow erp-frontend/src/api/wms/stock-posting erp-frontend/src/views/wms/stock-record
  git commit -m "fix: read inventory records from event ledger"
  ```

---

### Task 8: 修复库存配置有效值和通知逻辑

**Files:**
- Create: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/dto/EffectiveInventoryConfig.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/service/InventoryConfigService.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/mapper/InventoryConfigMapper.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/task/InventoryAlertTask.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/vo/ForecastDetailVO.java`
- Create: `erp-backend/admin/src/test/java/com/erp/admin/wms/InventoryConfigServiceTest.java`
- Create: `erp-backend/admin/src/test/java/com/erp/admin/wms/InventoryAlertTaskTest.java`

**Interfaces:**
- Produces: `getEffectiveConfig(regionId, skuCode)`。
- Guarantees: SKU 级安全库存、阈值和通知开关统一生效。

- [ ] **Step 1: 编写配置优先级和越权失败测试。**

  覆盖 SKU 覆盖全局、字段为空回退全局、跨货主 ID 无法更新/删除、非法 SKU 或区域拒绝保存。

- [ ] **Step 2: 编写通知失败测试。**

  SKU `notifyEnabled=false` 时即使状态为 `STOCKOUT` 也不发送；SKU 自定义阈值影响状态和邮件展示。

- [ ] **Step 3: 运行测试确认失败。**

  ```powershell
  Set-Location erp-backend
  mvn -pl admin -Dtest=InventoryConfigServiceTest,InventoryAlertTaskTest test
  ```

- [ ] **Step 4: 实现有效配置对象和作用域安全 CRUD。**

  删除改为：先按“当前货主 + ID”查询，再条件删除；保存时验证 SKU、区域均属于当前货主可用范围。

- [ ] **Step 5: 修改通知任务。**

  邮件筛选逐条调用已批量加载的有效配置，不在循环内逐 SKU 查询数据库。邮件按实际阈值分组或逐行显示阈值，不能继续传入一个全局阈值代表全部 SKU。

- [ ] **Step 6: 补齐预测详情字段。**

  `ForecastDetailVO` 增加：

  ```text
  notifyEnabled
  notifyThresholdDays
  configSource
  ```

- [ ] **Step 7: 运行测试并提交。**

  ```powershell
  Set-Location erp-backend
  mvn -pl admin -Dtest=InventoryConfigServiceTest,InventoryAlertTaskTest test
  git add admin/src/main/java/com/erp/admin/wms/model/dto/EffectiveInventoryConfig.java admin/src/main/java/com/erp/admin/wms/service/InventoryConfigService.java admin/src/main/java/com/erp/admin/wms/mapper/InventoryConfigMapper.java admin/src/main/java/com/erp/admin/wms/task/InventoryAlertTask.java admin/src/main/java/com/erp/admin/wms/model/vo/ForecastDetailVO.java admin/src/test/java/com/erp/admin/wms/InventoryConfigServiceTest.java admin/src/test/java/com/erp/admin/wms/InventoryAlertTaskTest.java
  git commit -m "fix: apply effective inventory alert config"
  ```

---

### Task 9: 修复库存预测库存口径和 SKU 全集

**Files:**
- Create: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/dto/RegionSkuKey.java`
- Create: `erp-backend/admin/src/main/java/com/erp/admin/wms/service/ForecastSkuUniverseService.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/service/RegionSalesDataProvider.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/order/mapper/ErpOrderMapper.java`
- Modify: `erp-backend/admin/src/main/resources/mapper/order/ErpOrderMapper.xml`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/service/ShippingOrderService.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/facade/InventoryForecastFacade.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/vo/ForecastSummaryVO.java`
- Create: `erp-backend/admin/src/test/java/com/erp/admin/wms/ForecastSkuUniverseServiceTest.java`
- Create: `erp-backend/admin/src/test/java/com/erp/admin/wms/InventoryForecastFacadeTest.java`

**Interfaces:**
- Produces: 区域 + SKU 候选集合，来源为库存、销量、在途、待发货和配置的并集。
- Guarantees: `sellable = available`，不再二次扣预占。

- [ ] **Step 1: 编写预测失败测试。**

  覆盖：

  - `quantity=24, reserved=4` 时 available/sellable 均为 `20`；
  - 零库存但有销量时产生 `STOCKOUT` 行；
  - 只有在途或只有配置时仍进入候选集合；
  - 每个 SKU 使用自己的阈值；
  - 空库存列表不触发整个页面提前返回。

- [ ] **Step 2: 运行测试确认失败。**

  ```powershell
  Set-Location erp-backend
  mvn -pl admin -Dtest=ForecastSkuUniverseServiceTest,InventoryForecastFacadeTest test
  ```

- [ ] **Step 3: 实现候选集合提供者。**

  返回实际存在的 `regionId:skuCode` 键，不做“全部区域 × 全部 SKU”的笛卡尔积。销量 Provider 增加独立的候选键查询，避免必须先有库存 SKU 才能查销量。

- [ ] **Step 4: 改造预测汇总和详情。**

  删除 `stockList.isEmpty()` 的提前返回；以候选键驱动，缺少仓内库存时使用全零 DTO；当前可售直接取 `availableQuantity`。

- [ ] **Step 5: 批量加载有效配置。**

  汇总页不得在每个 SKU 循环中调用三次配置查询。新增批量方法，结果按 `regionId:skuCode` 建 Map。

- [ ] **Step 6: 运行测试。**

  ```powershell
  Set-Location erp-backend
  mvn -pl admin -Dtest=ForecastSkuUniverseServiceTest,InventoryForecastFacadeTest,InventoryConfigServiceTest,NewInventoryFeatureDependencyGuardTest test
  ```

- [ ] **Step 7: 提交。**

  ```powershell
  git add erp-backend/admin/src/main/java/com/erp/admin/wms/model/dto/RegionSkuKey.java erp-backend/admin/src/main/java/com/erp/admin/wms/service/ForecastSkuUniverseService.java erp-backend/admin/src/main/java/com/erp/admin/wms/service/RegionSalesDataProvider.java erp-backend/admin/src/main/java/com/erp/admin/order/mapper/ErpOrderMapper.java erp-backend/admin/src/main/resources/mapper/order/ErpOrderMapper.xml erp-backend/admin/src/main/java/com/erp/admin/wms/service/ShippingOrderService.java erp-backend/admin/src/main/java/com/erp/admin/wms/facade/InventoryForecastFacade.java erp-backend/admin/src/main/java/com/erp/admin/wms/model/vo/ForecastSummaryVO.java erp-backend/admin/src/test/java/com/erp/admin/wms/ForecastSkuUniverseServiceTest.java erp-backend/admin/src/test/java/com/erp/admin/wms/InventoryForecastFacadeTest.java
  git commit -m "fix: forecast from logical available inventory"
  ```

---

### Task 10: 修复多 SKU 部分到货和发货生产测算输入

**Files:**
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/vo/ShippingOrderInboundedQuantityVO.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/mapper/PurchaseInboundMapper.java`
- Modify: `erp-backend/admin/src/main/resources/mapper/wms/PurchaseInboundMapper.xml`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/service/ShippingOrderService.java`
- Create: `erp-backend/admin/src/main/java/com/erp/admin/wms/service/ShipProdInputProvider.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/facade/ShipProdCalcFacade.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/vo/ShipProdCalcRowVO.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/vo/ShipProdCalcDetailVO.java`
- Create: `erp-backend/admin/src/test/java/com/erp/admin/wms/ShippingOrderIncomingPlanTest.java`
- Create: `erp-backend/admin/src/test/java/com/erp/admin/wms/ShipProdInputProviderTest.java`
- Create: `erp-backend/admin/src/test/java/com/erp/admin/wms/ShipProdCalcFacadeTest.java`

**Interfaces:**
- Changes: 已入库统计键从 `shippingOrderId` 改为 `shippingOrderId:skuCode`。
- Produces: 测算 A/B/C/E 输入和 FBO 同步新鲜度。

- [ ] **Step 1: 编写多 SKU 部分到货失败测试。**

  示例：同一物流单 SKU-A 计划 10 已入 6，SKU-B 计划 8 已入 1；剩余必须分别为 4 和 7，不能把总入库 7 从两条计划各减一次。

- [ ] **Step 2: 修改入库聚合 SQL。**

  SQL 必须选择并分组：

  ```sql
  pio.shipping_order_id,
  pioi.sku_code
  GROUP BY pio.shipping_order_id, pioi.sku_code
  ```

- [ ] **Step 3: 编写测算输入失败测试。**

  当前基线断言：A 海外仓 `20`、B FBO `12`；FBO-only SKU 必须出现在汇总；详情和汇总输入一致；过期 FBO 返回警告。

- [ ] **Step 4: 实现 `ShipProdInputProvider`。**

  - A 从 `OwnerInventoryQueryService` 读取新库存 available；
  - B 从 `FboInventorySnapshotService.sumQuantityBySku` 读取；
  - C 从物流单剩余到货读取；
  - E 从采购未发货读取；
  - SKU 全集包含 A/B/C/E/销量。

- [ ] **Step 5: 改造 Facade 汇总和详情共用 Provider。**

  删除两处 `.fbo(0)`；不复制两套输入组装代码。VO 增加 `fboLastSyncedAt/fboStale`。

- [ ] **Step 6: 运行测试。**

  ```powershell
  Set-Location erp-backend
  mvn -pl admin -Dtest=ShippingOrderIncomingPlanTest,ShipProdInputProviderTest,ShipProdCalcFacadeTest,NewInventoryFeatureDependencyGuardTest test
  ```

- [ ] **Step 7: 提交。**

  ```powershell
  git add erp-backend/admin/src/main/java/com/erp/admin/wms/model/vo/ShippingOrderInboundedQuantityVO.java erp-backend/admin/src/main/java/com/erp/admin/wms/mapper/PurchaseInboundMapper.java erp-backend/admin/src/main/resources/mapper/wms/PurchaseInboundMapper.xml erp-backend/admin/src/main/java/com/erp/admin/wms/service/ShippingOrderService.java erp-backend/admin/src/main/java/com/erp/admin/wms/service/ShipProdInputProvider.java erp-backend/admin/src/main/java/com/erp/admin/wms/facade/ShipProdCalcFacade.java erp-backend/admin/src/main/java/com/erp/admin/wms/model/vo/ShipProdCalcRowVO.java erp-backend/admin/src/main/java/com/erp/admin/wms/model/vo/ShipProdCalcDetailVO.java erp-backend/admin/src/test/java/com/erp/admin/wms/ShippingOrderIncomingPlanTest.java erp-backend/admin/src/test/java/com/erp/admin/wms/ShipProdInputProviderTest.java erp-backend/admin/src/test/java/com/erp/admin/wms/ShipProdCalcFacadeTest.java
  git commit -m "fix: use logical and fbo stock in planning"
  ```

---

### Task 11: 修复预测、配置和测算前端

**Files:**
- Modify: `erp-frontend/src/api/wms/inventory-forecast/types.ts`
- Modify: `erp-frontend/src/views/wms/inventory-forecast/InventoryForecastPage.vue`
- Modify: `erp-frontend/src/views/wms/inventory-forecast/components/ForecastDetailDrawer.vue`
- Modify: `erp-frontend/src/views/wms/inventory-forecast/components/SafetyStockEditModal.vue`
- Modify: `erp-frontend/src/views/wms/inventory-config/InventoryConfigPage.vue`
- Modify: `erp-frontend/src/views/wms/inventory-config/components/SkuConfigFormModal.vue`
- Modify: `erp-frontend/src/api/wms/ship-prod-calc/types.ts`
- Modify: `erp-frontend/src/views/wms/ship-prod-calc/ShipProdCalcPage.vue`
- Modify: `erp-frontend/src/views/wms/ship-prod-calc/components/ShipProdDetailDrawer.vue`
- Create: `erp-frontend/src/views/wms/inventory-forecast/forecast-stock.vitest.ts`
- Create: `erp-frontend/src/views/wms/ship-prod-calc/ship-prod-input.vitest.ts`

**Interfaces:**
- Consumes: 有效配置来源、真实通知开关、FBO 数量与同步时间。

- [ ] **Step 1: 编写前端失败测试。**

  断言：available 20 不再渲染为 16；详情弹窗真实回填通知开关和阈值；FBO 12 进入“现货 A+B”；过期快照显示警告。

- [ ] **Step 2: 运行测试确认失败。**

  ```powershell
  Set-Location erp-frontend
  pnpm exec vitest run src/views/wms/inventory-forecast/forecast-stock.vitest.ts src/views/wms/ship-prod-calc/ship-prod-input.vitest.ts
  ```

- [ ] **Step 3: 更新类型和页面。**

  删除对后端不存在字段的隐式访问；显示配置来源“SKU 覆盖/全局默认”；测算页分列 A、B、C、E。

- [ ] **Step 4: 运行前端验证。**

  ```powershell
  Set-Location erp-frontend
  pnpm exec vitest run src/views/wms/inventory-forecast/forecast-stock.vitest.ts src/views/wms/ship-prod-calc/ship-prod-input.vitest.ts
  pnpm type-check
  pnpm build-only
  ```

- [ ] **Step 5: 提交。**

  ```powershell
  git add erp-frontend/src/api/wms/inventory-forecast/types.ts erp-frontend/src/views/wms/inventory-forecast/InventoryForecastPage.vue erp-frontend/src/views/wms/inventory-forecast/components/ForecastDetailDrawer.vue erp-frontend/src/views/wms/inventory-forecast/components/SafetyStockEditModal.vue erp-frontend/src/views/wms/inventory-config/InventoryConfigPage.vue erp-frontend/src/views/wms/inventory-config/components/SkuConfigFormModal.vue erp-frontend/src/api/wms/ship-prod-calc/types.ts erp-frontend/src/views/wms/ship-prod-calc/ShipProdCalcPage.vue erp-frontend/src/views/wms/ship-prod-calc/components/ShipProdDetailDrawer.vue erp-frontend/src/views/wms/inventory-forecast/forecast-stock.vitest.ts erp-frontend/src/views/wms/ship-prod-calc/ship-prod-input.vitest.ts
  git commit -m "fix: align forecast and planning inventory inputs"
  ```

---

### Task 12: 退出旧库存运行时依赖

**Files:**
- Modify or retire after route audit:
  - `erp-backend/admin/src/main/java/com/erp/admin/wms/facade/ShippingOrderFacade.java`
  - `erp-backend/admin/src/main/java/com/erp/admin/wms/facade/TransferOrderFacade.java`
  - `erp-backend/admin/src/main/java/com/erp/admin/wms/service/WmsInboundExecutionService.java`
  - legacy sections of `erp-backend/admin/src/main/java/com/erp/admin/wms/service/StocktakeService.java`
- Delete only after all callers are gone:
  - `erp-backend/admin/src/main/java/com/erp/admin/wms/service/InventoryPostingEngine.java`
  - `erp-backend/admin/src/main/java/com/erp/admin/wms/service/StockPostingService.java`
  - `erp-backend/admin/src/main/java/com/erp/admin/wms/service/StockPostingQueryService.java`
  - `erp-backend/admin/src/main/java/com/erp/admin/wms/service/StockPostingItemService.java`
  - `erp-backend/admin/src/main/java/com/erp/admin/wms/service/StockFlowService.java`
  - `erp-backend/admin/src/main/java/com/erp/admin/wms/service/InventoryService.java`
  - `erp-backend/admin/src/main/java/com/erp/admin/wms/mapper/InventoryMapper.java`
  - `erp-backend/admin/src/main/resources/mapper/wms/InventoryMapper.xml`
  - `erp-backend/admin/src/main/java/com/erp/admin/wms/mapper/StockFlowMapper.java`
  - `erp-backend/admin/src/main/resources/mapper/wms/StockFlowMapper.xml`
  - `erp-backend/admin/src/main/java/com/erp/admin/wms/mapper/StockPostingMapper.java`
  - `erp-backend/admin/src/main/resources/mapper/wms/StockPostingMapper.xml`
  - `erp-backend/admin/src/main/java/com/erp/admin/wms/mapper/StockPostingItemMapper.java`
  - `erp-backend/admin/src/main/resources/mapper/wms/StockPostingItemMapper.xml`
  - `erp-backend/admin/src/main/java/com/erp/admin/wms/model/entity/Inventory.java`
  - `erp-backend/admin/src/main/java/com/erp/admin/wms/model/entity/StockFlow.java`
  - `erp-backend/admin/src/main/java/com/erp/admin/wms/model/entity/StockPosting.java`
  - `erp-backend/admin/src/main/java/com/erp/admin/wms/model/entity/StockPostingItem.java`
  - `erp-backend/admin/src/main/java/com/erp/admin/wms/converter/InventoryConverter.java`
  - `erp-backend/admin/src/main/java/com/erp/admin/wms/converter/StockFlowConverter.java`
  - `erp-backend/admin/src/main/java/com/erp/admin/wms/converter/StockPostingConverter.java`
- Create: `erp-backend/admin/src/test/java/com/erp/admin/wms/LegacyInventoryRuntimeDependencyTest.java`

**Interfaces:**
- Produces: 主运行时代码不再读写 `wms_inventory/wms_stock_flow/wms_stock_posting`。
- Does not: 本任务不删除数据库旧表。

- [ ] **Step 1: 对遗留入口做路由和菜单使用审计。**

  分三类记录：已被逻辑库位流程替代、仍在使用但可迁移、仅维护物流/采购状态。不能仅因类名旧就直接删除。

- [ ] **Step 2: 编写全运行时依赖失败测试。**

  扫描 `admin/src/main/java` 与 `admin/src/main/resources/mapper`，允许实体/迁移历史留档，但禁止 Spring Controller、Service、Facade 和运行时 Mapper SQL 引用旧库存表。

- [ ] **Step 3: 迁移或停用四个遗留入口。**

  - 物流单发货/到货只更新物流单状态与明细剩余量，不再向旧库存桶过账；
  - 旧调拨入口若已被逻辑库位移库替代，隐藏菜单并返回明确停用错误，不自动猜测源库位；
  - 旧入库执行入口切换到逻辑上架服务，不能同时写两套库存；
  - 盘点保留逻辑库位流程，删除旧快照/旧过账分支。

- [ ] **Step 4: 删除无调用的旧运行时 Bean 和 Mapper。**

  删除前运行：

  ```powershell
  git grep -n "InventoryPostingEngine\|StockPostingService\|StockPostingQueryService\|StockFlowService\|InventoryService\|InventoryMapper\|StockFlowMapper\|StockPostingMapper" -- erp-backend/admin/src/main
  ```

  Expected: 仅显示即将删除的声明文件，不再有业务调用方。

- [ ] **Step 5: 运行全后端测试。**

  ```powershell
  Set-Location erp-backend
  mvn -pl admin test
  ```

- [ ] **Step 6: 提交。**

  ```powershell
  git add erp-backend/admin/src/main/java/com/erp/admin/wms/facade/ShippingOrderFacade.java erp-backend/admin/src/main/java/com/erp/admin/wms/facade/TransferOrderFacade.java erp-backend/admin/src/main/java/com/erp/admin/wms/service/WmsInboundExecutionService.java erp-backend/admin/src/main/java/com/erp/admin/wms/service/StocktakeService.java erp-backend/admin/src/test/java/com/erp/admin/wms/LegacyInventoryRuntimeDependencyTest.java
  git add -u -- erp-backend/admin/src/main/java/com/erp/admin/wms/service/InventoryPostingEngine.java erp-backend/admin/src/main/java/com/erp/admin/wms/service/StockPostingService.java erp-backend/admin/src/main/java/com/erp/admin/wms/service/StockPostingQueryService.java erp-backend/admin/src/main/java/com/erp/admin/wms/service/StockPostingItemService.java erp-backend/admin/src/main/java/com/erp/admin/wms/service/StockFlowService.java erp-backend/admin/src/main/java/com/erp/admin/wms/service/InventoryService.java erp-backend/admin/src/main/java/com/erp/admin/wms/mapper/InventoryMapper.java erp-backend/admin/src/main/resources/mapper/wms/InventoryMapper.xml erp-backend/admin/src/main/java/com/erp/admin/wms/mapper/StockFlowMapper.java erp-backend/admin/src/main/resources/mapper/wms/StockFlowMapper.xml erp-backend/admin/src/main/java/com/erp/admin/wms/mapper/StockPostingMapper.java erp-backend/admin/src/main/resources/mapper/wms/StockPostingMapper.xml erp-backend/admin/src/main/java/com/erp/admin/wms/mapper/StockPostingItemMapper.java erp-backend/admin/src/main/resources/mapper/wms/StockPostingItemMapper.xml erp-backend/admin/src/main/java/com/erp/admin/wms/model/entity/Inventory.java erp-backend/admin/src/main/java/com/erp/admin/wms/model/entity/StockFlow.java erp-backend/admin/src/main/java/com/erp/admin/wms/model/entity/StockPosting.java erp-backend/admin/src/main/java/com/erp/admin/wms/model/entity/StockPostingItem.java erp-backend/admin/src/main/java/com/erp/admin/wms/converter/InventoryConverter.java erp-backend/admin/src/main/java/com/erp/admin/wms/converter/StockFlowConverter.java erp-backend/admin/src/main/java/com/erp/admin/wms/converter/StockPostingConverter.java
  git commit -m "refactor: retire legacy inventory runtime"
  ```

  注意：如果遗留入口存在尚未迁移的真实生产流程，本任务可拆到第二次发布，但六个功能仍必须保持新表切换；数据库旧表不得在第一批发布中删除。

---

### Task 13: 数据迁移、全量验证和上线验收

**Files:**
- Create: `docs/operations/inventory-new-core-release-checklist.md`
- Update: `docs/audits/2026-08-26-inventory-new-core-preflight.md`

**Interfaces:**
- Produces: 可签字的迁移、回归、核对和回滚记录。

- [ ] **Step 1: 在备份环境执行 V135 并核对 schema。**

  ```sql
  SHOW CREATE TABLE wms_inventory_event;
  SHOW CREATE TABLE wms_inventory_event_line;
  SELECT quality, COUNT(*), SUM(quantity)
  FROM wms_location_inventory
  WHERE deleted = 0
  GROUP BY quality;
  ```

  Expected: 新库存品质只含 `GOOD/DEFECTIVE`，事件表和索引完整。

- [ ] **Step 2: 运行后端定向回归。**

  ```powershell
  Set-Location erp-backend
  mvn -pl admin -Dtest=LocationInventorySchemaTest,LocationInventoryServiceTest,InventoryEventServiceTest,InventoryEventQueryServiceTest,OwnerInventoryQueryServiceTest,RegionInventoryServiceTest,InventoryConfigServiceTest,InventoryAlertTaskTest,InventoryForecastFacadeTest,ShippingOrderIncomingPlanTest,ShipProdCalcFacadeTest,NewInventoryFeatureDependencyGuardTest test
  ```

- [ ] **Step 3: 运行后端全量测试与打包。**

  ```powershell
  Set-Location erp-backend
  mvn -pl admin test
  mvn -pl admin -DskipTests package
  ```

- [ ] **Step 4: 运行前端全量验证。**

  ```powershell
  Set-Location erp-frontend
  pnpm exec vitest run
  pnpm type-check
  pnpm build-only
  ```

- [ ] **Step 5: 执行 API 冒烟。**

  使用货主、WMS 服务商、平台各一个测试账号验证：

  ```text
  /wms/inventory/summary
  /wms/inventory/summary-by-warehouse
  /wms/inventory/summary-by-sku/page
  /wms/inventory/page
  /wms/inventory/detail
  /wms/stock-flow/page
  /wms/stock-posting/page
  /wms/inventory-forecast/summary
  /wms/ship-prod-calc/summary
  ```

  Expected: 身份作用域正确，无旧库存 SQL。

- [ ] **Step 6: 执行业务事件冒烟。**

  在测试仓依次执行一笔上架、预占、释放、移库、盘盈、盘亏、退货上架、报废、签出。每一步核对：

  ```text
  库存行 quantity/reserved/version
  事件头数量
  事件明细 delta 与 before/after
  业务单据状态
  重复提交幂等
  ```

- [ ] **Step 7: 执行当前数据验收 SQL。**

  ```sql
  SELECT
    SUM(CASE WHEN quality = 'GOOD' THEN quantity ELSE 0 END) AS good_physical,
    SUM(CASE WHEN quality = 'GOOD' THEN reserved_quantity ELSE 0 END) AS reserved,
    SUM(CASE WHEN quality = 'GOOD' THEN quantity - reserved_quantity ELSE 0 END) AS available,
    SUM(CASE WHEN quality = 'DEFECTIVE' THEN quantity ELSE 0 END) AS defective
  FROM wms_location_inventory
  WHERE erp_tenant_id = 6 AND deleted = 0;

  SELECT SUM(quantity) AS fbo_quantity, MAX(synced_at) AS last_synced_at
  FROM wms_fbo_inventory_snapshot
  WHERE tenant_id = 6;
  ```

  Expected before新增业务冒烟数据: `good_physical=24`、`reserved=4`、`available=20`、`defective=0`、`fbo=12`。

- [ ] **Step 8: 检查运行 SQL 和日志。**

  六个页面访问期间搜索 SQL 日志，确认不存在：

  ```text
  FROM wms_inventory
  FROM wms_stock_flow
  FROM wms_stock_posting
  FROM wms_physical_inventory
  ```

- [ ] **Step 9: 记录回滚点并上线。**

  回滚顺序：停止流量 → 回滚应用版本 → 保留 V135 新表和已写事件 → 核对库存数量。禁止回滚为旧表读写或开启双写。

- [ ] **Step 10: 更新交接文档并提交。**

  ```powershell
  git add docs/operations/inventory-new-core-release-checklist.md docs/audits/2026-08-26-inventory-new-core-preflight.md
  git commit -m "docs: record inventory new core release verification"
  ```

## 完成定义

只有同时满足以下条件才可宣布完成：

- 六个功能全部通过新库存查询层或新事件账本取数。
- 新库存所有写入口都有同事务事件记录和幂等键。
- 预测没有二次扣预占，零库存 SKU 不会被遗漏。
- 配置的 SKU 覆盖值和通知开关真实生效。
- 多 SKU 部分到货按 SKU 独立扣减。
- 发货生产测算同时使用新库存 A 和 FBO B。
- 前后端定向测试、全量测试、类型检查和生产构建通过。
- 三类身份数据隔离通过。
- 当前基线 24/4/20、FBO 12 对账一致。
- 六个功能运行日志无旧库存表 SQL。
- 上线清单、备份位置、回滚点和验收人已记录。
