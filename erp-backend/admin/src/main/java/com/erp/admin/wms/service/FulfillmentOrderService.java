package com.erp.admin.wms.service;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.erp.admin.wms.mapper.WmsFulfillmentItemMapper;
import com.erp.admin.wms.mapper.WmsFulfillmentOrderMapper;
import com.erp.admin.wms.model.dto.FulfillmentCreateCommand;
import com.erp.admin.wms.model.entity.WmsFulfillmentItem;
import com.erp.admin.wms.model.entity.WmsFulfillmentOrder;
import com.erp.admin.wms.model.enums.FulfillmentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Service
@RequiredArgsConstructor
public class FulfillmentOrderService {

	private static final Set<String> SOURCES = new HashSet<>();

	static {
		SOURCES.add("OZON");
		SOURCES.add("WB");
		SOURCES.add("YANDEX");
		SOURCES.add("MANUAL");
	}

	private final WmsFulfillmentOrderMapper orderMapper;
	private final WmsFulfillmentItemMapper itemMapper;
	private final FulfillmentReservationService reservationService;

	@Transactional(rollbackFor = Exception.class)
	public Long createAndReserve(FulfillmentCreateCommand command) {
		validate(command);
		String sourceType = command.getSourceType().trim().toUpperCase(Locale.ROOT);
		WmsFulfillmentOrder existing = orderMapper.selectBySource(sourceType, command.getSourceOrderId());
		if (existing != null) {
			Assert.isTrue(existing.getFulfillmentStatus() != FulfillmentStatus.CANCELLED
					&& existing.getFulfillmentStatus() != FulfillmentStatus.CANCEL_RETURNING,
					"该订单的仓库履约已取消，不允许再次提交");
			return existing.getId();
		}

		WmsFulfillmentOrder order = buildOrder(command, sourceType);
		Assert.isTrue(orderMapper.insert(order) == 1, "履约单创建失败");
		List<WmsFulfillmentItem> persistedItems = new ArrayList<>();
		for (FulfillmentCreateCommand.Item source : command.getItems()) {
			WmsFulfillmentItem item = buildItem(order.getId(), source);
			Assert.isTrue(itemMapper.insert(item) == 1, "履约商品快照保存失败");
			persistedItems.add(item);
		}
		reservationService.reserve(order.getId(), command, persistedItems);
		Assert.isTrue(orderMapper.transit(order.getId(), FulfillmentStatus.DRAFT,
				FulfillmentStatus.WAITING_SHELF) == 1, "履约单状态已变化，请重试");
		return order.getId();
	}

	public FulfillmentStatus statusOf(Long fulfillmentId) {
		WmsFulfillmentOrder order = orderMapper.selectById(fulfillmentId);
		Assert.notNull(order, "履约单不存在");
		return order.getFulfillmentStatus();
	}

	@Transactional(rollbackFor = Exception.class)
	public void cancelAndRelease(Long fulfillmentId, String reason) {
		Assert.notNull(fulfillmentId, "履约单不能为空");
		WmsFulfillmentOrder order = orderMapper.selectForUpdate(fulfillmentId);
		Assert.notNull(order, "履约单不存在");
		Assert.isTrue(order.getFulfillmentStatus() == FulfillmentStatus.WAITING_SHELF,
				"只有待下架订单可以由货主取消");
		reservationService.release(fulfillmentId);
		Assert.isTrue(orderMapper.transit(fulfillmentId, FulfillmentStatus.WAITING_SHELF,
				FulfillmentStatus.CANCELLED) == 1, "履约单状态已变化，请刷新后重试");
		if (reason != null && !reason.trim().isEmpty()) {
			WmsFulfillmentOrder update = new WmsFulfillmentOrder();
			update.setId(fulfillmentId);
			update.setCancelReason(reason.trim());
			orderMapper.updateById(update);
		}
	}

	private WmsFulfillmentOrder buildOrder(FulfillmentCreateCommand command, String sourceType) {
		WmsFulfillmentOrder order = new WmsFulfillmentOrder();
		order.setTenantId(command.getTenantId());
		order.setWmsTenantId(command.getWmsTenantId());
		order.setErpTenantId(command.getErpTenantId());
		order.setWarehouseId(command.getWarehouseId());
		order.setShopId(command.getShopId());
		order.setLogisticsProductId(command.getLogisticsProductId());
		order.setLogisticsProductCode(command.getLogisticsProductCode());
		order.setLogisticsProductName(command.getLogisticsProductName());
		order.setLogisticsProductDescription(command.getLogisticsProductDescription());
		order.setLogisticsProductDefaultFee(command.getLogisticsProductDefaultFee());
		order.setLogisticsProductActualFee(command.getLogisticsProductActualFee());
		order.setLogisticsProductCurrency(command.getLogisticsProductCurrency());
		order.setFulfillmentNo("FO-" + sourceType + "-" + command.getSourceOrderId());
		order.setSourceType(sourceType);
		order.setSourceOrderId(command.getSourceOrderId());
		order.setSourceOrderNo(command.getSourceOrderNo().trim());
		order.setPlatformStatus(command.getPlatformStatus());
		order.setFulfillmentStatus(FulfillmentStatus.DRAFT);
		order.setRecipientName(command.getRecipientName());
		order.setRecipientPhone(command.getRecipientPhone());
		order.setRecipientAddress(command.getRecipientAddress());
		order.setVersion(0);
		order.setDeleted(0L);
		return order;
	}

	private WmsFulfillmentItem buildItem(Long fulfillmentId, FulfillmentCreateCommand.Item source) {
		WmsFulfillmentItem item = new WmsFulfillmentItem();
		item.setFulfillmentOrderId(fulfillmentId);
		item.setSkuCode(source.getSkuCode().trim());
		item.setWarehouseSkuCode(source.getWarehouseSkuCode().trim());
		item.setSkuName(source.getSkuName());
		item.setImageUrl(source.getImageUrl());
		item.setQuality(source.getQuality());
		item.setQuantity(source.getQuantity());
		item.setPickedQuantity(0);
		item.setOuterLengthMm(source.getOuterLengthMm());
		item.setOuterWidthMm(source.getOuterWidthMm());
		item.setOuterHeightMm(source.getOuterHeightMm());
		item.setOuterGrossWeightG(source.getOuterGrossWeightG());
		item.setDeleted(0L);
		return item;
	}

	static void validate(FulfillmentCreateCommand command) {
		Assert.notNull(command, "履约命令不能为空");
		Assert.notNull(command.getTenantId(), "平台租户不能为空");
		Assert.notNull(command.getWmsTenantId(), "WMS服务商不能为空");
		Assert.notNull(command.getErpTenantId(), "货主不能为空");
		Assert.notNull(command.getWarehouseId(), "目标仓库不能为空");
		Assert.hasText(command.getSourceType(), "订单来源不能为空");
		Assert.isTrue(SOURCES.contains(command.getSourceType().trim().toUpperCase(Locale.ROOT)), "订单来源不正确");
		Assert.notNull(command.getSourceOrderId(), "来源订单不能为空");
		Assert.hasText(command.getSourceOrderNo(), "来源订单号不能为空");
		Assert.notEmpty(command.getItems(), "履约商品不能为空");
		Set<String> dimensions = new HashSet<>();
		for (FulfillmentCreateCommand.Item item : command.getItems()) {
			Assert.hasText(item.getSkuCode(), "SKU不能为空");
			Assert.hasText(item.getWarehouseSkuCode(), item.getSkuCode() + " 缺少内部SKU码");
			Assert.hasText(item.getQuality(), item.getSkuCode() + " 品质不能为空");
			positive(item.getQuantity(), item.getSkuCode() + " 数量必须大于0");
			positive(item.getOuterLengthMm(), item.getSkuCode() + " 缺少外箱长度");
			positive(item.getOuterWidthMm(), item.getSkuCode() + " 缺少外箱宽度");
			positive(item.getOuterHeightMm(), item.getSkuCode() + " 缺少外箱高度");
			positive(item.getOuterGrossWeightG(), item.getSkuCode() + " 缺少单箱毛重");
			Assert.isTrue(dimensions.add(item.getSkuCode().trim() + "|" + item.getQuality()),
					"履约商品存在重复SKU，请先汇总数量");
		}
	}

	private static void positive(Integer value, String message) {
		Assert.isTrue(value != null && value > 0, message);
	}
}
