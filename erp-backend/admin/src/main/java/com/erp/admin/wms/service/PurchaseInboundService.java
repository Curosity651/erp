package com.erp.admin.wms.service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.common.tenant.TenantContext;
import com.erp.admin.tenant.mapper.SysTenantMapper;
import com.erp.admin.tenant.model.entity.SysTenant;
import com.erp.admin.product.model.vo.SkuBriefVO;
import com.erp.admin.product.service.SkuBriefService;
import com.erp.admin.wms.converter.PurchaseInboundConverter;
import com.erp.admin.wms.exception.OptimisticLockException;
import com.erp.admin.wms.mapper.PurchaseInboundMapper;
import com.erp.admin.wms.model.dto.CustomReturnDTO;
import com.erp.admin.wms.model.dto.ManualInboundDTO;
import com.erp.admin.wms.model.dto.PurchaseInboundDTO;
import com.erp.admin.wms.model.dto.PurchaseInboundItemDTO;
import com.erp.admin.wms.model.entity.PurchaseInboundOrder;
import com.erp.admin.wms.model.enums.CustomReturnType;
import com.erp.admin.wms.model.enums.InboundSourceType;
import com.erp.admin.wms.model.enums.PurchaseInboundStatus;
import com.erp.admin.wms.model.qo.PurchaseInboundQO;
import com.erp.admin.wms.model.vo.PurchaseInboundDetailVO;
import com.erp.admin.wms.model.vo.PurchaseInboundItemVO;
import com.erp.admin.wms.model.vo.PurchaseInboundPageVO;
import com.erp.admin.wms.model.vo.PurchaseInboundSimpleVO;
import com.erp.admin.wms.model.vo.PurchaseInboundStatsVO;
import com.erp.admin.wms.model.vo.ShippingItemForInboundVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.mybatisplus.service.impl.ExtendServiceImpl;
import org.ballcat.mybatisplus.toolkit.PageUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * 采购入库单服务
 *
 * @author erp
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseInboundService extends ExtendServiceImpl<PurchaseInboundMapper, PurchaseInboundOrder> {

	private final PurchaseInboundItemService purchaseInboundItemService;

	private final LogisticsProviderService logisticsProviderService;

	private final SkuBriefService skuBriefService;

	private final SysTenantMapper sysTenantMapper;

	/**
	 * 分页查询
	 * @param pageParam 分页参数
	 * @param qo 查询条件
	 * @return 分页结果
	 */
	public PageResult<PurchaseInboundPageVO> queryPage(PageParam pageParam, PurchaseInboundQO qo) {
		// 数据级可见性：货主身份强制只看自身（忽略前端传入的货主/服务商筛选）；
		// 平台身份(scope=null)看全部，保留前端传入的货主/服务商作为可选筛选条件。
		Long scope = inboundErpScope();
		if (scope != null) {
			qo.setErpTenantId(scope);
			qo.setWmsTenantId(null);
		}

		// 默认按入库日期倒序排列
		if (pageParam.getSorts() == null || pageParam.getSorts().isEmpty()) {
			PageParam.Sort sort = new PageParam.Sort();
			sort.setField("inbound_date");
			sort.setAsc(false);
			pageParam.getSorts().add(sort);
		}

		IPage<PurchaseInboundPageVO> page = PageUtil.prodPage(pageParam);
		baseMapper.queryPage(page, qo);

		List<PurchaseInboundPageVO> records = page.getRecords();
		if (!records.isEmpty()) {
			fillPageVOExtendInfo(records);
		}

		return new PageResult<>(records, page.getTotal());
	}

	/**
	 * 获取入库单详情
	 * @param id 入库单ID
	 * @return 入库单详情
	 */
	public PurchaseInboundDetailVO getDetail(Long id) {
		// 数据级可见性：货主只能看自己的入库单；平台超管(scope=null)看全部
		Long scope = inboundErpScope();
		if (scope != null) {
			PurchaseInboundOrder order = this.getById(id);
			Assert.isTrue(order != null && scope.equals(order.getErpTenantId()), "入库单不存在");
		}

		PurchaseInboundDetailVO detail = baseMapper.selectDetailById(id);
		Assert.notNull(detail, "入库单不存在");

		// 查询明细列表
		List<PurchaseInboundItemVO> items = purchaseInboundItemService.getVoListByInboundOrderId(id);
		detail.setItems(items);

		// 注意：物流单扩展信息由 Controller 层组装，避免 Service 间依赖

		return detail;
	}

	/**
	 * 编辑入库单
	 * @param dto 入库单DTO
	 */
	@Transactional(rollbackFor = Exception.class)
	public void update(PurchaseInboundDTO dto) {
		PurchaseInboundOrder order = this.getById(dto.getId());
		Assert.notNull(order, "入库单不存在");
		assertOwnership(order);

		PurchaseInboundStatus status = PurchaseInboundStatus.valueOf(order.getOrderStatus());
		Assert.isTrue(status == PurchaseInboundStatus.DRAFT, "只有草稿状态的入库单可以编辑");

		// 校验明细不为空
		Assert.notEmpty(dto.getItems(), "入库明细不能为空");

		// 校验实到数量不超过应到数量
		validateItemQuantities(dto.getItems());

		// 删除原有明细
		purchaseInboundItemService.deleteByInboundOrderId(order.getId());

		// 更新入库单（入库单号不可修改）
		order.setShippingOrderId(dto.getShippingOrderId());
		order.setWarehouseId(dto.getWarehouseId());
		order.setInboundDate(dto.getInboundDate());
		order.setRemark(dto.getRemark());
		boolean updated = this.updateById(order);
		if (!updated) {
			throw new OptimisticLockException("数据已被其他用户修改，请刷新后重试");
		}

		// 保存新明细
		purchaseInboundItemService.batchSave(order.getId(), dto.getItems());

		log.info("Updated purchase inbound order, id={}", order.getId());
	}

	/**
	 * 获取物流单待入库明细
	 * @param shippingOrderId 物流单ID
	 * @return 待入库明细列表
	 */
	public List<ShippingItemForInboundVO> getShippingItems(Long shippingOrderId) {
		List<ShippingItemForInboundVO> items = baseMapper.selectShippingItemsForInbound(shippingOrderId);
		if (items.isEmpty()) {
			return items;
		}

		// 填充 SKU 简要信息
		skuBriefService.enrichForQuery(items, ShippingItemForInboundVO::getSkuCode, ShippingItemForInboundVO::setSkuBrief);

		return items;
	}

	/**
	 * 导出入库单列表
	 * @param qo 查询条件
	 * @return 导出数据列表
	 */
	public List<PurchaseInboundPageVO> listForExport(PurchaseInboundQO qo) {
		// 数据级可见性：强制按当前身份的货主作用域过滤（忽略前端传入）
		qo.setErpTenantId(inboundErpScope());
		List<PurchaseInboundPageVO> records = baseMapper.selectListForExport(qo);

		if (!records.isEmpty()) {
			fillPageVOExtendInfo(records);
		}

		return records;
	}

	/**
	 * 填充分页VO扩展信息（物流商名称、统计信息、采购单号）
	 * @param records 分页记录列表
	 */
	private void fillPageVOExtendInfo(List<PurchaseInboundPageVO> records) {
		List<Long> ids = records.stream().map(PurchaseInboundPageVO::getId).collect(Collectors.toList());

		// 批量填充物流商名称
		logisticsProviderService.enrichProviderName(
				records,
				PurchaseInboundPageVO::getProviderId,
				PurchaseInboundPageVO::setProviderName
		);

		// 批量查询统计信息（通过 ItemService）
		Map<Long, PurchaseInboundStatsVO> statsMap = purchaseInboundItemService.getStatsByOrderIds(ids);

		// 批量查询采购单号（通过 ItemService）
		Map<Long, List<String>> purchaseOrderNosMap = purchaseInboundItemService.getPurchaseOrderNosByOrderIds(ids);

		// 批量填充货主名称 + 所属服务商（ID/名称）
		fillOwnerAndOperator(records);

		// 填充扩展信息
		for (PurchaseInboundPageVO vo : records) {
			// 填充统计信息
			PurchaseInboundStatsVO stats = statsMap.get(vo.getId());
			if (stats != null) {
				vo.setItemSummary(stats.getItemSummary());
				vo.setTotalExpectedQty(stats.getTotalExpectedQty());
				vo.setTotalActualQty(stats.getTotalActualQty());
				vo.setTotalShortQty(stats.getTotalShortQty());
				vo.setSkuCount(stats.getSkuCount());
			}
			// 填充采购单号
			vo.setPurchaseOrderNos(purchaseOrderNosMap.get(vo.getId()));
		}
	}

	/**
	 * 批量填充货主名称与所属 WMS 服务商（ID + 名称）。
	 * <p>货主 → 服务商映射取自 {@code sys_tenant.parent_wms_tenant_id}（业务表 wms_tenant_id 不可靠）。
	 */
	private void fillOwnerAndOperator(List<PurchaseInboundPageVO> records) {
		// 1) 查货主租户：得到货主名 + 其上级服务商ID
		Map<Long, SysTenant> ownerMap = loadTenants(records.stream()
				.map(PurchaseInboundPageVO::getErpTenantId)
				.collect(Collectors.toList()));

		// 2) 再查服务商租户名
		Map<Long, SysTenant> operatorMap = loadTenants(ownerMap.values().stream()
				.map(SysTenant::getParentWmsTenantId)
				.collect(Collectors.toList()));

		for (PurchaseInboundPageVO vo : records) {
			SysTenant owner = vo.getErpTenantId() == null ? null : ownerMap.get(vo.getErpTenantId());
			if (owner == null) {
				continue;
			}
			vo.setOwnerName(owner.getTenantName());
			Long operatorId = owner.getParentWmsTenantId();
			vo.setOperatorId(operatorId);
			SysTenant operator = operatorId == null ? null : operatorMap.get(operatorId);
			if (operator != null) {
				vo.setOperatorName(operator.getTenantName());
			}
		}
	}

	/** 批量按ID查租户，返回 id → 租户实体（自动去重、忽略 null）。 */
	private Map<Long, SysTenant> loadTenants(Collection<Long> tenantIds) {
		List<Long> ids = tenantIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
		if (ids.isEmpty()) {
			return Collections.emptyMap();
		}
		return sysTenantMapper.selectBatchIds(ids)
				.stream()
				.collect(Collectors.toMap(SysTenant::getId, Function.identity(), (a, b) -> a));
	}

	/**
	 * 校验入库单号唯一性
	 * @param inboundNo 入库单号
	 * @param excludeId 排除的ID（编辑时使用）
	 */
	private void checkInboundNoUnique(String inboundNo, Long excludeId) {
		PurchaseInboundOrder existing = baseMapper.selectByInboundNo(inboundNo, excludeId);
		Assert.isNull(existing, "入库单号已存在");
	}

	/**
	 * 校验实到数量不超过应到数量
	 * @param items 明细DTO列表
	 */
	private void validateItemQuantities(List<PurchaseInboundItemDTO> items) {
		for (PurchaseInboundItemDTO item : items) {
			Assert.isTrue(item.getActualQuantity() <= item.getExpectedQuantity(),
					String.format("SKU[%s]实到数量(%d)不能超过应到数量(%d)",
							item.getSkuCode(), item.getActualQuantity(), item.getExpectedQuantity()));
		}
	}

	/**
	 * 根据物流单ID查询关联入库单
	 * @param shippingOrderId 物流单ID
	 * @return 入库单简要信息列表
	 */
	public List<PurchaseInboundSimpleVO> getByShippingOrderId(Long shippingOrderId) {
		return baseMapper.selectByShippingOrderId(shippingOrderId);
	}

	/**
	 * 判断物流单是否存在已确认的入库单
	 * @param shippingOrderId 物流单ID
	 * @return true-存在，false-不存在
	 */
	public boolean hasConfirmedInboundByShippingOrderId(Long shippingOrderId) {
		return baseMapper.existsConfirmedByShippingOrderId(shippingOrderId);
	}

	/**
	 * 根据ID获取入库单（校验存在性）
	 * @param id 入库单ID
	 * @return 入库单实体
	 */
	public PurchaseInboundOrder getByIdOrThrow(Long id) {
		PurchaseInboundOrder order = this.getById(id);
		Assert.notNull(order, "入库单不存在，id=" + id);
		return order;
	}

	/**
	 * 创建入库单（仅创建自身，不处理关联的数量占用）
	 * <p>
	 * 注意：warehouseId 必须由调用方（Facade）设置
	 * </p>
	 * @param dto 入库单DTO
	 * @return 入库单ID
	 */
	@Transactional(rollbackFor = Exception.class)
	public Long createOrder(PurchaseInboundDTO dto) {
		// 校验入库单号唯一性
		checkInboundNoUnique(dto.getInboundNo(), null);

		// warehouseId 由 Facade 设置，不再依赖 ShippingOrderService
		Assert.notNull(dto.getWarehouseId(), "仓库ID不能为空");
		Assert.notNull(dto.getShippingOrderId(), "物流单ID不能为空");

		// 校验明细不为空
		Assert.notEmpty(dto.getItems(), "入库明细不能为空");

		// 校验实到数量不超过应到数量
		validateItemQuantities(dto.getItems());

		// 转换实体
		PurchaseInboundOrder order = PurchaseInboundConverter.INSTANCE.dtoToEntity(dto);

		// 来源=采购、货主归属=当前货主、初始状态=草稿
		order.setSourceType(InboundSourceType.PURCHASE.name());
		order.setErpTenantId(resolveOwnerTenantId());
		order.setOrderStatus(PurchaseInboundStatus.DRAFT.name());

		// 保存入库单
		this.save(order);

		// 保存明细
		purchaseInboundItemService.batchSave(order.getId(), dto.getItems());

		log.info("Created purchase inbound order, id={}, inboundNo={}, warehouseId={}",
				order.getId(), order.getInboundNo(), order.getWarehouseId());
		return order.getId();
	}

	/**
	 * 更新为已取消状态
	 * @param id 入库单ID
	 */
	@Transactional(rollbackFor = Exception.class)
	public void updateToCancelled(Long id) {
		PurchaseInboundOrder order = this.getById(id);
		Assert.notNull(order, "入库单不存在");
		assertOwnership(order);
		order.setOrderStatus(PurchaseInboundStatus.CANCELLED.name());
		boolean updated = this.updateById(order);
		if (!updated) {
			throw new OptimisticLockException("数据已被其他用户修改，请刷新后重试");
		}
		log.info("Updated purchase inbound order to cancelled, id={}", id);
	}

	/**
	 * 提交入库单（草稿 → 已提交），提交后流转给平台收货/上架
	 * @param id 入库单ID
	 */
	@Transactional(rollbackFor = Exception.class)
	public void updateToSubmitted(Long id) {
		PurchaseInboundOrder order = this.getById(id);
		Assert.notNull(order, "入库单不存在");
		assertOwnership(order);
		order.setOrderStatus(PurchaseInboundStatus.SUBMITTED.name());
		boolean updated = this.updateById(order);
		if (!updated) {
			throw new OptimisticLockException("数据已被其他用户修改，请刷新后重试");
		}
		log.info("Submitted purchase inbound order, id={}", id);
	}

	/**
	 * 创建自定义入库单（source_type=MANUAL，无物流/采购关联）
	 * @param dto 自定义入库单DTO
	 * @return 入库单ID
	 */
	@Transactional(rollbackFor = Exception.class)
	public Long createManualOrder(ManualInboundDTO dto) {
		checkInboundNoUnique(dto.getInboundNo(), null);
		Assert.notNull(dto.getWarehouseId(), "仓库ID不能为空");
		Assert.notEmpty(dto.getItems(), "入库明细不能为空");

		PurchaseInboundOrder order = new PurchaseInboundOrder();
		order.setInboundNo(dto.getInboundNo());
		order.setWarehouseId(dto.getWarehouseId());
		order.setInboundDate(dto.getInboundDate());
		order.setRemark(dto.getRemark());
		order.setSourceType(InboundSourceType.MANUAL.name());
		order.setErpTenantId(resolveOwnerTenantId());
		order.setOrderStatus(PurchaseInboundStatus.DRAFT.name());
		this.save(order);

		purchaseInboundItemService.saveManualItems(order.getId(), dto.getItems());

		log.info("Created manual inbound order, id={}, inboundNo={}, warehouseId={}",
				order.getId(), order.getInboundNo(), order.getWarehouseId());
		return order.getId();
	}

	/**
	 * 编辑自定义入库单（仅草稿可编辑）
	 * @param dto 自定义入库单DTO
	 */
	@Transactional(rollbackFor = Exception.class)
	public void updateManualOrder(ManualInboundDTO dto) {
		PurchaseInboundOrder order = this.getById(dto.getId());
		Assert.notNull(order, "入库单不存在");
		assertOwnership(order);
		Assert.isTrue(PurchaseInboundStatus.DRAFT.name().equals(order.getOrderStatus()), "只有草稿状态的入库单可以编辑");
		Assert.notEmpty(dto.getItems(), "入库明细不能为空");

		purchaseInboundItemService.deleteByInboundOrderId(order.getId());

		order.setWarehouseId(dto.getWarehouseId());
		order.setInboundDate(dto.getInboundDate());
		order.setRemark(dto.getRemark());
		boolean updated = this.updateById(order);
		if (!updated) {
			throw new OptimisticLockException("数据已被其他用户修改，请刷新后重试");
		}

		purchaseInboundItemService.saveManualItems(order.getId(), dto.getItems());
		log.info("Updated manual inbound order, id={}", order.getId());
	}

	/**
	 * 创建自定义退货单（source_type=CUSTOM_RETURN，无物流/采购关联）
	 * @param dto 自定义退货单DTO
	 * @return 退货单ID
	 */
	@Transactional(rollbackFor = Exception.class)
	public Long createCustomReturnOrder(CustomReturnDTO dto) {
		checkInboundNoUnique(dto.getInboundNo(), null);
		Assert.notNull(dto.getWarehouseId(), "仓库ID不能为空");
		Assert.notEmpty(dto.getItems(), "退货明细不能为空");
		Assert.isTrue(CustomReturnType.isValid(dto.getReturnType()), "退货类型不合法");

		PurchaseInboundOrder order = new PurchaseInboundOrder();
		order.setInboundNo(dto.getInboundNo());
		order.setReturnType(dto.getReturnType());
		order.setRefNo(dto.getRefNo());
		order.setWarehouseId(dto.getWarehouseId());
		order.setInboundDate(dto.getInboundDate());
		order.setRemark(dto.getRemark());
		order.setSourceType(InboundSourceType.CUSTOM_RETURN.name());
		order.setErpTenantId(resolveOwnerTenantId());
		order.setOrderStatus(PurchaseInboundStatus.DRAFT.name());
		this.save(order);

		purchaseInboundItemService.saveCustomReturnItems(order.getId(), dto.getItems());

		log.info("Created custom return order, id={}, inboundNo={}, warehouseId={}",
				order.getId(), order.getInboundNo(), order.getWarehouseId());
		return order.getId();
	}

	/**
	 * 编辑自定义退货单（仅草稿可编辑）
	 * @param dto 自定义退货单DTO
	 */
	@Transactional(rollbackFor = Exception.class)
	public void updateCustomReturnOrder(CustomReturnDTO dto) {
		PurchaseInboundOrder order = this.getById(dto.getId());
		Assert.notNull(order, "退货单不存在");
		assertOwnership(order);
		Assert.isTrue(PurchaseInboundStatus.DRAFT.name().equals(order.getOrderStatus()), "只有草稿状态的退货单可以编辑");
		Assert.notEmpty(dto.getItems(), "退货明细不能为空");
		Assert.isTrue(CustomReturnType.isValid(dto.getReturnType()), "退货类型不合法");

		purchaseInboundItemService.deleteByInboundOrderId(order.getId());

		order.setReturnType(dto.getReturnType());
		order.setRefNo(dto.getRefNo());
		order.setWarehouseId(dto.getWarehouseId());
		order.setInboundDate(dto.getInboundDate());
		order.setRemark(dto.getRemark());
		boolean updated = this.updateById(order);
		if (!updated) {
			throw new OptimisticLockException("数据已被其他用户修改，请刷新后重试");
		}

		purchaseInboundItemService.saveCustomReturnItems(order.getId(), dto.getItems());
		log.info("Updated custom return order, id={}", order.getId());
	}

	// ==================== 数据级可见性 ====================

	/**
	 * 入库单列表/详情的货主可见域：货主(>0)→仅自己的单；平台超管(BLOCK=-1)/无上下文→null=看全部（需收货/上架）。
	 * WMS 服务商→自身运营商 id（非货主，过滤后 0 行，符合预期，服务商不直接管理货主入库单）。
	 */
	private Long inboundErpScope() {
		Long tenant = TenantContext.getCurrentTenant();
		if (tenant == null || TenantContext.BLOCK_TENANT_ID.equals(tenant)) {
			return null;
		}
		return tenant;
	}

	/**
	 * 建单时解析货物归属货主：当前货主上下文为正值则取之；否则兜底为存量默认货主 1。
	 */
	private Long resolveOwnerTenantId() {
		Long tenant = TenantContext.getCurrentTenant();
		return (tenant != null && tenant > 0) ? tenant : 1L;
	}

	/**
	 * 校验当前身份对入库单的归属权（货主仅限自己的单；平台超管放行）。
	 */
	private void assertOwnership(PurchaseInboundOrder order) {
		Long scope = inboundErpScope();
		Assert.isTrue(scope == null || scope.equals(order.getErpTenantId()), "入库单不存在");
	}

	/**
	 * 删除入库单（仅删除自身，不处理关联的数量释放）
	 * @param id 入库单ID
	 */
	@Transactional(rollbackFor = Exception.class)
	public void deleteOrder(Long id) {
		// 归属校验，避免跨租户删除
		PurchaseInboundOrder order = this.getById(id);
		Assert.notNull(order, "入库单不存在");
		assertOwnership(order);

		// 删除明细
		purchaseInboundItemService.deleteByInboundOrderId(id);

		// 删除入库单
		this.removeById(id);

		log.info("Deleted purchase inbound order, id={}", id);
	}

}
