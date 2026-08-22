package com.erp.admin.wms.service;

import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.admin.platform.finance.service.WarehouseBillingService;
import com.erp.admin.wms.mapper.WmsFulfillmentItemMapper;
import com.erp.admin.wms.mapper.WmsFulfillmentOrderMapper;
import com.erp.admin.wms.model.dto.FulfillmentPackDTO;
import com.erp.admin.wms.model.entity.WmsFulfillmentItem;
import com.erp.admin.wms.model.entity.WmsFulfillmentOrder;
import com.erp.admin.wms.model.enums.FulfillmentStatus;
import com.erp.admin.wms.model.vo.FulfillmentBatchResultVO;
import com.erp.admin.wms.service.platform.PlatformLabelResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

@Service
@RequiredArgsConstructor
public class FulfillmentShippingService {
	private final WmsFulfillmentOrderMapper orderMapper;
	private final WmsFulfillmentItemMapper itemMapper;
	private final FulfillmentProgressService progressService;
	private final PlatformLabelVerificationService labelVerificationService;
	private final FulfillmentPlatformActionService platformActions;
	private final LocationInventoryService inventoryService;
	private final WarehouseBillingService billingService;
	private final TransactionTemplate transactionTemplate;

	public List<WmsFulfillmentOrder> listWorkOrders() {
		return orderMapper.selectList(Wrappers.<WmsFulfillmentOrder>lambdaQuery()
				.in(WmsFulfillmentOrder::getFulfillmentStatus, FulfillmentStatus.WAITING_PACK,
						FulfillmentStatus.PACKED)
				.orderByAsc(WmsFulfillmentOrder::getCreateTime));
	}

	public PlatformLabelResult printLabel(Long fulfillmentId) {
		WmsFulfillmentOrder order = requireStatus(fulfillmentId, FulfillmentStatus.WAITING_PACK,
				FulfillmentStatus.PACKED);
		PlatformLabelResult result = platformActions.fetchLabel(fulfillmentId);
		Assert.isTrue(result != null && result.isSuccess(), "面单获取失败");
		order.setLabelFileUrl(result.getLabelUrl());
		order.setLabelBarcode(result.getLabelBarcode());
		order.setLabelFetchedTime(LocalDateTime.now());
		orderMapper.updateById(order);
		return result;
	}

	public void verifyLabel(Long fulfillmentId, String barcode) {
		WmsFulfillmentOrder order = requireStatus(fulfillmentId, FulfillmentStatus.WAITING_PACK,
				FulfillmentStatus.PACKED);
		Assert.hasText(order.getLabelBarcode(), "请先获取并打印面单");
		Assert.isTrue(labelVerificationService.matches(order, barcode), "面单条码与当前订单不匹配");
		platformActions.markReady(fulfillmentId);
		order.setLabelVerifiedTime(LocalDateTime.now());
		orderMapper.updateById(order);
	}

	public void pack(Long fulfillmentId, FulfillmentPackDTO dto) {
		WmsFulfillmentOrder order = requireStatus(fulfillmentId, FulfillmentStatus.WAITING_PACK);
		Assert.notNull(order.getLabelVerifiedTime(), "必须先扫描核验当前订单面单");
		order.setCarrierCode(dto.getCarrierCode());
		order.setCarrierName(dto.getCarrierName());
		order.setTrackingNo(dto.getTrackingNo());
		order.setPackageWeightKg(dto.getPackageWeightKg());
		order.setPackedTime(LocalDateTime.now());
		orderMapper.updateById(order);
		Assert.isTrue(orderMapper.transit(fulfillmentId, FulfillmentStatus.WAITING_PACK,
				FulfillmentStatus.PACKED) == 1, "订单状态已变化，请刷新后重试");
		progressService.sync(order, FulfillmentStatus.PACKED);
	}

	public FulfillmentBatchResultVO ship(List<Long> fulfillmentIds) {
		Assert.notEmpty(fulfillmentIds, "请选择待签出订单");
		FulfillmentBatchResultVO result = new FulfillmentBatchResultVO();
		for (Long id : fulfillmentIds) {
			try {
				shipOne(id);
				result.addSuccess(id);
			}
			catch (RuntimeException ex) {
				result.addFailure(id, ex.getMessage() == null ? "签出失败" : ex.getMessage());
			}
		}
		return result;
	}

	private void shipOne(Long fulfillmentId) {
		WmsFulfillmentOrder current = orderMapper.selectById(fulfillmentId);
		Assert.notNull(current, "履约订单不存在");
		if (current.getFulfillmentStatus() == FulfillmentStatus.SHIPPED) return;
		Assert.isTrue(current.getFulfillmentStatus() == FulfillmentStatus.PACKED, "订单尚未完成打包");
		platformActions.finalizeShipment(fulfillmentId);
		transactionTemplate.executeWithoutResult(status -> completeLocalShipment(fulfillmentId));
	}

	private void completeLocalShipment(Long fulfillmentId) {
		WmsFulfillmentOrder order = orderMapper.selectForUpdate(fulfillmentId);
		Assert.notNull(order, "履约订单不存在");
		if (order.getFulfillmentStatus() == FulfillmentStatus.SHIPPED) return;
		Assert.isTrue(order.getFulfillmentStatus() == FulfillmentStatus.PACKED, "订单状态已变化");
		inventoryService.ship(fulfillmentId);
		int quantity = itemMapper.selectList(Wrappers.<WmsFulfillmentItem>lambdaQuery()
				.eq(WmsFulfillmentItem::getFulfillmentOrderId, fulfillmentId))
				.stream().mapToInt(item -> item.getQuantity() == null ? 0 : item.getQuantity()).sum();
		billingService.recordFulfillmentOutbound(order, quantity);
		order.setShippedTime(LocalDateTime.now());
		orderMapper.updateById(order);
		Assert.isTrue(orderMapper.transit(fulfillmentId, FulfillmentStatus.PACKED,
				FulfillmentStatus.SHIPPED) == 1, "签出状态更新失败");
		progressService.sync(order, FulfillmentStatus.SHIPPED);
	}

	private WmsFulfillmentOrder requireStatus(Long id, FulfillmentStatus... statuses) {
		WmsFulfillmentOrder order = orderMapper.selectById(id);
		Assert.notNull(order, "履约订单不存在");
		for (FulfillmentStatus status : statuses) {
			if (order.getFulfillmentStatus() == status) return order;
		}
		throw new IllegalStateException("当前订单状态不允许此操作");
	}
}
