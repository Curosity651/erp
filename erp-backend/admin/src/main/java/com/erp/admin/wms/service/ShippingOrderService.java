package com.erp.admin.wms.service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.product.service.SkuBriefService;
import com.erp.admin.product.service.SupplierService;
import com.erp.admin.system.model.vo.SysFileVO;
import com.erp.admin.system.service.SysFileService;
import com.erp.admin.wms.converter.ShippingOrderConverter;
import com.erp.admin.wms.exception.OptimisticLockException;
import com.erp.admin.wms.mapper.PurchaseInboundMapper;
import com.erp.admin.wms.mapper.ShippingOrderMapper;
import com.erp.admin.wms.model.dto.ShippingOrderDTO;
import com.erp.admin.wms.model.dto.ShippingOrderItemDTO;
import com.erp.admin.wms.model.entity.PurchaseInboundOrderItem;
import com.erp.admin.wms.model.entity.ShippingOrder;
import com.erp.admin.wms.model.entity.ShippingOrderItem;
import com.erp.admin.wms.model.enums.ShippingMethod;
import com.erp.admin.wms.model.enums.ShippingStatus;
import com.erp.admin.wms.model.param.ReceivedQuantityUpdateParam;
import com.erp.admin.wms.model.qo.AvailableItemQO;
import com.erp.admin.wms.model.qo.AvailableShippingQO;
import com.erp.admin.wms.model.qo.ShippingOrderQO;
import com.erp.admin.wms.model.vo.AvailableItemVO;
import com.erp.admin.wms.model.vo.AvailableShippingVO;
import com.erp.admin.wms.model.vo.IncomingPlanVO;
import com.erp.admin.wms.model.vo.InTransitQuantityVO;
import com.erp.admin.wms.model.vo.ShippingOrderArrivalStatsVO;
import com.erp.admin.wms.model.vo.ShippingOrderInboundedQuantityVO;
import com.erp.admin.wms.model.vo.ShippingOrderDetailVO;
import com.erp.admin.wms.model.vo.ShippingOrderExportVO;
import com.erp.admin.wms.model.vo.ShippingOrderItemVO;
import com.erp.admin.wms.model.vo.ShippingOrderPageVO;
import com.erp.admin.wms.model.vo.ShippingOrderSimpleVO;
import com.erp.admin.wms.model.vo.ShippingOrderStatsVO;
import com.erp.admin.wms.model.vo.ShippingQuantityStatsVO;
import com.erp.admin.wms.service.helper.ShippingOrderPaymentHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.mybatisplus.service.impl.ExtendServiceImpl;
import org.ballcat.mybatisplus.toolkit.PageUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * 物流单服务
 *
 * @author erp
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShippingOrderService extends ExtendServiceImpl<ShippingOrderMapper, ShippingOrder> {

	private final ShippingOrderItemService shippingOrderItemService;

	private final SysFileService sysFileService;

	private final RegionService regionService;

	private final LogisticsProviderService logisticsProviderService;

	private final SkuBriefService skuBriefService;

	private final SupplierService supplierService;

	private final ShippingOrderPaymentHelper paymentHelper;

	private final PurchaseInboundMapper purchaseInboundMapper;

	/**
	 * 允许的付款凭证文件类型
	 */
	private static final List<String> ALLOWED_VOUCHER_TYPES = java.util.Arrays.asList(
			"application/pdf", "image/jpeg", "image/jpg", "image/png"
	);

	/**
	 * 分页查询
	 * @param pageParam 分页参数
	 * @param qo 查询条件
	 * @return 分页结果
	 */
	public PageResult<ShippingOrderPageVO> queryPage(PageParam pageParam, ShippingOrderQO qo) {
		// 默认按发货日期倒序排列
		if (pageParam.getSorts() == null || pageParam.getSorts().isEmpty()) {
			PageParam.Sort sort = new PageParam.Sort();
			sort.setField("shipping_date");
			sort.setAsc(false);
			pageParam.getSorts().add(sort);
		}

		IPage<ShippingOrderPageVO> page = PageUtil.prodPage(pageParam);
		baseMapper.queryPage(page, qo);

		List<ShippingOrderPageVO> records = page.getRecords();
		if (records.isEmpty()) {
			return new PageResult<>(records, page.getTotal());
		}

		// 批量填充数据
		logisticsProviderService.enrichProviderName(records,
			ShippingOrderPageVO::getProviderId,
			ShippingOrderPageVO::setProviderName);

		regionService.enrichRegionDisplay(
				records,
				ShippingOrderPageVO::getTargetRegionId,
				ShippingOrderPageVO::setTargetRegion
		);

		// 批量填充SKU种类数和数量统计
		List<Long> ids = records.stream().map(ShippingOrderPageVO::getId).collect(Collectors.toList());
		Map<Long, Integer> skuCountMap = shippingOrderItemService.getSkuCountMapByShippingOrderIds(ids);
		Map<Long, ShippingQuantityStatsVO> quantityStatsMap = shippingOrderItemService.getQuantityStatsMapByShippingOrderIds(ids);

		for (ShippingOrderPageVO record : records) {
			Long id = record.getId();
			record.setSkuCount(skuCountMap.getOrDefault(id, 0));

			// 填充发货/入库数量
			ShippingQuantityStatsVO stats = quantityStatsMap.get(id);
			if (stats != null) {
				record.setTotalShippedQuantity(stats.getTotalQuantity());
				record.setTotalReceivedQuantity(stats.getReceivedQuantity());
				record.setPendingInboundQuantity(stats.getPendingInboundQuantity());
			} else {
				record.setTotalShippedQuantity(0);
				record.setTotalReceivedQuantity(0);
				record.setPendingInboundQuantity(0);
			}
		}

		return new PageResult<>(records, page.getTotal());
	}


	/**
	 * 获取物流单详情
	 * @param id 物流单ID
	 * @return 物流单详情
	 */
	public ShippingOrderDetailVO getDetail(Long id) {
		ShippingOrderDetailVO detail = baseMapper.selectDetailById(id);
		Assert.notNull(detail, "物流单不存在");

		// 填充物流商名称
		if (detail.getProviderId() != null) {
			String providerName = logisticsProviderService.getNameById(detail.getProviderId());
			detail.setProviderName(providerName);
		}

		// 填充区域名称
		regionService.enrichRegionDisplay(Collections.singletonList(detail),
				ShippingOrderDetailVO::getTargetRegionId,
				ShippingOrderDetailVO::setTargetRegion);

		// 填充付款凭证文件信息
		if (detail.getPaymentVoucherFileId() != null) {
			SysFileVO fileVO = sysFileService.getFileInfo(detail.getPaymentVoucherFileId());
			if (fileVO != null) {
				detail.setPaymentVoucherFileName(fileVO.getFileName());
				detail.setPaymentVoucherFileSize(fileVO.getFileSize());
			}
		}

		// 查询明细列表
		List<ShippingOrderItemVO> items = shippingOrderItemService.getVoListByShippingOrderId(id);
		detail.setItems(items);

		return detail;
	}

	/**
	 * 创建物流单（草稿占用校验由 Facade 在同一事务中完成）
	 * @param dto 物流单DTO
	 * @return 物流单ID
	 */
	@Transactional(rollbackFor = Exception.class)
	public Long createOrder(ShippingOrderDTO dto) {
		// 校验物流单号唯一性
		checkShippingNoUnique(dto.getShippingNo(), null);

		// 校验明细不为空
		Assert.notEmpty(dto.getItems(), "货物明细不能为空");

		// 校验费用字段（根据物流方式）
		validateCostFields(dto);

		// 转换实体
		ShippingOrder order = ShippingOrderConverter.INSTANCE.dtoToEntity(dto);

		// 自动计算发货件数（从明细汇总）
		order.setPackageCount(calculatePackageCount(dto.getItems()));

		// 设置初始状态
		order.setShippingStatus(ShippingStatus.PENDING.name());

		// 处理付款信息
		paymentHelper.processPaymentInfo(order, dto.getPaymentStatus(), dto.getPaymentVoucherFileId());

		// 保存物流单
		this.save(order);

		// 保存明细
		shippingOrderItemService.batchSave(order.getId(), dto.getItems());

		log.info("Created shipping order, id={}, shippingNo={}", order.getId(), order.getShippingNo());
		return order.getId();
	}

	/**
	 * 更新物流单全部字段及明细（PENDING 状态使用）
	 * @param id 物流单ID
	 * @param dto 物流单DTO
	 */
	@Transactional(rollbackFor = Exception.class)
	public void updateWithItems(Long id, ShippingOrderDTO dto) {
		ShippingOrder order = this.getById(id);
		Assert.notNull(order, "物流单不存在");

		// 校验物流单号唯一性（排除自身）
		checkShippingNoUnique(dto.getShippingNo(), id);

		// 删除原有明细
		shippingOrderItemService.deleteByShippingOrderId(id);

		// 更新物流单
		ShippingOrderConverter.INSTANCE.updateEntity(dto, order);

		// 自动计算发货件数
		order.setPackageCount(calculatePackageCount(dto.getItems()));

		// 处理付款信息
		paymentHelper.processPaymentInfo(order, dto.getPaymentStatus(), dto.getPaymentVoucherFileId());

		// 保存更新
		boolean updated = this.updateById(order);
		if (!updated) {
			throw new OptimisticLockException("数据已被其他用户修改，请刷新后重试");
		}

		// 保存新明细
		shippingOrderItemService.batchSave(id, dto.getItems());
		List<ShippingOrderItem> items = shippingOrderItemService.getByShippingOrderId(id);
		shippingOrderItemService.validateAndLockShippingAllocation(id, items);

		log.info("Updated shipping order with items, id={}", id);
	}

	/**
	 * 更新物流单（SHIPPED 状态使用）
	 * 可更新：目标区域、物流/费用信息（未付款时）、备注、付款信息
	 * @param id 物流单ID
	 * @param dto 物流单DTO
	 * @param newRegionId 新目标区域ID（如果区域变更）
	 */
	@Transactional(rollbackFor = Exception.class)
	public void updateForShipped(Long id, ShippingOrderDTO dto, Long newRegionId) {
		ShippingOrder order = this.getById(id);
		Assert.notNull(order, "物流单不存在");

		// 更新目标区域
		if (newRegionId != null) {
			order.setTargetRegionId(newRegionId);
		}

		// 更新物流/费用信息
		updateLogisticsAndCostFields(order, dto);

		// 更新备注
		order.setRemark(dto.getRemark());

		// 预计到货日期
		order.setEstimatedArrivalDate(dto.getEstimatedArrivalDate());
		order.setEstimatedDays(dto.getEstimatedDays());

		// 处理付款信息
		paymentHelper.processPaymentInfo(order, dto.getPaymentStatus(), dto.getPaymentVoucherFileId());

		// 检查是否满足完成条件
		paymentHelper.checkAndUpdateCompletedStatus(order);

		// 保存更新
		boolean updated = this.updateById(order);
		if (!updated) {
			throw new OptimisticLockException("数据已被其他用户修改，请刷新后重试");
		}

		log.info("Updated shipping order for shipped status, id={}", id);
	}

	/**
	 * 更新物流单 ARRIVED 状态使用
	 * 可更新：物流/费用信息（未付款时）、备注、付款信息
	 * @param id 物流单ID
	 * @param dto 物流单DTO
	 */
	@Transactional(rollbackFor = Exception.class)
	public void updateForArrived(Long id, ShippingOrderDTO dto) {
		ShippingOrder order = this.getById(id);
		Assert.notNull(order, "物流单不存在");

		// 更新物流/费用信息
		updateLogisticsAndCostFields(order, dto);

		// 更新备注
		order.setRemark(dto.getRemark());

		// 预计到货日期
		order.setEstimatedArrivalDate(dto.getEstimatedArrivalDate());
		order.setEstimatedDays(dto.getEstimatedDays());

		// 处理付款信息
		paymentHelper.processPaymentInfo(order, dto.getPaymentStatus(), dto.getPaymentVoucherFileId());

		// 检查是否满足完成条件
		paymentHelper.checkAndUpdateCompletedStatus(order);

		// 保存更新
		boolean updated = this.updateById(order);
		if (!updated) {
			throw new OptimisticLockException("数据已被其他用户修改，请刷新后重试");
		}

		log.info("Updated shipping order for arrived status, id={}", id);
	}

	/**
	 * 获取可发货采购明细
	 * @param qo 查询条件
	 * @return 可发货采购明细列表
	 */
	public List<AvailableItemVO> getAvailableItems(AvailableItemQO qo) {
		List<AvailableItemVO> records = baseMapper.selectAvailableItems(qo);

		if (!records.isEmpty()) {
			// 填充 SKU 简要信息
			skuBriefService.enrichForQuery(
					records,
					AvailableItemVO::getSkuCode,
					AvailableItemVO::setSkuBrief
			);

			// 批量填充供应商名称
			Set<Long> supplierIds = records.stream()
					.map(AvailableItemVO::getSupplierId)
					.filter(Objects::nonNull)
					.collect(Collectors.toSet());

			if (!supplierIds.isEmpty()) {
				Map<Long, String> supplierNameMap = supplierService.getNameMapByIds(supplierIds);
				for (AvailableItemVO vo : records) {
					if (vo.getSupplierId() != null) {
						vo.setSupplierName(supplierNameMap.get(vo.getSupplierId()));
					}
				}
			}
		}

		return records;
	}

	/**
	 * 校验物流单号唯一性
	 * @param shippingNo 物流单号
	 * @param excludeId 排除的ID（编辑时使用）
	 */
	private void checkShippingNoUnique(String shippingNo, Long excludeId) {
		ShippingOrder existing = baseMapper.selectByShippingNo(shippingNo, excludeId);
		Assert.isNull(existing, "物流单号已存在");
	}

	/**
	 * 更新物流/费用字段
	 * @param order 物流单实体
	 * @param dto 物流单DTO
	 */
	private void updateLogisticsAndCostFields(ShippingOrder order, ShippingOrderDTO dto) {
		order.setShippingMethod(dto.getShippingMethod());
		order.setShippingRoute(dto.getShippingRoute());
		order.setTotalWeight(dto.getTotalWeight());
		order.setUnitPrice(dto.getUnitPrice());
		order.setShippingFee(dto.getShippingFee());
		order.setMiscFee(dto.getMiscFee());
		order.setTotalAmount(dto.getTotalAmount());
		order.setTotalAmountCny(dto.getTotalAmountCny());
	}

	/**
	 * 校验费用字段（根据物流方式）
	 * @param dto 物流单DTO
	 */
	private void validateCostFields(ShippingOrderDTO dto) {
		String shippingMethod = dto.getShippingMethod();
		
		if (ShippingMethod.GRAY.getCode().equals(shippingMethod)) {
			// 灰关模式：物流单价必填
			Assert.notNull(dto.getUnitPrice(), "灰关模式下，物流单价不能为空");
			Assert.isTrue(dto.getUnitPrice().compareTo(java.math.BigDecimal.ZERO) > 0, "灰关模式下，物流单价必须大于0");
		} else if (ShippingMethod.WHITE.getCode().equals(shippingMethod)) {
			// 白关模式：运输费用和杂费必填
			Assert.notNull(dto.getShippingFee(), "白关模式下，运输费用不能为空");
			Assert.isTrue(dto.getShippingFee().compareTo(java.math.BigDecimal.ZERO) > 0, "白关模式下，运输费用必须大于0");
			Assert.notNull(dto.getMiscFee(), "白关模式下，杂费不能为空");
			Assert.isTrue(dto.getMiscFee().compareTo(java.math.BigDecimal.ZERO) >= 0, "白关模式下，杂费不能为负数");
		} else {
			// 非法的物流方式
			throw new IllegalArgumentException("无效的物流方式: " + shippingMethod);
		}
	}

	/**
	 * 计算发货件数（从明细汇总）
	 * @param items 明细DTO列表
	 * @return 发货件数
	 */
	private Integer calculatePackageCount(List<ShippingOrderItemDTO> items) {
		if (CollectionUtils.isEmpty(items)) {
			return 0;
		}
		return items.stream()
				.mapToInt(item -> item.getQuantity() != null ? item.getQuantity() : 0)
				.sum();
	}

	/**
	 * 更新付款状态
	 * @param id 物流单ID
	 * @param paymentStatus 付款状态: 0-未付 / 1-已付
	 * @param voucherFileId 付款凭证文件ID（paymentStatus=1时必传）
	 */
	@Transactional(rollbackFor = Exception.class)
	public void updatePaymentStatus(Long id, Integer paymentStatus, Long voucherFileId) {
		ShippingOrder order = this.getById(id);
		Assert.notNull(order, "物流单不存在");

		paymentHelper.processPaymentInfo(order, paymentStatus, voucherFileId);

		// 检查是否满足完成条件：全部到货 + 已付款
		paymentHelper.checkAndUpdateCompletedStatus(order);

		boolean updated = this.updateById(order);
		if (!updated) {
			throw new OptimisticLockException("数据已被其他用户修改，请刷新后重试");
		}

		log.info("Updated payment status, id={}, paymentStatus={}", id, paymentStatus);
	}

	/**
	 * 导出物流单列表
	 * @param qo 查询条件
	 * @return 导出数据列表
	 */
	public List<ShippingOrderExportVO> listForExport(ShippingOrderQO qo) {
		List<ShippingOrderExportVO> records = baseMapper.selectListForExport(qo);

		if (!records.isEmpty()) {
			// 批量填充物流商名称
			logisticsProviderService.enrichProviderName(records,
					ShippingOrderExportVO::getProviderId,
					ShippingOrderExportVO::setProviderName);


			// 批量查询关联采购单号
			List<Long> ids = records.stream().map(ShippingOrderExportVO::getId).collect(Collectors.toList());
			List<ShippingOrderStatsVO> statsList = baseMapper.selectPurchaseOrderNosByIds(ids);

			// Service 层聚合：按物流单ID分组，拼接采购单号
			Map<Long, String> purchaseOrderNosMap = statsList.stream()
					.collect(Collectors.groupingBy(
							ShippingOrderStatsVO::getShippingOrderId,
							Collectors.mapping(ShippingOrderStatsVO::getPurchaseOrderNo,
									Collectors.joining(", "))
					));

			// 填充采购单号
			for (ShippingOrderExportVO vo : records) {
				vo.setPurchaseOrderNos(purchaseOrderNosMap.get(vo.getId()));
			}
		}

		return records;
	}

	/**
	 * 根据采购单ID查询关联物流单
	 * @param purchaseOrderId 采购单ID
	 * @return 关联物流单列表
	 */
	public List<ShippingOrderSimpleVO> getByPurchaseOrderId(Long purchaseOrderId) {
		List<ShippingOrderSimpleVO> records = baseMapper.selectByPurchaseOrderId(purchaseOrderId);

		if (!records.isEmpty()) {
			// 批量填充物流商名称
			logisticsProviderService.enrichProviderName(records,
					ShippingOrderSimpleVO::getProviderId,
					ShippingOrderSimpleVO::setProviderName);
		}

		return records;
	}

	/**
	 * 根据ID获取物流单实体（校验存在性）
	 * @param id 物流单ID
	 * @return 物流单实体
	 * @throws IllegalArgumentException 物流单不存在时抛出
	 */
	public ShippingOrder getShippingOrderByIdOrThrow(Long id) {
		ShippingOrder order = this.getById(id);
		Assert.notNull(order, "物流单不存在，id=" + id);
		return order;
	}

	/**
	 * 根据ID获取物流单（校验存在性）- Facade 使用
	 * @param id 物流单ID
	 * @return 物流单实体
	 */
	public ShippingOrder getByIdOrThrow(Long id) {
		return getShippingOrderByIdOrThrow(id);
	}

	/**
	 * 更新为已发货状态
	 * @param id 物流单ID
	 * @param postingId 库存过账单ID
	 */
	@Transactional(rollbackFor = Exception.class)
	public void updateToShipped(Long id, Long postingId) {
		ShippingOrder order = this.getById(id);
		order.setShippingStatus(ShippingStatus.SHIPPED.name());
		order.setStockPostingId(postingId);
		boolean updated = this.updateById(order);
		if (!updated) {
			throw new OptimisticLockException("数据已被其他用户修改，请刷新后重试");
		}
		log.info("Updated shipping order to shipped, id={}, postingId={}", id, postingId);
	}

	/**
	 * 重新计算到货状态
	 * @param shippingOrderId 物流单ID
	 */
	@Transactional(rollbackFor = Exception.class)
	public void recalculateArrivalStatus(Long shippingOrderId) {
		ShippingOrder order = this.getById(shippingOrderId);
		Assert.notNull(order, "物流单不存在");

		// 获取到货统计
		ShippingOrderArrivalStatsVO stats = shippingOrderItemService.getArrivalStats(shippingOrderId);

		// 计算新状态
		ShippingStatus newStatus;
		if (!stats.isPartialArrived() && !stats.isAllArrived()) {
			// 无到货，保持已发货状态
			return;
		} else if (stats.isAllArrived()) {
			newStatus = ShippingStatus.ALL_ARRIVED;
		} else {
			newStatus = ShippingStatus.PARTIAL_ARRIVED;
		}

		// 更新状态
		order.setShippingStatus(newStatus.name());
		boolean updated = this.updateById(order);
		if (!updated) {
			throw new OptimisticLockException("数据已被其他用户修改，请刷新后重试");
		}

		// 检查是否满足完成条件
		paymentHelper.checkAndUpdateCompletedStatus(order);
		if (ShippingStatus.COMPLETED.name().equals(order.getShippingStatus())) {
			this.updateById(order);
		}

		log.info("Recalculated arrival status for shipping order, id={}, status={}", shippingOrderId, newStatus);
	}

	/**
	 * 删除物流单（仅删除自身，不处理关联的数量释放）
	 * @param id 物流单ID
	 */
	@Transactional(rollbackFor = Exception.class)
	public void deleteOrder(Long id) {
		// 删除明细
		shippingOrderItemService.deleteByShippingOrderId(id);

		// 删除物流单
		this.removeById(id);

		log.info("Deleted shipping order, id={}", id);
	}

	/**
	 * 更新物流单明细的已到货数量
	 * @param shippingOrderId 物流单ID
	 * @param inboundItems 入库单明细列表
	 */
	@Transactional(rollbackFor = Exception.class)
	public void updateItemReceivedQuantity(Long shippingOrderId, List<PurchaseInboundOrderItem> inboundItems) {
		if (inboundItems == null || inboundItems.isEmpty()) {
			return;
		}

		List<ReceivedQuantityUpdateParam> params = inboundItems.stream()
				.filter(item -> item.getActualQuantity() != null && item.getActualQuantity() > 0)
				.map(item -> ReceivedQuantityUpdateParam.builder()
						.shippingOrderItemId(item.getShippingOrderItemId())
						.quantity(item.getActualQuantity())
						.build())
				.collect(Collectors.toList());

		shippingOrderItemService.batchIncreaseReceivedQuantity(params);
		log.info("Updated received quantity for shipping order, id={}, itemCount={}", shippingOrderId, params.size());
	}

	/**
	 * 更新物流单到货状态
	 * @param shippingOrderId 物流单ID
	 */
	@Transactional(rollbackFor = Exception.class)
	public void updateArrivalStatus(Long shippingOrderId) {
		ShippingOrder order = this.getById(shippingOrderId);
		Assert.notNull(order, "物流单不存在");

		// 获取到货统计
		ShippingOrderArrivalStatsVO stats = shippingOrderItemService.getArrivalStats(shippingOrderId);

		// 计算新状态
		ShippingStatus newStatus;
		if (!stats.isPartialArrived() && !stats.isAllArrived()) {
			// 无到货，保持已发货状态
			return;
		} else if (stats.isAllArrived()) {
			newStatus = ShippingStatus.ALL_ARRIVED;
		} else {
			newStatus = ShippingStatus.PARTIAL_ARRIVED;
		}

		// 更新状态
		order.setShippingStatus(newStatus.name());
		boolean updated = this.updateById(order);
		if (!updated) {
			throw new OptimisticLockException("数据已被其他用户修改，请刷新后重试");
		}

		// 检查是否满足完成条件
		paymentHelper.checkAndUpdateCompletedStatus(order);
		if (ShippingStatus.COMPLETED.name().equals(order.getShippingStatus())) {
			this.updateById(order);
		}

		log.info("Updated arrival status for shipping order, id={}, status={}", shippingOrderId, newStatus);
	}

	/**
	 * 根据物流单ID获取物流单信息（用于入库单创建页回填）
	 * @param shippingOrderId 物流单ID
	 * @return 物流单信息
	 */
	public AvailableShippingVO getShippingById(Long shippingOrderId) {
		ShippingOrder shippingOrder = this.baseMapper.selectById(shippingOrderId);
		if (shippingOrder == null) {
			return null;
		}

		AvailableShippingVO vo = new AvailableShippingVO();
		vo.setId(shippingOrder.getId());
		vo.setShippingNo(shippingOrder.getShippingNo());
		vo.setProviderId(shippingOrder.getProviderId());
		vo.setShippingDate(shippingOrder.getShippingDate());
		vo.setTargetRegionId(shippingOrder.getTargetRegionId());

		// 填充物流商名称
		if (shippingOrder.getProviderId() != null) {
			logisticsProviderService.enrichProviderName(
					Collections.singletonList(vo),
					AvailableShippingVO::getProviderId,
					AvailableShippingVO::setProviderName
			);
		}

		// 填充区域名称
		if (shippingOrder.getTargetRegionId() != null) {
			regionService.enrichRegionDisplay(
					Collections.singletonList(vo),
					AvailableShippingVO::getTargetRegionId,
					AvailableShippingVO::setTargetRegion);
		}

		// 填充数量统计
		ShippingQuantityStatsVO stats = baseMapper.selectShippingQuantityStats(shippingOrderId);
		if (stats != null) {
			vo.setTotalQuantity(stats.getTotalQuantity());
			vo.setReceivedQuantity(stats.getReceivedQuantity());
			vo.setPendingQuantity(stats.getTotalQuantity() - stats.getReceivedQuantity());
		}

		return vo;
	}

	/**
	 * 获取可入库物流单列表
	 * @param qo 查询条件
	 * @return 可入库物流单列表
	 */
	public List<AvailableShippingVO> getAvailableShipping(AvailableShippingQO qo) {
		List<AvailableShippingVO> list = baseMapper.selectAvailableShipping(qo);

		if (!list.isEmpty()) {
			// 批量填充区域名称
			regionService.enrichRegionDisplay(list,
					AvailableShippingVO::getTargetRegionId,
					AvailableShippingVO::setTargetRegion);
		}

		return list;
	}

	/**
	 * 批量查询在途库存数量
	 * <p>
	 * 查询已发货和部分到货状态的物流单，按区域+SKU汇总待入库数量
	 *
	 * @param regionIds 区域ID集合
	 * @param skuCodes SKU编码集合
	 * @return Map key="regionId:skuCode", value=在途数量
	 */
	public Map<String, Integer> getInTransitQuantityMap(Set<Long> regionIds, Set<String> skuCodes) {
		if (CollectionUtils.isEmpty(regionIds) || CollectionUtils.isEmpty(skuCodes)) {
			return Collections.emptyMap();
		}
		List<InTransitQuantityVO> results = baseMapper.selectInTransitQuantity(
				regionIds, skuCodes,
				Arrays.asList(ShippingStatus.SHIPPED.name(), ShippingStatus.PARTIAL_ARRIVED.name()));

		Map<String, Integer> map = new HashMap<>();
		for (InTransitQuantityVO vo : results) {
			String key = vo.getRegionId() + ":" + vo.getSkuCode();
			map.merge(key, vo.getQuantity(), Integer::sum);
		}
		return map;
	}

	/**
	 * 查询入库计划
	 * <p>
	 * 查询指定区域和SKU的在途物流单预计到货信息
	 *
	 * @param regionId 区域ID
	 * @param skuCode SKU编码
	 * @return 入库计划列表
	 */
	public List<IncomingPlanVO> getIncomingPlan(Long regionId, String skuCode) {
		// 1. 查询原始物流单预计入库数据
		List<IncomingPlanVO> plans = baseMapper.selectIncomingPlan(regionId, skuCode,
				Arrays.asList(ShippingStatus.SHIPPED.name(), ShippingStatus.PARTIAL_ARRIVED.name()));

		if (CollectionUtils.isEmpty(plans)) {
			return Collections.emptyList();
		}

		// 2. 提取物流单ID列表
		List<Long> shippingOrderIds = plans.stream()
				.map(IncomingPlanVO::getShippingOrderId)
				.filter(Objects::nonNull)
				.distinct()
				.collect(Collectors.toList());

		if (CollectionUtils.isEmpty(shippingOrderIds)) {
			return plans;
		}

		// 3. 查询已入库数量统计（按物流单ID分组）
		List<ShippingOrderInboundedQuantityVO> inboundedList = purchaseInboundMapper.sumInboundedQuantityByShippingOrderIds(shippingOrderIds);
		Map<Long, Integer> inboundedMap = inboundedList.stream()
				.collect(Collectors.toMap(
						ShippingOrderInboundedQuantityVO::getShippingOrderId,
						ShippingOrderInboundedQuantityVO::getInboundedQuantity
				));

		// 4. 计算剩余数量并过滤
		return plans.stream()
				.map(plan -> {
					Integer inboundedQty = inboundedMap.getOrDefault(plan.getShippingOrderId(), 0);
					int remainingQty = plan.getQuantity() - inboundedQty;

					// 只返回剩余数量大于0的记录
					if (remainingQty > 0) {
						IncomingPlanVO newPlan = new IncomingPlanVO();
						newPlan.setShippingOrderId(plan.getShippingOrderId());
						newPlan.setShippingNo(plan.getShippingNo());
						newPlan.setEstimatedArrivalDate(plan.getEstimatedArrivalDate());
						newPlan.setQuantity(remainingQty);
						return newPlan;
					}
					return null;
				})
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}

	/**
	 * 批量查询入库计划按日期分组
	 * <p>
	 * 查询多个区域+SKU 的在途物流单，按 "regionId:skuCode" → 日期 → 数量 聚合。
	 * 仅包含有预计到达日期的在途/部分到达物流单。
	 *
	 * @param regionIds 区域 ID 集合
	 * @param skuCodes     SKU 编码集合
	 * @return Map<"regionId:skuCode", Map<到达日期, 入库量>>
	 */
	public Map<String, Map<LocalDate, Integer>> batchGetIncomingByDate(
			Set<Long> regionIds, Set<String> skuCodes) {
		if (CollectionUtils.isEmpty(regionIds) || CollectionUtils.isEmpty(skuCodes)) {
			return Collections.emptyMap();
		}

		List<String> statuses = Arrays.asList(
				ShippingStatus.SHIPPED.name(),
				ShippingStatus.PARTIAL_ARRIVED.name());

		Map<String, Map<LocalDate, Integer>> result = new HashMap<>();
		for (Long regionId : regionIds) {
			for (String skuCode : skuCodes) {
				List<IncomingPlanVO> plans = baseMapper.selectIncomingPlan(regionId, skuCode, statuses);
				if (plans.isEmpty()) {
					continue;
				}

				// 扣减已入库数量（与 getIncomingPlan 保持一致）
				List<Long> shippingOrderIds = plans.stream()
						.map(IncomingPlanVO::getShippingOrderId)
						.filter(Objects::nonNull)
						.distinct()
						.collect(Collectors.toList());

				Map<Long, Integer> inboundedMap = Collections.emptyMap();
				if (!shippingOrderIds.isEmpty()) {
					List<ShippingOrderInboundedQuantityVO> inboundedList =
							purchaseInboundMapper.sumInboundedQuantityByShippingOrderIds(shippingOrderIds);
					inboundedMap = inboundedList.stream()
							.collect(Collectors.toMap(
									ShippingOrderInboundedQuantityVO::getShippingOrderId,
									ShippingOrderInboundedQuantityVO::getInboundedQuantity
							));
				}

				String key = regionId + ":" + skuCode;
				Map<LocalDate, Integer> dateMap = new HashMap<>();
				for (IncomingPlanVO plan : plans) {
					if (plan.getEstimatedArrivalDate() == null) {
						continue;
					}
					int inboundedQty = inboundedMap.getOrDefault(plan.getShippingOrderId(), 0);
					int remainingQty = plan.getQuantity() - inboundedQty;
					if (remainingQty > 0) {
						dateMap.merge(plan.getEstimatedArrivalDate(), remainingQty, Integer::sum);
					}
				}
				if (!dateMap.isEmpty()) {
					result.put(key, dateMap);
				}
			}
		}
		return result;
	}

	/**
	 * 批量查询待发货库存数量
	 * <p>
	 * 查询待发货状态的物流单，按区域+SKU汇总待发货数量。
	 * 待发货指采购已确认但物流单尚未发货的货物。
	 *
	 * @param regionIds 区域ID集合
	 * @param skuCodes SKU编码集合
	 * @return Map key="regionId:skuCode", value=待发货数量
	 */
	public Map<String, Integer> getPendingShipmentQuantityMap(Set<Long> regionIds, Set<String> skuCodes) {
		if (CollectionUtils.isEmpty(regionIds) || CollectionUtils.isEmpty(skuCodes)) {
			return Collections.emptyMap();
		}
		// 查询待发货状态的物流单
		List<InTransitQuantityVO> results = baseMapper.selectInTransitQuantity(
				regionIds, skuCodes,
				Collections.singletonList(ShippingStatus.PENDING.name()));

		Map<String, Integer> map = new HashMap<>();
		for (InTransitQuantityVO vo : results) {
			String key = vo.getRegionId() + ":" + vo.getSkuCode();
			map.merge(key, vo.getQuantity(), Integer::sum);
		}
		return map;
	}

}
