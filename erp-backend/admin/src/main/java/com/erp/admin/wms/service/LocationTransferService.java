package com.erp.admin.wms.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.erp.admin.tenant.mapper.SysTenantMapper;
import com.erp.admin.tenant.model.entity.SysTenant;
import com.erp.admin.tenant.service.TenantIdentityService;
import com.erp.admin.wms.model.entity.WmsLocation;
import com.erp.admin.wms.model.entity.WmsPhysicalInventory;
import com.erp.admin.wms.model.entity.WmsZone;
import com.erp.admin.wms.model.vo.AvailableLocationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.common.core.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * 库位调整（库内移库）服务。平台把某库位的货移到同仓的另一个库位。
 *
 * <p>支持物理与虚拟库位之间的四向移动。虚拟库位是仓库公共位置；移入物理库位时校验
 * 服务商租赁范围、品质分区以及托位/托盘容量。移动只改变存放位置，库存归属、SKU、
 * 入库批次和品质保持不变。批次真源变更与流水见 {@link WmsPhysicalInventoryService#locationTransfer}。
 *
 * @author erp
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocationTransferService {

	private static final String ZONE_STANDARD = "STANDARD";

	private static final String ZONE_RETURN = "RETURN";

	private final WmsPhysicalInventoryService physicalInventoryService;

	private final WmsLocationService wmsLocationService;

	private final WmsZoneService wmsZoneService;

	private final WmsRackAssignmentService wmsRackAssignmentService;

	private final WmsPalletService wmsPalletService;

	private final SysTenantMapper sysTenantMapper;

	private final TenantIdentityService tenantIdentityService;

	/**
	 * 校验并解析单条移库计划（不执行、不动库存），供库位调整单在建单/执行时复用。
	 *
	 * <p>逐条校验同仓、源批可用量、物理货架租赁范围、品质分区和托盘容量。
	 * @param source             源批次（真源实体）
	 * @param warehouseId        所选仓库
	 * @param targetLocationCode 目标库位编码
	 * @param quantity           移动数量
	 * @return 解析结果（目标分区ID + 是否落库置良品）
	 */
	public Resolution resolveLine(WmsPhysicalInventory source, Long warehouseId, String targetLocationCode,
			int quantity) {
		Assert.notNull(source, "源批次不存在");
		// ① 同仓
		Assert.isTrue(warehouseId != null && warehouseId.equals(source.getWarehouseId()), "源批次不属于所选仓库");
		Assert.isTrue(targetLocationCode != null && !targetLocationCode.equals(source.getLocationCode()),
				"目标库位与源库位相同");

		Map<String, WmsLocation> locByCode = wmsLocationService.listByWarehouse(warehouseId).stream()
				.filter(l -> l.getLocationCode() != null)
				.collect(Collectors.toMap(WmsLocation::getLocationCode, l -> l, (a, b) -> a));
		WmsLocation target = locByCode.get(targetLocationCode);
		Assert.notNull(target, "目标库位不存在：" + targetLocationCode);

		// 源批可用量校验（各方向通用）
		int reserved = source.getReservedQty() == null ? 0 : source.getReservedQty();
		int available = source.getQuantity() - reserved;
		Assert.isTrue(quantity > 0 && quantity <= available,
				String.format("批次[%s]移动数量(%d)超过可用(%d)", source.getLocationCode(), quantity, available));

		boolean sourceVirtual = source.getContainerStored() != null && source.getContainerStored() == 1;
		boolean targetVirtual = target.getIsVirtual() != null && target.getIsVirtual() == 1;

		// 虚拟库位是仓库级公共位置，不参与租架校验；库存归属和品质保留在批次上。
		if (targetVirtual) {
			return new Resolution(target.getZoneId(), false, VirtualMove.INTO);
		}

		Map<Long, String> zoneTypeById = wmsZoneService.listByWarehouse(warehouseId).stream()
				.collect(Collectors.toMap(WmsZone::getId, z -> z.getZoneType() == null ? "" : z.getZoneType(),
						(a, b) -> a));
		String sourceZone = source.getZoneId() == null ? null : zoneTypeById.get(source.getZoneId());
		String targetZone = target.getZoneId() == null ? null : zoneTypeById.get(target.getZoneId());

		String requiredTargetZone = "DAMAGED".equals(source.getQuality()) ? "DEFECTIVE" : ZONE_STANDARD;
		if (!requiredTargetZone.equals(targetZone)) {
			throw new BusinessException(400, "目标库位分区与货物品质不匹配：" + targetLocationCode);
		}
		if (!sourceVirtual && !ZONE_STANDARD.equals(sourceZone) && !ZONE_RETURN.equals(sourceZone)
				&& !"DEFECTIVE".equals(sourceZone)) {
			throw new BusinessException(400, "源库位不属于可调整的物理分区");
		}

		// ② 目标货架必须是该货主的服务商当前有效租用的货架
		Set<String> allowedRacks = resolveAllowedRacks(warehouseId, source.getErpTenantId());
		if (!allowedRacks.contains(target.getRackNo())) {
			throw new BusinessException(400, "目标货架不属于该货主的服务商租用范围：" + targetLocationCode);
		}

		// ③ 目标库位必须还有空托位，或存在同货主同品质且未满、品种数未超限的托盘。
		if (!wmsPalletService.hasCapacityForTransfer(warehouseId, targetLocationCode,
				source.getErpTenantId(), source.getSkuCode(), source.getQuality())) {
			throw new BusinessException(400, "目标库位没有可用托位或可合并托盘：" + targetLocationCode);
		}

		// 取回(OUTOF)：虚拟库位 → 标准库位。目标的 ③②④ 已在上方按标准库位校验；源在虚拟区，不再校验源分区。
		if (sourceVirtual) {
			return new Resolution(target.getZoneId(), false, VirtualMove.OUTOF);
		}

		return new Resolution(target.getZoneId(), false, VirtualMove.NONE);
	}

	/** 移库方向：普通库内移库 / 收纳进虚拟库位 / 从虚拟库位取回。 */
	public enum VirtualMove {

		NONE, INTO, OUTOF

	}

	/** 单条移库解析结果：目标分区 + 是否落库置良品 + 虚拟移动方向。 */
	public static class Resolution {

		private final Long targetZoneId;

		private final boolean toGood;

		private final VirtualMove virtualMove;

		public Resolution(Long targetZoneId, boolean toGood) {
			this(targetZoneId, toGood, VirtualMove.NONE);
		}

		public Resolution(Long targetZoneId, boolean toGood, VirtualMove virtualMove) {
			this.targetZoneId = targetZoneId;
			this.toGood = toGood;
			this.virtualMove = virtualMove;
		}

		public Long getTargetZoneId() {
			return targetZoneId;
		}

		public boolean isToGood() {
			return toGood;
		}

		public VirtualMove getVirtualMove() {
			return virtualMove;
		}

	}

	/**
	 * 列出某源批次可移入的目标库位候选。
	 * <p>普通源(标准/退货区)：物理标准库位(服务商租架、空闲或可合并) + <b>本仓虚拟库位</b>(收纳积压货，INTO)。
	 * <p>虚拟源(container_stored=1，即已在虚拟库位)：可移到物理库位，也可移到其他虚拟库位。
	 * <p>普通源不在 退货区/标准区时返回空。
	 */
	public List<AvailableLocationVO> listCandidateTargets(Long physicalInventoryId) {
		assertPlatform();
		WmsPhysicalInventory source = physicalInventoryService.getById(physicalInventoryId);
		Assert.notNull(source, "源批次不存在");
		Long wh = source.getWarehouseId();
		boolean sourceVirtual = source.getContainerStored() != null && source.getContainerStored() == 1;

		List<WmsZone> zones = wmsZoneService.listByWarehouse(wh);
		Map<Long, WmsZone> zoneById = zones.stream().collect(Collectors.toMap(WmsZone::getId, z -> z, (a, b) -> a));
		String sourceZone = source.getZoneId() == null ? null
				: (zoneById.get(source.getZoneId()) == null ? null : zoneById.get(source.getZoneId()).getZoneType());
		// 普通源必须处于可操作的物理分区；虚拟源不受此限。
		if (!sourceVirtual && !ZONE_STANDARD.equals(sourceZone) && !ZONE_RETURN.equals(sourceZone)
				&& !"DEFECTIVE".equals(sourceZone)) {
			return new java.util.ArrayList<>();
		}
		Set<String> allowedRacks = resolveAllowedRacks(wh, source.getErpTenantId());
		List<AvailableLocationVO> result = new java.util.ArrayList<>();
		for (WmsLocation l : wmsLocationService.listByWarehouse(wh)) {
			if (l.getLocationCode() == null || l.getLocationCode().equals(source.getLocationCode())) {
				continue;
			}
			boolean locVirtual = l.getIsVirtual() != null && l.getIsVirtual() == 1;
			if (locVirtual) {
				WmsZone vz = l.getZoneId() == null ? null : zoneById.get(l.getZoneId());
				AvailableLocationVO vo = new AvailableLocationVO();
				vo.setLocationId(l.getId());
				vo.setLocationCode(l.getLocationCode());
				vo.setZoneId(l.getZoneId());
				vo.setZoneName(vz == null ? "虚拟库位区" : vz.getZoneName());
				vo.setZoneType(vz == null ? VirtualLocationService.ZONE_VIRTUAL : vz.getZoneType());
				vo.setRackNo(l.getRackNo());
				vo.setColumnNo(l.getColumnNo());
				vo.setIsVirtual(1);
				result.add(vo);
				continue;
			}
			// 物理候选保留品质：良品去标准区，不良品去不良品区。
			if (!allowedRacks.contains(l.getRackNo())) {
				continue;
			}
			WmsZone z = l.getZoneId() == null ? null : zoneById.get(l.getZoneId());
			String requiredZone = "DAMAGED".equals(source.getQuality()) ? "DEFECTIVE" : ZONE_STANDARD;
			if (z == null || !requiredZone.equals(z.getZoneType())) {
				continue;
			}
			if (!wmsPalletService.hasCapacityForTransfer(wh, l.getLocationCode(),
					source.getErpTenantId(), source.getSkuCode(), source.getQuality())) {
				continue;
			}
			AvailableLocationVO vo = new AvailableLocationVO();
			vo.setLocationId(l.getId());
			vo.setLocationCode(l.getLocationCode());
			vo.setZoneId(l.getZoneId());
			vo.setZoneName(z.getZoneName());
			vo.setZoneType(z.getZoneType());
			vo.setRackNo(l.getRackNo());
			vo.setColumnNo(l.getColumnNo());
			vo.setIsVirtual(0);
			result.add(vo);
		}
		return result;
	}

	private void assertPlatform() {
		String type = tenantIdentityService.currentIdentity(null).getIdentityType();
		if (!TenantIdentityService.IDENTITY_OVERSEAS_PLATFORM.equals(type)) {
			throw new BusinessException(403, "仅海外仓平台可执行库位调整");
		}
	}

	/** 货主(erp_tenant_id) → 父服务商 → 该仓当前有效租用货架排号；任一环缺失返回空集。 */
	private Set<String> resolveAllowedRacks(Long warehouseId, Long erpTenantId) {
		if (erpTenantId == null) {
			return Collections.emptySet();
		}
		SysTenant owner = sysTenantMapper.selectById(erpTenantId);
		Long operatorId = owner == null ? null : owner.getParentWmsTenantId();
		if (operatorId == null) {
			return Collections.emptySet();
		}
		return wmsRackAssignmentService.activeRackNos(warehouseId, operatorId);
	}

}
