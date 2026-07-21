# 单据流转 & 库存联合校验 — Bug 记录

> 范围：采购单 / 物流单 / 入库单 / 自定义入库单 / 海外仓入库 / 上架 / 出库单 / 海外仓出库 / 退货单 / 海外仓退货
> 重点：**仓库库存错误** 与 **单据状态流转错误**
> 环境：backend 8081(local profile) · frontend 5174 · mysql 3307 · 校验日期 2026-07-03
> 身份：海外仓平台 super_admin / WMS服务商 wms_admin / 货主 jhin（均 123123）

---

> **修复进度（2026-07-05）**：BUG #1 / #2 / #4 / #5 已修复并编译通过（BUILD SUCCESS）；BUG #3 因需统一设计（方案A/B）暂留，待定方案后单独动手。
> （BUG #5 为本轮排查"库位无法重新生成"时新发现：库位软删+唯一键冲突，详见下方 BUG #5。）

## BUG #1 —【库存/库位】库位在货物全部出库清零后被永久占用，无法再次上架 🔴中高 ✅已修复

**位置**
- `wms/mapper/WmsPhysicalInventoryMapper.java:73` `listOccupiedLocationCodes(warehouseId)`
- `wms/service/OutboundShippingService.java:166-171` `ship()` 扣库存
- `wms/service/WmsInboundExecutionService.java:192,201-204` 上架库位独占校验 & `listAvailableLocations`

**问题**
`listOccupiedLocationCodes` 判定"已占用"的口径是 **只要该仓存在一条 `location_code` 非空的批次行即算占用**，
**没有 `quantity > 0` 过滤**：

```java
default List<String> listOccupiedLocationCodes(Long warehouseId) {
    return this.selectList(WrappersX.lambdaQueryX(WmsPhysicalInventory.class)
        .select(WmsPhysicalInventory::getLocationCode)
        .eq(WmsPhysicalInventory::getWarehouseId, warehouseId)
        .isNotNull(WmsPhysicalInventory::getLocationCode))   // ← 缺 .gt(quantity, 0)
        ...
}
```

而签出 `ship()` 扣库存时对批次是 **UPDATE `quantity -= take`**，`quantity` 归零后 **不删除该行**
（全代码库对 `wms_physical_inventory` 无任何 delete/remove，已确认）。

**后果（状态/库存流转错误）**
一个库位的货被全部出库后，批次行残留（quantity=0），但仍被 `listOccupiedLocationCodes` 判为"已占用"：
1. `listAvailableLocations`（上架可选库位）过滤掉它 → 该空库位再也选不到；
2. `putaway()` 对它抛 `PUTAWAY_LOCATION_OCCUPIED` → 永远无法再上架。

随着货物正常进出，仓库可用库位会被"用尽"，物理为空却被系统锁死，只能手工清 DB。

**旁证（口径自相矛盾）**
新做的「仓储概览」占用口径用的是 `pi.quantity > 0`（正确），与上架守卫的"存在即占用"口径**不一致**：
同一个已清零库位，仓储概览显示"空闲"，上架却报"已占用"。两处对"占用"的定义必须统一。

**建议修复**
`listOccupiedLocationCodes` 增加 `.gt(WmsPhysicalInventory::getQuantity, 0)`（与仓储概览、FIFO 口径统一）；
或在 `ship()` 扣减后对 `quantity==0` 的批次行做删除/归档。前者改动最小、风险最低。

**✅ 已修复（2026-07-05）**：`WmsPhysicalInventoryMapper.listOccupiedLocationCodes` 已加 `.gt(quantity, 0)`，
出库清零的库位恢复为空闲、可再次上架；占用口径与仓储概览/FIFO 统一。编译通过。

**验证状态**：代码级确认（静态）。尚未跑完整 入库→上架→出库→签出 一轮做运行期复现（见下方"待运行期复现"）。

---

## BUG #2 —【库存/库位】退货质检回库上架不校验库位（可写到不存在/已占用库位）🔴中高

**位置**：`wms/service/ReturnQcService.java:169-188` `qc()`

**问题**
退货质检 `qc()` 把良/次品回库时，直接拿前端传的 `line.getLocationCode()` 调
`physicalInventoryService.putaway(put)`，**只校验了 `locationCode` 非空**（`Assert.isTrue(line.getLocationCode()!=null ...)`），
既 **不校验该库位是否真实存在于本仓**，也 **不校验该库位是否已被其它批次占用**。

对比正规入库上架 `WmsInboundExecutionService.putaway`（同样调 `putaway`）会强制：
库位存在(`locByCode.get`==null→报错) + 独占(`occupied.contains`→报错) + 品质↔分区联动。退货质检**整套守卫缺失**。

**后果**
1. 库位编码写错/瞎填 → 在 `wms_physical_inventory` 生成一条挂在**不存在库位**上的"幽灵批次"；
2. 传一个**已被占用**的库位 → 同一 `location_code` 出现**两个批次**，打破"一库位一批次"独占不变式
   （上架守卫、仓储概览 distinct 占用统计都以此为前提）；
3. 品质与分区可不匹配（良品塞进不良品区库位）。

**建议修复**：`qc()` 回库前复用入库上架同一套校验（库位存在 + 独占 + 品质↔分区），或抽公共方法共用。

---

## BUG #3 —【库存】库存调整只改聚合快照 wms_inventory，不落批次，导致调整量对出库不可见且会被覆盖 🔴中高

**位置**
- `wms/service/AdjustmentService.java:280-349`（构建 AVAILABLE/DAMAGED 桶过账）
- `wms/service/InventoryPostingEngine.java:128-159`（把增量写进 `wms_inventory` 桶，**不碰批次**）
- `wms/service/WmsInventoryAggregator.java:95-128` `refreshSnapshot`（从批次**重算并覆盖** available/reserved/damaged）
- `wms/mapper/WmsPhysicalInventoryMapper.java` `selectFifoAllocatableForUpdate`（出库下架**只读批次**）

**问题（两个源自相矛盾）**
系统有两套库存数：批次 SSOT `wms_physical_inventory` 与聚合快照 `wms_inventory`。
上架/签出/退货都调 `refreshSnapshot`，它把 available/reserved/damaged **从批次重算后整体覆盖** wms_inventory（只保 in_transit）。
而 `AdjustmentService`（OTHER_IN/OTHER_OUT/OFFLINE_SALE/OFFLINE_PURCHASE/TO_DAMAGED/DAMAGE_DISPOSE）走过账引擎
**只把增量加到 wms_inventory 桶，不生成/不修改任何批次**。

**后果**
1. **调增对出库不可见**：OTHER_IN 调增只抬高 wms_inventory.available，不产生批次；出库下架走 FIFO **只读批次**
   → 明明调增了库存，下架仍判"可用良品不足"整单挂起 BACKORDER。
2. **调整被覆盖抹除**：该 SKU 之后任意一次上架/签出/退货质检触发 `refreshSnapshot` → available/damaged **从批次重算覆盖**
   → 之前的调整量凭空消失。
3. 于是「库存快照页(读 wms_inventory)」与「批次/下架口径」对同一 SKU 给出**不一致**数字，且随操作漂移。

（注：SCRAP 桶与 region in_transit 不被 refreshSnapshot 重算，不受影响；受影响的是 AVAILABLE/DAMAGED 类调整。）

**影响范围（同一根因，全部只改聚合不落批次 → 会被 refreshSnapshot 抹除 / 对 FIFO 出库不可见）**
| 入口 | 过账桶 | 位置 | 具体风险 |
|---|---|---|---|
| 库存调整 Adjustment | AVAILABLE/DAMAGED | `AdjustmentService`+`InventoryPostingEngine` | 调增出库看不到；被下次 refresh 抹除 |
| 盘点 Stocktake | AVAILABLE(盘盈/盘亏) | `StocktakeService.java:341-366` | **盘点纠偏白做**：盘亏后批次仍有幻量，FIFO 照样能拣，且下次 refresh 还原 |
| 销售出库预占 SalesOutbound | AVAILABLE(扣减=预占) | `SalesOutboundFacade.java:206` `doStockPosting` | 提交与下架之间若同 SKU 发生 putaway/签出/退货质检 → refresh 抹除预占 → **可超卖** |
| 自定义出库预占 CustomOutbound | AVAILABLE(扣减=预占) | `CustomOutboundFacade.java:103` `doStockPosting` | 同上，预占被抹 → 可超卖 |

> 注：出库预占在"无批次事件穿插"的顺发场景下聚合链自洽、不会重复扣减（提交扣聚合 available，下架 `reserved_qty+=take` 后 refresh 得到相同 available）。
> 缺陷点在**跨源污染**：任一批次事件(putaway/ship/returnQc)对同 (货主,仓,SKU) 触发 `refreshSnapshot`，会把批次重算值覆盖掉尚未下架的聚合预占/盘点/调整增量。

**建议修复**：统一为"批次是唯一真源"——调整/盘点/预占涉及 AVAILABLE/DAMAGED 时也落批次（新增/扣减批次或引入调整批次、把预占落到批次 `reserved_qty`）；
或反过来让 `refreshSnapshot` 不整体覆盖、改为增量对账。二选一，否则聚合过账与批次重算会永久打架。

---

## BUG #4 —【并发/状态流转】签出 ship() 无行锁/状态 CAS，并发签出致幻量库存与重复副作用 🟠中

**位置**：`wms/service/OutboundShippingService.java:141-214` `ship()`

**问题**
`ship()` 是"读状态→循环扣批次→改单状态"的 check-then-act，中间**无行锁、无订单状态 CAS**，构成 TOCTOU：
- L144 `salesOutboundMapper.selectById(...)` 是**普通读，非 `FOR UPDATE`**；
- `SalesOutboundOrder` **无 `@Version`**；L213 `updateById(order)` 按主键更新，**无 `WHERE order_status='PACKED'`**；
- 于是两个并发签出可**同时通过** PACKED 校验并各自推进。

对比：下架 `OutboundPickingService.pick()` 用 `selectFifoAllocatableForUpdate`（`FOR UPDATE` 行锁，防超卖 C7），
**ship() 漏加了同样的锁** —— 对称性缺失，基本可判定为遗漏而非有意。

**各副作用的实际保护程度（已实测核对）**
| 副作用 | 保护 | 结论 |
|---|---|---|
| 批次扣减 `physicalInventoryMapper.updateById(batch)` (L171) | `WmsPhysicalInventory` **有 `@Version`**(L78) + `OptimisticLockerInnerInterceptor` 已注册 | ⚠️ 半保护：UPDATE 带 `AND version=?`，但**代码忽略影响行数**，0 行被静默丢弃 → 扣减被丢 → **幻量库存**；`refreshSnapshot` 再把错值算进聚合 |
| 计费 `clientBillingRecordMapper.insert` (L202) | `wms_client_billing_record.biz_id` **有唯一索引** `uk_client_billing_biz` | ✅ 不会重复计费，但靠 DuplicateKey 硬回滚；且**仅 `logisticsProductId!=null && fee>0` 才 insert**，无运费单无此网 |
| 订单状态 / 其它库存副作用 | 仅非原子的 Java 状态判断 | ❌ 无并发保护 |

**后果**
1. 两张共享同一批次的出库单并发签出 → 一次扣减被乐观锁 0 行静默吞掉 → 批次少扣 → **幻量库存**。
2. 无运费的出库单并发/重试签出 → 状态与库存副作用可重复触发（计费唯一约束在此不生效）。

**建议修复**
- 签出入口对订单做**状态 CAS**：`UPDATE ... SET order_status='SHIPPED' WHERE id=? AND order_status='PACKED'`，
  校验 affected rows=1（或给 `SalesOutboundOrder` 加 `@Version`）→ 单赢者推进，直接消灭 TOCTOU。
- 批次扣减**校验 `updateById` 返回值**（0 行即失败/重试），或改条件扣减 `WHERE quantity>=? AND reserved_qty>=?`；
  @Version 已在，关键是别再忽略返回值。
- 订单级签出幂等由上面的状态 CAS 提供即可；计费侧唯一约束已具备。

**验证状态**：代码级 + DB 索引/注解实测确认（静态）。并发运行期复现（两线程同时 ship 同单/共享批次）待用量上限重置后补。

---

## BUG #5 —【库位管理】库位"重新生成"因软删+唯一键冲突必然失败 🔴中高 ✅已修复

**位置**
- `wms/model/entity/WmsLocation.java:70` `@TableLogic private Long deleted`（逻辑删除）
- `wms/mapper/WmsLocationMapper.java` `deleteByWarehouse`（重建前清旧）
- `wms/service/WmsLocationGenerator.java:69` 调 `deleteByWarehouse` 后 `saveBatch` 重建
- DB 唯一索引 `uk_wh_loc(warehouse_id, location_code)`（**不含 deleted 列**，实测）

**问题**
`WmsLocation` 配了 `@TableLogic`，`deleteByWarehouse` 的 `this.delete(...)` 实为
`UPDATE wms_location SET deleted=1 WHERE warehouse_id=?`——旧库位行**物理仍在表内**，只标 deleted=1，
仍占用 `uk_wh_loc(warehouse_id, location_code)`。随后 `saveBatch` 插入同 `location_code` 的新行
→ 撞唯一键 `DuplicateKeyException` → 整个事务回滚 → **重新生成必然失败**。

**后果（用户实测现象）**
只要仓库曾生成过库位（`location_generated=1`），再点"重新生成"就报错/无效：
- 有库存的仓（如 1号仓 70 件在 A1-01）先被 `countByWarehouse(库存)>0` 挡在前置守卫（LOCATION_OCCUPIED）；
- 库存为空、结构合法、有 STANDARD 分区的仓（如 2号 defregre）过了前置校验，**卡在删旧→插新的唯一键冲突**，
  且回滚后不留 deleted=1 痕迹，排查时看不到（活库 wms_location 全是 deleted=0，正因历次重建都在此步回滚）。

**✅ 已修复（2026-07-05）**：`WmsLocationMapper.deleteByWarehouse` 改为原生 `@Delete("DELETE FROM wms_location WHERE warehouse_id=#{warehouseId}")`
**物理删除**，绕过逻辑删除、真正释放 `location_code`，重建不再撞唯一键。库位系整体替换的基础设施、无软删审计诉求，
且删前已有"该仓无库存批次"守卫兜底（`wms_physical_inventory` 按 location_code 字符串关联、不引用库位 id），物理删安全。编译通过。

**顺带（BUG#1 同根加固）**：`WmsPhysicalInventoryMapper.countByWarehouse`（重建前的库存占用守卫）原缺 `quantity>0`，
出库清零后残留 0 量批次行会让"物理已空的仓"永久重建不了；已加 `.gt(quantity, 0)`，与 listOccupiedLocationCodes/仓储概览口径统一。

**验证状态**：代码级确认（静态）+ 活库索引/实体注解实测。运行期复现（2号仓点重新生成应成功）待重启后端后跑。

---

## 已静态核对、暂未发现问题的路径

- **入库收货/上架 `WmsInboundExecutionService`**：状态机 SUBMITTED→RECEIVED→COMPLETED 守卫完整；
  上架数量必须**恰好覆盖**收货数量(`coversExactly`)；库位存在性+独占+品质↔分区强校验；库存增量走
  `WmsPhysicalInventoryService.putaway`（写批次→同事务聚合刷新 wms_inventory→写 wms_stock_flow）。归属
  取入库单 `erp_tenant_id`。仅平台可作业(`assertPlatform`)。
- **出库下架 `OutboundPickingService.pick`**：两阶段（先 `SELECT FOR UPDATE` 锁批次算全量分配，全够才应用），
  任一 SKU 不足→整单 BACKORDER 不部分下架；FIFO(inbound_date→pick_order→id)；`reserved_qty += take`；
  重复下架有幂等守卫。
- **出库签出 `OutboundShippingService.ship`**：仅 PACKED 可签出（SHIPPED/COMPLETED 幂等拒绝）；据拣货分配逐批
  `quantity -= take` 且 `reserved_qty -= take`，`Assert newQty>=0` 防负库存；同事务聚合刷新；物流费幂等(bizId)。
- **自定义入库 `ManualInboundController`**：与采购入库共用 `wms_purchase_inbound_order`(source_type=MANUAL)，收货/上架走**同一套
  已加固的批次 putaway**（`WmsInboundExecutionService`，仅少了采购链的区域在途出账，属预期）→ 库存增量正确，无新问题。
- **自定义退货 `CustomReturnController`**：经 `PurchaseInboundService`/退货路径，回库上架继承 **BUG#2** 的退货质检 putaway 缺校验问题。
- **采购单→物流单→收货回写**：`receive()` 内 `increaseReceivedQuantity`+`recalculateArrivalStatus`(物流单)、
  `increaseReceivedQuantity`+`updateReceivingStatus`(采购单) 均在 SUBMITTED→RECEIVED 一次性事务内，受状态守卫幂等，
  累计回写正确。**遗留观察点(非 bug)**：收货(RECEIVED)与上架(COMPLETED)是两个独立事务/接口，收货后若不上架，
  采购单显示"已收货"但物理库存无批次——属两步作业的设计取舍，非缺陷。

---

## 基线一致性（只读核对，2026-07-03）
当前库无任何在途单据操作，批次 SSOT 与聚合快照**完全一致**、无 quantity=0 残留行：

| 源 | erp_tenant | wh | sku | avail | reserved | damaged |
|---|---|---|---|---|---|---|
| 批次汇总 wms_physical_inventory | 6 | 1 | E2ESKU | 70 | 0 | 0 |
| 聚合快照 wms_inventory | 6 | 1 | E2ESKU | 70 | 0 | 0 |

→ 三个 BUG 均为**潜伏型**：只有跑到「签出」(BUG#1)、「退货质检回库」(BUG#2)、「库存调整」(BUG#3)才显形。基线本身健康。

## 待运行期复现 / 继续核对（受本周用量上限影响，暂以静态确认为主）
- [ ] 运行期复现 BUG #1：驱动一轮 入库→上架→出库→签出，签出后查 `wms_physical_inventory`(quantity=0 残留) 与上架可选库位缺失。
- [ ] 运行期复现 BUG #3：对某 SKU 做 OTHER_IN 调增 → 立刻查出库下架是否仍缺货；再触发一次上架 → 查调整量是否被 refreshSnapshot 抹除。
- [x] 入库收货/上架 `WmsInboundExecutionService`（静态：状态机+数量覆盖+库位独占，OK）
- [x] 出库下架/签出 `OutboundPickingService`/`OutboundShippingService`（静态：两阶段锁+防负库存，OK；但见 BUG#1）
- [x] 退货质检 `ReturnQcService`（静态：见 BUG#2）
- [x] 库存调整 `AdjustmentService`/`InventoryPostingEngine`/`WmsInventoryAggregator`（静态：见 BUG#3）
- [x] 自定义入库 `ManualInboundController`（静态：复用批次 putaway，OK）
- [x] 自定义出库 `CustomOutboundFacade`（静态：预占走聚合过账，见 BUG#3 影响范围）
- [x] 自定义退货 `CustomReturnController`（静态：继承 BUG#2 退货 putaway）
- [x] 盘点 `StocktakeService`（静态：只改聚合不落批次，已并入 BUG#3 影响范围）
- [x] 采购单→物流单→收货回写（静态：状态守卫幂等、累计回写正确，OK）
- [ ] BUG#1/#3 运行期复现（待本周用量上限 Jul 4 17:00 PT 重置后跑）
- [ ] BUG#4 并发运行期复现（两线程同时对同单/共享批次 ship，观察批次少扣与副作用重复）
