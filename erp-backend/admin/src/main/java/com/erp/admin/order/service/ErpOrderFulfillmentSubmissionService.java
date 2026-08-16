package com.erp.admin.order.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.erp.admin.common.tenant.TenantContext;
import com.erp.admin.common.tenant.WmsTenantContext;
import com.erp.admin.order.mapper.ErpOrderItemMapper;
import com.erp.admin.order.mapper.ErpOrderMapper;
import com.erp.admin.order.model.dto.SubmitFulfillmentDTO;
import com.erp.admin.order.model.entity.ErpOrder;
import com.erp.admin.order.model.entity.ErpOrderItem;
import com.erp.admin.product.mapper.SkuMapper;
import com.erp.admin.product.model.entity.Sku;
import com.erp.admin.product.service.WarehouseSkuCodeService;
import com.erp.admin.shop.mapper.ShopMapper;
import com.erp.admin.shop.model.entity.Shop;
import com.erp.admin.wms.model.dto.FulfillmentCreateCommand;
import com.erp.admin.wms.model.enums.FulfillmentStatus;
import com.erp.admin.wms.service.FulfillmentOrderService;
import com.erp.admin.wms.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Service
@RequiredArgsConstructor
public class ErpOrderFulfillmentSubmissionService {

	private final ErpOrderMapper orderMapper;
	private final ErpOrderItemMapper orderItemMapper;
	private final ShopMapper shopMapper;
	private final SkuMapper skuMapper;
	private final WarehouseSkuCodeService warehouseSkuCodeService;
	private final WarehouseService warehouseService;
	private final FulfillmentOrderService fulfillmentOrderService;

	@Transactional(rollbackFor = Exception.class)
	public Long submit(SubmitFulfillmentDTO dto) {
		Assert.notNull(dto, "提交参数不能为空");
		ErpOrder order = orderMapper.selectById(dto.getErpOrderId());
		Assert.notNull(order, "订单不存在");
		Assert.isTrue("FBS".equalsIgnoreCase(order.getFulfillmentType()), "只有FBS订单可以提交海外仓履约");
		Shop shop = shopMapper.selectById(order.getShopId());
		Assert.notNull(shop, "订单店铺不存在");
		Long warehouseId = dto.getWmsWarehouseId() != null ? dto.getWmsWarehouseId() : shop.getDefaultWmsWarehouseId();
		Assert.notNull(warehouseId, "请先为店铺设置默认WMS仓库，或在确认时选择仓库");
		warehouseService.validateOperableOwnWarehouse(warehouseId);

		Long erpTenantId = TenantContext.getCurrentTenant();
		Long wmsTenantId = WmsTenantContext.getCurrentWmsTenant();
		Assert.isTrue(erpTenantId != null && erpTenantId > 0, "当前货主身份无效");
		Assert.notNull(wmsTenantId, "当前货主尚未绑定WMS服务商");

		FulfillmentCreateCommand command = buildCommand(order, erpTenantId, wmsTenantId, warehouseId);
		Long fulfillmentId = fulfillmentOrderService.createAndReserve(command);
		ErpOrder update = new ErpOrder();
		update.setId(order.getId());
		update.setFulfillmentOrderId(fulfillmentId);
		update.setWmsWarehouseId(warehouseId);
		update.setWarehouseFulfillmentStatus(FulfillmentStatus.WAITING_SHELF.name());
		Assert.isTrue(orderMapper.updateById(update) == 1, "订单仓库状态更新失败");
		return fulfillmentId;
	}

	@Transactional(rollbackFor = Exception.class)
	public void cancel(Long erpOrderId, String reason) {
		ErpOrder order = orderMapper.selectById(erpOrderId);
		Assert.notNull(order, "订单不存在");
		Assert.notNull(order.getFulfillmentOrderId(), "订单尚未提交仓库履约");
		fulfillmentOrderService.cancelAndRelease(order.getFulfillmentOrderId(), reason);
		ErpOrder update = new ErpOrder();
		update.setId(order.getId());
		update.setWarehouseFulfillmentStatus(FulfillmentStatus.CANCELLED.name());
		orderMapper.updateById(update);
	}

	private FulfillmentCreateCommand buildCommand(ErpOrder order, Long erpTenantId, Long wmsTenantId,
			Long warehouseId) {
		List<ErpOrderItem> orderItems = orderItemMapper.selectByOrderId(order.getId());
		Assert.notEmpty(orderItems, "订单没有商品明细");
		List<FulfillmentCreateCommand.Item> items = new ArrayList<>();
		for (ErpOrderItem orderItem : orderItems) {
			Sku sku = skuMapper.selectBySkuCode(orderItem.getSkuCode());
			Assert.notNull(sku, "订单SKU未在货主SKU主数据中登记：" + orderItem.getSkuCode());
			FulfillmentCreateCommand.Item item = new FulfillmentCreateCommand.Item();
			item.setSkuCode(orderItem.getSkuCode());
			item.setWarehouseSkuCode(warehouseSkuCodeService.build(erpTenantId, orderItem.getSkuCode()));
			item.setSkuName(sku.getChineseName());
			item.setQuality("GOOD");
			item.setQuantity(orderItem.getQuantity());
			item.setOuterLengthMm(sku.getOuterLengthMm());
			item.setOuterWidthMm(sku.getOuterWidthMm());
			item.setOuterHeightMm(sku.getOuterHeightMm());
			item.setOuterGrossWeightG(sku.getOuterGrossWeightG());
			items.add(item);
		}
		FulfillmentCreateCommand command = new FulfillmentCreateCommand();
		command.setTenantId(TenantContext.BLOCK_TENANT_ID);
		command.setWmsTenantId(wmsTenantId);
		command.setErpTenantId(erpTenantId);
		command.setWarehouseId(warehouseId);
		command.setShopId(order.getShopId());
		command.setSourceType(sourceType(order.getPlatform()));
		command.setSourceOrderId(order.getId());
		command.setSourceOrderNo(order.getPlatformOrderId());
		command.setPlatformStatus(order.getPlatformStatus());
		command.setItems(items);
		return command;
	}

	private String sourceType(String platform) {
		String value = platform == null ? "" : platform.trim().toLowerCase(Locale.ROOT);
		if ("ozon".equals(value)) return "OZON";
		if ("wildberries".equals(value) || "wb".equals(value)) return "WB";
		if ("yandex".equals(value) || "yd".equals(value)) return "YANDEX";
		throw new IllegalArgumentException("暂不支持的平台：" + platform);
	}
}
