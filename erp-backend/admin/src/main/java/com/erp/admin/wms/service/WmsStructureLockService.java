package com.erp.admin.wms.service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.erp.admin.tenant.mapper.SysTenantMapper;
import com.erp.admin.tenant.model.entity.SysTenant;
import com.erp.admin.wms.enums.WmsResultCode;
import com.erp.admin.wms.mapper.WmsPhysicalInventoryMapper;
import com.erp.admin.wms.mapper.WmsRackAssignmentMapper;
import com.erp.admin.wms.mapper.WmsPalletMapper;
import com.erp.admin.wms.mapper.LocationTransferOrderMapper;
import com.erp.admin.wms.mapper.StocktakeMapper;
import com.erp.admin.wms.model.entity.WmsRackAssignment;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.core.exception.BusinessException;
import org.springframework.stereotype.Service;

/**
 * 库位结构锁定判定（C1 加固）。
 *
 * <p>库位管理的「保存结构 / 重新生成」是整仓级破坏性操作，满足以下任一即锁定：
 * <ul>
 * <li><b>A 有货物占用</b>：{@code wms_physical_inventory} 该仓存在 quantity&gt;0 的批次；</li>
 * <li><b>B 有当前有效分配</b>：{@code wms_rack_assignment} 该仓存在当前有效（已生效未结束）的货架分配。</li>
 * </ul>
 * 后端硬闸（{@link #assertEditable}）与前端置灰共用同一口径（{@link #compute} 提供展示数据）。
 *
 * @author erp
 */
@Service
@RequiredArgsConstructor
public class WmsStructureLockService {

	private final WmsPhysicalInventoryMapper physicalInventoryMapper;

	private final WmsRackAssignmentMapper rackAssignmentMapper;

	private final SysTenantMapper sysTenantMapper;

	private final WmsPalletMapper palletMapper;

	private final LocationTransferOrderMapper locationTransferOrderMapper;

	private final StocktakeMapper stocktakeMapper;

	/** 结构锁定判定结果（纯值对象，供 VO 展示与 assert 复用）。 */
	public static final class LockInfo {

		/** 有货物占用（A）。 */
		public final boolean occupied;

		/** 有货占用的库位数（去重 location_code）。 */
		public final int occupiedLocationCount;

		/** 有当前有效分配（B）。 */
		public final boolean assigned;

		/** 已分配的货架排数（去重 rack_no，当前有效）。 */
		public final int assignedRackCount;

		/** 已分配的服务商名称（去重，当前有效）。 */
		public final List<String> assignedOperatorNames;

		public final int activePalletCount;

		public final int unfinishedTransferCount;

		public final int inProgressStocktakeCount;

		LockInfo(boolean occupied, int occupiedLocationCount, boolean assigned, int assignedRackCount,
				List<String> assignedOperatorNames, int activePalletCount, int unfinishedTransferCount,
				int inProgressStocktakeCount) {
			this.occupied = occupied;
			this.occupiedLocationCount = occupiedLocationCount;
			this.assigned = assigned;
			this.assignedRackCount = assignedRackCount;
			this.assignedOperatorNames = assignedOperatorNames;
			this.activePalletCount = activePalletCount;
			this.unfinishedTransferCount = unfinishedTransferCount;
			this.inProgressStocktakeCount = inProgressStocktakeCount;
		}

		/** 是否锁定（A 或 B）。 */
		public boolean isLocked() {
			return occupied || assigned || activePalletCount > 0 || unfinishedTransferCount > 0
					|| inProgressStocktakeCount > 0;
		}

	}

	/**
	 * 计算某仓的结构锁定状态（占用 + 分配的明细，供前端展示与文案拼接）。
	 * @param warehouseId 仓库ID
	 * @return 锁定信息
	 */
	public LockInfo compute(Long warehouseId) {
		// A：有货占用（quantity>0 的库位数，口径与 listOccupiedLocationCodes 统一）
		int occupiedCount = physicalInventoryMapper.listBlockingPhysicalLocationCodes(warehouseId).size();

		// B：当前有效分配
		LocalDate today = LocalDate.now(ZoneOffset.UTC);
		List<WmsRackAssignment> actives = rackAssignmentMapper.listByWarehouse(warehouseId).stream()
				.filter(a -> WmsRackAssignmentService.isActive(a.getEffectiveFrom(), a.getEffectiveTo(), today))
				.collect(Collectors.toList());
		Set<String> activeRacks = actives.stream().map(WmsRackAssignment::getRackNo).collect(Collectors.toSet());
		Set<Long> operatorIds = actives.stream().map(WmsRackAssignment::getWmsTenantId)
				.collect(Collectors.toCollection(LinkedHashSet::new));

		List<String> operatorNames;
		if (operatorIds.isEmpty()) {
			operatorNames = Collections.emptyList();
		}
		else {
			Map<Long, String> nameById = sysTenantMapper.selectBatchIds(operatorIds).stream()
					.collect(Collectors.toMap(SysTenant::getId, SysTenant::getTenantName, (a, b) -> a));
			operatorNames = operatorIds.stream().map(id -> nameById.getOrDefault(id, "服务商#" + id))
					.collect(Collectors.toList());
		}

		int activePalletCount = Math.toIntExact(palletMapper.countActiveByWarehouse(warehouseId));
		int unfinishedTransferCount = Math.toIntExact(
				locationTransferOrderMapper.countUnfinishedByWarehouse(warehouseId));
		int inProgressStocktakeCount = Math.toIntExact(stocktakeMapper.countInProgressByWarehouse(warehouseId));
		return new LockInfo(occupiedCount > 0, occupiedCount, !actives.isEmpty(), activeRacks.size(), operatorNames,
				activePalletCount, unfinishedTransferCount, inProgressStocktakeCount);
	}

	/**
	 * 硬闸：仓库被锁定则拒绝结构类操作（保存结构 / 重新生成库位）。
	 * @param warehouseId 仓库ID
	 */
	public void assertEditable(Long warehouseId) {
		LockInfo info = compute(warehouseId);
		if (info.occupied) {
			throw new BusinessException(WmsResultCode.LOCATION_OCCUPIED.getCode(),
					WmsResultCode.LOCATION_OCCUPIED.getMessage());
		}
		if (info.assigned) {
			throw new BusinessException(WmsResultCode.RACK_ASSIGNED_LOCKED.getCode(),
					WmsResultCode.RACK_ASSIGNED_LOCKED.getMessage());
		}
		if (info.activePalletCount > 0) {
			throw new BusinessException(409, "该仓库仍有在库托盘，无法重新生成库位");
		}
		if (info.unfinishedTransferCount > 0) {
			throw new BusinessException(409, "该仓库仍有未完成的库位调整单，无法重新生成库位");
		}
		if (info.inProgressStocktakeCount > 0) {
			throw new BusinessException(409, "该仓库仍有进行中的盘点任务，无法重新生成库位");
		}
	}

}
