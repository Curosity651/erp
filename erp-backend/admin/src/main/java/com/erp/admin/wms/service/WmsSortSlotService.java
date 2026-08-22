package com.erp.admin.wms.service;

import com.erp.admin.wms.mapper.WmsSalesOutboundPackageMapper;
import com.erp.admin.wms.mapper.WmsSortSlotMapper;
import com.erp.admin.wms.model.entity.WmsSalesOutboundPackage;
import com.erp.admin.wms.model.entity.WmsSortSlot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WmsSortSlotService {

	public static final String STATUS_AVAILABLE = "AVAILABLE";
	public static final String STATUS_RESERVED = "RESERVED";
	public static final String STATUS_SORTING = "SORTING";
	public static final String STATUS_READY = "READY";
	public static final String STATUS_PACKING = "PACKING";

	private final WmsSortSlotMapper slotMapper;
	private final WmsSalesOutboundPackageMapper packageMapper;

	@Transactional(rollbackFor = Exception.class)
	public List<WmsSortSlot> reserveForTask(Long taskId, Long warehouseId,
			List<WmsSalesOutboundPackage> packages) {
		if (packages == null || packages.isEmpty()) {
			return Collections.emptyList();
		}
		List<WmsSortSlot> slots = slotMapper.selectAvailableForUpdate(warehouseId, packages.size());
		Assert.isTrue(slots.size() == packages.size(),
				"可用分货格不足，需要 " + packages.size() + " 个，当前仅 " + slots.size() + " 个");
		LocalDateTime now = LocalDateTime.now();
		for (int index = 0; index < packages.size(); index++) {
			WmsSortSlot slot = slots.get(index);
			WmsSalesOutboundPackage pack = packages.get(index);
			slot.setSlotStatus(STATUS_RESERVED);
			slot.setTaskId(taskId);
			slot.setOutboundOrderId(pack.getOutboundOrderId());
			slot.setPackageId(pack.getId());
			slot.setReservedTime(now);
			slot.setReleasedTime(null);
			Assert.isTrue(slotMapper.updateById(slot) == 1, "分货格占用冲突，请重试");

			pack.setSortSlotId(slot.getId());
			pack.setSortSlotScanCode(slot.getScanCode());
			pack.setSortCode(slot.getSlotCode());
			pack.setSortStatus(SalesOutboundPackageService.SORT_PENDING);
			Assert.isTrue(packageMapper.updateById(pack) == 1, "平台订单包裹绑定分货格失败");
		}
		return slots;
	}

	@Transactional(rollbackFor = Exception.class)
	public void releaseByPackage(Long packageId) {
		WmsSortSlot slot = slotMapper.selectByPackageIdForUpdate(packageId);
		if (slot == null) {
			return;
		}
		clearBinding(slot);
		Assert.isTrue(slotMapper.updateById(slot) == 1, "分货格释放冲突，请刷新重试");
	}

	@Transactional(rollbackFor = Exception.class)
	public void releaseTask(Long taskId) {
		for (WmsSortSlot slot : slotMapper.selectByTaskIdForUpdate(taskId)) {
			if (slot.getPackageId() != null) {
				WmsSalesOutboundPackage pack = packageMapper.selectByIdForUpdate(slot.getPackageId());
				if (pack != null) {
					pack.setSortSlotId(null);
					pack.setSortSlotScanCode(null);
					pack.setSortCode(null);
					Assert.isTrue(packageMapper.updateById(pack) == 1, "平台订单包裹释放分货格失败");
				}
			}
			clearBinding(slot);
			Assert.isTrue(slotMapper.updateById(slot) == 1, "分货格释放冲突，请刷新重试");
		}
	}

	@Transactional(rollbackFor = Exception.class)
	public void markTaskSorting(Long taskId) {
		for (WmsSortSlot slot : slotMapper.selectByTaskIdForUpdate(taskId)) {
			if (!STATUS_RESERVED.equals(slot.getSlotStatus())) {
				continue;
			}
			slot.setSlotStatus(STATUS_SORTING);
			slot.setOccupiedTime(LocalDateTime.now());
			Assert.isTrue(slotMapper.updateById(slot) == 1, "分货格状态更新冲突，请刷新重试");
		}
	}

	@Transactional(rollbackFor = Exception.class)
	public void markReady(Long packageId) {
		updatePackageSlotStatus(packageId, STATUS_READY);
	}

	@Transactional(rollbackFor = Exception.class)
	public void markPacking(Long packageId) {
		updatePackageSlotStatus(packageId, STATUS_PACKING);
	}

	public Long locateActivePackage(String scanCode) {
		if (scanCode == null || scanCode.trim().isEmpty()) {
			return null;
		}
		String normalized = scanCode.trim();
		WmsSortSlot slot = slotMapper.selectByScanCode(normalized);
		if (slot == null) {
			List<WmsSortSlot> matches = slotMapper.selectActiveBySlotCode(normalized);
			Assert.isTrue(matches.size() <= 1, "该格口编号存在于多个仓库，请扫描格口二维码");
			slot = matches.isEmpty() ? null : matches.get(0);
		}
		return slot == null || STATUS_AVAILABLE.equals(slot.getSlotStatus()) ? null : slot.getPackageId();
	}

	public WmsSortSlot getById(Long slotId) {
		return slotId == null ? null : slotMapper.selectById(slotId);
	}

	private void updatePackageSlotStatus(Long packageId, String status) {
		WmsSortSlot slot = slotMapper.selectByPackageIdForUpdate(packageId);
		if (slot == null) {
			return;
		}
		slot.setSlotStatus(status);
		if (STATUS_READY.equals(status)) {
			slot.setReadyTime(LocalDateTime.now());
		}
		else if (STATUS_PACKING.equals(status)) {
			slot.setPackingTime(LocalDateTime.now());
		}
		Assert.isTrue(slotMapper.updateById(slot) == 1, "分货格状态更新冲突，请刷新重试");
	}

	private void clearBinding(WmsSortSlot slot) {
		slot.setSlotStatus(STATUS_AVAILABLE);
		slot.setTaskId(null);
		slot.setOutboundOrderId(null);
		slot.setPackageId(null);
		slot.setReservedTime(null);
		slot.setOccupiedTime(null);
		slot.setReadyTime(null);
		slot.setPackingTime(null);
		slot.setReleasedTime(LocalDateTime.now());
	}

}
