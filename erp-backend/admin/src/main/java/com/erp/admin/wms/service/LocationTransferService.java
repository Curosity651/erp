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
 * <p>四条约束（与上架规则一致）：①同仓；②目标货架属该货主的服务商当前租用；
 * ③分区：目标必为标准区 STANDARD，源只能退货区 RETURN 或标准区 STANDARD（即 RETURN→STANDARD 或 STANDARD→STANDARD）；
 * ④目标库位独占，仅同 SKU 同批次可合并。<b>退货区→标准区直接置为良品 GOOD。</b>
 * 批次真源变更与流水见 {@link WmsPhysicalInventoryService#locationTransfer}。
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

	private final SysTenantMapper sysTenantMapper;

	private final TenantIdentityService tenantIdentityService;

	/**
	 * 校验并解析单条移库计划（不执行、不动库存），供库位调整单在建单/执行时复用。
	 *
	 * <p>逐条校验四约束：①同仓；②目标货架属该货主服务商当前租用；③分区(RETURN/STANDARD→STANDARD)；
	 * ④目标独占或同 SKU 同批次可合并；并校验源批可用量 ≥ 移动数量。校验通过返回目标分区与是否置良品。
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

		// 收纳(INTO)：普通库位 → 虚拟库位。仅良品可入；跳过 ②服务商租架/③分区/④独占（虚拟库位不占物理货架、可容多批）。
		if (targetVirtual) {
			if (!"GOOD".equals(source.getQuality())) {
				throw new BusinessException(400, "仅良品(GOOD)可放入虚拟库位");
			}
			return new Resolution(target.getZoneId(), false, VirtualMove.INTO);
		}

		Map<Long, String> zoneTypeById = wmsZoneService.listByWarehouse(warehouseId).stream()
				.collect(Collectors.toMap(WmsZone::getId, z -> z.getZoneType() == null ? "" : z.getZoneType(),
						(a, b) -> a));
		String sourceZone = source.getZoneId() == null ? null : zoneTypeById.get(source.getZoneId());
		String targetZone = target.getZoneId() == null ? null : zoneTypeById.get(target.getZoneId());

		// ③ 分区：目标必为标准区（取回与普通移库都要求落到标准区）；源分区校验对虚拟源(取回)跳过
		if (!ZONE_STANDARD.equals(targetZone)) {
			throw new BusinessException(400, "目标库位必须在标准区：" + targetLocationCode);
		}
		if (!sourceVirtual && !ZONE_STANDARD.equals(sourceZone) && !ZONE_RETURN.equals(sourceZone)) {
			throw new BusinessException(400, "只允许 退货区→标准区 或 标准区→标准区 的移库");
		}

		// ② 目标货架必须是该货主的服务商当前有效租用的货架
		Set<String> allowedRacks = resolveAllowedRacks(warehouseId, source.getErpTenantId());
		if (!allowedRacks.contains(target.getRackNo())) {
			throw new BusinessException(400, "目标货架不属于该货主的服务商租用范围：" + targetLocationCode);
		}

		// ④ 目标库位独占：为空可放；有货仅当同 SKU 同批次可合并
		List<WmsPhysicalInventory> atTarget = physicalInventoryService.listAtLocation(warehouseId, targetLocationCode);
		for (WmsPhysicalInventory b : atTarget) {
			if (b.getQuantity() != null && b.getQuantity() > 0
					&& (!source.getSkuCode().equals(b.getSkuCode())
							|| !source.getInboundItemId().equals(b.getInboundItemId()))) {
				throw new BusinessException(400, "目标库位已被其他货物占用：" + targetLocationCode);
			}
		}

		// 取回(OUTOF)：虚拟库位 → 标准库位。目标的 ③②④ 已在上方按标准库位校验；源在虚拟区，不再校验源分区。
		if (sourceVirtual) {
			return new Resolution(target.getZoneId(), false, VirtualMove.OUTOF);
		}

		// 普通移库(NONE)：退货区→标准区落库后置为良品
		boolean toGood = ZONE_RETURN.equals(sourceZone);
		return new Resolution(target.getZoneId(), toGood, VirtualMove.NONE);
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
	 * <p>虚拟源(container_stored=1，即已在虚拟库位)：仅物理标准库位(取回 OUTOF)，不含虚拟库位。
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
		// 普通源必须在 退货区/标准区；虚拟源(取回)不受此限
		if (!sourceVirtual && !ZONE_STANDARD.equals(sourceZone) && !ZONE_RETURN.equals(sourceZone)) {
			return new java.util.ArrayList<>();
		}
		Set<String> allowedRacks = resolveAllowedRacks(wh, source.getErpTenantId());
		Set<String> occupied = new java.util.HashSet<>(physicalInventoryService.occupiedLocationCodes(wh));

		List<AvailableLocationVO> result = new java.util.ArrayList<>();
		for (WmsLocation l : wmsLocationService.listByWarehouse(wh)) {
			if (l.getLocationCode() == null || l.getLocationCode().equals(source.getLocationCode())) {
				continue;
			}
			boolean locVirtual = l.getIsVirtual() != null && l.getIsVirtual() == 1;
			if (locVirtual) {
				// 虚拟库位仅对普通源作为"收纳(INTO)"候选；虚拟→虚拟无意义
				if (sourceVirtual) {
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
				result.add(vo);
				continue;
			}
			// 物理标准库位候选：服务商租架 + 标准区 + 空闲或可合并
			if (!allowedRacks.contains(l.getRackNo())) {
				continue;
			}
			WmsZone z = l.getZoneId() == null ? null : zoneById.get(l.getZoneId());
			if (z == null || !ZONE_STANDARD.equals(z.getZoneType())) {
				continue;
			}
			if (occupied.contains(l.getLocationCode()) && !mergeable(wh, l.getLocationCode(), source)) {
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

	/** 目标库位是否可合并：其上现存批次均为同 SKU + 同入库批次。 */
	private boolean mergeable(Long warehouseId, String locationCode, WmsPhysicalInventory source) {
		for (WmsPhysicalInventory b : physicalInventoryService.listAtLocation(warehouseId, locationCode)) {
			if (b.getQuantity() != null && b.getQuantity() > 0
					&& (!source.getSkuCode().equals(b.getSkuCode())
							|| !source.getInboundItemId().equals(b.getInboundItemId()))) {
				return false;
			}
		}
		return true;
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
