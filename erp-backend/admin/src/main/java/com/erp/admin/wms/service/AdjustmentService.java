package com.erp.admin.wms.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.tenant.model.vo.TenantIdentityVO;
import com.erp.admin.tenant.service.TenantIdentityService;
import com.erp.admin.wms.mapper.AdjustmentMapper;
import com.erp.admin.wms.model.dto.AdjustmentDTO;
import com.erp.admin.wms.model.dto.AdjustmentItemDTO;
import com.erp.admin.wms.model.entity.AdjustmentOrder;
import com.erp.admin.wms.model.entity.AdjustmentOrderItem;
import com.erp.admin.wms.model.entity.WmsPhysicalInventory;
import com.erp.admin.wms.model.enums.AdjustmentStatus;
import com.erp.admin.wms.model.enums.AdjustmentType;
import com.erp.admin.wms.model.qo.AdjustmentQO;
import com.erp.admin.wms.model.vo.AdjustmentDetailVO;
import com.erp.admin.wms.model.vo.AdjustmentItemVO;
import com.erp.admin.wms.model.vo.AdjustmentPageVO;
import com.erp.admin.wms.model.vo.AdjustmentStatsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.common.core.exception.BusinessException;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.mybatisplus.service.impl.ExtendServiceImpl;
import org.ballcat.mybatisplus.toolkit.PageUtil;
import org.ballcat.security.core.PrincipalAttributeAccessor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * 报废单服务（原「库存调整单」收敛为仅「报废」，并改为货主审批流）。
 *
 * <p>流程：<b>平台</b>发起报废(选货主+仓+具体批次+数量)→发起即<b>冻结</b>待报废批次(reserved_qty)
 * →状态 PENDING_OWNER 待货主确认；<b>货主</b>确认→真正扣减批次(quantity)、写 SCRAP 流水→SCRAPPED；
 * 货主驳回或平台撤销→释放冻结→REJECTED / CANCELLED。批次真源操作见 {@link WmsPhysicalInventoryService}。
 *
 * @author erp
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdjustmentService extends ExtendServiceImpl<AdjustmentMapper, AdjustmentOrder> {

	/** 单号生成最大重试次数 */
	private static final int MAX_RETRY_ATTEMPTS = 3;

	private final AdjustmentItemService adjustmentItemService;

	private final WarehouseService warehouseService;

	private final WmsPhysicalInventoryService physicalInventoryService;

	private final TenantIdentityService tenantIdentityService;

	private final PrincipalAttributeAccessor principalAttributeAccessor;

	// ==================== 查询 ====================

	/**
	 * 分页查询。货主端强制只看自己的报废单；平台端看全部（可按条件收窄）。
	 */
	public PageResult<AdjustmentPageVO> queryPage(PageParam pageParam, AdjustmentQO qo) {
		scopeToOwnerIfErpUser(qo);

		IPage<AdjustmentPageVO> page = PageUtil.prodPage(pageParam);
		baseMapper.queryPage(page, qo);

		List<AdjustmentPageVO> records = page.getRecords();
		if (records.isEmpty()) {
			return new PageResult<>(records, page.getTotal());
		}

		List<Long> ids = records.stream().map(AdjustmentPageVO::getId).collect(Collectors.toList());
		Map<Long, AdjustmentStatsVO> statsMap = baseMapper.selectStatsByIds(ids)
				.stream()
				.collect(Collectors.toMap(AdjustmentStatsVO::getAdjustmentOrderId, Function.identity()));

		for (AdjustmentPageVO vo : records) {
			AdjustmentStatsVO stats = statsMap.get(vo.getId());
			vo.setSkuCount(stats != null ? stats.getSkuCount() : 0);
			vo.setTotalQuantity(stats != null ? stats.getTotalQuantity() : 0);
		}

		return new PageResult<>(records, page.getTotal());
	}

	/**
	 * 报废单详情。货主端只能看自己的。
	 */
	public AdjustmentDetailVO getDetail(Long id) {
		AdjustmentDetailVO detail = baseMapper.selectDetailById(id);
		Assert.notNull(detail, "报废单不存在");

		TenantIdentityVO identity = tenantIdentityService.currentIdentity(null);
		if (TenantIdentityService.IDENTITY_ERP_USER.equals(identity.getIdentityType())) {
			Assert.isTrue(identity.getTenantId() != null && identity.getTenantId().equals(detail.getErpTenantId()),
					"无权查看该报废单");
		}

		List<AdjustmentStatsVO> statsVOS = baseMapper.selectStatsByIds(Collections.singleton(id));
		if (!statsVOS.isEmpty()) {
			AdjustmentStatsVO s = statsVOS.get(0);
			detail.setSkuCount(s.getSkuCount());
			detail.setTotalQuantity(s.getTotalQuantity());
		}

		List<AdjustmentItemVO> items = adjustmentItemService.getVoListByAdjustmentOrderId(id);
		detail.setItems(items);
		return detail;
	}

	/**
	 * 生成报废单号：AD + yyyyMMdd + 4 位序号。
	 */
	public String generateAdjustmentNo() {
		String prefix = "AD" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
		int count = baseMapper.countTodayOrders(prefix);
		return prefix + String.format("%04d", count + 1);
	}

	// ==================== 平台：发起 / 撤销 ====================

	/**
	 * 平台发起报废：校验批次归属与可用 → 建单(PENDING_OWNER) → 冻结待报废批次。
	 * @param dto 报废单DTO（items 每行锁定一个具体批次 physicalInventoryId + 数量）
	 * @return 报废单ID
	 */
	@Transactional(rollbackFor = Exception.class)
	public Long create(AdjustmentDTO dto) {
		assertPlatform();
		Assert.notEmpty(dto.getItems(), "报废明细不能为空");
		Assert.notNull(dto.getErpTenantId(), "货主不能为空");
		Assert.notNull(dto.getWarehouseId(), "仓库不能为空");
		Assert.notNull(warehouseService.getById(dto.getWarehouseId()), "仓库不存在");

		// 加载并校验批次（存在 / 属本仓本货主 / 可用充足），构建明细
		List<AdjustmentOrderItem> items = new ArrayList<>();
		for (AdjustmentItemDTO d : dto.getItems()) {
			Assert.notNull(d.getPhysicalInventoryId(), "目标批次不能为空");
			WmsPhysicalInventory batch = physicalInventoryService.getById(d.getPhysicalInventoryId());
			Assert.notNull(batch, "批次不存在：" + d.getPhysicalInventoryId());
			Assert.isTrue(dto.getWarehouseId().equals(batch.getWarehouseId()), "批次不属于所选仓库");
			Assert.isTrue(dto.getErpTenantId().equals(batch.getErpTenantId()), "批次不属于所选货主");
			int reserved = batch.getReservedQty() == null ? 0 : batch.getReservedQty();
			int available = batch.getQuantity() - reserved;
			Assert.isTrue(d.getQuantity() != null && d.getQuantity() > 0 && d.getQuantity() <= available,
					String.format("批次[%s]报废数量(%s)超过可用(%d)", batch.getLocationCode(), d.getQuantity(), available));

			AdjustmentOrderItem item = new AdjustmentOrderItem();
			item.setPhysicalInventoryId(batch.getId());
			item.setErpTenantId(dto.getErpTenantId());
			item.setSkuCode(batch.getSkuCode());
			item.setLocationCode(batch.getLocationCode());
			item.setQuality(batch.getQuality());
			item.setQuantity(d.getQuantity());
			item.setRemark(d.getRemark());
			items.add(item);
		}

		AdjustmentOrder order = new AdjustmentOrder();
		order.setWarehouseId(dto.getWarehouseId());
		order.setErpTenantId(dto.getErpTenantId());
		order.setAdjustmentType(AdjustmentType.SCRAP.name());
		order.setAdjustmentDate(dto.getAdjustmentDate() != null ? dto.getAdjustmentDate() : LocalDate.now());
		order.setAdjustmentReason(dto.getAdjustmentReason());
		order.setAdjustmentNo(generateAdjustmentNo());
		order.setOrderStatus(AdjustmentStatus.PENDING_OWNER.name());
		order.setCreateBy(currentUserId());
		saveOrderWithRetry(order);

		for (AdjustmentOrderItem item : items) {
			item.setAdjustmentOrderId(order.getId());
		}
		adjustmentItemService.saveBatch(items);

		// 冻结待报废批次（同事务；任一不足则整单回滚）
		for (AdjustmentOrderItem item : items) {
			physicalInventoryService.reserveBatch(item.getPhysicalInventoryId(), item.getQuantity());
		}

		log.info("平台发起报废, id={}, no={}, erpTenantId={}, items={}", order.getId(), order.getAdjustmentNo(),
				order.getErpTenantId(), items.size());
		return order.getId();
	}

	/**
	 * 平台撤销（仅待货主确认可撤销）：释放冻结 → CANCELLED。
	 */
	@Transactional(rollbackFor = Exception.class)
	public void cancel(Long id) {
		assertPlatform();
		AdjustmentOrder order = requireOrder(id);
		Assert.isTrue(AdjustmentStatus.PENDING_OWNER.name().equals(order.getOrderStatus()),
				"只有待货主确认的报废单可以撤销");
		releaseAll(id);
		order.setOrderStatus(AdjustmentStatus.CANCELLED.name());
		this.updateById(order);
		log.info("平台撤销报废, id={}, no={}", order.getId(), order.getAdjustmentNo());
	}

	// ==================== 货主：确认 / 驳回 ====================

	/**
	 * 货主确认销毁（仅待货主确认，且为本货主）：逐批次真正扣减 + 写 SCRAP 流水 → SCRAPPED。
	 */
	@Transactional(rollbackFor = Exception.class)
	public void ownerConfirm(Long id) {
		AdjustmentOrder order = requireOrder(id);
		assertOwnerOf(order);
		Assert.isTrue(AdjustmentStatus.PENDING_OWNER.name().equals(order.getOrderStatus()),
				"只有待货主确认的报废单可以确认");

		List<AdjustmentOrderItem> items = adjustmentItemService.getByAdjustmentOrderId(id);
		Assert.notEmpty(items, "报废明细为空");
		for (AdjustmentOrderItem item : items) {
			physicalInventoryService.scrapBatch(item.getPhysicalInventoryId(), item.getQuantity(),
					order.getAdjustmentNo());
		}

		LocalDateTime now = LocalDateTime.now();
		Long uid = currentUserId();
		order.setOrderStatus(AdjustmentStatus.SCRAPPED.name());
		order.setOwnerActionTime(now);
		order.setOwnerActionBy(uid);
		order.setConfirmTime(now);
		order.setConfirmBy(uid);
		this.updateById(order);
		log.info("货主确认报废销毁, id={}, no={}", order.getId(), order.getAdjustmentNo());
	}

	/**
	 * 货主驳回（仅待货主确认，且为本货主）：释放冻结 → REJECTED。
	 */
	@Transactional(rollbackFor = Exception.class)
	public void ownerReject(Long id, String reason) {
		AdjustmentOrder order = requireOrder(id);
		assertOwnerOf(order);
		Assert.isTrue(AdjustmentStatus.PENDING_OWNER.name().equals(order.getOrderStatus()),
				"只有待货主确认的报废单可以驳回");

		releaseAll(id);
		order.setOrderStatus(AdjustmentStatus.REJECTED.name());
		order.setRejectReason(reason);
		order.setOwnerActionTime(LocalDateTime.now());
		order.setOwnerActionBy(currentUserId());
		this.updateById(order);
		log.info("货主驳回报废, id={}, no={}, reason={}", order.getId(), order.getAdjustmentNo(), reason);
	}

	// ==================== 删除 ====================

	/**
	 * 删除报废单（仅已取消/已驳回可删；已销毁需留痕不可删）。
	 */
	@Transactional(rollbackFor = Exception.class)
	public void delete(List<Long> ids) {
		for (Long id : ids) {
			AdjustmentOrder order = requireOrder(id);
			Assert.isTrue(AdjustmentStatus.CANCELLED.name().equals(order.getOrderStatus())
							|| AdjustmentStatus.REJECTED.name().equals(order.getOrderStatus()),
					"只有已取消或已驳回的报废单可以删除");
			adjustmentItemService.deleteByAdjustmentOrderId(id);
			this.removeById(id);
			log.info("删除报废单, id={}, no={}", order.getId(), order.getAdjustmentNo());
		}
	}

	// ==================== 内部 ====================

	private void releaseAll(Long orderId) {
		List<AdjustmentOrderItem> items = adjustmentItemService.getByAdjustmentOrderId(orderId);
		for (AdjustmentOrderItem item : items) {
			physicalInventoryService.releaseBatch(item.getPhysicalInventoryId(), item.getQuantity());
		}
	}

	private AdjustmentOrder requireOrder(Long id) {
		AdjustmentOrder order = this.getById(id);
		Assert.notNull(order, "报废单不存在");
		return order;
	}

	private void assertPlatform() {
		String type = tenantIdentityService.currentIdentity(null).getIdentityType();
		if (!TenantIdentityService.IDENTITY_OVERSEAS_PLATFORM.equals(type)) {
			throw new BusinessException(403, "仅海外仓平台可发起/撤销报废");
		}
	}

	private void assertOwnerOf(AdjustmentOrder order) {
		TenantIdentityVO identity = tenantIdentityService.currentIdentity(null);
		if (!TenantIdentityService.IDENTITY_ERP_USER.equals(identity.getIdentityType())
				|| identity.getTenantId() == null || !identity.getTenantId().equals(order.getErpTenantId())) {
			throw new BusinessException(403, "仅货主本人可确认/驳回该报废单");
		}
	}

	private void scopeToOwnerIfErpUser(AdjustmentQO qo) {
		TenantIdentityVO identity = tenantIdentityService.currentIdentity(null);
		if (TenantIdentityService.IDENTITY_ERP_USER.equals(identity.getIdentityType())) {
			qo.setErpTenantId(identity.getTenantId());
		}
	}

	private Long currentUserId() {
		try {
			return principalAttributeAccessor.getUserId();
		}
		catch (Exception ignore) {
			return null;
		}
	}

	/**
	 * 保存报废单（带重试处理单号冲突）。
	 */
	private void saveOrderWithRetry(AdjustmentOrder order) {
		for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
			try {
				this.save(order);
				return;
			}
			catch (DuplicateKeyException e) {
				if (attempt == MAX_RETRY_ATTEMPTS) {
					throw new IllegalStateException("报废单号生成失败，请重试", e);
				}
				order.setAdjustmentNo(generateAdjustmentNo());
				order.setId(null);
			}
		}
	}

}
