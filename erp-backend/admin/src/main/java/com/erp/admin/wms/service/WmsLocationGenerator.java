package com.erp.admin.wms.service;

import java.util.ArrayList;
import java.util.List;

import com.erp.admin.wms.enums.WmsResultCode;
import com.erp.admin.wms.mapper.WarehouseMapper;
import com.erp.admin.wms.model.dto.WarehouseStructureDTO;
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

	private final WarehouseMapper warehouseMapper;

	private final WmsZoneService wmsZoneService;

	private final WmsLocationService wmsLocationService;

	private final WmsStructureLockService wmsStructureLockService;

	private final WmsPalletService palletService;

	public int countPhysicalSlots(Long warehouseId) {
		return palletService.countPhysicalSlots(warehouseId);
	}

	/**
	 * Saves configuration and rebuilds physical locations in one transaction.
	 */
	@Transactional(rollbackFor = Exception.class)
	public int saveStructureAndGenerate(WarehouseStructureDTO dto) {
		warehouseService.validateOperableOwnWarehouse(dto.getId());
		Warehouse current = warehouseMapper.selectByIdForUpdate(dto.getId());
		if (current == null) {
			throw new IllegalStateException("仓库不存在或已被删除");
		}
		validateDimensions(dto.getRackRows(), dto.getRackColumns());
		wmsStructureLockService.assertEditable(dto.getId());

		Warehouse update = new Warehouse();
		update.setId(dto.getId());
		update.setRackRows(dto.getRackRows());
		update.setRackColumns(dto.getRackColumns());
		update.setRackNoPrefix(dto.getRackNoPrefix() == null ? "" : dto.getRackNoPrefix());
		update.setCodePadWidth(dto.getCodePadWidth());
		update.setDefaultLocationType(dto.getDefaultLocationType());
		update.setPalletLevels(dto.getPalletLevels() == null ? 6 : Math.max(1, Math.min(dto.getPalletLevels(), 12)));
		update.setPalletPositionsPerLevel(dto.getPalletPositionsPerLevel() == null
				? 3 : Math.max(1, Math.min(dto.getPalletPositionsPerLevel(), 9)));
		update.setMaxSkuKindsPerPallet(dto.getMaxSkuKindsPerPallet() == null ? 4 : dto.getMaxSkuKindsPerPallet());
		update.setAllowCrossOwnerMix(dto.getAllowCrossOwnerMix() == null ? 0 : dto.getAllowCrossOwnerMix());
		update.setDefaultPalletLengthMm(dto.getDefaultPalletLengthMm());
		update.setDefaultPalletWidthMm(dto.getDefaultPalletWidthMm());
		update.setDefaultPalletHeightMm(dto.getDefaultPalletHeightMm());
		update.setDefaultPalletMaxWeightKg(dto.getDefaultPalletMaxWeightKg());
		update.setDefaultPalletUtilization(dto.getDefaultPalletUtilization());
		if (!warehouseService.updateById(update)) {
			throw new IllegalStateException("仓库结构保存失败，请刷新后重试");
		}

		current.setRackRows(update.getRackRows());
		current.setRackColumns(update.getRackColumns());
		current.setRackNoPrefix(update.getRackNoPrefix());
		current.setCodePadWidth(update.getCodePadWidth());
		current.setDefaultLocationType(update.getDefaultLocationType());
		current.setPalletLevels(update.getPalletLevels());
		current.setPalletPositionsPerLevel(update.getPalletPositionsPerLevel());
		return generateLocations(current);
	}

	/**
	 * 为仓库批量生成（或按新结构重新生成）库位。
	 * @param warehouseId 仓库ID
	 * @return 生成的库位数量
	 */
	@Transactional(rollbackFor = Exception.class)
	public int generateLocations(Long warehouseId) {
		warehouseService.validateOperableOwnWarehouse(warehouseId);
		Warehouse wh = warehouseMapper.selectByIdForUpdate(warehouseId);
		if (wh == null) {
			throw new IllegalStateException("仓库不存在或已被删除");
		}
		return generateLocations(wh);
	}

	private int generateLocations(Warehouse wh) {
		Long warehouseId = wh.getId();
		int rows = nz(wh.getRackRows());
		int columns = nz(wh.getRackColumns());
		validateDimensions(rows, columns);
		Long zoneId = wmsZoneService.findDefaultStandardZoneId(warehouseId);
		if (zoneId == null) {
			throw new BusinessException(WmsResultCode.STANDARD_ZONE_REQUIRED.getCode(),
					WmsResultCode.STANDARD_ZONE_REQUIRED.getMessage());
		}

		// 重新生成：有货占用或已分配服务商则拒绝（统一守卫 A+B），否则清空旧库位重建
		boolean regenerate = (wh.getLocationGenerated() != null && wh.getLocationGenerated() == 1)
				|| wmsLocationService.countPhysicalByWarehouse(warehouseId) > 0;
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
		if (!warehouseService.updateById(mark)) {
			throw new IllegalStateException("库位生成状态保存失败，请刷新后重试");
		}

		return list.size();
	}

	private void validateDimensions(Integer rows, Integer columns) {
		if (rows == null || columns == null || rows <= 0 || columns <= 0 || rows > 100 || columns > 100
				|| (long) rows * columns > 5000L) {
			throw new BusinessException(WmsResultCode.INVALID_RACK_STRUCTURE.getCode(),
					WmsResultCode.INVALID_RACK_STRUCTURE.getMessage());
		}
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
