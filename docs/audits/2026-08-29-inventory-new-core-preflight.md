# 库存管理新内核切换预检

日期：2026-08-29

## Git 基线

- 当前分支：`codex/logical-location-fulfillment`
- 工作区已有大量其他业务改动，本任务只修改库存六功能相关文件。
- 禁止使用 `reset`、`checkout --` 或覆盖其他协作者的未提交内容。

## 数据库基线

| 数据源 | 行数 | 实物数量 | 预占数量 | 可用数量 |
| --- | ---: | ---: | ---: | ---: |
| `wms_location_inventory` | 6 | 24 | 4 | 20 |
| `wms_inventory` | 0 | 0 | 0 | 0 |
| `wms_stock_flow` | 0 | - | - | - |
| `wms_stock_posting` | 0 | - | - | - |

`wms_fbo_inventory_snapshot` 当前货主 6 共 46 行，数量 12，最近同步时间为
`2026-08-26 15:00:00`。

## 迁移编号

仓库中存在两个尚未整理的 `V135` 文件。库存事件账本不得继续占用 `V135`，
实施时使用解决既有冲突后的下一个唯一版本。

## 切换原则

- `wms_location_inventory` 是仓内库存唯一事实源。
- 不向 `wms_inventory` 建立双写或定时回填。
- 新库存变化写入事件账本，旧流水和旧操作单仅保留历史回查。
- 可用数量固定为 `GOOD.quantity - GOOD.reserved_quantity`，下游不得重复扣减。
