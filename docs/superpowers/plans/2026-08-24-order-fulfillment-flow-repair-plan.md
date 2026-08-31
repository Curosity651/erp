# 订单履约链路收口修复实施计划

> **执行要求：** 实施时使用 `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans`，逐任务完成、测试和复核。

**目标：** 让平台订单只走“订单确认 → 订单级履约 → 下架 → 拣货 → 打包 → 签出”链路，保留新版人工出库，彻底阻止旧销售出库和旧自定义出库继续产生新业务数据。

**架构：** 以 `wms_fulfillment_order` 为唯一出库履约内核。ERP 平台订单与履约单一对一，物流产品和仓库在提交时形成快照；海外仓下架后按仓库生成拣货任务，派单失败可独立补偿，不重复调用平台接口。旧销售出库表仅暂时保留兼容，不再提供新增、编辑、确认和删除入口。

**技术栈：** Java 8、Spring Boot、MyBatis-Plus、MySQL 8、Vue 3、TypeScript、Ant Design Vue、Vitest、JUnit 5、Mockito。

**依据：** `docs/audits/2026-08-24-logistics-product-and-outbound-flow-audit.md`

## 全局业务约束

- 平台订单一单一包裹，一张 ERP 平台订单只能关联一张新版履约单。
- 平台订单不再创建销售出库单。
- 新版人工出库继续保留，并继续使用 `wms_fulfillment_order`，一张人工单对应一个包裹。
- 物流产品由 WMS 服务商维护；货主使用店铺默认值，确认发货时允许临时覆盖。
- 物流产品实际费用默认取产品单价，WMS 服务商可在签出前修改并填写原因。
- 货主在下架前可取消；下架后不能直接取消，异常订单按现有回退流程处理。
- 下架按仓库拆分拣货任务，不按货主拆分。
- 平台业务状态仍以平台同步结果为准，ERP 自己维护仓库履约状态。
- 本轮不删除旧表，不迁移历史财务记录，不重构平台订单同步模块。

---

### 任务一：关闭旧销售出库和旧自定义出库入口

**文件：**

- 新建：`erp-backend/sql/migration/V128__retire_legacy_outbound_entry.sql`
- 修改：`erp-frontend/src/router/constant-routes.ts`
- 修改：`erp-backend/admin/src/main/java/com/erp/admin/wms/controller/SalesOutboundController.java`
- 修改：`erp-backend/admin/src/main/java/com/erp/admin/wms/controller/CustomOutboundController.java`
- 新建测试：`erp-backend/admin/src/test/java/com/erp/admin/wms/LegacyOutboundRetirementTest.java`

**结果：** ERP 货主菜单只保留平台订单确认和“人工出库”；旧页面无法通过菜单或固定路由进入，旧写接口明确返回“旧流程已停用，请使用平台订单确认或人工出库”。

- [ ] 在迁移脚本中删除 ERP 货主角色对菜单 `160600` 及其子权限菜单的授权，并将旧销售出库菜单设为隐藏。
- [ ] 保留菜单 `162002`“人工出库”，但移除旧 `/wms/custom-outbound/form/...` 固定路由，避免绕过新人工出库页面。
- [ ] 将 `SalesOutboundController` 的新增、编辑、确认、取消、删除接口改为统一拒绝；分页和详情暂时只读保留。
- [ ] 将 `CustomOutboundController` 的写接口统一拒绝；`ManualFulfillmentController` 不受影响。
- [ ] 编写控制器契约测试，验证旧写接口不可用、新人工出库接口仍可用。
- [ ] 运行：`mvn -pl admin -Dtest=LegacyOutboundRetirementTest,ManualFulfillmentServiceTest test`。

### 任务二：建立新旧出库链路强互斥

**文件：**

- 修改：`erp-backend/admin/src/main/java/com/erp/admin/order/mapper/ErpOrderMapper.java`
- 修改：`erp-backend/admin/src/main/resources/mapper/order/ErpOrderMapper.xml`
- 修改：`erp-backend/admin/src/main/java/com/erp/admin/order/service/ErpOrderFulfillmentSubmissionService.java`
- 修改：`erp-backend/admin/src/main/java/com/erp/admin/wms/facade/SalesOutboundFacade.java`
- 修改测试：`erp-backend/admin/src/test/java/com/erp/admin/order/ErpOrderFulfillmentSubmissionTest.java`
- 新建测试：`erp-backend/admin/src/test/java/com/erp/admin/order/OutboundFlowMutualExclusionTest.java`

**接口：**

- 新增 `ErpOrderMapper.selectForFulfillmentSubmit(orderId, tenantId)`，使用 `FOR UPDATE` 锁定货主订单。
- 新履约允许条件：`fulfillment_order_id IS NULL`、`outbound_order_id IS NULL`、`outbound_status='NONE'`。
- 旧出库占用条件补充：`fulfillment_order_id IS NULL`。

- [ ] 先写测试：已有旧 `outbound_order_id` 的订单提交新版履约必须失败。
- [ ] 先写测试：已有 `fulfillment_order_id` 的订单不能进入旧销售出库占用。
- [ ] 先写并发测试：两个新版提交只能创建一张履约单并只预占一次库存。
- [ ] 在新版提交事务开始时按货主和订单 ID 加行锁，再检查新旧关联字段和 FBS 类型。
- [ ] 修改旧待选订单 SQL，增加 `fulfillment_order_id IS NULL` 和 `outbound_order_id IS NULL`。
- [ ] 修改旧 `allocateForOutboundWithVersion` SQL，增加 `fulfillment_order_id IS NULL`，防止绕过列表直接调用接口。
- [ ] 保留 `wms_fulfillment_order` 的 `(source_type, source_order_id, deleted)` 唯一约束作为第二层幂等保证。
- [ ] 运行：`mvn -pl admin -Dtest=ErpOrderFulfillmentSubmissionTest,OutboundFlowMutualExclusionTest,FulfillmentOrderServiceTest test`。

### 任务三：修正店铺默认仓库并支持确认时临时覆盖

**文件：**

- 修改：`erp-backend/sql/migration/V128__retire_legacy_outbound_entry.sql`
- 修改：`erp-frontend/src/views/order/components/OrderConfirmModal.vue`
- 修改：`erp-frontend/src/views/order/hooks/useOrderConfirm.ts`
- 修改：`erp-frontend/src/views/order/ozon-order/OzonOrderPage.vue`
- 修改：`erp-frontend/src/views/order/wb-order/WbOrderPage.vue`
- 修改：`erp-frontend/src/views/order/yd-order/YdOrderPage.vue`
- 修改测试：`erp-backend/admin/src/test/java/com/erp/admin/shop/ShopDefaultLogisticsProductTest.java`
- 新建前端测试：`erp-frontend/src/views/order/hooks/fulfillment-confirm.vitest.ts`

**结果：** 店铺默认仓库正常时无需重复选择；确认发货弹窗允许临时选择其他有权限的仓库。物流产品继续遵循同样的“店铺默认 + 本次覆盖”规则。

- [ ] 在数据迁移中将当前 JHIN 店铺 `6001-6004` 的默认 WMS 仓库设置为仓库 `53`，更新前先校验仓库存在、启用且 JHIN 有使用权限。
- [ ] 在确认弹窗增加“WMS 仓库”可清空选择框，提示“留空使用每个店铺默认仓库”。
- [ ] 打开弹窗时加载当前货主有权使用的仓库，不展示平台仓库和无权仓库。
- [ ] 在 `useOrderConfirm` 中增加 `wmsWarehouseId`，提交时传给 `submitOrderFulfillment`。
- [ ] 批量确认时，手工选择的仓库和物流产品应用到全部可确认订单；留空时每单读取各自店铺默认值。
- [ ] 店铺列表增加配置完整性展示：缺默认仓库或默认物流产品时显示“配置不完整”。
- [ ] 运行后端店铺配置测试和前端 Vitest，验证默认值、覆盖值和空配置提示。

### 任务四：增加下架后派单补偿机制

**文件：**

- 修改：`erp-backend/sql/migration/V128__retire_legacy_outbound_entry.sql`
- 修改：`erp-backend/admin/src/main/java/com/erp/admin/wms/model/entity/WmsFulfillmentOrder.java`
- 修改：`erp-backend/admin/src/main/java/com/erp/admin/wms/service/FulfillmentDispatchService.java`
- 修改：`erp-backend/admin/src/main/java/com/erp/admin/wms/service/FulfillmentPickingService.java`
- 修改：`erp-backend/admin/src/main/java/com/erp/admin/wms/controller/FulfillmentPickingController.java`
- 修改：`erp-frontend/src/api/wms/fulfillment/index.ts`
- 修改：`erp-frontend/src/api/wms/fulfillment/types.ts`
- 修改：`erp-frontend/src/views/platform/fulfillment-shelf/FulfillmentShelfPage.vue`
- 修改测试：`erp-backend/admin/src/test/java/com/erp/admin/wms/FulfillmentDispatchServiceTest.java`

**数据字段：**

- `dispatch_status`：`PENDING/SUCCEEDED/FAILED`。
- `dispatch_error`：最近一次派单失败原因。

**接口：**

- 新增 `POST /wms/fulfillment-picking/redispatch`，请求体为履约订单 ID 列表。
- 只允许 `WAITING_PICK` 且尚未关联拣货任务的订单重新派单。

- [ ] 写测试：平台下架成功、任务创建失败时，订单保持 `WAITING_PICK` 并标记 `FAILED`。
- [ ] 写测试：重新派单不能再次调用平台 `accept`。
- [ ] 写测试：已有任务的订单不能重复派单。
- [ ] 自动派单成功后写入 `SUCCEEDED` 并清空错误；失败时写入 `FAILED` 和错误原因。
- [ ] 订单下架页面继续展示 `WAITING_PICK`，状态显示“待派单”或“派单失败”，对应行提供“重新派单”。
- [ ] 重新派单仍按仓库分组，一个仓库生成一张任务，不按货主拆分。
- [ ] 任务订单关联表保持唯一约束，作为防重复派单的数据库兜底。
- [ ] 运行：`mvn -pl admin -Dtest=FulfillmentDispatchServiceTest,FulfillmentPickingServiceTest test`。

### 任务五：补齐平台订单履约收件信息快照

**文件：**

- 新建：`erp-backend/admin/src/main/java/com/erp/admin/order/service/FulfillmentRecipientSnapshotService.java`
- 修改：`erp-backend/admin/src/main/java/com/erp/admin/order/service/ErpOrderFulfillmentSubmissionService.java`
- 新建测试：`erp-backend/admin/src/test/java/com/erp/admin/order/FulfillmentRecipientSnapshotServiceTest.java`

**结果：** 能从平台标准字段或原始 JSON 获得的收件人、电话和地址写入履约快照；平台未提供时保持空值并由前端显示“平台未提供”。

- [ ] 为 Ozon、WB、Yandex 各准备一份脱敏 `raw_json` 测试样本。
- [ ] 实现按平台解析收件信息的独立服务，不把 JSON 解析堆进提交服务。
- [ ] 在 `buildCommand` 中设置 `recipientName/recipientPhone/recipientAddress`。
- [ ] 日志和异常信息不得打印完整手机号、地址或原始 JSON。
- [ ] 修改海外仓列表空值文案为“平台未提供”。
- [ ] 运行：`mvn -pl admin -Dtest=FulfillmentRecipientSnapshotServiceTest,ErpOrderFulfillmentSubmissionTest test`。

### 任务六：清理当前数据库旧链路孤儿数据

**文件：**

- 修改：`erp-backend/sql/migration/V128__retire_legacy_outbound_entry.sql`
- 新建：`docs/operations/order-fulfillment-v128-release-checklist.md`

- [ ] 迁移前查询并记录所有 `outbound_order_id` 非空但旧销售出库单不存在的订单。
- [ ] 当前确认的 42 条孤儿记录仅清空 `outbound_order_id`；保持 `outbound_status='NONE'`，不修改平台状态、金额和订单商品。
- [ ] 不删除任何 `erp_order`、店铺、SKU、SKU 映射和物流产品数据。
- [ ] 迁移后验证：孤儿引用为 0，新旧双关联为 0，所有启用店铺配置完整。
- [ ] 在发布清单中记录迁移前后 SQL、回滚 SQL 和验证结果。

### 任务七：端到端回归与发布验收

**文件：**

- 修改：`docs/operations/order-fulfillment-v128-release-checklist.md`
- 新建测试：`erp-backend/admin/src/test/java/com/erp/admin/wms/OrderFulfillmentFlowIntegrationTest.java`

- [ ] 后端运行全部履约专项测试，预期 0 失败。
- [ ] 前端运行订单确认、订单下架、拣货任务和出库作业相关 Vitest，预期 0 失败。
- [ ] 执行 `mvn -pl admin test` 和前端生产构建。
- [ ] 分别用 Ozon、WB、Yandex 各一条真实 FBS 订单验证店铺默认物流产品和临时覆盖物流产品。
- [ ] 验证确认发货后只生成 `wms_fulfillment_order`，不生成 `wms_sales_outbound_order`。
- [ ] 验证下架前取消释放库存，下架后禁止直接取消。
- [ ] 验证同仓库多货主订单生成一张拣货任务，不同仓库拆成多张任务。
- [ ] 验证逐单扫描与整单简化作业互斥，面单、打包和批量签出正常。
- [ ] 验证签出后库存只扣减一次，海外仓出库费与 WMS 物流产品费分别记账。
- [ ] 验证 ERP 内部仓库状态推进到 `SHIPPED`，平台状态仍由平台同步更新。
- [ ] 验证 ERP 货主菜单中不再出现“销售出库单”，但“人工出库”可正常创建和提交。

## 上线顺序与回滚

1. 备份 `erp_order`、`shop`、`sys_menu`、`sys_role_menu` 和全部履约表。
2. 先发布后端互斥与派单补偿，再执行 V128 数据迁移，最后发布前端菜单和确认弹窗。
3. 若发布失败，先回滚前端；后端互斥校验可继续保留，不会破坏旧数据。
4. V128 回滚只恢复菜单授权、店铺原默认仓库和 42 条孤儿引用备份，不回滚已经产生的新版履约业务。
5. 新版稳定运行一个完整账期后，再单独制定“删除旧销售出库代码和表”的第二阶段计划。
