package com.erp.admin.wms.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.erp.admin.product.service.WarehouseSkuCodeService;
import com.erp.admin.tenant.mapper.SysTenantMapper;
import com.erp.admin.tenant.model.entity.SysTenant;
import com.erp.admin.tenant.service.TenantIdentityService;
import com.erp.admin.wms.mapper.WmsPhysicalInventoryMapper;
import com.erp.admin.wms.model.entity.WmsLocation;
import com.erp.admin.wms.model.entity.WmsPhysicalInventory;
import com.erp.admin.wms.model.entity.WmsZone;
import com.erp.admin.wms.model.vo.AvailableLocationVO;
import com.erp.admin.wms.model.vo.LocationTransferSourceBatchVO;
import com.erp.admin.wms.model.vo.PalletSlotVO;
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

	private static final String ZONE_TEMP = "TEMP";

	private static final String ZONE_DEFECTIVE = "DEFECTIVE";

	private static final String QUALITY_GOOD = "GOOD";

	public static final String MOVE_PARTIAL = "PARTIAL";

	public static final String MOVE_WHOLE_PALLET = "WHOLE_PALLET";

	private final WmsPhysicalInventoryService physicalInventoryService;

	private final WmsPhysicalInventoryMapper physicalInventoryMapper;

	private final WmsLocationService wmsLocationService;

	private final WmsZoneService wmsZoneService;

	private final WmsRackAssignmentService wmsRackAssignmentService;

	private final WmsPalletService wmsPalletService;

	private final SysTenantMapper sysTenantMapper;

	private final TenantIdentityService tenantIdentityService;

	private final WarehouseSkuCodeService warehouseSkuCodeService;

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
		WmsLocation sourceLocation = locByCode.get(source.getLocationCode());
		Assert.notNull(sourceLocation, "源库位不存在：" + source.getLocationCode());
		WmsLocation target = locByCode.get(targetLocationCode);
		Assert.notNull(target, "目标库位不存在：" + targetLocationCode);

		SysTenant owner = sysTenantMapper.selectById(source.getErpTenantId());
		Long expectedWmsTenantId = owner == null ? null : owner.getParentWmsTenantId();
		Assert.notNull(expectedWmsTenantId, "货主未关联 WMS 服务商");
		Assert.isTrue(expectedWmsTenantId.equals(source.getWmsTenantId()), "源库存的服务商归属不正确");
		boolean sourceLocationVirtual = sourceLocation.getIsVirtual() != null
				&& sourceLocation.getIsVirtual() == 1;
		if (!sourceLocationVirtual) {
			Set<String> sourceAllowedRacks = wmsRackAssignmentService
					.activeRackNos(warehouseId, expectedWmsTenantId);
			Assert.isTrue(sourceAllowedRacks.contains(sourceLocation.getRackNo()),
					"源库位不属于该货主服务商的物理库位：" + source.getLocationCode());
		}

		// 源批可用量校验（各方向通用）
		int reserved = source.getReservedQty() == null ? 0 : source.getReservedQty();
		int available = source.getQuantity() - reserved;
		Assert.isTrue(quantity > 0 && quantity <= available,
				String.format("批次[%s]移动数量(%d)超过可用(%d)", source.getLocationCode(), quantity, available));

		Map<Long, String> zoneTypeById = wmsZoneService.listByWarehouse(warehouseId).stream()
				.collect(Collectors.toMap(WmsZone::getId, z -> z.getZoneType() == null ? "" : z.getZoneType(),
						(a, b) -> a));
		boolean sourceVirtual = source.getContainerStored() != null && source.getContainerStored() == 1;
		boolean targetVirtual = target.getIsVirtual() != null && target.getIsVirtual() == 1;
		String sourceZone = source.getZoneId() == null ? null : zoneTypeById.get(source.getZoneId());
		String targetZone = target.getZoneId() == null ? null : zoneTypeById.get(target.getZoneId());

		if (!QUALITY_GOOD.equals(source.getQuality()) || ZONE_DEFECTIVE.equals(sourceZone)) {
			throw new BusinessException(400, "不良品区库存不能进行库位调整，只能通过销毁流程处理");
		}
		if (!isMoveAllowed(sourceZone, sourceVirtual, targetZone, targetVirtual)) {
			throw new BusinessException(400, "不允许从" + zoneLabel(sourceZone, sourceVirtual)
					+ "移动到" + zoneLabel(targetZone, targetVirtual));
		}

		// 虚拟库位是仓库级公共位置，不参与租架和托位容量校验。
		if (targetVirtual) {
			return new Resolution(target.getZoneId(), 1, VirtualMove.INTO);
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

		int targetAllocatable = isAllocatableTarget(targetZone) ? 1 : 0;
		if (sourceVirtual) {
			return new Resolution(target.getZoneId(), targetAllocatable, VirtualMove.OUTOF);
		}

		return new Resolution(target.getZoneId(), targetAllocatable, VirtualMove.NONE);
	}

	public Resolution resolveWholePalletLine(WmsPhysicalInventory source, Long warehouseId,
			String targetLocationCode, int quantity) {
		Assert.notNull(source.getPalletId(), "源批次未绑定托盘，不能整托调整");
		Resolution resolution = resolveLine(source, warehouseId, targetLocationCode, quantity);
		if (VirtualMove.INTO.equals(resolution.getVirtualMove())) {
			return resolution;
		}
		Assert.isTrue(wmsPalletService.hasEmptySlotForWholePallet(warehouseId, targetLocationCode),
				"目标库位没有空托位，不能整托调整：" + targetLocationCode);
		return resolution;
	}

	public Resolution resolveLine(WmsPhysicalInventory source, Long warehouseId, String targetLocationCode,
			Long targetSlotId, Long targetPalletId, int quantity) {
		Resolution resolution = resolveLine(source, warehouseId, targetLocationCode, quantity);
		if (!VirtualMove.NONE.equals(resolution.getVirtualMove())) {
			Assert.isTrue(targetSlotId == null && targetPalletId == null,
					"虚拟库位不应指定目标托位或托盘");
			return resolution;
		}
		Assert.isTrue((targetSlotId == null) != (targetPalletId == null),
				"拆零移库必须指定一个目标空托位或目标半托盘");
		PalletSlotVO target = wmsPalletService.listSlots(warehouseId).stream()
				.filter(slot -> targetSlotId != null
						? targetSlotId.equals(slot.getSlotId())
						: targetPalletId.equals(slot.getPalletId()))
				.findFirst().orElse(null);
		Assert.notNull(target, targetSlotId != null ? "目标托位不存在" : "目标托盘不存在");
		Assert.isTrue(targetLocationCode.equals(target.getLocationCode()), "目标托位或托盘不属于所选库位");
		if (targetSlotId != null) {
			Assert.isTrue(target.getPalletId() == null && "EMPTY".equals(target.getSlotStatus()),
					"目标托位已被占用");
		}
		else {
			Assert.isTrue(wmsPalletService.canPalletAcceptTransfer(warehouseId, targetPalletId,
					source.getErpTenantId(), source.getSkuCode(), source.getQuality()),
					"目标托盘不能接收该货物");
		}
		return resolution;
	}

	public Resolution resolveWholePalletLine(WmsPhysicalInventory source, Long warehouseId,
			String targetLocationCode, Long targetSlotId, int quantity) {
		Resolution resolution = resolveWholePalletLine(source, warehouseId, targetLocationCode, quantity);
		Assert.notNull(targetSlotId, "整托调整必须指定目标托位");
		PalletSlotVO target = wmsPalletService.listSlots(warehouseId).stream()
				.filter(slot -> targetSlotId.equals(slot.getSlotId())).findFirst().orElse(null);
		Assert.notNull(target, "目标托位不存在");
		Assert.isTrue(targetLocationCode.equals(target.getLocationCode()), "目标托位不属于所选目标库位");
		Assert.isTrue(target.getPalletId() == null && "EMPTY".equals(target.getSlotStatus()),
				"目标托位已被占用");
		return resolution;
	}

	public List<LocationTransferSourceBatchVO> listSources(Long warehouseId, String locationCode,
			Long erpTenantId, String skuKeyword, String palletNo) {
		assertPlatform();
		Assert.notNull(warehouseId, "仓库不能为空");
		Map<String, WmsLocation> locations = wmsLocationService.listByWarehouse(warehouseId).stream()
				.filter(location -> location.getLocationCode() != null)
				.collect(Collectors.toMap(WmsLocation::getLocationCode, location -> location, (a, b) -> a));
		Map<Long, String> zoneTypes = wmsZoneService.listByWarehouse(warehouseId).stream()
				.collect(Collectors.toMap(WmsZone::getId, WmsZone::getZoneType, (a, b) -> a));
		Map<String, Long> activeRackOwners = wmsRackAssignmentService.activeRackOwners(warehouseId);
		Map<Long, Long> ownerParents = new java.util.HashMap<>();
		List<LocationTransferSourceBatchVO> result = physicalInventoryMapper.listTransferSources(warehouseId, locationCode,
				erpTenantId, skuKeyword, palletNo).stream()
				.filter(batch -> {
					WmsLocation source = locations.get(batch.getLocationCode());
					if (source == null) {
						return false;
					}
					Long expectedWmsTenantId = ownerParents.computeIfAbsent(batch.getErpTenantId(), id -> {
						SysTenant owner = sysTenantMapper.selectById(id);
						return owner == null || owner.getParentWmsTenantId() == null
								? Long.MIN_VALUE : owner.getParentWmsTenantId();
					});
					if (!expectedWmsTenantId.equals(batch.getWmsTenantId())) {
						return false;
					}
					if (!QUALITY_GOOD.equals(batch.getQuality())) {
						return false;
					}
					String sourceZone = batch.getZoneId() == null ? null : zoneTypes.get(batch.getZoneId());
					if (ZONE_DEFECTIVE.equals(sourceZone)) {
						return false;
					}
					return source.getIsVirtual() != null && source.getIsVirtual() == 1
							|| expectedWmsTenantId.equals(activeRackOwners.get(source.getRackNo()));
				})
				.collect(Collectors.toList());
		result.forEach(batch -> batch.setWarehouseSkuCode(
				warehouseSkuCodeService.build(batch.getErpTenantId(), batch.getSkuCode())));
		return result;
	}

	/**
	 * 创建调整单时可选择的库位。物理库位按服务商当前有效租架范围过滤，
	 * 虚拟库位是全仓公共位置，始终返回。
	 */
	public List<WmsLocation> listSelectableLocations(Long warehouseId, Long erpTenantId,
			String sourceLocationCode) {
		assertPlatform();
		Assert.notNull(warehouseId, "仓库不能为空");
		List<WmsLocation> all = wmsLocationService.listByWarehouse(warehouseId);
		Map<String, Long> activeRackOwners = wmsRackAssignmentService.activeRackOwners(warehouseId);
		Set<Long> allowedWmsTenantIds = resolveSelectableWmsTenants(
				warehouseId, erpTenantId, sourceLocationCode, all, activeRackOwners);
		boolean unrestrictedSourceList = erpTenantId == null
				&& (sourceLocationCode == null || sourceLocationCode.trim().isEmpty());
		WmsLocation source = sourceLocationCode == null ? null : all.stream()
				.filter(location -> sourceLocationCode.equals(location.getLocationCode()))
				.findFirst().orElse(null);
		Map<Long, String> zoneTypes = wmsZoneService.listByWarehouse(warehouseId).stream()
				.collect(Collectors.toMap(WmsZone::getId, WmsZone::getZoneType, (a, b) -> a));
		boolean sourceVirtual = source != null && source.getIsVirtual() != null && source.getIsVirtual() == 1;
		String sourceZone = source == null || source.getZoneId() == null ? null : zoneTypes.get(source.getZoneId());
		return all.stream()
				.filter(location -> location.getIsVirtual() != null && location.getIsVirtual() == 1
						|| activeRackOwners.containsKey(location.getRackNo())
						&& (unrestrictedSourceList
								|| allowedWmsTenantIds.contains(activeRackOwners.get(location.getRackNo()))))
				.filter(location -> {
					boolean virtual = location.getIsVirtual() != null && location.getIsVirtual() == 1;
					String zone = location.getZoneId() == null ? null : zoneTypes.get(location.getZoneId());
					if (source == null) {
						return virtual || !ZONE_DEFECTIVE.equals(zone);
					}
					return !sourceLocationCode.equals(location.getLocationCode())
							&& isMoveAllowed(sourceZone, sourceVirtual, zone, virtual);
				})
				.collect(Collectors.toList());
	}

	private Set<Long> resolveSelectableWmsTenants(Long warehouseId, Long erpTenantId,
			String sourceLocationCode, List<WmsLocation> all, Map<String, Long> activeRackOwners) {
		Set<Long> allowed = new HashSet<>();
		if (erpTenantId != null) {
			SysTenant owner = sysTenantMapper.selectById(erpTenantId);
			if (owner != null && owner.getParentWmsTenantId() != null) {
				allowed.add(owner.getParentWmsTenantId());
			}
		}
		if (sourceLocationCode == null || sourceLocationCode.trim().isEmpty()) {
			return allowed;
		}
		WmsLocation source = all.stream()
				.filter(location -> sourceLocationCode.equals(location.getLocationCode()))
				.findFirst().orElse(null);
		if (source == null) {
			return Collections.emptySet();
		}
		Set<Long> sourceWmsTenantIds = new HashSet<>();
		if (source.getIsVirtual() != null && source.getIsVirtual() == 1) {
			physicalInventoryMapper.listTransferSources(
					warehouseId, sourceLocationCode, erpTenantId, null, null).stream()
					.map(LocationTransferSourceBatchVO::getWmsTenantId)
					.filter(java.util.Objects::nonNull)
					.forEach(sourceWmsTenantIds::add);
		}
		else {
			Long rackOwner = activeRackOwners.get(source.getRackNo());
			if (rackOwner != null) {
				sourceWmsTenantIds.add(rackOwner);
			}
		}
		if (erpTenantId == null) {
			return sourceWmsTenantIds;
		}
		allowed.retainAll(sourceWmsTenantIds);
		return allowed;
	}

	/** 移库方向：普通库内移库 / 收纳进虚拟库位 / 从虚拟库位取回。 */
	public enum VirtualMove {

		NONE, INTO, OUTOF

	}

	/** 单条移库解析结果：目标分区 + 目标可分配属性 + 虚拟移动方向。 */
	public static class Resolution {

		private final Long targetZoneId;

		private final int targetAllocatable;

		private final VirtualMove virtualMove;

		public Resolution(Long targetZoneId, int targetAllocatable) {
			this(targetZoneId, targetAllocatable, VirtualMove.NONE);
		}

		public Resolution(Long targetZoneId, int targetAllocatable, VirtualMove virtualMove) {
			this.targetZoneId = targetZoneId;
			this.targetAllocatable = targetAllocatable;
			this.virtualMove = virtualMove;
		}

		public Long getTargetZoneId() {
			return targetZoneId;
		}

		public int getTargetAllocatable() {
			return targetAllocatable;
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
	public List<AvailableLocationVO> listCandidateTargets(Long physicalInventoryId, String moveMode) {
		return listCandidateTargets(physicalInventoryId, moveMode, null);
	}

	public List<AvailableLocationVO> listCandidateTargets(Long physicalInventoryId, String moveMode,
			String targetLocationCode) {
		assertPlatform();
		WmsPhysicalInventory source = physicalInventoryService.getById(physicalInventoryId);
		Assert.notNull(source, "源批次不存在");
		Long wh = source.getWarehouseId();
		boolean wholePallet = MOVE_WHOLE_PALLET.equalsIgnoreCase(moveMode);
		boolean sourceVirtual = source.getContainerStored() != null && source.getContainerStored() == 1;

		List<WmsZone> zones = wmsZoneService.listByWarehouse(wh);
		Map<Long, WmsZone> zoneById = zones.stream().collect(Collectors.toMap(WmsZone::getId, z -> z, (a, b) -> a));
		String sourceZone = source.getZoneId() == null ? null
				: (zoneById.get(source.getZoneId()) == null ? null : zoneById.get(source.getZoneId()).getZoneType());
		if (!QUALITY_GOOD.equals(source.getQuality()) || ZONE_DEFECTIVE.equals(sourceZone)) {
			return new java.util.ArrayList<>();
		}
		Set<String> allowedRacks = resolveAllowedRacks(wh, source.getErpTenantId());
		List<AvailableLocationVO> result = new java.util.ArrayList<>();
		List<PalletSlotVO> warehouseSlots = wmsPalletService.listSlots(wh);
		for (WmsLocation l : wmsLocationService.listByWarehouse(wh)) {
			if (l.getLocationCode() == null || l.getLocationCode().equals(source.getLocationCode())) {
				continue;
			}
			if (targetLocationCode != null && !targetLocationCode.trim().isEmpty()
					&& !targetLocationCode.equals(l.getLocationCode())) {
				continue;
			}
			boolean locVirtual = l.getIsVirtual() != null && l.getIsVirtual() == 1;
			WmsZone z = l.getZoneId() == null ? null : zoneById.get(l.getZoneId());
			String targetZone = z == null ? null : z.getZoneType();
			if (!isMoveAllowed(sourceZone, sourceVirtual, targetZone, locVirtual)) {
				continue;
			}
			if (locVirtual) {
				if (!wholePallet) {
					continue;
				}
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
				vo.setTargetType("VIRTUAL");
				result.add(vo);
				continue;
			}
			if (!allowedRacks.contains(l.getRackNo())) {
				continue;
			}
			if (z == null) {
				continue;
			}
			for (PalletSlotVO slot : warehouseSlots) {
				if (!l.getId().equals(slot.getLocationId())) {
					continue;
				}
				boolean empty = slot.getPalletId() == null && "EMPTY".equals(slot.getSlotStatus());
				boolean acceptablePallet = !wholePallet && slot.getPalletId() != null
						&& WmsPalletService.PARTIAL.equals(slot.getPalletStatus())
						&& wmsPalletService.canPalletAcceptTransfer(wh, slot.getPalletId(),
								source.getErpTenantId(), source.getSkuCode(), source.getQuality());
				if ((!wholePallet && !empty && !acceptablePallet) || (wholePallet && !empty)) {
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
				vo.setSlotId(slot.getSlotId());
				vo.setSlotCode(slot.getSlotCode());
				vo.setPalletId(slot.getPalletId());
				vo.setPalletNo(slot.getPalletNo());
				vo.setTargetType(acceptablePallet ? "EXISTING_PALLET" : "EMPTY_SLOT");
				result.add(vo);
			}
		}
		return result;
	}

	static boolean isMoveAllowed(String sourceZone, boolean sourceVirtual,
			String targetZone, boolean targetVirtual) {
		if (ZONE_DEFECTIVE.equals(sourceZone) || ZONE_DEFECTIVE.equals(targetZone)) {
			return false;
		}
		if (targetVirtual) {
			return sourceVirtual || ZONE_STANDARD.equals(sourceZone)
					|| ZONE_RETURN.equals(sourceZone) || ZONE_TEMP.equals(sourceZone);
		}
		if (sourceVirtual) {
			return ZONE_STANDARD.equals(targetZone) || ZONE_TEMP.equals(targetZone);
		}
		if (ZONE_STANDARD.equals(sourceZone)) {
			return ZONE_STANDARD.equals(targetZone) || ZONE_TEMP.equals(targetZone);
		}
		if (ZONE_RETURN.equals(sourceZone)) {
			return ZONE_STANDARD.equals(targetZone) || ZONE_RETURN.equals(targetZone)
					|| ZONE_TEMP.equals(targetZone);
		}
		if (ZONE_TEMP.equals(sourceZone)) {
			return ZONE_STANDARD.equals(targetZone) || ZONE_TEMP.equals(targetZone);
		}
		return false;
	}

	static boolean isAllocatableTarget(String targetZone) {
		return ZONE_STANDARD.equals(targetZone) || ZONE_RETURN.equals(targetZone);
	}

	private String zoneLabel(String zoneType, boolean virtual) {
		if (virtual) {
			return "虚拟库位";
		}
		if (ZONE_STANDARD.equals(zoneType)) {
			return "标准区";
		}
		if (ZONE_RETURN.equals(zoneType)) {
			return "退货区";
		}
		if (ZONE_TEMP.equals(zoneType)) {
			return "暂存区";
		}
		if (ZONE_DEFECTIVE.equals(zoneType)) {
			return "不良品区";
		}
		return "未配置分区";
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
