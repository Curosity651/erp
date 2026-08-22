package com.erp.admin.wms.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.erp.admin.tenant.enums.TenantType;
import com.erp.admin.tenant.mapper.SysTenantMapper;
import com.erp.admin.tenant.model.entity.SysTenant;
import com.erp.admin.platform.finance.mapper.WmsContractRackMapper;
import com.erp.admin.platform.finance.model.vo.ContractRackBindingVO;
import com.erp.admin.wms.enums.WmsResultCode;
import com.erp.admin.wms.mapper.WarehouseMapper;
import com.erp.admin.wms.mapper.WmsLocationMapper;
import com.erp.admin.wms.mapper.WmsPhysicalInventoryMapper;
import com.erp.admin.wms.mapper.WmsRackAssignmentMapper;
import com.erp.admin.wms.model.dto.RackAssignDTO;
import com.erp.admin.wms.model.entity.WmsLocation;
import com.erp.admin.wms.model.entity.WmsRackAssignment;
import com.erp.admin.wms.model.vo.RackVO;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.core.exception.BusinessException;
import org.ballcat.mybatisplus.service.impl.ExtendServiceImpl;
import org.ballcat.security.core.PrincipalAttributeAccessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 货架分配 + 二维预览服务（C2）。
 *
 * @author erp
 */
@Service
@RequiredArgsConstructor
public class WmsRackAssignmentService extends ExtendServiceImpl<WmsRackAssignmentMapper, WmsRackAssignment> {

	/** 临期阈值（天） */
	private static final int EXPIRING_DAYS = 30;

	private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

	private static final Pattern RACK_NO_PATTERN = Pattern.compile("^(.*?)(\\d+)$");

	private final WmsLocationMapper wmsLocationMapper;

	private final WmsPhysicalInventoryMapper physicalInventoryMapper;

	private final WarehouseMapper warehouseMapper;

	private final SysTenantMapper sysTenantMapper;

	private final PrincipalAttributeAccessor principalAttributeAccessor;

	private final WmsContractRackMapper contractRackMapper;

	/**
	 * 分配货架（一次可多排，每排一条记录）。同仓同排时间段重叠则拒。
	 * @param dto 分配请求
	 * @return 新建记录数
	 */
	@Transactional(rollbackFor = Exception.class)
	public int assign(RackAssignDTO dto) {
		Long operatorId = currentUserId();
		if (dto.getEffectiveTo() != null && dto.getEffectiveTo().isBefore(dto.getEffectiveFrom())) {
			throw new BusinessException(400, "结束日期不能早于生效日期");
		}
		if (warehouseMapper.selectByIdForUpdate(dto.getWarehouseId()) == null) {
			throw new BusinessException(400, "仓库不存在：" + dto.getWarehouseId());
		}
		Set<String> physicalRackNos = wmsLocationMapper.listAssignableByWarehouse(dto.getWarehouseId()).stream()
			.map(WmsLocation::getRackNo)
			.collect(Collectors.toSet());
		// 校验：目标必须是真实存在的 WMS 服务商（防止把货架分配给货主/平台/不存在的租户）
		SysTenant target = sysTenantMapper.selectById(dto.getWmsTenantId());
		if (target == null || !TenantType.WMS_OPERATOR.name().equals(target.getTenantType())) {
			throw new BusinessException(WmsResultCode.RACK_ASSIGN_FORBIDDEN.getCode(), "目标服务商不存在或类型非法");
		}
		List<WmsRackAssignment> toCreate = new ArrayList<>();
		for (String rackNo : dto.getRackNos()) {
			if (!physicalRackNos.contains(rackNo)) {
				throw new BusinessException(400, "排 " + rackNo + " 不存在或不是物理货架");
			}
			List<Long> stockOperators = physicalInventoryMapper
				.listBlockingWmsTenantIdsByRack(dto.getWarehouseId(), rackNo);
			boolean containsOtherOperator = stockOperators.stream()
				.anyMatch(id -> id == null || !id.equals(dto.getWmsTenantId()));
			if (containsOtherOperator) {
				throw new BusinessException(409, "排 " + rackNo + " 仍有其他服务商的库存，不能重新分配");
			}
			// 悲观锁读取该仓该排的全部分配：令并发 assign 串行化，避免各自通过重叠检查后双重分配
			List<WmsRackAssignment> existing = baseMapper.listByWarehouseAndRackForUpdate(dto.getWarehouseId(), rackNo);
			if (contractRackMapper.countOverlappingContractBindings(dto.getWarehouseId(), rackNo,
					dto.getEffectiveFrom(), dto.getEffectiveTo(), null) > 0) {
				throw new BusinessException(409, "排 " + rackNo + " 已由服务合同预留或占用，不能手工分配");
			}
			boolean overlap = existing.stream()
				.anyMatch(e -> periodsOverlap(dto.getEffectiveFrom(), dto.getEffectiveTo(), e.getEffectiveFrom(),
						e.getEffectiveTo()));
			if (overlap) {
				throw new BusinessException(WmsResultCode.RACK_ALREADY_ASSIGNED.getCode(),
						"排 " + rackNo + "：" + WmsResultCode.RACK_ALREADY_ASSIGNED.getMessage());
			}
			WmsRackAssignment a = new WmsRackAssignment();
			a.setWmsTenantId(dto.getWmsTenantId());
			a.setWarehouseId(dto.getWarehouseId());
			a.setRackNo(rackNo);
			a.setMonthlyFee(dto.getMonthlyFee());
			a.setEffectiveFrom(dto.getEffectiveFrom());
			a.setEffectiveTo(dto.getEffectiveTo());
			a.setContractFileUrl(dto.getContractFileUrl());
			a.setRemark(dto.getRemark());
			a.setCreateBy(operatorId);
			toCreate.add(a);
		}
		this.saveBatch(toCreate);
		return toCreate.size();
	}

	/**
	 * 解除分配（按记录ID删除）。
	 * @param id 分配记录ID
	 */
	@Transactional(rollbackFor = Exception.class)
	public void unassign(Long id) {
		WmsRackAssignment assignment = baseMapper.selectById(id);
		if (assignment == null) {
			throw new BusinessException(404, "货架分配记录不存在");
		}
		if (!physicalInventoryMapper
			.listBlockingWmsTenantIdsByRack(assignment.getWarehouseId(), assignment.getRackNo()).isEmpty()) {
			throw new BusinessException(409, "排 " + assignment.getRackNo() + " 仍有库存或预占，不能解除分配");
		}
		if (contractRackMapper.countOverlappingContractBindings(assignment.getWarehouseId(),
				assignment.getRackNo(), assignment.getEffectiveFrom(), assignment.getEffectiveTo(), null) > 0) {
			throw new BusinessException(409, "该货架已由服务合同预留或占用，不能解除分配");
		}
		if (contractRackMapper.countAssignmentLinks(id) > 0) {
			throw new BusinessException(409, "该货架由服务合同管理，不能在货架分配中解除");
		}
		if (!this.removeById(id)) {
			throw new BusinessException(409, "货架分配已发生变化，请刷新后重试");
		}
	}

	/**
	 * 仓库二维预览：按排聚合归属/状态/到期/库位数。
	 * @param warehouseId 仓库ID
	 * @param wmsTenantId 过滤：仅看某服务商（可空）
	 * @param status 过滤：IDLE/OCCUPIED（可空）
	 * @param expiringSoon 过滤：仅看临期（可空）
	 * @return 排预览列表
	 */
	public List<RackVO> preview(Long warehouseId, Long wmsTenantId, String status, Boolean expiringSoon) {
		LocalDate today = LocalDate.now(BUSINESS_ZONE);

		// 1) 该仓库各排库位数（按排有序）
		Map<String, Integer> rackLocationCount = new LinkedHashMap<>();
		for (WmsLocation loc : wmsLocationMapper.listAssignableByWarehouse(warehouseId)) {
			rackLocationCount.merge(loc.getRackNo(), 1, Integer::sum);
		}

		// 2) 当前有效分配：rackNo -> assignment
		Map<String, WmsRackAssignment> activeByRack = new java.util.HashMap<>();
		for (WmsRackAssignment a : baseMapper.listByWarehouse(warehouseId)) {
			if (isActive(a.getEffectiveFrom(), a.getEffectiveTo(), today)) {
				activeByRack.putIfAbsent(a.getRackNo(), a);
			}
		}

		Map<String, ContractRackBindingVO> contractByRack = contractRackMapper
			.listCurrentBindingsByWarehouse(warehouseId).stream()
			.collect(Collectors.toMap(ContractRackBindingVO::getRackNo, row -> row,
					(a, b) -> a.getRackAssignmentId() != null ? a : b));

		// 3) 服务商名称
		Set<Long> tenantIds = activeByRack.values().stream().map(WmsRackAssignment::getWmsTenantId)
			.collect(Collectors.toSet());
		tenantIds.addAll(contractByRack.values().stream().map(ContractRackBindingVO::getWmsTenantId)
			.collect(Collectors.toSet()));
		Map<Long, String> tenantNames = tenantIds.isEmpty() ? java.util.Collections.emptyMap()
				: sysTenantMapper.selectBatchIds(tenantIds).stream()
					.collect(Collectors.toMap(SysTenant::getId, SysTenant::getTenantName, (a, b) -> a));

		// 4) 组装
		List<RackVO> result = new ArrayList<>();
		for (Map.Entry<String, Integer> e : rackLocationCount.entrySet()) {
			String rackNo = e.getKey();
			WmsRackAssignment a = activeByRack.get(rackNo);
			ContractRackBindingVO binding = contractByRack.get(rackNo);
			RackVO vo = new RackVO();
			vo.setWarehouseId(warehouseId);
			vo.setRackNo(rackNo);
			vo.setLocationCount(e.getValue());
			if (a == null) {
				if (binding != null && "PENDING".equals(binding.getPaymentStatus())) {
					vo.setStatus("RESERVED");
					vo.setAssignedWmsTenantId(binding.getWmsTenantId());
					vo.setAssignedWmsTenantName(tenantNames.get(binding.getWmsTenantId()));
					vo.setMonthlyFee(binding.getMonthlyFee());
					vo.setEffectiveFrom(binding.getEffectiveFrom());
					vo.setEffectiveTo(binding.getEffectiveTo());
					vo.setContractControlled(true);
					vo.setContractId(binding.getContractId());
					vo.setContractNo(binding.getContractNo());
					vo.setRemark("待收款合同预留");
				}
				else {
					vo.setStatus("IDLE");
					vo.setContractControlled(false);
				}
				vo.setExpiringSoon(false);
			}
			else {
				vo.setStatus("OCCUPIED");
				vo.setAssignmentId(a.getId());
				vo.setAssignedWmsTenantId(a.getWmsTenantId());
				vo.setAssignedWmsTenantName(tenantNames.get(a.getWmsTenantId()));
				vo.setMonthlyFee(a.getMonthlyFee());
				vo.setEffectiveFrom(a.getEffectiveFrom());
				vo.setEffectiveTo(a.getEffectiveTo());
				vo.setRemark(a.getRemark());
				vo.setExpiringSoon(isExpiringSoon(a.getEffectiveTo(), today));
				boolean contractControlled = binding != null
						&& (binding.getRackAssignmentId() == null
								|| binding.getRackAssignmentId().equals(a.getId()));
				vo.setContractControlled(contractControlled);
				if (contractControlled) {
					vo.setContractId(binding.getContractId());
					vo.setContractNo(binding.getContractNo());
				}
			}
			result.add(vo);
		}

		// 5) 过滤
		return result.stream()
			.filter(v -> wmsTenantId == null || wmsTenantId.equals(v.getAssignedWmsTenantId()))
			.filter(v -> status == null || status.equals(v.getStatus()))
			.filter(v -> expiringSoon == null || expiringSoon.equals(v.getExpiringSoon()))
			.sorted(Comparator.comparing(RackVO::getRackNo, WmsRackAssignmentService::compareRackNo))
			.collect(Collectors.toList());
	}

	/**
	 * 某服务商在某仓「当前有效」分配到的货架排号集合（上架库位归属校验用）。
	 * @param warehouseId 仓库ID
	 * @param wmsTenantId 服务商租户ID（货主的父服务商）
	 * @return 当前有效的货架排号集合；服务商为空则空集
	 */
	public Set<String> activeRackNos(Long warehouseId, Long wmsTenantId) {
		if (wmsTenantId == null) {
			return java.util.Collections.emptySet();
		}
		LocalDate today = LocalDate.now(BUSINESS_ZONE);
		return baseMapper.listByWarehouse(warehouseId).stream()
			.filter(a -> wmsTenantId.equals(a.getWmsTenantId()))
			.filter(a -> isActive(a.getEffectiveFrom(), a.getEffectiveTo(), today))
			.map(WmsRackAssignment::getRackNo)
			.collect(Collectors.toSet());
	}

	/**
	 * 当前仓库每个有效货架排对应的 WMS 服务商。
	 */
	public Map<String, Long> activeRackOwners(Long warehouseId) {
		LocalDate today = LocalDate.now(BUSINESS_ZONE);
		return baseMapper.listByWarehouse(warehouseId).stream()
			.filter(a -> isActive(a.getEffectiveFrom(), a.getEffectiveTo(), today))
			.collect(Collectors.toMap(WmsRackAssignment::getRackNo,
					WmsRackAssignment::getWmsTenantId, (a, b) -> a, LinkedHashMap::new));
	}

	/** 当前仓库货架排的有效承租服务商；未分配时返回 null。 */
	public Long activeOperatorId(Long warehouseId, String rackNo) {
		LocalDate today = LocalDate.now(BUSINESS_ZONE);
		return baseMapper.listByWarehouseAndRack(warehouseId, rackNo).stream()
			.filter(a -> isActive(a.getEffectiveFrom(), a.getEffectiveTo(), today))
			.map(WmsRackAssignment::getWmsTenantId)
			.findFirst()
			.orElse(null);
	}

	private Long currentUserId() {
		try {
			return principalAttributeAccessor.getUserId();
		}
		catch (Exception ignore) {
			return null;
		}
	}

	// ==================== 纯函数（可单测） ====================

	/**
	 * 两个时间段是否重叠（null 结束日 = 开放至无穷）。
	 */
	public static boolean periodsOverlap(LocalDate aFrom, LocalDate aTo, LocalDate bFrom, LocalDate bTo) {
		LocalDate aEnd = aTo == null ? LocalDate.MAX : aTo;
		LocalDate bEnd = bTo == null ? LocalDate.MAX : bTo;
		return !aFrom.isAfter(bEnd) && !bFrom.isAfter(aEnd);
	}

	/**
	 * 分配在指定日期是否有效。
	 */
	public static boolean isActive(LocalDate from, LocalDate to, LocalDate today) {
		boolean started = !from.isAfter(today);
		boolean notEnded = to == null || !to.isBefore(today);
		return started && notEnded;
	}

	/**
	 * 是否即将到期（结束日非空且距今 ≤ 30 天）。
	 */
	public static boolean isExpiringSoon(LocalDate effectiveTo, LocalDate today) {
		return effectiveTo != null && !effectiveTo.isAfter(today.plusDays(EXPIRING_DAYS));
	}

	public static int compareRackNo(String left, String right) {
		if (left == null || right == null) {
			return Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER).compare(left, right);
		}
		Matcher lm = RACK_NO_PATTERN.matcher(left.trim());
		Matcher rm = RACK_NO_PATTERN.matcher(right.trim());
		if (lm.matches() && rm.matches()) {
			int prefix = lm.group(1).compareToIgnoreCase(rm.group(1));
			if (prefix != 0) {
				return prefix;
			}
			int number = Long.compare(Long.parseLong(lm.group(2)), Long.parseLong(rm.group(2)));
			if (number != 0) {
				return number;
			}
		}
		return left.compareToIgnoreCase(right);
	}

}
