package com.erp.admin.wms.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.product.model.entity.Supplier;
import com.erp.admin.product.model.vo.SkuBriefVO;
import com.erp.admin.product.service.SkuBriefService;
import com.erp.admin.product.service.SupplierService;
import com.erp.admin.wms.converter.PurchaseOrderConverter;
import com.erp.admin.wms.exception.OptimisticLockException;
import com.erp.admin.wms.mapper.PurchaseOrderFileMapper;
import com.erp.admin.wms.mapper.PurchaseOrderMapper;
import com.erp.admin.wms.model.dto.PurchaseOrderDTO;
import com.erp.admin.wms.model.entity.PurchaseOrder;
import com.erp.admin.wms.model.entity.PurchaseOrderItem;
import com.erp.admin.wms.model.enums.PurchaseOrderFileType;
import com.erp.admin.wms.model.enums.PurchaseOrderStatus;
import com.erp.admin.wms.model.enums.PurchaseReceivingStatus;
import com.erp.admin.wms.model.enums.PurchaseShippingStatus;
import com.erp.admin.wms.model.qo.PurchaseOrderQO;
import com.erp.admin.wms.model.vo.FileInfoVO;
import com.erp.admin.wms.model.vo.PurchaseOrderDetailVO;
import com.erp.admin.wms.model.vo.PurchaseOrderExportVO;
import com.erp.admin.wms.model.vo.PurchaseOrderItemVO;
import com.erp.admin.wms.model.vo.PurchaseOrderPageVO;
import com.erp.admin.wms.model.vo.PurchaseOrderQcItemVO;
import com.erp.admin.wms.model.vo.PurchaseOrderStatVO;
import com.erp.admin.wms.service.handler.ContractHandler;
import com.erp.admin.wms.service.handler.OtherFileHandler;
import com.erp.admin.wms.service.handler.PaymentHandler;
import com.erp.admin.wms.service.handler.QcDataHandler;
import com.erp.admin.wms.service.validator.PurchaseOrderValidator;
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
 * 采购单服务
 *
 * @author erp
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseOrderService extends ExtendServiceImpl<PurchaseOrderMapper, PurchaseOrder> {

	private final PurchaseOrderItemService purchaseOrderItemService;

	private final PurchaseOrderQcItemService purchaseOrderQcItemService;

	private final PurchaseOrderFileMapper purchaseOrderFileMapper;

	private final SkuBriefService skuBriefService;

	private final SupplierService supplierService;

	private final PaymentHandler paymentHandler;

	private final ContractHandler contractHandler;

	private final QcDataHandler qcDataHandler;

	private final OtherFileHandler otherFileHandler;

	private final PurchaseOrderValidator purchaseOrderValidator;

	/**
	 * 分页查询
	 * @param pageParam 分页参数
	 * @param qo 查询条件
	 * @return 分页结果
	 */
	public PageResult<PurchaseOrderPageVO> queryPage(PageParam pageParam, PurchaseOrderQO qo) {
		// 默认按下单日期倒序排列
		if (pageParam.getSorts() == null || pageParam.getSorts().isEmpty()) {
			PageParam.Sort sort = new PageParam.Sort();
			sort.setField("order_date");
			sort.setAsc(false);
			pageParam.getSorts().add(sort);
		}

		IPage<PurchaseOrderPageVO> page = PageUtil.prodPage(pageParam);
		baseMapper.queryPage(page, qo);

		List<PurchaseOrderPageVO> records = page.getRecords();
		if (!records.isEmpty()) {
			// 提取采购单ID列表
			List<Long> orderIds = records.stream()
					.map(PurchaseOrderPageVO::getId)
					.collect(Collectors.toList());

			// 批量查询统计数据
			Map<Long, PurchaseOrderStatVO> statMap = purchaseOrderItemService.getStatByOrderIds(orderIds);

			// 填充统计数据和状态描述
			for (PurchaseOrderPageVO vo : records) {
				vo.setOrderStatusDesc(getStatusDesc(vo.getOrderStatus()));
				vo.setShippingStatusDesc(getShippingStatusDesc(vo.getShippingStatus()));
				vo.setReceivingStatusDesc(getReceivingStatusDesc(vo.getReceivingStatus()));

				PurchaseOrderStatVO stat = statMap.get(vo.getId());
				if (stat != null) {
					vo.setSkuCount(stat.getSkuCount());
					vo.setTotalQuantity(stat.getTotalQuantity());
					vo.setTotalShippedQuantity(stat.getTotalShippedQuantity());
					vo.setTotalReceivedQuantity(stat.getTotalReceivedQuantity());
				} else {
					vo.setSkuCount(0);
					vo.setTotalQuantity(0);
					vo.setTotalShippedQuantity(0);
					vo.setTotalReceivedQuantity(0);
				}
			}
		}

		return new PageResult<>(records, page.getTotal());
	}

	/**
	 * 获取详情
	 * <p>
	 * 返回完整数据，包括付款凭证、合同文件、质检数据、其他附件
	 *
	 * @param id 采购单ID
	 * @return 详情VO
	 */
	public PurchaseOrderDetailVO getDetail(Long id) {
		PurchaseOrder order = this.getById(id);
		Assert.notNull(order, "采购单不存在");

		PurchaseOrderDetailVO vo = PurchaseOrderConverter.INSTANCE.entityToDetailVo(order);
		vo.setOrderStatusDesc(getStatusDesc(order.getOrderStatus()));
		vo.setShippingStatusDesc(getShippingStatusDesc(order.getShippingStatus()));
		vo.setReceivingStatusDesc(getReceivingStatusDesc(order.getReceivingStatus()));

		// 填充供应商名称
		if (order.getSupplierId() != null) {
			Supplier supplier = supplierService.getById(order.getSupplierId());
			if (supplier != null) {
				vo.setSupplierName(supplier.getName());
			}
		}

		// 查询明细
		List<PurchaseOrderItemVO> items = purchaseOrderItemService.getVoListByPurchaseOrderId(id);
		vo.setItems(items);

		// 计算统计数据
		int totalQuantity = 0;
		int totalShipped = 0;
		int totalReceived = 0;
		for (PurchaseOrderItemVO item : items) {
			totalQuantity += item.getQuantity() != null ? item.getQuantity() : 0;
			totalShipped += item.getShippedQuantity() != null ? item.getShippedQuantity() : 0;
			totalReceived += item.getReceivedQuantity() != null ? item.getReceivedQuantity() : 0;
		}
		vo.setTotalQuantity(totalQuantity);
		vo.setTotalShippedQuantity(totalShipped);
		vo.setTotalReceivedQuantity(totalReceived);

		// 查询付款凭证信息
		FileInfoVO prepayVoucher = purchaseOrderFileMapper.selectFileInfoByOrderIdAndType(
				id, PurchaseOrderFileType.PREPAY_VOUCHER.name());
		vo.setPrepayVoucherFile(prepayVoucher);

		FileInfoVO balanceVoucher = purchaseOrderFileMapper.selectFileInfoByOrderIdAndType(
				id, PurchaseOrderFileType.BALANCE_VOUCHER.name());
		vo.setBalanceVoucherFile(balanceVoucher);

		// 查询合同文件信息
		FileInfoVO contractFile = purchaseOrderFileMapper.selectFileInfoByOrderIdAndType(
				id, PurchaseOrderFileType.CONTRACT.name());
		vo.setContractFile(contractFile);

		// 查询质检数据
		List<PurchaseOrderQcItemVO> qcItems = purchaseOrderQcItemService.getQcItemList(id);
		vo.setQcItems(qcItems);

		// 查询其他附件
		List<FileInfoVO> otherFiles = purchaseOrderFileMapper.selectFileInfoListByOrderIdAndType(
				id, PurchaseOrderFileType.OTHER.name());
		vo.setOtherFiles(otherFiles);

		// 构建 SKU 展示信息映射表, 这里只要传入采购明细列表，质检数据的 skuCode 都包含在采购明细列表中
		Set<String> skuCodes = items.stream().map(PurchaseOrderItemVO::getSkuCode).filter(Objects::nonNull)
				.collect(Collectors.toSet());
		Map<String, SkuBriefVO> skuBriefMap =  skuBriefService.buildMapForQuery(skuCodes);
		vo.setSkuBriefMap(skuBriefMap);

		return vo;
	}

	/**
	 * 保存采购单（供 Facade 调用）
	 * <p>
	 * 支持同时提交基本信息、明细、付款信息、合同、质检数据、其他附件
	 *
	 * @param dto 采购单DTO
	 * @return 采购单ID
	 */
	@Transactional(rollbackFor = Exception.class)
	public Long saveOrder(PurchaseOrderDTO dto) {
		// 校验采购单号唯一性
		Assert.isTrue(!baseMapper.existsByOrderNo(dto.getOrderNo(), null), "采购单号已存在");

		// 校验付款信息
		purchaseOrderValidator.validatePaymentInfo(dto.getPaymentInfo());

		// 校验质检数据
		purchaseOrderValidator.validateQcData(dto.getQcData());

		// 校验合同信息
		purchaseOrderValidator.validateContractInfo(dto.getContractInfo());

		// 转换实体
		PurchaseOrder order = PurchaseOrderConverter.INSTANCE.dtoToEntity(dto);

		// 设置默认值
		if (order.getCurrencyCode() == null) {
			order.setCurrencyCode("CNY");
		}
		if (order.getTaxIncluded() == null) {
			order.setTaxIncluded(1);
		}
		if (order.getPrepayRatio() == null) {
			order.setPrepayRatio(new BigDecimal("30"));
		}

		// 初始化状态
		order.setOrderStatus(PurchaseOrderStatus.DRAFT.name());
		order.setShippingStatus(PurchaseShippingStatus.NOT_SHIPPED.name());
		order.setReceivingStatus(PurchaseReceivingStatus.NOT_RECEIVED.name());
		order.setPrepayStatus(0);
		order.setBalanceStatus(0);
		order.setTotalAmount(BigDecimal.ZERO);
		order.setPrepayAmount(BigDecimal.ZERO);

		// 保存采购单
		this.save(order);

		// 保存明细
		List<PurchaseOrderItem> items = purchaseOrderItemService.batchSave(order.getId(), dto.getItems());

		// 计算并更新金额
		BigDecimal totalAmount = purchaseOrderItemService.calculateTotalAmount(items);
		BigDecimal prepayAmount = calculatePrepayAmount(totalAmount, order.getPrepayRatio());

		order.setTotalAmount(totalAmount);
		order.setPrepayAmount(prepayAmount);

		// 处理付款信息
		paymentHandler.handlePaymentChange(order, dto.getPaymentInfo());

		// 处理合同附件
		contractHandler.handleContractChange(order, dto.getContractInfo());

		// 处理质检数据
		qcDataHandler.handleQcDataChange(order, dto.getQcData());

		// 处理其他附件
		otherFileHandler.handleOtherFilesChange(order, dto.getOtherFileIds());

		// 更新采购单（包含金额和可能被Handler修改的字段）
		this.updateById(order);

		log.info("Created purchase order, id={}, orderNo={}", order.getId(), order.getOrderNo());
		return order.getId();
	}

	/**
	 * 更新采购单（供 Facade 调用）
	 * <p>
	 * 根据状态控制可编辑字段：
	 * - 草稿状态：所有字段可编辑
	 * - 已确认及之后状态：仅付款信息、合同附件、质检数据、其他附件、备注可编辑
	 * - 已完成/已取消状态：不允许编辑
	 *
	 * @param dto 采购单DTO
	 */
	@Transactional(rollbackFor = Exception.class)
	public void updateOrder(PurchaseOrderDTO dto) {
		PurchaseOrder order = this.getById(dto.getId());
		Assert.notNull(order, "采购单不存在");

		PurchaseOrderStatus status = PurchaseOrderStatus.valueOf(order.getOrderStatus());

		// 校验是否可以进入编辑页
		Assert.isTrue(purchaseOrderValidator.canEnterEditPage(status), "当前状态不允许编辑");

		// 校验付款信息
		purchaseOrderValidator.validatePaymentInfo(dto.getPaymentInfo());

		if (status != PurchaseOrderStatus.COMPLETED) {
			purchaseOrderValidator.validateQcData(dto.getQcData());
			purchaseOrderValidator.validateContractInfo(dto.getContractInfo());
		}

		if (status.canEditAll()) {
			// 草稿状态：可编辑全部字段（除采购单号外）
			PurchaseOrderConverter.INSTANCE.updateEntityFromDto(dto, order);

			// 更新明细
			if (dto.getItems() != null && !dto.getItems().isEmpty()) {
				List<PurchaseOrderItem> items = purchaseOrderItemService.batchUpdate(order.getId(), dto.getItems());

				// 重新计算金额
				BigDecimal totalAmount = purchaseOrderItemService.calculateTotalAmount(items);
				BigDecimal prepayAmount = calculatePrepayAmount(totalAmount, order.getPrepayRatio());
				order.setTotalAmount(totalAmount);
				order.setPrepayAmount(prepayAmount);
			}
		} else {
			// 非草稿状态：校验是否修改了不可编辑的字段
			purchaseOrderValidator.validateFieldEditable(order, dto);

			if (status != PurchaseOrderStatus.COMPLETED) {
				order.setRemark(dto.getRemark());
			}
		}

		// 处理付款信息（所有可编辑状态都允许）
		paymentHandler.handlePaymentChange(order, dto.getPaymentInfo());

		if (status != PurchaseOrderStatus.COMPLETED) {
			contractHandler.handleContractChange(order, dto.getContractInfo());
			qcDataHandler.handleQcDataChange(order, dto.getQcData());
			otherFileHandler.handleOtherFilesChange(order, dto.getOtherFileIds());
		}

		this.updateById(order);
		log.info("Updated purchase order, id={}, status={}", order.getId(), status);
	}

	/**
	 * 删除采购单（供 Facade 调用）
	 * @param id 采购单ID
	 */
	@Transactional(rollbackFor = Exception.class)
	public void deleteOrder(Long id) {
		PurchaseOrder order = this.getById(id);
		Assert.notNull(order, "采购单不存在");

		PurchaseOrderStatus status = PurchaseOrderStatus.valueOf(order.getOrderStatus());
		Assert.isTrue(status.canDelete(), "仅草稿状态的采购单可以删除");
		Assert.isTrue(!Integer.valueOf(1).equals(order.getPrepayStatus())
				&& !Integer.valueOf(1).equals(order.getBalanceStatus()),
				"已付款的采购单不允许删除，请先走退款或冲销流程");

		// 删除明细
		purchaseOrderItemService.deleteByPurchaseOrderId(id);

		// 逻辑删除采购单
		this.removeById(id);
		log.info("Deleted purchase order, id={}, orderNo={}", id, order.getOrderNo());
	}

	/**
	 * 计算首付款金额
	 * @param totalAmount 合同总金额
	 * @param prepayRatio 首付款比例
	 * @return 首付款金额
	 */
	private BigDecimal calculatePrepayAmount(BigDecimal totalAmount, BigDecimal prepayRatio) {
		if (totalAmount == null || prepayRatio == null) {
			return BigDecimal.ZERO;
		}
		if (prepayRatio.compareTo(BigDecimal.ZERO) <= 0) {
			return BigDecimal.ZERO;
		}
		if (prepayRatio.compareTo(new BigDecimal("100")) > 0) {
			throw new IllegalArgumentException("首付款比例不能大于100");
		}
		return totalAmount.multiply(prepayRatio).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
	}

	/**
	 * 确认采购单（供 Facade 调用）
	 * @param id 采购单ID
	 */
	@Transactional(rollbackFor = Exception.class)
	public void updateToConfirmed(Long id) {
		PurchaseOrder order = this.getById(id);
		Assert.notNull(order, "采购单不存在");

		PurchaseOrderStatus currentStatus = PurchaseOrderStatus.valueOf(order.getOrderStatus());
		PurchaseOrderStatus targetStatus = PurchaseOrderStatus.CONFIRMED;

		Assert.isTrue(currentStatus.canTransitionTo(targetStatus),
				String.format("不允许从[%s]变更为[%s]", currentStatus.getDescription(), targetStatus.getDescription()));

		order.setOrderStatus(targetStatus.name());
		boolean updated = this.updateById(order);
		if (!updated) {
			throw new OptimisticLockException("数据已被其他用户修改，请刷新后重试");
		}
		log.info("Confirmed purchase order, id={}, orderNo={}", id, order.getOrderNo());
	}

	/**
	 * 开始生产（供 Facade 调用）
	 * @param id 采购单ID
	 */
	@Transactional(rollbackFor = Exception.class)
	public void updateToInProduction(Long id) {
		PurchaseOrder order = this.getById(id);
		Assert.notNull(order, "采购单不存在");

		PurchaseOrderStatus currentStatus = PurchaseOrderStatus.valueOf(order.getOrderStatus());
		PurchaseOrderStatus targetStatus = PurchaseOrderStatus.IN_PRODUCTION;

		Assert.isTrue(currentStatus.canTransitionTo(targetStatus),
				String.format("不允许从[%s]变更为[%s]", currentStatus.getDescription(), targetStatus.getDescription()));

		order.setOrderStatus(targetStatus.name());
		boolean updated = this.updateById(order);
		if (!updated) {
			throw new OptimisticLockException("数据已被其他用户修改，请刷新后重试");
		}
		log.info("Started production for purchase order, id={}, orderNo={}", id, order.getOrderNo());
	}

	/**
	 * 取消采购单（供 Facade 调用）
	 * @param id 采购单ID
	 */
	@Transactional(rollbackFor = Exception.class)
	public void updateToCancelled(Long id) {
		PurchaseOrder order = this.getById(id);
		Assert.notNull(order, "采购单不存在");

		PurchaseOrderStatus currentStatus = PurchaseOrderStatus.valueOf(order.getOrderStatus());
		Assert.isTrue(currentStatus.canCancel(),
				String.format("当前状态[%s]不允许取消", currentStatus.getDescription()));
		Assert.isTrue(!Integer.valueOf(1).equals(order.getPrepayStatus())
				&& !Integer.valueOf(1).equals(order.getBalanceStatus()),
				"已付款的采购单不允许取消，请先走退款或冲销流程");

		Assert.isTrue(!baseMapper.existsAssociatedShippingOrder(id), "存在关联物流单，无法取消");

		order.setOrderStatus(PurchaseOrderStatus.CANCELLED.name());
		boolean updated = this.updateById(order);
		if (!updated) {
			throw new OptimisticLockException("数据已被其他用户修改，请刷新后重试");
		}
		log.info("Cancelled purchase order, id={}, orderNo={}", id, order.getOrderNo());
	}

	/**
	 * 获取状态描述
	 * @param status 状态码
	 * @return 状态描述
	 */
	private String getStatusDesc(String status) {
		if (status == null) {
			return null;
		}
		try {
			return PurchaseOrderStatus.valueOf(status).getDescription();
		} catch (IllegalArgumentException e) {
			return status;
		}
	}

	/**
	 * 获取发货状态描述
	 */
	private String getShippingStatusDesc(String status) {
		if (status == null) {
			return null;
		}
		try {
			return PurchaseShippingStatus.valueOf(status).getDescription();
		} catch (IllegalArgumentException e) {
			return status;
		}
	}

	/**
	 * 获取入库状态描述
	 */
	private String getReceivingStatusDesc(String status) {
		if (status == null) {
			return null;
		}
		try {
			return PurchaseReceivingStatus.valueOf(status).getDescription();
		} catch (IllegalArgumentException e) {
			return status;
		}
	}

	/**
	 * 更新采购单发货状态（由物流单确认发货时调用）
	 * @param purchaseOrderId 采购单ID
	 */
	@Transactional(rollbackFor = Exception.class)
	public void updateShippingStatus(Long purchaseOrderId) {
		PurchaseOrder order = this.getById(purchaseOrderId);
		Assert.notNull(order, "采购单不存在");

		// 计算总发货数量
		PurchaseOrderStatVO stat = purchaseOrderItemService.getStatByOrderId(purchaseOrderId);
		int totalQuantity = stat.getTotalQuantity();
		int shippedQuantity = stat.getTotalShippedQuantity();

		// 计算新状态
		PurchaseShippingStatus newStatus = PurchaseShippingStatus.calculate(shippedQuantity, totalQuantity);
		order.setShippingStatus(newStatus.name());
		boolean updated = this.updateById(order);
		if (!updated) {
			throw new OptimisticLockException("数据已被其他用户修改，请刷新后重试");
		}

		log.info("Updated shipping status for purchase order, id={}, status={}", purchaseOrderId, newStatus);
	}

	/**
	 * 更新采购单入库状态（由采购入库单确认时调用）
	 * @param purchaseOrderId 采购单ID
	 */
	@Transactional(rollbackFor = Exception.class)
	public void updateReceivingStatus(Long purchaseOrderId) {
		PurchaseOrder order = this.getById(purchaseOrderId);
		Assert.notNull(order, "采购单不存在");

		// 计算总入库数量
		PurchaseOrderStatVO stat = purchaseOrderItemService.getStatByOrderId(purchaseOrderId);
		int totalQuantity = stat.getTotalQuantity();
		int receivedQuantity = stat.getTotalReceivedQuantity();

		// 计算新状态
		PurchaseReceivingStatus newStatus = PurchaseReceivingStatus.calculate(receivedQuantity, totalQuantity);
		order.setReceivingStatus(newStatus.name());

		// 如果全部入库，自动将业务状态更新为已完成
		if (newStatus == PurchaseReceivingStatus.ALL_RECEIVED) {
			order.setOrderStatus(PurchaseOrderStatus.COMPLETED.name());
			log.info("Auto completed purchase order, id={}", purchaseOrderId);
		}

		boolean updated = this.updateById(order);
		if (!updated) {
			throw new OptimisticLockException("数据已被其他用户修改，请刷新后重试");
		}
		log.info("Updated receiving status for purchase order, id={}, status={}", purchaseOrderId, newStatus);
	}

	/**
	 * 导出查询
	 * @param qo 查询条件
	 * @return 导出数据列表
	 */
	public List<PurchaseOrderExportVO> listForExport(PurchaseOrderQO qo) {
		List<PurchaseOrderExportVO> records = baseMapper.selectListForExport(qo);

		if (!records.isEmpty()) {
			// 提取采购单ID列表
			List<Long> orderIds = records.stream()
					.map(PurchaseOrderExportVO::getId)
					.collect(Collectors.toList());

			// 批量查询统计数据
			Map<Long, PurchaseOrderStatVO> statMap = purchaseOrderItemService.getStatByOrderIds(orderIds);

			// 填充统计数据
			for (PurchaseOrderExportVO vo : records) {
				PurchaseOrderStatVO stat = statMap.get(vo.getId());
				if (stat != null) {
					vo.setSkuCount(stat.getSkuCount());
					vo.setTotalQuantity(stat.getTotalQuantity());
					vo.setTotalShippedQuantity(stat.getTotalShippedQuantity());
					vo.setTotalReceivedQuantity(stat.getTotalReceivedQuantity());
				} else {
					vo.setSkuCount(0);
					vo.setTotalQuantity(0);
					vo.setTotalShippedQuantity(0);
					vo.setTotalReceivedQuantity(0);
				}
			}
		}

		return records;
	}

}
