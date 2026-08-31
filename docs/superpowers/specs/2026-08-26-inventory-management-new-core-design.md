# 库存管理六功能新库存内核切换设计

日期：2026-08-26  
状态：已确认方案，待按实施计划执行  
适用范围：库存总览、库存明细、库存记录、库存预测、库存配置、发货生产测算

## 1. 目标

本次修复将六个库存功能完全切换到新库存模型，并建立可持续的读写闭环：

- `wms_location_inventory` 是海外仓仓内库存唯一事实源（SSOT）。
- `wms_inventory_event` 与 `wms_inventory_event_line` 是新库存变化历史的唯一账本。
- `wms_fbo_inventory_snapshot` 独立表示平台 FBO 快照。
- 物流单及其明细独立表示待发货和在途库存。
- 六个功能不再读取、写入或回填 `wms_inventory`、`wms_stock_flow`、`wms_stock_posting`、`wms_stock_posting_item`、`wms_physical_inventory`。
- 不建立新旧库存双写，不通过定时任务把新库存同步回旧库存。

旧表第一阶段只作为离线备份保留，不再进入六个功能的运行时依赖；确认其他遗留模块完成迁移后再归档或删除。

## 2. 不在本次范围内

- 不把 FBO 快照写进 `wms_location_inventory`。
- 不把采购未发货、物流在途写进 `wms_location_inventory`。
- 不伪造切换日前的库位级历史流水。
- 不在未建立 Git 检查点和数据库备份前清理旧表。
- 不重写发货生产测算的数学模型，只修正输入数据和输入口径。

## 3. 已确认的核心决策

### 3.1 当前状态与变化历史分离

```text
当前仓内状态             变化历史
wms_location_inventory  wms_inventory_event
                         └─ wms_inventory_event_line
```

`wms_location_inventory` 只保存当前数量、预占数量和版本号。业务单据、操作人、原因、变更前后值等审计信息写入事件账本。

### 3.2 库存来源边界

| 库存含义 | 唯一来源 | 是否进入仓内可用 |
|---|---|---|
| 海外仓良品实物 | `wms_location_inventory.quantity`，品质为 `GOOD` | 是 |
| 海外仓预占 | `wms_location_inventory.reserved_quantity`，品质为 `GOOD` | 否 |
| 海外仓不良品 | `wms_location_inventory.quantity`，品质为 `DEFECTIVE` | 否 |
| FBO 平台库存 | `wms_fbo_inventory_snapshot.quantity` | 仅进入测算模型 B，不计海外仓可用 A |
| 物流在途 | 已发货/部分到货物流单明细的 `quantity - received_quantity` | 否，作为未来到货 C |
| 采购未发货 | 采购明细的 `quantity - shipped_quantity` | 否，作为生产/采购链路 E |

### 3.3 数量公式

对指定货主、仓库、区域或 SKU 聚合时：

```text
goodPhysical = SUM(quantity WHERE quality = 'GOOD')
reserved     = SUM(reserved_quantity WHERE quality = 'GOOD')
available    = SUM(quantity - reserved_quantity WHERE quality = 'GOOD')
defective    = SUM(quantity WHERE quality = 'DEFECTIVE')
physical     = goodPhysical + defective
```

约束：

- `quantity >= 0`
- `reserved_quantity >= 0`
- `reserved_quantity <= quantity`
- `available` 已经扣除了预占，任何下游不得再次执行 `available - reserved`。
- “仓内良品”显示 `goodPhysical`，“不良品”单独显示 `defective`，“仓内实物总量”显示 `physical`。
- FBO、在途和采购未发货必须单列，不能混入仓内实物数量。

### 3.4 品质统一

新库存内核只保存：

- `GOOD`
- `DEFECTIVE`

输入边界暂时兼容旧值 `DAMAGED`，但写入前统一映射为 `DEFECTIVE`。迁移脚本把 `wms_location_inventory` 内已有 `DAMAGED` 更新为 `DEFECTIVE`。旧批次表中的品质值不在本次迁移中改写。

## 4. 新的后端分层

### 4.1 货主库存只读层

新增 `OwnerInventoryQueryMapper` 和 `OwnerInventoryQueryService`，统一提供：

- 全局汇总；
- 按仓库汇总；
- 按 SKU 汇总；
- 按区域和 SKU 汇总；
- 库存明细分页；
- 指定仓库 + SKU 的库位组成；
- 指定 SKU 集合的仓内可用量；
- 区域/SKU 库存全集。

所有 SQL 显式包含 `erp_tenant_id` 货主作用域、`deleted = 0`、有效自有仓条件。服务商和平台查看权限通过 `ErpOwnerScopeService.readScope()` 解析，不接受前端传入货主 ID。

现有 `LocationInventoryQueryService` 继续服务平台端“库位库存”页面；货主库存页面使用新的聚合查询层，避免混淆平台仓库操作权限与货主数据权限。

### 4.2 统一库存视图 DTO

`RegionSkuStockDTO` 调整为明确语义：

```text
goodPhysicalQuantity
availableQuantity
reservedQuantity
defectiveQuantity
```

在途数量不再从仓内库存 Mapper 返回，由物流单服务单独提供。预测和测算必须显式组合两类数据，避免字段名掩盖数据来源。

### 4.3 事件账本

新增事件头 `wms_inventory_event`：

| 字段 | 含义 |
|---|---|
| `event_no` | 可展示的库存事件号 |
| `tenant_id` | 平台租户 |
| `wms_tenant_id` | WMS 服务商 |
| `erp_tenant_id` | 货主 |
| `warehouse_id` | 仓库 |
| `event_type` | 事件类型 |
| `source_type/source_id/source_no` | 来源业务单据 |
| `operator_id/operator_name` | 操作人快照 |
| `reason` | 调整或操作原因 |
| `idempotency_key` | 业务幂等键 |
| `occurred_at` | 业务发生时间 |

新增事件明细 `wms_inventory_event_line`，每一行代表一条受影响的库位库存：

| 字段 | 含义 |
|---|---|
| `event_id/line_no` | 事件与行号 |
| `inventory_id` | 新库存行 ID |
| `location_id` | 本行库位 |
| `counterpart_location_id` | 移库对方库位，可空 |
| `sku_code/quality` | SKU 与品质快照 |
| `quantity_delta` | 实物数量有符号变化 |
| `reserved_delta` | 预占数量有符号变化 |
| `before_quantity/after_quantity` | 实物变化前后 |
| `before_reserved/after_reserved` | 预占变化前后 |

移动一个 SKU 产生一个事件头、两条事件行：源库位 `quantity_delta=-N`，目标库位 `quantity_delta=+N`。预占产生 `quantity_delta=0, reserved_delta=+N`；释放为 `0,-N`；签出为 `-N,-N`。

事件类型至少包括：

```text
INBOUND_PUTAWAY
RETURN_PUTAWAY
RESERVE
RELEASE
SHIP
MOVE
STOCKTAKE_GAIN
STOCKTAKE_LOSS
SCRAP_RESERVE
SCRAP_RELEASE
SCRAP
INITIALIZE
```

### 4.4 写入原子性

`LocationInventoryService` 接收 `InventoryMutationContext`，其中包含事件类型、来源单据、原因、操作人和幂等键。库存更新与事件头/明细写入必须处于同一数据库事务：

1. 锁定库存行；
2. 验证可用量、预占量、品质与库位；
3. 条件更新库存并增加版本号；
4. 写事件头和事件明细；
5. 提交事务。

任意一步失败必须整体回滚。事件写入不得使用 `REQUIRES_NEW`。

幂等键由业务来源生成，例如：

```text
PUTAWAY:{putawayOrderId}:{lineId}
FULFILLMENT_RESERVE:{fulfillmentId}:{itemId}
FULFILLMENT_RELEASE:{fulfillmentId}
FULFILLMENT_SHIP:{fulfillmentId}
MOVE:{transferOrderId}
STOCKTAKE:{stocktakeOrderId}:{itemId}
SCRAP:{adjustmentOrderId}:{itemId}:{stage}
RETURN:{returnQcOrderId}:{itemId}:{quality}
```

同一幂等键重复提交时返回已完成结果，不再次改变库存。

## 5. 六个功能的修复设计

### 5.1 库存总览

保留现有接口路径：

- `GET /wms/inventory/summary`
- `GET /wms/inventory/summary-by-warehouse`
- `GET /wms/inventory/summary-by-sku/page`
- `GET /wms/inventory/summary-by-region`
- `GET /wms/inventory/region-stats`

控制器改用 `OwnerInventoryQueryService`。汇总卡片和表格统一使用新数量公式。

按 SKU 汇总由三类来源拼接：

- 海外仓仓内：`wms_location_inventory`；
- FBO：`wms_fbo_inventory_snapshot`；
- 在途：物流单明细。

仓库数量只统计当前查看者有权查看且启用的自有仓，不再统计全部启用仓库。

### 5.2 库存明细

`GET /wms/inventory/page` 改为库位粒度分页，至少展示：

- 区域、仓库；
- 排/分区、库位编码和库位名称；
- SKU；
- 品质；
- 实物数量、预占数量、可用数量；
- 更新时间。

查询条件增加 `locationCode` 和 `quality`。`hasDamaged` 在服务端转换为 `quality = DEFECTIVE`，前端逐步改名为“仅不良品”。

`GET /wms/inventory/detail` 返回指定仓库 + SKU 的汇总、库位组成及新事件账本最近记录，不再返回旧库存 ID 或旧流水。

### 5.3 库存记录

页面继续保留“按流水”和“按操作单”两个视图：

- 按流水：查询 `wms_inventory_event_line`；
- 按操作单：查询 `wms_inventory_event`，详情加载其事件明细。

为降低前端路由和权限迁移风险，第一阶段保留 `/wms/stock-flow/*` 与 `/wms/stock-posting/*` 路径，但控制器底层改为 `InventoryEventQueryService`，返回新事件 VO；原 `StockFlowService` 和 `StockPostingQueryService` 不再被库存记录控制器调用。

今日统计口径：

- 入库：正的 `quantity_delta`，排除 MOVE 的目标行；
- 出库：`SHIP` 的负 `quantity_delta` 绝对值；
- 预占：`RESERVE` 的正 `reserved_delta`；
- 释放：`RELEASE` 和 `SCRAP_RELEASE` 的负 `reserved_delta` 绝对值；
- 移库不计入总入库/总出库，单独作为移库事件展示。

### 5.4 库存预测

修复原则：

- 当前可售直接使用新库存 `availableQuantity`，不再减第二次预占。
- 即使当前库存为零，只要存在销量、在途、采购未发货或 SKU 配置，也必须进入预测 SKU 全集。
- SKU 全集为“仓内库存 ∪ 销量历史 ∪ 在途物流 ∪ 待发货 ∪ SKU 配置”。
- 在途从物流单取数，不从仓内库存 DTO 取数。
- 每个区域 + SKU 使用自身有效配置，不再统一使用全局阈值判定状态。

新增统一有效配置对象：

```text
EffectiveInventoryConfig {
  safetyStock,
  thresholdDays,
  notifyEnabled,
  source // SKU / GLOBAL
}
```

预测详情补齐 `notifyEnabled`、`notifyThresholdDays`、配置来源字段，和前端编辑弹窗保持一致。

物流部分到货必须按 `(shipping_order_id, sku_code)` 扣减已入库数量，禁止把一个物流单的总入库量从每个 SKU 重复扣除。

### 5.5 库存配置

配置表继续独立存在，不迁移到库存表。修复内容：

- CRUD 显式校验当前货主、区域和 SKU 归属；
- 删除使用“当前货主 + 配置 ID”条件，不使用裸 `removeById`；
- 保存前统一校验安全库存和阈值非负、通知开关非空；
- 预测汇总、预测详情和邮件任务都使用 `EffectiveInventoryConfig`；
- SKU 级 `notifyEnabled=false` 必须阻止该 SKU 进入邮件通知；
- SKU 级阈值必须影响该 SKU 的状态判断和邮件内容。

### 5.6 发货生产测算

模型输入修复为：

```text
A overseas   = 海外仓良品可用量（新库存 available）
B fbo        = FBO 快照数量
C transit    = 已发货/部分到货物流单剩余数量
D factoryDone= 当前项目无可靠来源，继续为 0 并展示“未接入”
E producing  = 已确认/生产中采购单未发货数量
```

SKU 全集必须包含 FBO-only SKU 和在途-only SKU。汇总与详情使用同一数据加载器，避免汇总正确、详情仍为零。

FBO 同步时间超过 24 小时或不存在时，测算结果增加过期警告；过期数据仍参与计算，但必须明确提示。

## 6. 前端修复

### 6.1 库存总览与明细

- 更新 API 类型，明确 `physicalQuantity/goodPhysicalQuantity/availableQuantity/reservedQuantity/defectiveQuantity`。
- 明细表增加库位与品质列。
- 详情抽屉显示库位组成和新事件。
- 实现当前仅提示“开发中”的库存明细导出。
- 数量为 0 时显示 0，不以空值代替。

### 6.2 库存记录

- 操作单展示事件号、事件类型、来源单据、仓库、操作人、原因和时间。
- 流水展示库位、对方库位、品质、实物变化、预占变化和变更前后值。
- 移库使用一个业务事件展示，详情中显示源、目标两条明细。
- 实现流水和操作单导出，不再保留 `console.log` 占位实现。

### 6.3 预测、配置和测算

- 预测页显示有效配置来源。
- 详情编辑弹窗接收后端真实的 `notifyEnabled` 与阈值。
- 发货生产测算显示海外仓、FBO、在途和采购未发货四类输入及 FBO 新鲜度。

## 7. 权限与隔离

- 货主只能查看自己的 `erp_tenant_id`。
- WMS 服务商只能查看名下货主，且仓库/库位操作继续受服务商仓库权限约束。
- 平台角色按已有权限查看全部或指定服务商范围。
- 任意详情接口都必须先按作用域查询，不能 `selectById` 后仅依赖前端隐藏。
- 事件头和明细同时保存货主与服务商快照，查询时至少按事件头 `erp_tenant_id` 过滤。

## 8. 数据库与性能

迁移文件预留为 `V135__inventory_event_ledger_and_quality.sql`。执行前必须确认仓库中尚未出现其他 `V135`；若冲突，先更新本设计和实施计划中的版本号。

索引至少包括：

```text
wms_location_inventory
  (erp_tenant_id, warehouse_id, quality, sku_code, deleted)
  (erp_tenant_id, sku_code, quality, deleted)

wms_inventory_event
  UNIQUE (tenant_id, idempotency_key)
  (erp_tenant_id, occurred_at, id)
  (warehouse_id, occurred_at, id)
  (source_type, source_id)

wms_inventory_event_line
  UNIQUE (event_id, line_no)
  (inventory_id, id)
  (location_id, id)
  (sku_code, id)
```

分页查询必须在数据库分页，不允许先加载全部库位库存后在 Java 内分页。SKU、仓库、库位和操作人展示信息必须批量加载，避免 N+1。

## 9. 切换和旧表处置

### 9.1 第一阶段：运行时切换

- 建新事件表并统一品质。
- 所有新库存写操作写入事件账本。
- 六个功能切换到新查询层和事件账本。
- 加入静态守卫测试，禁止六个功能重新引用旧表/旧服务。
- 旧表不回填、不双写、不参与页面查询。

### 9.2 第二阶段：遗留消费者退出

仓库内仍存在 `InventoryPostingEngine`、`StockPostingService` 等遗留依赖。实施时必须逐项确认其路由是否仍启用：

- 已由逻辑库位流程替代：移除调用或隐藏遗留入口；
- 仍有合法业务：改为调用 `LocationInventoryService` 和事件账本；
- 只处理采购/物流状态、不代表仓内实物：改为更新业务单据，不得写旧库存桶。

只有当代码扫描、接口回归和运行日志均证明旧表无读写后，才允许单独创建归档/删除迁移。删除旧表不与本次修复同批上线。

## 10. 回滚原则

- 数据库迁移以新增表、增加索引和品质值归一为主，不在首发版本删除旧表。
- 应用回滚只回滚代码；新事件表和已写事件保留。
- 品质归一执行前备份受影响行；如必须回退应用，可在兼容层继续把 `DEFECTIVE` 映射为旧展示“残品”，不回写 `DAMAGED`。
- 禁止回滚为“新旧双写”。如新查询失败，应修复新查询或暂时关闭相关页面。

## 11. 验收基线

当前开发数据的最低验收值：

```text
wms_location_inventory 实物数量 = 24
reserved_quantity               = 4
available                       = 20
FBO 快照数量                     = 12
海外仓 + FBO 持有量              = 36（当前全部为 GOOD）
```

必须验证：

- 库存总览显示海外仓良品 24、预占 4、可用 20，FBO 12 单列。
- 库存明细能定位到 6 条库位库存记录。
- 新发生一次预占、释放、移库、盘点、退货上架、报废和签出后均有正确事件。
- 任何事件重复提交不重复改库存。
- 预测不再二次扣预占，零库存但有销量的 SKU 显示为断货。
- 多 SKU 物流单部分到货时，各 SKU 剩余量独立正确。
- 发货生产测算的海外仓输入为 20，FBO 输入为 12，并显示快照时间。
- 货主、服务商和平台三类身份查询不串数据。
- 六个功能相关运行 SQL 中不出现旧库存表。

