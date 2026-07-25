package com.erp.admin.wms.service;

import java.util.ArrayList;
import java.util.List;

import com.erp.admin.wms.enums.WmsResultCode;
import com.erp.admin.wms.model.entity.Warehouse;
import com.erp.admin.wms.model.entity.WmsLocation;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.core.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 库位批量生成（C1，《完整技术设计方案》§4.4.1）。
 *
 * <p>按 仓库结构参数「排×列」笛卡尔积生成库位，编码 {@code {rackNo}-{补零列}}（如 A1-03）；
 * 要求已建默认 STANDARD 分区；整体事务。
 *
 * <p>支持<b>重新生成</b>：若已生成（{@code location_generated=1}），先校验该仓无货物落位
 * （{@code wms_physical_inventory} 无该仓批次），通过则清空旧库位再按新结构重建；有占用则拒绝。
 *
 * @author erp
 */
@Service
@RequiredArgsConstructor
public class WmsLocationGenerator {

	private final WarehouseService warehouseService;

	private final WmsZoneService wmsZoneService;

	private final WmsLocationService wmsLocationService;

	private final WmsStructureLockService wmsStructureLockService;

	private final WmsPalletService palletService;

	/**
	 * 为仓库批量生成（或按新结构重新生成）库位。
	 * @param warehouseId 仓库ID
	 * @return 生成的库位数量
	 */
	@Transactional(rollbackFor = Exception.class)
	public int generateLocations(Long warehouseId) {
		Warehouse wh = warehouseService.validateOperableOwnWarehouse(warehouseId);
		int rows = nz(wh.getRackRows());
		int columns = nz(wh.getRackColumns());
		if (rows <= 0 || columns <= 0 || rows > 100 || columns > 100
				|| (long) rows * columns > 5000L) {
			throw new BusinessException(WmsResultCode.INVALID_RACK_STRUCTURE.getCode(),
					WmsResultCode.INVALID_RACK_STRUCTURE.getMessage());
		}
		Long zoneId = wmsZoneService.findDefaultStandardZoneId(warehouseId);
		if (zoneId == null) {
			throw new BusinessException(WmsResultCode.STANDARD_ZONE_REQUIRED.getCode(),
					WmsResultCode.STANDARD_ZONE_REQUIRED.getMessage());
		}

		// 重新生成：有货占用或已分配服务商则拒绝（统一守卫 A+B），否则清空旧库位重建
		boolean regenerate = wh.getLocationGenerated() != null && wh.getLocationGenerated() == 1;
		if (regenerate) {
			wmsStructureLockService.assertEditable(warehouseId);
			palletService.deletePhysicalSlots(warehouseId);
			wmsLocationService.deletePhysicalByWarehouse(warehouseId);
		}

		int pad = wh.getCodePadWidth() == null ? 2 : wh.getCodePadWidth();
		String prefix = wh.getRackNoPrefix() == null ? "" : wh.getRackNoPrefix();
		String locationType = wh.getDefaultLocationType();

		List<WmsLocation> list = new ArrayList<>(rows * columns);
		for (int r = 1; r <= rows; r++) {
			String rackNo = prefix + r;
			for (int c = 1; c <= columns; c++) {
				WmsLocation loc = new WmsLocation();
				loc.setWarehouseId(warehouseId);
				loc.setZoneId(zoneId);
				loc.setRackNo(rackNo);
				loc.setColumnNo(c);
				loc.setLocationCode(buildLocationCode(prefix, r, c, pad));
				loc.setLocationType(locationType);
				loc.setPickType("PICK");
				loc.setIsVirtual(0);
				list.add(loc);
			}
		}
		wmsLocationService.saveBatch(list);
		palletService.ensureSlots(warehouseId);

		// 置幂等标记（重新生成时已为1，重复置无副作用）
		Warehouse mark = new Warehouse();
		mark.setId(warehouseId);
		mark.setLocationGenerated(1);
		warehouseService.updateById(mark);

		return list.size();
	}

	/**
	 * 拼接库位编码 {@code {prefix}{row}-{补零列}}（纯函数，便于单测）。
	 * @param prefix 排号前缀
	 * @param row 排序号(1开始)
	 * @param column 列号
	 * @param pad 补零位宽
	 * @return 库位编码，如 A1-03
	 */
	public static String buildLocationCode(String prefix, int row, int column, int pad) {
		String safePrefix = StringUtils.hasText(prefix) ? prefix : "";
		String fmt = "%0" + pad + "d";
		return safePrefix + row + "-" + String.format(fmt, column);
	}

	/**
	 * 预计生成库位数 = 排×列（纯函数，便于单测）。
	 */
	public static int countLocations(int rows, int columns) {
		return rows * columns;
	}

	private static int nz(Integer v) {
		return v == null ? 0 : v;
	}

}
