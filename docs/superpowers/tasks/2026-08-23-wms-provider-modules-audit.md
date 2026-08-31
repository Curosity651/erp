# WMS 服务商模块审计基线

## 审计范围

- 货主管理
- 物流产品管理
- 财务收入
- 财务支出
- 仓储概览
- 运营数据分析
- 与上述模块直接关联的店铺默认物流产品、出库计费和月度账单

## 已确认的业务口径

1. 物流产品由 WMS 服务商维护，所属 ERP 货主可见并使用。
2. 物流产品费用是默认单次费用，签出时允许记录实际费用。
3. ERP 货主向 WMS 服务商支付物流产品费用；海外仓作业费用属于平台向 WMS 服务商收取的费用。
4. 财务金额按原币种分别展示，不做自动汇率换算，不允许跨币种直接相加或计算净收益。
5. 公共暂存库位允许多个 WMS 服务商使用，但库存归属始终由 `wms_tenant_id + erp_tenant_id` 决定。
6. 服务商仓储概览可以统计自己名下货主位于公共暂存库位的库存，但不向服务商或货主暴露公共暂存库位的具体物理名称。
7. 已被店铺默认配置、待发货单或未完成履约单引用的物流产品不得删除；应优先停用。
8. 服务商看板与运营数据分析功能重复，只保留运营数据分析。

## 已完成前置项

`V126__remove_duplicate_provider_dashboard.sql` 已完成以下处理：

- 删除活动菜单 `180500 服务商看板`；
- 将权限节点 `180501` 移到 `900400 运营数据分析`；
- 清除角色对 `180500` 的映射并保留 `180501` 权限；
- 已在当前数据库执行并通过 `OperatorDashboardMenuMigrationTest`。

后续执行计划不得重新创建服务商看板，也不得新增第二套运营统计接口。

## 审计发现

### P1：财务收入跨币种错误汇总

`wms_client_billing_record` 支持 RUB、CNY、USD，但收入汇总和运营分析按月份或物流产品直接合计 `amount`，没有把 `currency` 纳入分组。前端同时把所有金额固定显示为卢布，导致不同币种被错误相加和错误标注。

影响文件：

- `erp-backend/admin/src/main/resources/mapper/wms/OperatorFinanceMapper.xml`
- `erp-backend/admin/src/main/resources/mapper/wms/OperatorDashboardMapper.xml`
- `erp-backend/admin/src/main/java/com/erp/admin/wms/service/OperatorFinanceService.java`
- `erp-backend/admin/src/main/java/com/erp/admin/wms/service/OperatorDashboardService.java`
- `erp-frontend/src/views/wms/finance-income/index.vue`
- `erp-frontend/src/views/dashboard/operator/index.vue`

### P1：服务商支出缺少币种字段并混合账单状态

平台仓储作业费当前按 CNY 计价，但 `wms_monthly_bill` 没有币种字段，前端却固定显示卢布。运营分析还把 DRAFT、DISPUTED、CONFIRMED、PAID 全部计入同一个“支出”，无法区分草稿估算、争议、已确认应付和已付款。

### P1：物流产品删除缺少引用保护

删除产品时只执行逻辑删除，没有检查：

- 店铺 `default_logistics_product_id`；
- 待下架、拣货中、待签出的履约单；
- 尚未结束的旧销售出库业务。

删除后，使用该产品作为默认值的店铺会在订单确认时出现“物流产品不可用”。历史计费记录已有快照，不需要阻止历史记录查询，但活动业务必须阻止删除。

### P1：仓储概览把“货架分配范围”错误当成“库存归属范围”

当前查询只统计落在服务商有效货架分配中的库存，可能漏掉该服务商货主存放在公共暂存库位的库存；同时货主汇总没有明确以 `pi.wms_tenant_id` 收窄，在货架重新分配或历史数据残留时存在统计其他服务商库存的风险。

正确口径：

- 容量、分配排数：按有效货架分配统计；
- 库存件数、SKU 数、货主占用：按库存自身 `wms_tenant_id` 统计；
- 公共暂存库位库存计入数量，但不计入已分配物理库位数量。

### P2：物流产品编码前后端规则不一致

前端提示产品编码“选填”，后端服务强制非空，DTO 又没有 `@NotBlank`。用户会在提交后才收到错误。

统一规则：产品编码必填，保存前去除首尾空格并转为大写，同一服务商内唯一。

### P2：物流产品新建和编辑共用新增权限

同一个 POST 接口同时执行新建和编辑，但只校验 `wms:logistics-product:add`。结果是只有编辑权限的用户不能编辑，而只有新增权限的用户可以通过携带 ID 编辑。

统一规则：POST 仅新建，PUT `/{id}` 仅编辑，并分别校验 add/edit 权限。

### P2：财务收入明细仍只关联旧销售出库单

新履约链路写入 `fulfillment_order_id`，收入明细仍只关联 `outbound_order_id -> wms_sales_outbound_order`，导致新履约收入的业务单号为空。产品名称和单价也从当前产品表补齐，无法准确还原历史计费时的快照。

正确口径：

- 新履约优先显示履约单号和平台订单号；
- 旧数据回退显示旧销售出库单号；
- 产品名称、说明、金额、币种使用计费记录快照和实际记账值。

### P2：账期使用 JVM 默认时区

物流费用使用 `YearMonth.now()` 生成账期，而运营出库统计按莫斯科时间计算。北京时间和莫斯科时间跨月的三个小时内，收入账期与出库月份可能不一致。

统一规则：俄罗斯仓业务账期统一使用 `Europe/Moscow`，已存在 `shipped_time` 时优先从签出时间转换后生成账期。

### 正常项：货主管理

货主管理已有服务商身份隔离、管理员写权限和停用后会话拦截。普通操作未发现必须修改的功能错误。后续只补回归测试和输入校验，不调整现有业务流程。

## 当前数据库观察

- 当前服务商 `WMS_FIRST` 下存在货主和物流产品。
- 多个店铺引用同一默认物流产品。
- 当前履约计费记录已使用新 `fulfillment_order_id`。
- 当前月度账单数据较少，币种和状态问题在真实计费增长后会直接暴露，不能因为当前页面数据少而忽略。

