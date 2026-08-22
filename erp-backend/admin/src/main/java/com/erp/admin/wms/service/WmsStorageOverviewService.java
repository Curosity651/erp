package com.erp.admin.wms.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

import com.erp.admin.common.tenant.WmsTenantContext;
import com.erp.admin.wms.mapper.WmsStorageOverviewMapper;
import com.erp.admin.wms.model.vo.StorageOverviewVO;
import com.erp.admin.wms.model.vo.StorageOverviewVO.OwnerRow;
import com.erp.admin.wms.model.vo.StorageOverviewVO.RackRow;
import com.erp.admin.wms.model.vo.StorageOverviewVO.WarehouseRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 服务商「仓储概览」只读服务。作用域一律取自 {@link WmsTenantContext}（登录服务商自身），不接受客户端传租户。
 *
 * @author erp
 */
@Service
@RequiredArgsConstructor
public class WmsStorageOverviewService {

	private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

	/** 临期阈值（天），与货架分配一致。 */
	private static final int EXPIRING_DAYS = 30;

	private final WmsStorageOverviewMapper mapper;

	/** 汇总 + 仓库列表。 */
	public StorageOverviewVO summary() {
		Long wmsTenantId = WmsTenantContext.getCurrentWmsTenant();
		LocalDate today = LocalDate.now(BUSINESS_ZONE);
		StorageOverviewVO vo = new StorageOverviewVO();
		if (wmsTenantId == null) {
			vo.setWarehouseCount(0);
			vo.setRackCount(0);
			vo.setAllocatedLocations(0L);
			vo.setOccupiedLocations(0L);
			vo.setOnHandQty(0L);
			vo.setOccupancyRate(BigDecimal.ZERO);
			vo.setWarehouses(Collections.emptyList());
			return vo;
		}
		List<WarehouseRow> rows = mapper.listWarehouses(wmsTenantId, today);
		long alloc = 0;
		long occ = 0;
		long qty = 0;
		int racks = 0;
		for (WarehouseRow r : rows) {
			r.setOccupancyRate(rate(r.getOccupiedLocations(), nz(r.getAllocatedLocations())));
			alloc += nz(r.getAllocatedLocations());
			occ += nz(r.getOccupiedLocations());
			qty += nz(r.getOnHandQty());
			racks += r.getRackCount() == null ? 0 : r.getRackCount();
		}
		vo.setWarehouseCount(rows.size());
		vo.setRackCount(racks);
		vo.setAllocatedLocations(alloc);
		vo.setOccupiedLocations(occ);
		vo.setOnHandQty(qty);
		vo.setOccupancyRate(rate(occ, alloc));
		vo.setWarehouses(rows);
		return vo;
	}

	/** 某仓的货架维度。 */
	public List<RackRow> racks(Long warehouseId) {
		Long wmsTenantId = WmsTenantContext.getCurrentWmsTenant();
		if (wmsTenantId == null) {
			return Collections.emptyList();
		}
		LocalDate today = LocalDate.now(BUSINESS_ZONE);
		List<RackRow> rows = mapper.listRacks(wmsTenantId, warehouseId, today);
		for (RackRow r : rows) {
			r.setOccupancyRate(rate(r.getOccupiedCount(), nz(r.getLocationCount())));
			r.setExpiringSoon(r.getEffectiveTo() != null && !r.getEffectiveTo().isAfter(today.plusDays(EXPIRING_DAYS)));
		}
		return rows;
	}

	/** 某仓的货主维度（含占本仓比例）。 */
	public List<OwnerRow> owners(Long warehouseId) {
		Long wmsTenantId = WmsTenantContext.getCurrentWmsTenant();
		if (wmsTenantId == null) {
			return Collections.emptyList();
		}
		LocalDate today = LocalDate.now(BUSINESS_ZONE);
		List<OwnerRow> rows = mapper.listOwners(wmsTenantId, warehouseId, today);
		long total = 0;
		for (OwnerRow r : rows) {
			total += nz(r.getOnHandQty());
		}
		for (OwnerRow r : rows) {
			r.setSharePct(rate(r.getOnHandQty(), total));
		}
		return rows;
	}

	private static long nz(Long v) {
		return v == null ? 0L : v;
	}

	/** 百分比（一位小数）；分母 &lt;= 0 时为 0。 */
	private static BigDecimal rate(Long numerator, long denominator) {
		long n = nz(numerator);
		if (denominator <= 0) {
			return BigDecimal.ZERO;
		}
		return BigDecimal.valueOf(n * 100).divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP);
	}

}
