package com.erp.admin.wms.service;

import java.util.List;

import com.erp.admin.wms.enums.WmsResultCode;
import com.erp.admin.wms.mapper.InventoryMapper;
import com.erp.admin.wms.mapper.WmsPhysicalInventoryMapper;
import com.erp.admin.wms.model.entity.Inventory;
import com.erp.admin.wms.model.entity.WmsPhysicalInventory;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.core.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 库存聚合刷新器（D1·方案②）。
 *
 * <p>
 * 把批次级 SSOT（wms_physical_inventory）按聚合键（货架归属 × 货物归属 × 仓 × SKU）重算成桶快照，
 * upsert 到降级后的 wms_inventory（乐观锁）。规则见《现状-设计差异对照表》§2.3：
 * </p>
 * <ul>
 * <li>available = Σ(quantity - reserved_qty)，条件 quality=GOOD AND allocatable=1；</li>
 * <li>damaged = Σ quantity，条件 quality=DAMAGED；</li>
 * <li>reserved = Σ reserved_qty；in_transit 维持现状（由在途逻辑维护）。</li>
 * </ul>
 *
 * @author erp
 */
@Service
@RequiredArgsConstructor
public class WmsInventoryAggregator {

	private static final Logger log = LoggerFactory.getLogger(WmsInventoryAggregator.class);

	/** 乐观锁刷新最大重试次数。 */
	private static final int MAX_RETRY = 3;

	private final WmsPhysicalInventoryMapper physicalInventoryMapper;

	private final InventoryMapper inventoryMapper;

	/** 三桶聚合结果（纯值对象）。 */
	public static final class Buckets {

		public final int available;

		public final int reserved;

		public final int damaged;

		public Buckets(int available, int reserved, int damaged) {
			this.available = available;
			this.reserved = reserved;
			this.damaged = damaged;
		}

	}

	/**
	 * 纯聚合函数（无副作用，可单测）：把一组批次折算为三桶。
	 * @param batches 同一聚合键下的批次列表
	 * @return 三桶聚合
	 */
	public static Buckets aggregate(List<WmsPhysicalInventory> batches) {
		int available = 0;
		int reserved = 0;
		int damaged = 0;
		if (batches != null) {
			for (WmsPhysicalInventory b : batches) {
				int qty = nz(b.getQuantity());
				int res = nz(b.getReservedQty());
				// M-7：大小写不敏感兜底历史脏数据；不改 allocatable 逻辑（良品但不可分配的合法状态仍不误算 damaged）
				if ("DAMAGED".equalsIgnoreCase(b.getQuality())) {
					damaged += qty;
				}
				else {
					// GOOD
					reserved += res;
					if (b.getAllocatable() != null && b.getAllocatable() == 1) {
						available += (qty - res);
					}
				}
			}
		}
		return new Buckets(available, reserved, damaged);
	}

	private static int nz(Integer v) {
		return v == null ? 0 : v;
	}

	/**
	 * L-1：聚合直写快照绕过过账引擎的不可负校验，此处对可用量补下限守卫。
	 * <p>聚合出负（多为某批次 reserved_qty &gt; quantity 的脏数据/竞态残留）时截 0 并打 WARN——
	 * 读侧重算不因脏数据阻断快照刷新；过量出库/预占的硬拦截由过账引擎侧 {@code AVAILABLE.allowNegative=false} 负责。
	 */
	private int clampAvailable(Buckets b, Long erpTenantId, Long warehouseId, String skuCode) {
		if (b.available < 0) {
			log.warn("库存聚合出负可用量(已截0)：erp={} wh={} sku={} available={} reserved={} damaged={}，疑似批次 reserved_qty>quantity 脏数据",
					erpTenantId, warehouseId, skuCode, b.available, b.reserved, b.damaged);
			return 0;
		}
		return b.available;
	}

	/**
	 * 同事务刷新聚合快照：按聚合键重算并 upsert wms_inventory（乐观锁，冲突重试）。
	 * <p>M-1：快照按 (货主×仓×SKU) 收敛、wms_tenant_id 恒 0（与过账引擎 getOrCreate 建行口径统一），
	 * 聚合该货主该 SKU 跨全部货架的批次。入参 {@code wmsTenantId} 保留仅为兼容调用方，不再参与快照行键，
	 * 避免两写者用不同 wms 维把同 (erp,wh,sku) 分裂成两行。
	 * @return 刷新后的三桶
	 */
	@Transactional(rollbackFor = Exception.class)
	public Buckets refreshSnapshot(Long wmsTenantId, Long erpTenantId, Long warehouseId, String skuCode) {
		List<WmsPhysicalInventory> batches = physicalInventoryMapper.listForAggregateByErp(erpTenantId,
				warehouseId, skuCode);
		Buckets b = aggregate(batches);

		Inventory inv = inventoryMapper.selectByErpWarehouseSku(erpTenantId, warehouseId, skuCode);
		if (inv == null) {
			inv = new Inventory();
			inv.setWmsTenantId(0L);
			inv.setErpTenantId(erpTenantId);
			inv.setWarehouseId(warehouseId);
			inv.setSkuCode(skuCode);
			inv.setAvailableQuantity(clampAvailable(b, erpTenantId, warehouseId, skuCode));
			inv.setReservedQuantity(b.reserved);
			inv.setInTransitQuantity(0);
			inv.setDamagedQuantity(b.damaged);
			inventoryMapper.insert(inv);
			return b;
		}

		for (int i = 0; i < MAX_RETRY; i++) {
			int rows = inventoryMapper.updateWithVersion(inv.getId(),
					clampAvailable(b, erpTenantId, warehouseId, skuCode), b.reserved,
					nz(inv.getInTransitQuantity()), b.damaged, inv.getVersion());
			if (rows > 0) {
				return b;
			}
			// 版本冲突：重读快照行 version 的同时，重新聚合最新批次重算三桶（H-3）。
			// 否则 RC 隔离下会用陈旧聚合覆盖、漏并发已提交批次 → 快照永久性偏低（丢更新）。
			inv = inventoryMapper.selectById(inv.getId());
			if (inv == null) {
				break;
			}
			b = aggregate(physicalInventoryMapper.listForAggregateByErp(erpTenantId, warehouseId, skuCode));
		}
		throw new BusinessException(WmsResultCode.INVENTORY_VERSION_CONFLICT.getCode(),
				WmsResultCode.INVENTORY_VERSION_CONFLICT.getMessage());
	}

}
